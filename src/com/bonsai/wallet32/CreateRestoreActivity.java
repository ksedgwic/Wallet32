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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class CreateRestoreActivity extends Activity {

    private static Logger mLogger =
        LoggerFactory.getLogger(CreateRestoreActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_restore);

        // If we don't have experimental mode on remove experimental
        // features.
        SharedPreferences sharedPref =
            PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isExperimental =
            sharedPref.getBoolean(SettingsActivity.KEY_EXPERIMENTAL, false);
        if (!isExperimental) {
            findViewById(R.id.countersigned_spacer).setVisibility(View.GONE);
            findViewById(R.id.countersigned).setVisibility(View.GONE);
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby_actions, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent intent;
        switch (item.getItemId()) {
        case R.id.action_about:
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void createWallet(View view) {
        mLogger.info("create wallet");

        Intent intent = new Intent(this, PasscodeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "create");
        intent.putExtras(bundle);
        startActivity(intent);

        // We're done here ...
        finish();
    }

    public void restoreWallet(View view) {
        mLogger.info("restore wallet");

        Intent intent = new Intent(this, PasscodeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "restore");
        intent.putExtras(bundle);
        startActivity(intent);

        // Prevent the user from coming back here.
        finish();
    }

    public void pairWallet(View view) {
        mLogger.info("pair wallet");

        Intent intent = new Intent(this, PasscodeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "pair");
        intent.putExtras(bundle);
        startActivity(intent);

        // Prevent the user from coming back here.
        finish();
    }

    public void countersignedWallet(View view) {
        mLogger.info("countersigned wallet");

        Intent intent = new Intent(this, PasscodeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "countersigned");
        intent.putExtras(bundle);
        startActivity(intent);

        // Prevent the user from coming back here.
        finish();
    }
}
