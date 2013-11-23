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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class PasscodeActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(PasscodeActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_passcode);
        mLogger.info("PasscodeActivity created");
	}

    @SuppressLint("InlinedApi")
	@Override
    protected void onResume() {
        super.onResume();
        mLogger.info("PasscodeActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLogger.info("PasscodeActivity paused");
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_actions, menu);
        return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.action_settings:
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void openSettings()
    {
        // FIXME - Implement this.
    }

    public void enterDigit(View view) {
        // Which button was clicked?
        String val;
        switch (view.getId()) {
        case R.id.button_1:	val = "1";		break;
        case R.id.button_2:	val = "2";		break;
        case R.id.button_3:	val = "3";		break;
        case R.id.button_4:	val = "4";		break;
        case R.id.button_5:	val = "5";		break;
        case R.id.button_6:	val = "6";		break;
        case R.id.button_7:	val = "7";		break;
        case R.id.button_8:	val = "8";		break;
        case R.id.button_9:	val = "9";		break;
        case R.id.button_0:	val = "0";		break;
        default:			val = "?";		break;
        }

        // Update the textview.
        TextView pctv = (TextView) findViewById(R.id.passcode);
        String pcstr = pctv.getText().toString();
        String pcstr2 = pcstr + val;
        pctv.setText(pcstr2);
    }

    public void deleteDigit(View view) {
        TextView pctv = (TextView) findViewById(R.id.passcode);
        String pcstr = pctv.getText().toString();
        int len = pcstr.length();
        if (len == 0)
            return;		// Nothing to do here.
        else
            pctv.setText(pcstr.substring(0, len - 1));	// Strip last.
    }

    public void clearPasscode(View view) {
        TextView pctv = (TextView) findViewById(R.id.passcode);
        pctv.setText("");	// Clear the string.
    }

    public void submitPasscode(View view) {
        // Spin up the WalletService.
        startService(new Intent(this, WalletService.class));

        // Off to the main activity.
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        // And we're done with this activity.
        finish();
    }
}
