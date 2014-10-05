/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bonsai.wallet32;

// HACKED version of DownloadListener which reports progress more
// frequently.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Peer;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * <p>An implementation of {@link AbstractPeerEventListener} that listens to chain download events and tracks progress
 * as a percentage. The default implementation prints progress to stdout, but you can subclass it and override the
 * progress method to update a GUI instead.</p>
 */
public class MyDownloadListener extends AbstractPeerEventListener {

    private class ProgressRecord {
        public long		mTimestamp;
        public int		mBlocksLeft;
        public ProgressRecord(long timestamp, int blocksLeft) {
            this.mTimestamp = timestamp;
            this.mBlocksLeft = blocksLeft;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MyDownloadListener.class);
    private int originalBlocksLeft = -1;
    private int lastPercent = 0;
    private long lastUpdateTime = 0;
    private Semaphore done = new Semaphore(0);
    private boolean caughtUp = false;
    private LinkedList<ProgressRecord> progress = new LinkedList<ProgressRecord>();

    private static final long PROGRESS_GRANULARITY = 10 * 1000;

    @Override
    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
        startDownload(blocksLeft);
        originalBlocksLeft = blocksLeft;
        if (blocksLeft == 0) {
            doneDownload();
            done.release();
        }
    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
        if (caughtUp)
            return;

        if (blocksLeft == 0) {
            caughtUp = true;
            doneDownload();
            done.release();
        }

        if (blocksLeft < 0 || originalBlocksLeft <= 0)
            return;

        long now = System.currentTimeMillis();

        long msecsLeft = estimateComplete(now, blocksLeft);

        double pct = 100.0 - (100.0 * (blocksLeft / (double) originalBlocksLeft));
        long delta = now - lastUpdateTime;
        if (delta >= 500) {
            progress(pct, blocksLeft, new Date(block.getTimeSeconds() * 1000), msecsLeft);
            lastPercent = (int) pct;
            lastUpdateTime = now;
        }
    }

    private long estimateComplete(long now, int blocksLeft) {
        // Only record progress every PROGRESS_GRANULARITY msecs.
        if (progress.size() == 0) {
            progress.add(new ProgressRecord(now, blocksLeft));
        } else {
            ProgressRecord newrec = progress.getLast();
            if (now - newrec.mTimestamp >= PROGRESS_GRANULARITY)
                progress.add(new ProgressRecord(now, blocksLeft));
        }

        // Prune records that are too old.
        while (progress.size() > 1) {
            ProgressRecord oldrec = progress.getFirst();
            long deltaBlocks = oldrec.mBlocksLeft - blocksLeft;
            if (deltaBlocks < blocksLeft)
                break;
            progress.removeFirst();
        }

        // Look at the oldest record in the queue.
        ProgressRecord prec = progress.getFirst();

        long deltaTime = now - prec.mTimestamp;
        int deltaBlocks = prec.mBlocksLeft - blocksLeft;

        // Make sure we have some delta.
        if (deltaTime == 0 || deltaBlocks == 0)
            return 0;

        double blocksPerMillisecond = (double) deltaBlocks / (double) deltaTime;
        double mSecLeft = (double) blocksLeft / blocksPerMillisecond;

        return (long) mSecLeft;
    }

    /**
     * Called when download progress is made.
     *
     * @param pct  the percentage of chain downloaded, estimated
     * @param date the date of the last block downloaded
     */
    protected void progress(double pct, int blocksSoFar, Date date, long msecsLeft) {
        log.info(String.format("Chain download %d%% done with %d blocks to go, block date %s, complete in %d seconds",
                               (int) pct,
                               blocksSoFar,
                               DateFormat.getDateTimeInstance().format(date),
                               msecsLeft / 1000));
    }

    /**
     * Called when download is initiated.
     *
     * @param blocks the number of blocks to download, estimated
     */
    protected void startDownload(int blocks) {
        if (blocks > 0)
            log.info("Downloading block chain of size " + blocks + ". " +
                    (blocks > 1000 ? "This may take a while." : ""));
    }

    /**
     * Called when we are done downloading the block chain.
     */
    protected void doneDownload() {
    }

    /**
     * Wait for the chain to be downloaded.
     */
    public void await() throws InterruptedException {
        done.acquire();
    }
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
