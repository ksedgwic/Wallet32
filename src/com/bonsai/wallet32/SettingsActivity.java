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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.DialogFragment;

public class SettingsActivity extends PreferenceActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SettingsActivity.class);
    private Resources mRes;

    public static final String KEY_FIAT_RATE_SOURCE = "pref_fiatRateSource";
    public static final String KEY_RESCAN_BLOCKCHAIN = "pref_rescanBlockchain";

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
                            bundle.putBoolean("createPasscode", true);
                            bundle.putBoolean("changePasscode", true);
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
                            showConfirmDialog(mRes.getString
                                              (R.string.pref_rescan_confirm));
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
                            startActivity(intent);
                            finish();	// All done here...
                            return true;
                        }
                    });
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

    public void showConfirmDialog(String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
        // set title
        alertDialogBuilder
            .setTitle(mRes.getString(R.string.pref_rescan_title));
 
        // set dialog message
        alertDialogBuilder
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(mRes.getString(R.string.pref_rescan_yes),
                               new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
                        if (mWalletService != null)
                        {
                            // Kick off the rescan.
                            mWalletService.rescanBlockchain();

                            // Back to the main activity for progress.
                            Intent intent =
                                new Intent(mThis, MainActivity.class);
                            intent.setFlags
                                (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(intent);

                            // All done here...
                            finish();
                        }
					}
                })
            .setNegativeButton(mRes.getString(R.string.pref_rescan_no),
                               new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					}
				});
 
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
 
        // show it
        alertDialog.show();
    }
}
