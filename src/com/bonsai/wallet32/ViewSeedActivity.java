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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ViewSeedActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewSeedActivity.class);

    private MnemonicCoder	mCoder;

    private boolean			mSeedFetched;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        try {
			mCoder = new MnemonicCoder(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        mSeedFetched = false;

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_seed);

        mLogger.info("ViewSeedActivity created");
	}

	@Override
    protected void onWalletStateChanged() {
        // FIXME - Is the seed always set when the WalletService is
        // bound?  If so we can use onWalletServiceBound and lose the
        // stupid mSeedFetched flag ...
        //
        updateSeed();
    }

    private void updateSeed() {
        if (mWalletService == null)
            return;

        // Don't need further updating after we fetched it once.
        if (mSeedFetched)
            return;

        byte[] seed = mWalletService.getSeed();
        if (seed == null)
            return;

        mLogger.info("saw seed " + new String(Hex.encode(seed)));

        mSeedFetched = true;

        TextView hextv = (TextView) findViewById(R.id.seed_hex);
        hextv.setText(new String(Hex.encode(seed)));

        StringBuilder builder = new StringBuilder();
        List<String> words = mCoder.encode(seed);
        for (int ii = 0; ii < words.size(); ii += 3) {
            if (ii != 0)
                builder.append("\n");

            builder.append(String.format("%-9s %-9s %-9s",
                                         words.get(ii),
                                         words.get(ii+1),
                                         words.get(ii+2)));
        }
        TextView mnetv = (TextView) findViewById(R.id.seed_mnemonic);
        mnetv.setText(builder.toString());
    }

    public void seedDone(View view) {
        Intent intent = new Intent(this, SyncProgressActivity.class);
        startActivity(intent);
        finish();	// All done here ...
    }
}
