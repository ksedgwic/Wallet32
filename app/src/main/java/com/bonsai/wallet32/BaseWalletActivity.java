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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public abstract class BaseWalletActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(BaseWalletActivity.class);

    protected LocalBroadcastManager mLBM;
    protected Resources mRes;

    protected WalletApplication	mApp;

    protected WalletService	mWalletService;

    protected double mFiatPerBTC = 0.0;

    protected static BTCFmt mBTCFmt = null;

    protected ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                onWalletServiceBound();
                updateRate();
                updateWalletStatus();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
                onWalletServiceUnbound();
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLBM = LocalBroadcastManager.getInstance(this);
        mRes = getResources();
        mApp = (WalletApplication) getApplicationContext();

		super.onCreate(savedInstanceState);

        mBTCFmt = mApp.getBTCFmt();

        // By default we should have an up.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLogger.info("BaseWalletActivity created");
	}

    @Override
    protected void onStart() {

		super.onStart();

        // All derived classes represent "logged in" activities; make
        // sure we haven't short-cut here from the recent activities
        // menu etc.
        //
        if (!mApp.isLoggedIn())
        {
            mLogger.info("started without login; back to the lobby");

            // Go to the lobby and get logged in ...
            Intent intent = new Intent(this, LobbyActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @SuppressLint("InlinedApi")
	@Override
    protected void onResume() {
        super.onResume();

        mLogger.info("BaseWalletActivity resumed");

        mApp.cancelBackgroundTimeout();

        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        // Refetch the BTC format object in case it's changed.
        mBTCFmt = mApp.getBTCFmt();

        mLBM.registerReceiver(mWalletStateChangedReceiver,
                              new IntentFilter("wallet-state-changed"));
        mLBM.registerReceiver(mRateChangedReceiver,
                              new IntentFilter("rate-changed"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLogger.info("BaseWalletActivity paused");

        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);
        mLBM.unregisterReceiver(mRateChangedReceiver);

        mApp.startBackgroundTimeout();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.base_actions, menu);
        return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent intent;
        switch (item.getItemId()) {
        case R.id.action_settings:
            intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        case R.id.action_view_seed:
            if (mApp.passcodeFreshlyEntered()) {
                intent = new Intent(this, ViewSeedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                intent = new Intent(this, PasscodeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("action", "viewseed");
                intent.putExtras(bundle);
            }
            startActivity(intent);
            return true;
        case R.id.action_about:
            intent = new Intent(this, AboutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver mWalletStateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWalletStatus();
            }
        };

    private BroadcastReceiver mRateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateRate();
            }
        };

    private void updateRate() {
        if (mWalletService == null)
            return;

        mFiatPerBTC = mWalletService.getRate();

        onRateChanged();
    }

    private void updateWalletStatus() {
        if (mWalletService == null)
            return;

        onWalletStateChanged();
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
            boolean hasOK = getArguments().getBoolean("hasOK");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            builder.setMessage(msg);
            if (hasOK) {
                builder.setPositiveButton(R.string.base_error_ok,
                                          new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface di,
                                                                  int id) {
                                                  // Do we need to do anything?
                                              }
                                          });
            }
            return builder.create();
        }
    }

    protected DialogFragment showErrorDialog(String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        args.putBoolean("hasOK", true);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
        return df;
    }

    protected DialogFragment showModalDialog(String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        args.putBoolean("hasOK", false);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "note");
        return df;
    }

    protected void onWalletServiceBound() {
    }

    protected void onWalletServiceUnbound() {
    }

    protected void onWalletStateChanged() {
    }

    protected void onRateChanged() {
    }
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
