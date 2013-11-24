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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;

import eu.livotov.zxscan.ZXScanHelper;

public class SendBitcoinActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SendBitcoinActivity.class);

    protected EditText mBTCAmountEditText;
    protected EditText mFiatAmountEditText;

    protected EditText mBTCFeeEditText;
    protected EditText mFiatFeeEditText;

    protected double mFiatPerBTC;

    protected boolean mUserSetAmountFiat;
    protected boolean mUserSetFeeFiat;

    private Resources mRes;
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

    @SuppressLint({ "HandlerLeak", "DefaultLocale" })
	@Override
    public void onCreate(Bundle savedInstanceState) {

        mRes = getResources();
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

        // Set the default fee value.
        double defaultFee = WalletService.getDefaultFee();
        String defaultFeeString = String.format("%.5f", defaultFee);
        mBTCFeeEditText.setText(defaultFeeString);

        mLogger.info("SendBitcoinActivity created");
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

    private List<Integer> mAccountIds;
    private int mCheckedFromId = -1;

    private OnCheckedChangeListener mSendFromListener =
        new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton cb,
                                         boolean isChecked) {
                if (cb.isChecked()) {
                    TableLayout table =
                        (TableLayout) findViewById(R.id.from_choices);
                    mCheckedFromId = cb.getId();
                    for (Integer acctid : mAccountIds) {
                        int rbid = acctid.intValue();
                        if (rbid != mCheckedFromId) {
                            RadioButton rb =
                                (RadioButton) table.findViewById(rbid);
                            rb.setChecked(false);
                        }
                    }
                }
			}
        };

    private void addAccountRow(TableLayout table,
                               int acctId,
                               String acctName,
                               double btc,
                               double fiat) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.send_from_row, table, false);

        RadioButton tv0 = (RadioButton) row.findViewById(R.id.from_account);
        tv0.setId(acctId);		// Change id to the acctId.
        tv0.setText(acctName);
        tv0.setOnCheckedChangeListener(mSendFromListener);
        if (acctId == mCheckedFromId)
            tv0.setChecked(true);

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
        mAccountIds = new ArrayList<Integer>();

        // double sumbtc = 0.0;
        List<Balance> balances = mWalletService.getBalances();
        if (balances != null) {
            for (Balance bal : balances) {
                // sumbtc += bal.balance;
                addAccountRow(table,
                              bal.accountId,
                              bal.accountName,
                              bal.balance,
                              bal.balance * mFiatPerBTC);
                mAccountIds.add(bal.accountId);
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

    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            builder
                .setMessage(msg)
                .setPositiveButton(R.string.send_error_ok,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface di,
                                                           int id) {
                                           // Do we need to do anything?
                                       }
                                   });
            return builder.create();
        }
    }

    @SuppressLint("DefaultLocale")
	protected void onActivityResult(final int requestCode,
                                    final int resultCode,
                                    final Intent data)
    {
        if (resultCode == RESULT_OK && requestCode == 12345)
        {
            String scannedCode = ZXScanHelper.getScannedCode(data);
            mLogger.info("saw scannedCode " + scannedCode);

            NetworkParameters params =
                mWalletService == null ? null : mWalletService.getParams();

            // Is the scanned code a bitcoin URI?
            try {
				BitcoinURI uri = new BitcoinURI(params, scannedCode);
                Address addr = uri.getAddress();
                BigInteger amt = uri.getAmount();

                EditText addrEditText =
                    (EditText) findViewById(R.id.to_address);
                addrEditText.setText(addr.toString(), 
                                     TextView.BufferType.EDITABLE);

                if (amt != null) {
                    double amtval = amt.doubleValue() / 1e8;
                    String amtstr = String.format("%f", amtval);
                    mBTCAmountEditText.setText(amtstr,
                                               TextView.BufferType.EDITABLE);
                }
			} catch (BitcoinURIParseException ex) {

                // Is it just a plain address?
                try {
                    Address addr = new Address(params, scannedCode);

                    EditText addrEditText =
                        (EditText) findViewById(R.id.to_address);
                    addrEditText.setText(addr.toString(), 
                                         TextView.BufferType.EDITABLE);

                } catch (WrongNetworkException ex2) {
                    String msg = mRes.getString(R.string.send_error_wrongnw);
                    mLogger.warn(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } catch (AddressFormatException ex2) {
                    String msg = mRes.getString(R.string.send_error_badqr);
                    mLogger.warn(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
			}
        }
    }

    private void showErrorDialog(String msg) {
        DialogFragment df = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
    }

    public void scanQR(View view) {
        // CaptureActivity
        ZXScanHelper.scan(this, 12345);
    }

    public void sendBitcoin(View view) {
        if (mWalletService == null) {
            showErrorDialog(mRes.getString(R.string.send_error_nowallet));
            return;
        }

        // Which account was selected?
        if (mCheckedFromId == -1) {
            showErrorDialog(mRes.getString(R.string.send_error_noaccount));
            return;
        }

        // Fetch the address.
        EditText addrEditText = (EditText) findViewById(R.id.to_address);
        String addrString = addrEditText.getText().toString();
        if (addrString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_noaddr));
            return;
        }

        // Fetch the amount to send.
        EditText amountEditText = (EditText) findViewById(R.id.amount_btc);
        String amountString = amountEditText.getText().toString();
        if (amountString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_noamount));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_badamount));
            return;
        }

        // Fetch the fee amount.
        EditText feeEditText = (EditText) findViewById(R.id.fee_btc);
        String feeString = feeEditText.getText().toString();
        if (feeString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_nofee));
            return;
        }
        double fee;
        try {
            fee = Double.parseDouble(feeString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_badfee));
            return;
        }

        try {
            mWalletService.sendCoinsFromAccount(mCheckedFromId,
                                                addrString,
                                                amount,
                                                fee);

            // For now return to main screen on success.
            finish();

        } catch (RuntimeException ex) {
            showErrorDialog(ex.getMessage());
            return;
        }
    }
}
