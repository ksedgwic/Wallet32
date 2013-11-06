package com.bonsai.androidelectrum;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SendBitcoinActivity extends ActionBarActivity {

    protected EditText mBTCAmountEditText;
    protected EditText mFiatAmountEditText;

    protected EditText mBTCFeeEditText;
    protected EditText mFiatFeeEditText;

    protected double mFiatPerBTC;

    protected boolean mUserSetAmountFiat;
    protected boolean mUserSetFeeFiat;

    private Logger mLogger;
    private LocalBroadcastManager mLBM;

    private WalletService	mWalletService;

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateWalletStatus();
                updateRate();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

    @SuppressLint("HandlerLeak")
	@Override
    public void onCreate(Bundle savedInstanceState) {

        mLogger = LoggerFactory.getLogger(MainActivity.class);
        mLBM = LocalBroadcastManager.getInstance(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_bitcoin);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFiatPerBTC = 0.0;

        // Start off presuming the user set the BTC amount.
        mUserSetAmountFiat = false;
        mUserSetFeeFiat = false;

        mBTCAmountEditText = (EditText) findViewById(R.id.amount_btc);
        mFiatAmountEditText = (EditText) findViewById(R.id.amount_fiat);

        mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);
        mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);

        mBTCFeeEditText = (EditText) findViewById(R.id.fee_btc);
        mFiatFeeEditText = (EditText) findViewById(R.id.fee_fiat);

        mBTCFeeEditText.addTextChangedListener(mBTCFeeWatcher);
        mFiatFeeEditText.addTextChangedListener(mFiatFeeWatcher);

        mLogger.info("SendBitcoinActivity created");
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

        mLogger.info("SendBitcoinActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);
        mLBM.unregisterReceiver(mRateChangedReceiver);

        mLogger.info("SendBitcoinActivity paused");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_bitcoin_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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

    // NOTE - This code implements a pair of "cross updating" fields.
    // If the user changes the BTC amount the fiat field is constantly
    // updated at the current mFiatPerBTC rate.  If the user changes
    // the fiat field the BTC field is constantly updated at the
    // current rate.

    private final TextWatcher mBTCAmountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {
                // Note that the user changed the BTC last.
                mUserSetAmountFiat = false;
            }

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

			@Override
            public void afterTextChanged(Editable ss) {
                updateAmountFields();
            }

        };

    private final TextWatcher mFiatAmountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {
                mUserSetAmountFiat = true;
            }

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

			@Override
            public void afterTextChanged(Editable ss) {
                updateAmountFields();
            }
        };

    @SuppressLint("DefaultLocale")
	protected void updateAmountFields() {
        // Which field did the user last edit?
        if (mUserSetAmountFiat) {
            // The user set the Fiat amount.
            String ss = mFiatAmountEditText.getText().toString();

            // Avoid recursion by removing the other fields listener.
            mBTCAmountEditText.removeTextChangedListener
                (mBTCAmountWatcher);

            String bbs;
            try {
                double ff = Double.parseDouble(ss.toString());
                double bb;
                if (mFiatPerBTC == 0.0) {
                    bbs = "";
                }
                else {
                    bb = ff / mFiatPerBTC;
                    bbs = String.format("%.4f", bb);
                }
            } catch (final NumberFormatException ex) {
                bbs = "";
            }
            mBTCAmountEditText.setText(bbs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);
        } else {
            // The user set the BTC amount.
            String ss = mBTCAmountEditText.getText().toString();

            // Avoid recursion by removing the other fields listener.
            mFiatAmountEditText.removeTextChangedListener
                (mFiatAmountWatcher);

            String ffs;
            try {
                double bb = Double.parseDouble(ss.toString());
                double ff = bb * mFiatPerBTC;
                ffs = String.format("%.2f", ff);
            } catch (final NumberFormatException ex) {
                ffs = "";
            }
            mFiatAmountEditText.setText(ffs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);
        }
    }

    // NOTE - This code implements a pair of "cross updating" fields.
    // If the user changes the BTC fee the fiat field is constantly
    // updated at the current mFiatPerBTC rate.  If the user changes
    // the fiat field the BTC field is constantly updated at the
    // current rate.

    private final TextWatcher mBTCFeeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {
                // Note that the user changed the BTC last.
                mUserSetFeeFiat = false;
            }

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

			@Override
            public void afterTextChanged(Editable ss) {
                updateFeeFields();
            }

        };

    private final TextWatcher mFiatFeeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {
                mUserSetFeeFiat = true;
            }

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

			@Override
            public void afterTextChanged(Editable ss) {
                updateFeeFields();
            }
        };

    @SuppressLint("DefaultLocale")
	protected void updateFeeFields() {
        // Which field did the user last edit?
        if (mUserSetFeeFiat) {
            // The user set the Fiat fee.
            String ss = mFiatFeeEditText.getText().toString();

            // Avoid recursion by removing the other fields listener.
            mBTCFeeEditText.removeTextChangedListener
                (mBTCFeeWatcher);

            String bbs;
            try {
                double ff = Double.parseDouble(ss.toString());
                double bb;
                if (mFiatPerBTC == 0.0) {
                    bbs = "";
                }
                else {
                    bb = ff / mFiatPerBTC;
                    bbs = String.format("%.5f", bb);
                }
            } catch (final NumberFormatException ex) {
                bbs = "";
            }
            mBTCFeeEditText.setText(bbs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mBTCFeeEditText.addTextChangedListener(mBTCFeeWatcher);
        } else {
            // The user set the BTC fee.
            String ss = mBTCFeeEditText.getText().toString();

            // Avoid recursion by removing the other fields listener.
            mFiatFeeEditText.removeTextChangedListener
                (mFiatFeeWatcher);

            String ffs;
            try {
                double bb = Double.parseDouble(ss.toString());
                double ff = bb * mFiatPerBTC;
                ffs = String.format("%.3f", ff);
            } catch (final NumberFormatException ex) {
                ffs = "";
            }
            mFiatFeeEditText.setText(ffs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mFiatFeeEditText.addTextChangedListener(mFiatFeeWatcher);
        }
    }

    private void addAccountRow(TableLayout table,
                               int acctId,
                               String acctName,
                               double btc,
                               double fiat) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.send_from_row, table, false);

        CheckBox tv0 = (CheckBox) row.findViewById(R.id.from_account);
        tv0.setId(acctId);		// Change id to the acctId.
        tv0.setText(acctName);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%.04f BTC", btc));

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.02f USD", fiat));

        table.addView(row);
    }

    private void updateAccounts() {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(R.id.from_choices);

        // Clear any existing table content.
        table.removeAllViews();

        double sumbtc = 0.0;
        List<Balance> balances = mWalletService.getBalances();
        if (balances != null) {
            for (Balance bal : balances) {
                sumbtc += bal.balance;
                addAccountRow(table,
                              bal.accountId,
                              bal.accountName,
                              bal.balance,
                              bal.balance * mFiatPerBTC);
            }
        }
    }

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
        updateAccounts();
    }

    private void updateRate() {
        if (mWalletService != null) {
            mFiatPerBTC = mWalletService.getRate();
            updateAmountFields();
            updateFeeFields();
            updateAccounts();
        }
    }

    public void sendBitcoin(View view) {
        if (mWalletService == null) {
            // FIXME - need error message here.
            return;
        }

        // Which account was selected?

        // Fetch the address.

        // Fetch the amount to send.

        // Fetch the fee amount.
    }
}
