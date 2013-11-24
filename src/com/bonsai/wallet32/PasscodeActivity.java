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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class PasscodeActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(PasscodeActivity.class);

    private enum State {
        PASSCODE_CREATE,
        PASSCODE_CONFIRM,
        PASSCODE_ENTER
    }

    private Resources mRes;

    private State	mState;
    private String	mPasscode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_passcode);

        mRes = getResources();

        Bundle bundle = getIntent().getExtras();
        boolean createPasscode = bundle.getBoolean("createPasscode");
        mState = createPasscode ? State.PASSCODE_CREATE : State.PASSCODE_ENTER;

        TextView msgtv = (TextView) findViewById(R.id.message);
        switch (mState) {
        case PASSCODE_CREATE:
            msgtv.setText(R.string.passcode_create);
            break;
        case PASSCODE_CONFIRM:
            msgtv.setText(R.string.passcode_confirm);
            break;
        case PASSCODE_ENTER:
            msgtv.setText(R.string.passcode_enter);
            break;
        }

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
        setPasscode(getPasscode() + val);
    }

    public void deleteDigit(View view) {
        String pcstr = getPasscode();
        int len = pcstr.length();
        if (len == 0)
            return;		// Nothing to do here.
        else
            setPasscode(pcstr.substring(0, len - 1));	// Strip last.
    }

    public void clearPasscode(View view) {
        setPasscode("");	// Clear the string.
    }

    public void submitPasscode(View view) {
        switch (mState) {
        case PASSCODE_CREATE:	confirmPasscode();		break;
        case PASSCODE_CONFIRM:	checkPasscode();		break;
        case PASSCODE_ENTER:	validatePasscode();		break;
        }
    }

    // We're creating a passcode and it's been entered once.
    private void confirmPasscode() {
        // Fetch the first version of the passcode.
        mPasscode = getPasscode();

        // Clear the passcode field.
        setPasscode("");	// Clear the string.

        // Ask the user to confirm it.
        TextView msgtv = (TextView) findViewById(R.id.message);
        msgtv.setText(R.string.passcode_confirm);

        mState = State.PASSCODE_CONFIRM;
    }

    // We're creating a passcode and it's been entered a second time.
    private void checkPasscode() {
        // Fetch the second version of the passcode.
        String passcode = getPasscode();

        // Do they match?
        if (passcode.equals(mPasscode)) {
            // Matched!  Store the passcode in the application context.
            WalletApplication wallapp =
                (WalletApplication) getApplicationContext();
            wallapp.mPasscode = passcode;

            // Create the wallet.
            WalletUtil.createWallet(getApplicationContext());

            // Spin up the WalletService.
            startService(new Intent(this, WalletService.class));

            Intent intent = new Intent(this, ViewSeedActivity.class);
            startActivity(intent);

            // And we're done here ...
            finish();
        } else {
            // Didn't match, try again ...

            showErrorDialog(mRes.getString(R.string.passcode_mismatch));

            // Clear the passcode.
            setPasscode("");	// Clear the string.

            // Ask the user to create again.
            TextView msgtv = (TextView) findViewById(R.id.message);
            msgtv.setText(R.string.passcode_create);

            mState = State.PASSCODE_CREATE;
        }
    }

    // We're opening a wallet and the passcode has been entered.
    private void validatePasscode() {

        // FIXME - need to validate the passcode!

        // Spin up the WalletService.
        startService(new Intent(this, WalletService.class));

        // Off to the main activity.
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        // And we're done with this activity.
        finish();
    }

    // Retrieve the passcode, strip decorations.
    private String getPasscode() {
        TextView pctv = (TextView) findViewById(R.id.passcode);
        String val = pctv.getText().toString();
        return val.replaceAll("-", "");		// Strip '-' chars.
    }

    // Set the passcode, add decorations.
    private void setPasscode(String val) {
        StringBuilder bldr = new StringBuilder();
        int len = val.length();
        for (int ii = 0; ii < len; ii += 4) {
            if (ii != 0)
                bldr.append("-");
            int end = (ii + 4 > len) ? len : ii + 4;
            bldr.append(val.substring(ii, end));
        }
        TextView pctv = (TextView) findViewById(R.id.passcode);
        pctv.setText(bldr.toString());
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            builder
                .setMessage(msg)
                .setPositiveButton(R.string.send_error_ok,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface di,
                                                           int id) {
                                           // Do we need to do anything?
                                       }
                                   });
            return builder.create();
        }
    }

    private void showErrorDialog(String msg) {
        DialogFragment df = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
    }
}
