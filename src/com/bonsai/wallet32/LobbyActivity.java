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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class LobbyActivity extends Activity {

    private static Logger mLogger =
        LoggerFactory.getLogger(LobbyActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);

        mLogger.info("Lobby starting");

        // Is there an existing wallet?
        File dir = getApplicationContext().getFilesDir();
        String filePrefix = "wallet32";		// FIXME - Also in WalletService
        String path = filePrefix + ".hdwallet";	// FIXME - Also in WalletService
        File walletFile = new File(dir, path);
        if (walletFile.exists()) {

            mLogger.info("Existing wallet found");

            // Spin up the WalletService.
            startService(new Intent(this, WalletService.class));

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        } else {

            mLogger.info("No existing wallet");

            Intent intent = new Intent(this, CreateRestoreActivity.class);
            startActivity(intent);
        }

        // Prevent the user from coming back here.
        finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby, menu);
		return true;
	}

}
