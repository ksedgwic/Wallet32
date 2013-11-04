package com.bonsai.androidelectrum;

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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends ActionBarActivity {

    private Logger mLogger;
    private LocalBroadcastManager mLBM;
    private Resources mRes;

    private WalletService	mWalletService;

    // Used to convert dp to px programatically.
    private static Float mScale;
    public static int dpToPixel(int dp, Context context) {
        if (mScale == null)
            mScale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dp * mScale);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateWalletStatus();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLogger = LoggerFactory.getLogger(MainActivity.class);
        mLBM = LocalBroadcastManager.getInstance(this);
        mRes = getResources();

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

        updateBalances();

        mLogger.info("MainActivity created");
	}

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLBM.registerReceiver(mMessageReceiver,
                              new IntentFilter("wallet-state-changed"));

        mLogger.info("MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mMessageReceiver);

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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWalletStatus();
            }
        };

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
    }

    private void addBalanceHeader(TableLayout table) {
        // create a new TableRow
        TableRow row = new TableRow(this);

        TableRow.LayoutParams rowParams =
            new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                                      TableRow.LayoutParams.WRAP_CONTENT);
        rowParams.leftMargin = dpToPixel(10, this);
        rowParams.rightMargin = dpToPixel(10, this);

        TextView tv0 = new TextView(this);
        // tv0.setText(mRes.getString(R.string.balance_header_acct));
        tv0.setText("");	// Looks better blank ...
        tv0.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        tv0.setTypeface(tv0.getTypeface(), Typeface.BOLD);
        tv0.setLayoutParams(rowParams);
        tv0.setGravity(Gravity.LEFT);
        row.addView(tv0);

        TextView tv1 = new TextView(this);
        tv1.setText(mRes.getString(R.string.balance_header_btc));
        tv1.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        tv1.setTypeface(tv1.getTypeface(), Typeface.BOLD);
        tv1.setLayoutParams(rowParams);
        tv1.setGravity(Gravity.CENTER);
        row.addView(tv1);

        TextView tv2 = new TextView(this);
        tv2.setText(mRes.getString(R.string.balance_header_fiat));
        tv2.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        tv2.setTypeface(tv2.getTypeface(), Typeface.BOLD);
        tv2.setLayoutParams(rowParams);
        tv2.setGravity(Gravity.CENTER);
        row.addView(tv2);

        // add the TableRow to the TableLayout
        table.addView(row, rowParams);
    }

    private void addBalanceRow(TableLayout table,
                               String acct,
                               double btc,
                               double fiat,
                               boolean isTotal) {
        // create a new TableRow
        TableRow row = new TableRow(this);

        TableRow.LayoutParams rowParams =
            new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                                      TableRow.LayoutParams.WRAP_CONTENT);
        rowParams.leftMargin = dpToPixel(10, this);
        rowParams.rightMargin = dpToPixel(10, this);

        TextView tv0 = new TextView(this);
        tv0.setText(acct);
        tv0.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        if (isTotal)
            tv0.setTypeface(tv0.getTypeface(), Typeface.BOLD);
        tv0.setLayoutParams(rowParams);
        tv0.setGravity(Gravity.LEFT);
        row.addView(tv0);

        TextView tv1 = new TextView(this);
        tv1.setText(String.format("%.04f", btc));
        tv1.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        if (isTotal)
            tv1.setTypeface(tv0.getTypeface(), Typeface.BOLD);
        tv1.setLayoutParams(rowParams);
        tv1.setGravity(Gravity.RIGHT);
        row.addView(tv1);

        TextView tv2 = new TextView(this);
        tv2.setText(String.format("%.02f", fiat));
        tv2.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        if (isTotal)
            tv2.setTypeface(tv0.getTypeface(), Typeface.BOLD);
        tv2.setLayoutParams(rowParams);
        tv2.setGravity(Gravity.RIGHT);
        row.addView(tv2);

        // add the TableRow to the TableLayout
        table.addView(row, rowParams);
    }

    private void updateBalances() {

        TableLayout table = (TableLayout) findViewById(R.id.balance_table);
        addBalanceHeader(table);
        addBalanceRow(table, "Account0", 1.0000, 210.00, false);
        addBalanceRow(table, "Account1", 0.1000, 21.00, false);
        addBalanceRow(table, "Total", 1.1000, 231.00, true);
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
        // Do something in response to button
    }

    public void exitApp(View view) {
        mLogger.info("Application Exiting");
        stopService(new Intent(this, WalletService.class));
        finish();
        System.exit(0);
    }
}
