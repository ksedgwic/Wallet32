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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

public class RescanActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(RescanActivity.class);

    private long mScanTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_rescan);

        // Choose the epoch button by default.
        RadioButton epochButton = (RadioButton) findViewById(R.id.epoch_choice);
        epochButton.setChecked(true);
        onEpochClicked(epochButton);

        mLogger.info("RescanActivity created");
	}

    public void onFullClicked(View view) {
        mLogger.info("full rescan selected");
        mScanTime = 0;
    }

    public void onEpochClicked(View view) {
        mLogger.info("epoch rescan selected");
        mScanTime = HDAddress.EPOCH;
    }

    public void doRescan(View view) {
        if (mWalletService != null)
        {
            // Kick off the rescan.
            mWalletService.rescanBlockchain(mScanTime);

            // Back to the main activity for progress.
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);

            finish();
        }
    }

    public void doCancel(View view) {
        finish();
    }
}
