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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
    SharedPreferences mPrefs;

    private boolean mChangePasscode;

    private boolean	mShowPasscode;
    private State	mState;
    private boolean	mRestoreWallet;
    private String	mPasscode;
    private String	mLastPasscode;

    private WalletService mWalletService;

    protected ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_passcode);

        mRes = getResources();

        Bundle bundle = getIntent().getExtras();
        boolean createPasscode = bundle.getBoolean("createPasscode");
        mState = createPasscode ? State.PASSCODE_CREATE : State.PASSCODE_ENTER;
        mChangePasscode = bundle.getBoolean("changePasscode");
        mRestoreWallet = bundle.getBoolean("restoreWallet");

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

        // Set the state of the show passcode checkbox.
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowPasscode = mPrefs.getBoolean("pref_showPasscode", false);
        CheckBox chkbx = (CheckBox) findViewById(R.id.show_passcode);
        chkbx.setChecked(mShowPasscode);
        chkbx.setOnCheckedChangeListener
            (new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        mShowPasscode = isChecked;
                        SharedPreferences.Editor editor = mPrefs.edit();
                        editor.putBoolean("pref_showPasscode", mShowPasscode);
                        editor.commit();
                        setPasscode(mPasscode);	// redisplay
                    }
                });

        setPasscode("");

        mLogger.info("PasscodeActivity created");
	}

    @SuppressLint("InlinedApi")
	@Override
    protected void onResume() {
        super.onResume();

        // NOTE - this passcode activity can happen on initial create
        // and login and the WalletService will not be started at that
        // time.  This is ok.
        //
        // We need a WalletService binding for the case where we change
        // the passcode and in this case it will be running ...
        //
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLogger.info("PasscodeActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
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
        setPasscode(mPasscode + val);
    }

    public void deleteDigit(View view) {
        int len = mPasscode.length();
        if (len == 0)
            return;		// Nothing to do here.
        else
            setPasscode(mPasscode.substring(0, len - 1));	// Strip last.
    }

    public void clearPasscode(View view) {
        setPasscode("");	// Clear the string.
    }

    public void submitPasscode(View view) {
        // We don't currently allow empty passcodes.
        // If we do, we'll have to side-step the keyCrypter.deriveKey
        // step because it doesn't like empty passcodes ...
        if (mPasscode.length() == 0) {
            showErrorDialog(mRes.getString(R.string.passcode_errortitle),
                            mRes.getString(R.string.passcode_empty));
            return;
        }

        switch (mState) {
        case PASSCODE_CREATE:	confirmPasscode();		break;
        case PASSCODE_CONFIRM:	checkPasscode();		break;
        case PASSCODE_ENTER:	validatePasscode();		break;
        }
    }

    // We're creating a passcode and it's been entered once.
    private void confirmPasscode() {
        // Stash the first version of the passcode.
        mLastPasscode = mPasscode;

        // Clear the passcode field.
        setPasscode("");	// Clear the string.

        // Ask the user to confirm it.
        TextView msgtv = (TextView) findViewById(R.id.message);
        msgtv.setText(R.string.passcode_confirm);

        mState = State.PASSCODE_CONFIRM;
    }

    // We're creating a passcode and it's been entered a second time.
    private void checkPasscode() {
        // Do they match?
        if (mPasscode.equals(mLastPasscode)) {
            // They matched ... setup async
            new SetupPasscodeTask().execute(mPasscode);
        }

        else {
            // Didn't match, try again ...

            showErrorDialog(mRes.getString(R.string.passcode_errortitle),
                            mRes.getString(R.string.passcode_mismatch));
            // Clear the passcode.
            setPasscode("");	// Clear the string.

            // Ask the user to create again.
            TextView msgtv = (TextView) findViewById(R.id.message);
            msgtv.setText(R.string.passcode_create);

            mState = State.PASSCODE_CREATE;
        }
    }

    private void setupComplete() {

        // Are we going on to create or restore?
        if (mRestoreWallet) {
            Intent intent = new Intent(this, RestoreWalletActivity.class);
            startActivity(intent);
        }
        else {
            // If we are changing the passcode the wallet is already
            // started ...
            if (!mChangePasscode) {
                // Create the wallet.
                WalletUtil.createWallet(getApplicationContext());

                // Spin up the WalletService.
                Intent svcintent = new Intent(this, WalletService.class);
                Bundle bundle = new Bundle();
                bundle.putString("SyncState", "CREATED");
                svcintent.putExtras(bundle);
                startService(svcintent);

                Intent intent = new Intent(this, ViewSeedActivity.class);
                startActivity(intent);
            }
        }

        // And we're done here ...
        finish();
    }

    // We're opening a wallet and the passcode has been entered.
    private void validatePasscode() {
        new ValidatePasscodeTask().execute(mPasscode);
    }

    private void validateComplete(boolean isValid) {

        if (!isValid) {
            showErrorDialog(mRes.getString(R.string.passcode_errortitle),
                            mRes.getString(R.string.passcode_invalid));

            // Clear the passcode.
            setPasscode("");	// Clear the string.

            // Ask the user to create again.
            TextView msgtv = (TextView) findViewById(R.id.message);
            msgtv.setText(R.string.passcode_enter);

            mState = State.PASSCODE_ENTER;
        }

        else {
            // Spin up the WalletService.
            Intent svcintent = new Intent(this, WalletService.class);
            Bundle bundle = new Bundle();
            bundle.putString("SyncState", "STARTUP");
            svcintent.putExtras(bundle);
            startService(svcintent);

            // Off to the main activity.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            // And we're done with this activity.
            finish();
        }
    }

    // Set the passcode, add decorations, optionally hide values.
    private void setPasscode(String val) {
        mPasscode = val;
        StringBuilder bldr = new StringBuilder();
        int len = val.length();
        for (int ii = 0; ii < len; ii += 4) {
            if (ii != 0)
                bldr.append("-");
            int end = (ii + 4 > len) ? len : ii + 4;
            if (mShowPasscode) {
                bldr.append(val.substring(ii, end));
            }
            else {
                for (int jj = ii; jj < end; ++jj)
                    bldr.append("*");
            }
        }
        TextView pctv = (TextView) findViewById(R.id.passcode);
        pctv.setText(bldr.toString());
    }

    public static class MyDialogFragment extends DialogFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");
            String title = getArguments().getString("title");
            boolean hasOK = getArguments().getBoolean("hasOK");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            builder.setTitle(title);
            builder.setMessage(msg);
            if (hasOK) {
                builder
                    .setPositiveButton(R.string.base_error_ok,
                                       new DialogInterface.OnClickListener() {
                                           public void onClick(DialogInterface di,
                                                               int id) {
                                              }
                                          });
            }
            return builder.create();
        }
    }

    protected DialogFragment showErrorDialog(String title, String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", msg);
        args.putBoolean("hasOK", true);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
        return df;
    }

    protected DialogFragment showModalDialog(String title, String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", msg);
        args.putBoolean("hasOK", false);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "note");
        return df;
    }

    private class SetupPasscodeTask extends AsyncTask<String, Void, Void> {
        DialogFragment df;

        @Override
        protected void onPreExecute() {
            df = showModalDialog(mRes.getString(R.string.passcode_waittitle),
                                 mRes.getString(R.string.passcode_waitsetup));
        }

		protected Void doInBackground(String... arg0)
        {
            String passcode = arg0[0];
            // This takes a while (scrypt) ...
            WalletUtil.setPasscode(getApplicationContext(), mWalletService,
                                   passcode, mChangePasscode);
			return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            df.dismiss();
            setupComplete();
        }
    }

    private class ValidatePasscodeTask extends AsyncTask<String, Void, Boolean> {
        DialogFragment df;

        @Override
        protected void onPreExecute() {
            df = showModalDialog(mRes.getString(R.string.passcode_waittitle),
                                 mRes.getString(R.string.passcode_waitvalidate));
        }

		protected Boolean doInBackground(String... arg0)
        {
            String passcode = arg0[0];
            // This takes a while (scrypt) ...
            return WalletUtil.passcodeValid(getApplicationContext(), passcode);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            df.dismiss();
            validateComplete(result.booleanValue());
        }
    }
}
