// Copyright (C) 2013-2014  Bonsai Software, Inc.
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
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar.LayoutParams;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class LobbyActivity extends Activity {

    private static Logger mLogger =
        LoggerFactory.getLogger(LobbyActivity.class);

    private WalletApplication mWalletApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);

        mLogger.info("Lobby starting");

        // If this is the first time set preferences to default values.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Always set the rescan value to CANCEL.
        SharedPreferences settings =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SettingsActivity.KEY_RESCAN_BLOCKCHAIN, "CANCEL");
        editor.commit();

        mWalletApp = (WalletApplication) getApplicationContext();

        // Were we called with VIEW intent URI (another app wants to send)?
        {
            final Intent intent = getIntent();
            final String action = intent.getAction();
            final Uri intentUri = intent.getData();
            final String scheme =
                intentUri != null ? intentUri.getScheme() : null;
			if (Intent.ACTION_VIEW.equals(action)
                && intentUri != null
                && "bitcoin".equals(scheme))
            {
                mLogger.info("saw URI " + intentUri.toString());
                mWalletApp.setIntentURI(intentUri.toString());
            }
        }

        // Is the wallet already open?
        if (WalletService.mIsRunning) {
            mLogger.info("Wallet already open");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }

        else {
            List<WalletApplication.WalletEntry> walletList = mWalletApp.listWallets();
            if (walletList.size() == 1) {
                // If there is one wallet, open it by default.
                openWallet(walletList.get(0).mPath);
                finish();
            }
            else {
                LinearLayout ll =
                    (LinearLayout) findViewById(R.id.button_layout);
                LayoutParams lp =
                    new LayoutParams(LayoutParams.MATCH_PARENT,
                                     LayoutParams.WRAP_CONTENT);

                for (WalletApplication.WalletEntry entry : walletList) {
                    Button butt = new Button(this);
                    butt.setText(entry.mName);
                    butt.setTag(entry.mPath);
                    butt.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                String path = (String) view.getTag();
                                mWalletApp.makeWalletDirectory(path);
                                openWallet(path);
                            }
                        });
                            
                    ll.addView(butt, lp);
                }
            }
        }
	}

    public void openWallet(String walletPath) {

        mWalletApp.setWalletDirName(walletPath);

        File walletFile = mWalletApp.getHDWalletFile(null);
        if (walletFile.exists()) {

            mLogger.info("Existing wallet found");

            Intent intent = new Intent(this, PasscodeActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("action", "login");
            intent.putExtras(bundle);
            startActivity(intent);

        } else {

            mLogger.info("No existing wallet");

            Intent intent = new Intent(this, CreateRestoreActivity.class);
            startActivity(intent);
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby_actions, menu);
		return true;
	}
}
