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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(MainActivity.class);

    private WalletApplication	mWalletApp;

    private View mDialogView = null;
    private DialogFragment mSyncProgressDialog = null;

    private static SimpleDateFormat mDateFormatter =
        new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        // Turn off "up" navigation since we are the top-level.
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		setContentView(R.layout.activity_main);

        mWalletApp = (WalletApplication) getApplicationContext();

        mLogger.info("MainActivity created");
	}

    @Override
    protected void onStart() {
		super.onStart();

        // If the WalletService is already ready and we have
        // an intent uri we should handle that immediately.
        if (mWalletService != null &&
            mWalletService.getState() == WalletService.State.READY)
        {
            String intentURI = mWalletApp.getIntentURI();
            if (intentURI != null) {
                mWalletApp.setIntentURI(null);	// Clear it ASAP.
                Intent intent = new Intent(this, SendBitcoinActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("uri", intentURI);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }
    }

	@Override
    protected void onWalletServiceBound() {
        onWalletStateChanged();
    }

	@Override
    protected void onWalletStateChanged() {
        if (mWalletService == null)
            return;

        // If we aren't ready make sure a sync progress dialog is up.
        if (mWalletService.getState() != WalletService.State.READY)
            if (mSyncProgressDialog == null)
                showSyncProgressDialog();

        if (mWalletService.getState() == WalletService.State.SYNCING) {
            int pctdone = (int) mWalletService.getPercentDone();

            String timeLeft = formatTimeLeft(mWalletService.getMsecsLeft());

            updateSyncStats(String.format("%d%%", pctdone),
                            String.format("%d", mWalletService.getBlocksToGo()),
                            mDateFormatter.format(mWalletService.getScanDate()),
                            timeLeft);

            if (mDialogView != null) {
                ProgressBar pb =
                    (ProgressBar) mDialogView.findViewById(R.id.progress_bar);
                pb.setProgress(pctdone);
            }
        }

        else if (mWalletService.getState() == WalletService.State.READY) {
            if (mSyncProgressDialog != null) {
                mSyncProgressDialog.dismiss();
                mSyncProgressDialog = null;
                mDialogView = null;
            }

            // Did we have an intent uri? (Sent from another application ...)
            String intentURI = mWalletApp.getIntentURI();
            if (intentURI != null) {
                mWalletApp.setIntentURI(null);	// Clear it ASAP.
                Intent intent = new Intent(this, SendBitcoinActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("uri", intentURI);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }

        updateBalances();
    }

	@Override
    protected void onRateChanged() {
        updateBalances();
    }

    private String formatTimeLeft(long msec) {
        final long SECOND = 1000;
        final long MINUTE = 60 * SECOND;
        final long HOUR = 60 * MINUTE;

        long hrs = msec / HOUR;
        long mins = (msec - (hrs * HOUR)) / MINUTE;
        long secs = (msec - (hrs * HOUR) - (mins * MINUTE)) / SECOND;

        if (msec > HOUR)
            return String.format("%d:%02d:%02d hrs", hrs, mins, secs);
        else if (msec > MINUTE)
            return String.format("%d:%02d min", mins, secs);
        else
            return String.format("%d sec", secs);
    }

    private void doExit() {
        mLogger.info("Application exiting");
        if (mWalletService != null)
            mWalletService.shutdown();
        mLogger.info("Stopping WalletService");
        stopService(new Intent(this, WalletService.class));

        // Cancel any remaining notifications.
        NotificationManager nm =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();

        mLogger.info("Finished");
        finish();
        mLogger.info("Exiting");
        System.exit(0);
    }

    @SuppressLint("ValidFragment")
	public class SyncProgressDialogFragment extends DialogFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String details = getArguments().getString("details");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            mDialogView = inflater.inflate(R.layout.dialog_sync_progress, null);
            TextView detailsTextView =
                (TextView) mDialogView.findViewById(R.id.sync_details);
            detailsTextView.setText(details);
            builder.setView(mDialogView)
                .setNegativeButton(R.string.sync_abort,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog,
                                                           int id) {
                                           mLogger.info("Abort sync selected");
                                           doExit();
                                       }
                                   });      
            return builder.create();
        }
    }

    private void showSyncProgressDialog() {
        String details;

        switch(mWalletService.getSyncState()) {
        case CREATED:
            details = mRes.getString(R.string.sync_details_created);
            break;
        case RESTORE:
            details = mRes.getString(R.string.sync_details_restore);
            break;
        case STARTUP:
            details = mRes.getString(R.string.sync_details_startup);
            break;
        case RESCAN:
            details = mRes.getString(R.string.sync_details_rescan);
            break;
        case RERESCAN:
            details = mRes.getString(R.string.sync_details_rerescan);
            break;
        default:
            details = "???";	// Shouldn't happen
            break;
        }

        DialogFragment df = new SyncProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString("details", details);
        df.setArguments(args);
        df.setCancelable(false);
        df.show(getSupportFragmentManager(), "sync_progress_dialog");
        mSyncProgressDialog = df;
    }

    private void updateSyncStats(String pctstr, String blksstr,
                                 String datestr, String cmplstr) {
        if (mDialogView == null)
            return;

        TextView pcttv = (TextView) mDialogView.findViewById(R.id.percent);
        pcttv.setText(pctstr);

        TextView blkstv = (TextView) mDialogView.findViewById(R.id.blocks_left);
        blkstv.setText(blksstr);
                
        TextView datetv = (TextView) mDialogView.findViewById(R.id.scan_date);
        datetv.setText(datestr);

        TextView cmpltv = (TextView) mDialogView.findViewById(R.id.scan_cmpl);
        cmpltv.setText(cmplstr);
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

    public void sweepKey(View view) {
        Intent intent = new Intent(this, SweepKeyActivity.class);
        startActivity(intent);
    }

    public void exitApp(View view) {
        mLogger.info("Exit selected");
        doExit();
    }
}
