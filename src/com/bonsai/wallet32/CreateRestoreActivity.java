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

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class CreateRestoreActivity extends Activity {

    private static Logger mLogger =
        LoggerFactory.getLogger(CreateRestoreActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_restore);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.create_restore, menu);
		return true;
	}

    public void createWallet(View view) {
        mLogger.info("create wallet");

        NetworkParameters params = MainNetParams.get();

        String filePrefix = "wallet32";

        // Generate a new seed.
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[16];
        random.nextBytes(seed);

        // Setup a wallet with the seed.
        HDWallet hdwallet = new HDWallet(params,
                                         getApplicationContext().getFilesDir(),
                                         filePrefix,
                                         seed);
        hdwallet.persist();

        // Spin up the WalletService.
        startService(new Intent(this, WalletService.class));

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        // Prevent the user from coming back here.
        finish();
    }

    public void restoreWallet(View view) {
        mLogger.info("restore wallet");
        // FIXME - Implement this.
    }
}
