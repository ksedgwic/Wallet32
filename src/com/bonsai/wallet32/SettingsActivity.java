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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bonsai.wallet32.WalletApplication.WalletEntry;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SettingsActivity.class);
    private Resources mRes;

    public static final String KEY_BTC_UNITS = "pref_btcUnits";
    public static final String KEY_FIAT_RATE_SOURCE = "pref_fiatRateSource";
    public static final String KEY_RESCAN_BLOCKCHAIN = "pref_rescanBlockchain";
    public static final String KEY_EXPERIMENTAL = "pref_experimental";

    private WalletService	mWalletService = null;
    private SettingsActivity	mThis;


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

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mThis = this;

        mRes = getResources();

        {
            Preference butt =
                (Preference) findPreference("pref_changePasscode");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            Intent intent =
                                new Intent(mThis, PasscodeActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("action", "change");
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_addAccount");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            showConfirmDialog
                                (mRes.getString(R.string.pref_add_account_title),
                                 mRes.getString(R.string.pref_add_account_confirm),
                                 mRes.getString(R.string.pref_add_account_yes),
                                 mRes.getString(R.string.pref_add_account_no),
                                 mAddAccountConfirmed);
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_viewSeed");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            Intent intent =
                                new Intent(mThis, PasscodeActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("action", "viewseed");
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_showPairing");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            Intent intent =
                                new Intent(mThis, PasscodeActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("action", "showpairing");
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_rescanBlockchain");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            Intent intent =
                                new Intent(mThis, RescanActivity.class);
                            Bundle bundle = new Bundle();
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_sendLogs");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            sendLogs();
                            finish();
                            return true;
                        }
                    });
        }

        {
            Preference butt =
                (Preference) findPreference("pref_addWallet");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            WalletApplication wallapp =
                                (WalletApplication) getApplicationContext();
                            List<WalletEntry> wallets =
                                wallapp.loadWalletDirectory();
                            int sz = wallets.size();
                            String name = String.format("Wallet %d", sz + 1);
                            String path = String.format("wallet%03d", sz + 1);
                            wallets.add(new WalletEntry(name, path));
                            wallapp.makeWalletDirectory(path);
                            wallapp.persistWalletDirectory(wallets);
							return true;
                        }
                    });
        }

        {
            Preference butt = (Preference) findPreference("pref_about");
            butt.setOnPreferenceClickListener
                (new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            Intent intent =
                                new Intent(mThis, AboutActivity.class);
                            intent.setFlags
                                (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
        }

        // If we don't have experimental mode on remove experimental
        // features.
        SharedPreferences sharedPref =
            PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isExperimental =
            sharedPref.getBoolean(SettingsActivity.KEY_EXPERIMENTAL, false);
        if (!isExperimental) {
            PreferenceScreen prefScreen =
                (PreferenceScreen) getPreferenceScreen();

            // Dynamically remove the Add Wallet option.
            Preference pref = prefScreen.findPreference("pref_addWallet");
            prefScreen.removePreference(pref);
        }
    }

    private DialogInterface.OnClickListener mAddAccountConfirmed = 
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mLogger.info("add account confirmed");
                if (mWalletService != null)
                {
                    new AddAccountTask().execute();
                }
            }
        };

    private class AddAccountTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show
                (SettingsActivity.this, "",
                 mRes.getString(R.string.pref_add_account_adding));
        }

		protected Void doInBackground(Void... arg0)
        {
            mWalletService.addAccount();
			return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();

            // Back to the main activity.
            Intent intent = new Intent(mThis, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);

            // All done here...
            finish();
        }
    }

	@Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);
        mLogger.info("SettingsActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
        mLogger.info("SettingsActivity paused");
    }

    public void sendLogs() {
		final StringBuilder text = new StringBuilder();
		final ArrayList<Uri> attachments = new ArrayList<Uri>();
		final File cacheDir = getCacheDir();

        try
        {
            final File logDir = getDir("log", Context.MODE_PRIVATE);

            for (final File logFile : logDir.listFiles())
            {
                final String logFileName = logFile.getName();
                final File file;
                if (logFileName.endsWith(".log.gz"))
                    file = File.createTempFile
                        (logFileName.substring(0, logFileName.length() - 6),
                         ".log.gz", cacheDir);
                else if (logFileName.endsWith(".log"))
                    file = File.createTempFile
                        (logFileName.substring(0, logFileName.length() - 3),
                         ".log", cacheDir);
                else
                    continue;

                final InputStream is = new FileInputStream(logFile);
                final OutputStream os = new FileOutputStream(file);

                Io.copy(is, os);

                os.close();
                is.close();

                Io.chmod(file, 0777);

                attachments.add(Uri.fromFile(file));
            }
        }
        catch (final IOException x)
        {
            mLogger.info("problem writing attachment", x);
        }

		startSend(text, attachments);
	}

	private void startSend(final CharSequence text,
                           final ArrayList<Uri> attachments)
	{
		final Intent intent;

		if (attachments.size() == 0)
		{
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
		}
		else if (attachments.size() == 1)
		{
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_STREAM, attachments.get(0));
		}
		else
		{
			intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("text/plain");
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                                               attachments);
		}

		intent.putExtra(Intent.EXTRA_EMAIL,
                        new String[] { "wallet32-dump@bonsai.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, "wallet32 logs");
		intent.putExtra(Intent.EXTRA_TEXT, "-- LOGS ATTACHED --");

		startActivity(Intent.createChooser
                      (intent,
                       getString(R.string.send_logs_mail_intent_chooser)));
	}

    public void showConfirmDialog(String title,
                                  String msg,
                                  String yesstr,
                                  String nostr,
                                  DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
        // set title
        alertDialogBuilder.setTitle(title);
 
        // set dialog message
        alertDialogBuilder
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(yesstr, listener)
            .setNegativeButton
            (nostr,
             new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                 }
             });
 
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
 
        // show it
        alertDialog.show();
    }
}
