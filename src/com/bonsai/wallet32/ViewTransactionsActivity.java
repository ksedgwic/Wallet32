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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.WalletTransaction;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ViewTransactionsActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewTransactionsActivity.class);

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
                updateAccountSpinner();
                updateWalletStatus(); // Calls updateTransactions();
                updateRate();
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

		setContentView(R.layout.activity_view_transactions);

        // Do we want an account filter?
        // Intent intent = getIntent();
        // int accountId = intent.getExtras().getInteger("accountId");

        Spinner spinner = (Spinner) findViewById(R.id.account_spinner);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
                    int index = arg0.getSelectedItemPosition();
                    mLogger.info(String.format("selected item %d", index));
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
            });

        mLogger.info("ViewTransactionsActivity created");
	}

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLBM.registerReceiver(mWalletStateChangedReceiver,
                              new IntentFilter("wallet-state-changed"));
        mLBM.registerReceiver(mRateChangedReceiver,
                              new IntentFilter("rate-changed"));

        mLogger.info("ViewTransactionsActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);
        mLBM.unregisterReceiver(mRateChangedReceiver);

        mLogger.info("ViewTransactionsActivity paused");
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

    protected void openSettings()
    {
        // FIXME - Implement this.
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

    private void updateAccountSpinner() {
        if (mWalletService != null) {
            List<String> list = new ArrayList<String>();
            list.add("All Accounts");
            List<HDAccount> accts = mWalletService.getAccounts();
            for (HDAccount acct : accts)
                list.add(acct.getName());
            Spinner spinner = (Spinner) findViewById(R.id.account_spinner);
            ArrayAdapter<String> dataAdapter =
                new ArrayAdapter<String>(this,
                                         android.R.layout.simple_spinner_item,
                                         list);
            dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(dataAdapter);
        }
    }

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
        updateTransactions();
    }

    private void updateRate() {
        if (mWalletService != null) {
            mFiatPerBTC = mWalletService.getRate();
        }
        updateTransactions();
    }

    private void addTransactionHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transaction_table_header, table, false);
        table.addView(row);
    }

    private void addTransactionRow(TableLayout table,
                                   String datestr,
                                   String btcstr,
                                   String fiatstr,
                                   String btcbalstr,
                                   String fiatbalstr) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transaction_table_row, table, false);

        {
            TextView tv = (TextView) row.findViewById(R.id.row_date);
            tv.setText(datestr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_btc);
            tv.setText(btcstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_fiat);
            tv.setText(fiatstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_balance_btc);
            tv.setText(btcbalstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_balance_fiat);
            tv.setText(fiatbalstr);
        }

        table.addView(row);
    }

    private void updateTransactions() {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(R.id.transaction_table);

        // Clear any existing table content.
        table.removeAllViews();

        addTransactionHeader(table);

        SimpleDateFormat dateFormater =
            new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

        // Read all the transactions and sort by date.
        Iterable<WalletTransaction> txit = mWalletService.getTransactions();
        ArrayList<WalletTransaction> txs = new ArrayList<WalletTransaction>();
        for (WalletTransaction wtx : txit)
            txs.add(wtx);
        // Sort in reverse time order (most recent first).
        Collections.sort(txs, new Comparator<WalletTransaction>() {
                public int compare(WalletTransaction wt0,
                                   WalletTransaction wt1) {
                    Date dt0 = wt0.getTransaction().getUpdateTime();
                    Date dt1 = wt1.getTransaction().getUpdateTime();
                    return -dt0.compareTo(dt1);
                }
            });

        int acctnum = -1;	// All accounts.

        double btcbal = mWalletService.balanceForAccount(acctnum);
        
        for (WalletTransaction wtx : txs) {
            Transaction tx = wtx.getTransaction();

            String datestr = dateFormater.format(tx.getUpdateTime());

            double btc = mWalletService.amountForAccount(wtx, acctnum);
            String btcstr = String.format("%.5f", btc);

            double fiat = btc * mFiatPerBTC;
            String fiatstr = String.format("%.2f", fiat);

            String btcbalstr = String.format("%.5f", btcbal);

            double fiatbal = btcbal * mFiatPerBTC;
            String fiatbalstr = String.format("%.2f", fiatbal);

            addTransactionRow(table, datestr, btcstr, fiatstr,
                              btcbalstr, fiatbalstr);

            // We're working backward in time ...
            btcbal -= btc;
        }
    }
}
