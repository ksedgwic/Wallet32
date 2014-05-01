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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.wallet.WalletTransaction;

public class ViewTransactionsActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewTransactionsActivity.class);

    private int mAccountNum = -1;    // -1 means all accounts

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_transactions);

        // Was an account specified?
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey("accountId"))
            mAccountNum = intent.getExtras().getInt("accountId");

        Spinner spinner = (Spinner) findViewById(R.id.account_spinner);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
                    int index = arg0.getSelectedItemPosition();
                    mAccountNum = index - 1;
                    updateTransactions();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
            });

        mLogger.info("ViewTransactionsActivity created");
	}

	@Override
    protected void onWalletStateChanged() {
        updateAccountSpinner();
        updateTransactions();
    }

	@Override
    protected void onRateChanged() {
        updateTransactions();
    }

    private void updateAccountSpinner() {
        if (mWalletService != null) {
            mLogger.info("updating account spinner");
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
            spinner.setSelection(mAccountNum + 1);
        }
    }

    private void addTransactionHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transaction_table_header, table, false);

        TextView tv = (TextView) row.findViewById(R.id.header_btc);
        tv.setText(mBTCFmt.unitStr());

        table.addView(row);
    }

    private void addTransactionRow(String hash,
                                   TableLayout table,
                                   String datestr,
                                   String timestr,
                                   String confstr,
                                   String btcstr,
                                   String btcbalstr,
                                   String fiatstr,
                                   String fiatbalstr,
                                   boolean tintrow) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transaction_table_row, table, false);

        row.setTag(hash);

        {
            TextView tv = (TextView) row.findViewById(R.id.row_date);
            tv.setText(datestr);
        }
        {
            TextView tv = (TextView) row.findViewById(R.id.row_time);
            tv.setText(timestr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_confidence);
            tv.setText(confstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_btc_balance);
            tv.setText(btcbalstr);
        }
        {
            TextView tv = (TextView) row.findViewById(R.id.row_btc);
            tv.setText(btcstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_fiat_balance);
            tv.setText(fiatbalstr);
        }
        {
            TextView tv = (TextView) row.findViewById(R.id.row_fiat);
            tv.setText(fiatstr);
        }

        if (tintrow)
            row.setBackgroundColor(Color.parseColor("#ccffcc"));

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
            new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormater =
            new SimpleDateFormat("kk:mm:ss");

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
                    int cmp = -dt0.compareTo(dt1);
                    if (cmp == 0) {
                        // These two transactions happened in the same
                        // block (same time) so we should compare
                        // something else to keep the sorting order
                        // stable.
                        Sha256Hash h0 = wt0.getTransaction().getHash();
                        Sha256Hash h1 = wt1.getTransaction().getHash();
                        return -h0.compareTo(h1);
                    }
                    return cmp;
                }
            });

        long btcbal = mWalletService.balanceForAccount(mAccountNum);
        int rowcounter = 0;
        
        for (WalletTransaction wtx : txs) {
            Transaction tx = wtx.getTransaction();
            TransactionConfidence conf = tx.getConfidence();
            ConfidenceType ct = conf.getConfidenceType();

            long btc = mWalletService.amountForAccount(wtx, mAccountNum);
            if (btc != 0) {
                double fiat = mBTCFmt.fiatAtRate(btc, mFiatPerBTC);
                double fiatbal = mBTCFmt.fiatAtRate(btcbal, mFiatPerBTC);

                String hash = tx.getHashAsString();

                String datestr = dateFormater.format(tx.getUpdateTime());
                String timestr = timeFormater.format(tx.getUpdateTime());

                String btcstr = mBTCFmt.formatCol(btc, 0, true);
                String btcbalstr = mBTCFmt.formatCol(btcbal, 0, true);

                String fiatstr = String.format("%.02f", fiat);
                String fiatbalstr = String.format("%.02f", fiatbal);

                String confstr;
                switch (ct) {
                case UNKNOWN: confstr = "U"; break;
                case BUILDING:
                    int depth = conf.getDepthInBlocks();
                    confstr = depth > 100 ? "100+" : String.format("%d", depth);
                    break;
                case PENDING: confstr = "P"; break;
                case DEAD: confstr = "D"; break;
                default: confstr = "?"; break;
                }

                // This is just too noisy ...
                // mLogger.info("tx " + hash);

                boolean tintrow = rowcounter % 2 == 0;
                ++rowcounter;

                addTransactionRow(hash, table, datestr, timestr, confstr,
                                  btcstr, btcbalstr, fiatstr, fiatbalstr,
                                  tintrow);
            }

            // We're working backward in time ...
            // Dead transactions should not affect the balance ...
            if (ct != ConfidenceType.DEAD)
                btcbal -= btc;
        }
    }

    public void handleRowClick(View view) {
        // Dispatch to the transaction viewer.
        String hash = (String) view.getTag();
        Intent intent = new Intent(this, ViewTransactionActivity.class);
        intent.putExtra("hash", hash);
        startActivity(intent);
    }
}
