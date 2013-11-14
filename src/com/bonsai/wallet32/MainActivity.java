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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(MainActivity.class);

    private LocalBroadcastManager mLBM;
    private Resources mRes;

    private WalletService	mWalletService;

    private double mFiatPerBTC = 0.0;

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateRate();
                updateWalletStatus(); // calls updateBalances() ...
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLBM = LocalBroadcastManager.getInstance(this);
        mRes = getResources();

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

        mLogger.info("MainActivity created");
	}

    @SuppressLint("InlinedApi")
	@Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLBM.registerReceiver(mWalletStateChangedReceiver,
                              new IntentFilter("wallet-state-changed"));
        mLBM.registerReceiver(mRateChangedReceiver,
                              new IntentFilter("rate-changed"));

        mLogger.info("MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);
        mLBM.unregisterReceiver(mRateChangedReceiver);


        mLogger.info("MainActivity paused");
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
            openSettings();
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

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
        updateBalances();
    }

    private void updateRate() {
        if (mWalletService != null) {
            mFiatPerBTC = mWalletService.getRate();
            updateBalances();
        }
    }
    private void addBalanceHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.balance_table_header, table, false);
        table.addView(row);
    }

    private void addBalanceRow(TableLayout table,
                               String acct,
                               double btc,
                               double fiat,
                               boolean isTotal) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.balance_table_row, table, false);

        TextView tv0 = (TextView) row.findViewById(R.id.row_label);
        tv0.setText(acct);
        if (isTotal)
            tv0.setTypeface(tv0.getTypeface(), Typeface.BOLD);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%.05f", btc));
        if (isTotal)
            tv1.setTypeface(tv0.getTypeface(), Typeface.BOLD);

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.03f", fiat));
        if (isTotal)
            tv2.setTypeface(tv0.getTypeface(), Typeface.BOLD);

        table.addView(row);
    }

    private void updateBalances() {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(R.id.balance_table);

        // Clear any existing table content.
        table.removeAllViews();

        addBalanceHeader(table);

        double sumbtc = 0.0;
        List<Balance> balances = mWalletService.getBalances();
        if (balances != null) {
            for (Balance bal : balances) {
                sumbtc += bal.balance;
                addBalanceRow(table,
                              bal.accountName,
                              bal.balance,
                              bal.balance * mFiatPerBTC,
                              false);
            }
        }

        addBalanceRow(table, "Total", sumbtc, sumbtc * mFiatPerBTC, true);
    }

    protected void openSettings()
    {
        // FIXME - Implement this.
    }

    public void sendBitcoin(View view) {
        Intent intent = new Intent(this, SendBitcoinActivity.class);
        startActivity(intent);
    }

    public void receiveBitcoin(View view) {
        Intent intent = new Intent(this, ReceiveBitcoinActivity.class);
        startActivity(intent);
    }

    public void exitApp(View view) {
        mLogger.info("Application Exiting");
        stopService(new Intent(this, WalletService.class));
        finish();
        System.exit(0);
    }
}
