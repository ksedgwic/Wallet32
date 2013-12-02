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

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(MainActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

        // Is this entry from a create?
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            if (bundle.getBoolean("showSeed", false)) {
                // Yes, we should go straight to the seed view
                // so the user can back up their new seed ...
                intent = new Intent(this, ViewSeedActivity.class);
                startActivity(intent);
                return;
            }
        }

        mLogger.info("MainActivity created");
	}

	@Override
    protected void onWalletStateChanged() {
        updateBalances();
    }

	@Override
    protected void onRateChanged() {
        updateBalances();
    }

    private void addBalanceHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.balance_table_header, table, false);
        table.addView(row);
    }

    private void addBalanceRow(TableLayout table,
                               int accountId,
                               String acct,
                               double btc,
                               double fiat) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.balance_table_row, table, false);

        Button tv0 = (Button) row.findViewById(R.id.row_label);
        tv0.setId(accountId);
        tv0.setText(acct);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%.05f", btc));

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.03f", fiat));

        table.addView(row);
    }

    private void addBalanceSum(TableLayout table,
                               String acct,
                               double btc,
                               double fiat) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.balance_table_sum, table, false);

        TextView tv0 = (TextView) row.findViewById(R.id.row_label);
        tv0.setText(acct);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%.05f", btc));

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.03f", fiat));

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
                              bal.accountId,
                              bal.accountName,
                              bal.balance,
                              bal.balance * mFiatPerBTC);
            }
        }

        addBalanceSum(table, "Total", sumbtc, sumbtc * mFiatPerBTC);
    }

    public void viewAccount(View view) {
        int accountId = view.getId();
        Intent intent = new Intent(this, ViewAccountActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("accountId", accountId);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void sendBitcoin(View view) {
        Intent intent = new Intent(this, SendBitcoinActivity.class);
        startActivity(intent);
    }

    public void receiveBitcoin(View view) {
        Intent intent = new Intent(this, ReceiveBitcoinActivity.class);
        startActivity(intent);
    }

    public void viewTransactions(View view) {
        Intent intent = new Intent(this, ViewTransactionsActivity.class);
        startActivity(intent);
    }

    public void viewSeed(View view) {
        Intent intent = new Intent(this, ViewSeedActivity.class);
        startActivity(intent);
    }

    public void exitApp(View view) {
        mLogger.info("Application Exiting");
        stopService(new Intent(this, WalletService.class));
        finish();
        System.exit(0);
    }
}
