// Copyright (C) 2013  Bonsai Software, Inc.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bonsai.wallet32;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SyncProgressActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SyncProgressActivity.class);

    private static SimpleDateFormat mDateFormatter =
        new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sync_progress);

        // Clear the stats fields.
        updateStats("", "", "");

        mLogger.info("SyncProgressActivity created");
	}

	@Override
    protected void onWalletServiceBound() {
        // In case the WalletService is already READY ...
        onWalletStateChanged();
    }

	@Override
    protected void onWalletStateChanged() {
        if (mWalletService == null)
            return;

        if (mWalletService.getState() == WalletService.State.SYNCING) {
            int pctdone = (int) mWalletService.getPercentDone();

            updateStats(String.format("%d%%", pctdone),
                        String.format("%d", mWalletService.getBlocksToGo()),
                        mDateFormatter.format(mWalletService.getScanDate()));

            ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
            pb.setProgress(pctdone);
        }

        else if (mWalletService.getState() == WalletService.State.READY) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();	// All done here ...
        }
    }

    private void updateStats(String pctstr, String blksstr, String datestr) {
            TextView pcttv = (TextView) findViewById(R.id.percent);
            pcttv.setText(pctstr);

            TextView blkstv = (TextView) findViewById(R.id.blocks_left);
            blkstv.setText(blksstr);
                
            TextView datetv = (TextView) findViewById(R.id.scan_date);
            datetv.setText(datestr);
    }
}
