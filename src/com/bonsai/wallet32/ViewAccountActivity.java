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

import android.os.Bundle;

public class ViewAccountActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewAccountActivity.class);

    private int mAccountNum = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_account);

        // Do we want an account filter?
        // Intent intent = getIntent();
        // int accountId = intent.getExtras().getInteger("accountId");

        mLogger.info("ViewAccountActivity created");
	}

	@Override
    protected void onWalletStateChanged() {
        updateChains();
    }

    private void updateChains() {
    }

    /*
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

    private void addTransactionHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transaction_table_header, table, false);
        table.addView(row);
    }

    private void addTransactionRow(TableLayout table,
                                   String datestr,
                                   String btcstr,
                                   String btcbalstr,
                                   String confstr) {
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
            TextView tv = (TextView) row.findViewById(R.id.row_balance_btc);
            tv.setText(btcbalstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_confidence);
            tv.setText(confstr);
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

        double btcbal = mWalletService.balanceForAccount(mAccountNum);
        
        for (WalletTransaction wtx : txs) {
            Transaction tx = wtx.getTransaction();

            double btc = mWalletService.amountForAccount(wtx, mAccountNum);
            if (btc != 0.0) {
                String datestr = dateFormater.format(tx.getUpdateTime());
                String btcstr = String.format("%.5f", btc);
                String btcbalstr = String.format("%.5f", btcbal);

                String confstr;
                TransactionConfidence conf = tx.getConfidence();
                ConfidenceType ct = conf.getConfidenceType();
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

                addTransactionRow(table, datestr, btcstr, btcbalstr, confstr);
            }

            // We're working backward in time ...
            btcbal -= btc;
        }
    }
    */
}
