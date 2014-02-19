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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bonsai.wallet32.WalletService.AmountAndFee;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;

import eu.livotov.zxscan.ZXScanHelper;

public class SendBitcoinActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SendBitcoinActivity.class);

    protected EditText mToAddressEditText;

    protected EditText mBTCAmountEditText;
    protected EditText mFiatAmountEditText;

    protected EditText mBTCFeeEditText;
    protected EditText mFiatFeeEditText;

    protected boolean mUserSetAmountFiat;
    protected boolean mUserSetFeeFiat;

    @SuppressLint({ "HandlerLeak", "DefaultLocale" })
	@Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_bitcoin);

        // Was a URI specified?
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String intentURI = null;
        if (bundle != null && bundle.containsKey("uri"))
            intentURI = intent.getExtras().getString("uri");

        // Start off presuming the user set the BTC amount.
        mUserSetAmountFiat = false;
        mUserSetFeeFiat = false;

        mToAddressEditText = (EditText) findViewById(R.id.to_address);
        mToAddressEditText.addTextChangedListener(mToAddressWatcher);

        mBTCAmountEditText = (EditText) findViewById(R.id.amount_btc);
        mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);

        mFiatAmountEditText = (EditText) findViewById(R.id.amount_fiat);
        mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);

        mBTCFeeEditText = (EditText) findViewById(R.id.fee_btc);
        mBTCFeeEditText.addTextChangedListener(mBTCFeeWatcher);

        mFiatFeeEditText = (EditText) findViewById(R.id.fee_fiat);
        mFiatFeeEditText.addTextChangedListener(mFiatFeeWatcher);

        // Set the default fee value.
        double defaultFee = WalletService.getDefaultFee();
        String defaultFeeString = String.format("%.5f", defaultFee);
        mBTCFeeEditText.setText(defaultFeeString);

        // Is there an intent uri (from another application)?
        if (intentURI != null)
            updateToAddress(intentURI);

        mLogger.info("SendBitcoinActivity created");
    }

	@Override
    protected void onWalletStateChanged() {
        updateAccounts();
    }

	@Override
    protected void onRateChanged() {
        updateAmountFields();
        updateFeeFields();
        updateAccounts();
    }

    private final TextWatcher mToAddressWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

			@Override
            public void afterTextChanged(Editable ss) {
                String uri = mToAddressEditText.getText().toString();
                updateToAddress(uri);
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
                double ff = parseNumberWorkaround(ss.toString());
                double bb;
                if (mFiatPerBTC == 0.0) {
                    bbs = "";
                }
                else {
                    bb = ff / mFiatPerBTC;
                    bbs = String.format("%f", bb);
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
                double bb = parseNumberWorkaround(ss.toString());
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
                double ff = parseNumberWorkaround(ss.toString());
                double bb;
                if (mFiatPerBTC == 0.0) {
                    bbs = "";
                }
                else {
                    bb = ff / mFiatPerBTC;
                    bbs = String.format("%f", bb);
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
                double bb = parseNumberWorkaround(ss.toString());
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
        tv1.setText(String.format("%f BTC", btc));

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
                              bal.available,
                              bal.available * mFiatPerBTC);
                mAccountIds.add(bal.accountId);
            }
        }
    }

    private void updateToAddress(String toval) {

        // Avoid recursion by removing the field listener while
        // we possibly update the field value.
        mToAddressEditText.removeTextChangedListener(mToAddressWatcher);

        NetworkParameters params =
            mWalletService == null ? null : mWalletService.getParams();

        // Is this a bitcoin URI?
        try {
            BitcoinURI uri = new BitcoinURI(params, toval);
            Address addr = uri.getAddress();
            BigInteger amt = uri.getAmount();

            mToAddressEditText.setText(addr.toString(), 
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
                Address addr = new Address(params, toval);

                mToAddressEditText.setText(addr.toString(), 
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

        // Restore the field changed listener.
        mToAddressEditText.addTextChangedListener(mToAddressWatcher);
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
            updateToAddress(scannedCode);
        }
    }

    public void scanQR(View view) {
        // CaptureActivity
        ZXScanHelper.scan(this, 12345);
    }

    public void computeFee(View view) {
        // Which account was selected?
        if (mCheckedFromId == -1) {
            showErrorDialog(mRes.getString(R.string.send_error_noaccount));
            return;
        }

        // Fetch the amount to send.
        double amount = 0.0;
        String amountString = mBTCAmountEditText.getText().toString();
        if (amountString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_noamount));
            return;
        }
        try {
            amount = parseNumberWorkaround(amountString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_badamount));
            return;
        }

        double fee;
        try {
            fee = mWalletService.computeRecommendedFee(mCheckedFromId, amount);
		} catch (IllegalArgumentException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_dust));
            return;
		} catch (InsufficientMoneyException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_insufficient));
            return;
		}

        mLogger.info(String.format("recommended fee is %f", fee));
        String msg = mRes.getString(R.string.send_set_fee, fee);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        String feeString = String.format("%f", fee);
        mBTCFeeEditText.setText(feeString);
    }

    public void useAll(View view) {
        // Which account was selected?
        if (mCheckedFromId == -1) {
            showErrorDialog(mRes.getString(R.string.send_error_noaccount));
            return;
        }

        AmountAndFee amtnfee;
        try {
            amtnfee = mWalletService.useAll(mCheckedFromId);

            mLogger.info(String.format("setting amount to %f, fee to %f",
                                       amtnfee.mAmount, amtnfee.mFee));
            String msg = mRes.getString(R.string.send_set_amt_fee,
                                        amtnfee.mAmount, amtnfee.mFee);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            String amtString = String.format("%f", amtnfee.mAmount);
            mBTCAmountEditText.setText(amtString);

            String feeString = String.format("%f", amtnfee.mFee);
            mBTCFeeEditText.setText(feeString);

		} catch (InsufficientMoneyException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_insufficient));
		}
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
        String acctName = mWalletService.getAccount(mCheckedFromId).getName();
        
        // Fetch the address.
        String addrString = mToAddressEditText.getText().toString();
        if (addrString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_noaddr));
            return;
        }

        // Fetch the amount to send.
        double amount = 0.0;
        String amountString = mBTCAmountEditText.getText().toString();
        if (amountString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_noamount));
            return;
        }
        try {
            amount = parseNumberWorkaround(amountString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_badamount));
            return;
        }

        // Fetch the fee amount.
        double fee = 0.0;
        String feeString = mBTCFeeEditText.getText().toString();
        if (feeString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.send_error_nofee));
            return;
        }
        try {
            fee = parseNumberWorkaround(feeString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_badfee));
            return;
        }

        // Check to make sure we have enough money for this send.
        double avail = mWalletService.availableForAccount(mCheckedFromId);
        if (amount + fee > avail) {
            showErrorDialog(mRes.getString(R.string.send_error_insufficient));
            return;
        }

        // Check the recommended fee, generate warning dialog or
        // confirm send dialog ...
        try {
            double recommendedFee =
                mWalletService.computeRecommendedFee(mCheckedFromId, amount);

            if (fee > recommendedFee) {
                // Warn that fee is larger then recommended.
                mLogger.info(String.format("fee %f larger then recommended %f",
                                           fee, recommendedFee));
                // FIXME - Add fee-too-large dialog.
            }
            else if (fee < recommendedFee) {
                // Warn that fee is less then recommended.
                mLogger.info(String.format("fee %f less then recommended %f",
                                           fee, recommendedFee));
                // FIXME - Add fee-too-small dialog.
            }
            else {
                // Looks good, confirm the send.
                mLogger.info(String.format("fee %f equals recommended %f",
                                           fee, recommendedFee));
                showSendConfirmDialog(mCheckedFromId,
                                      acctName,
                                      addrString,
                                      amount,
                                      fee,
                                      mFiatPerBTC);
            }
            
		} catch (IllegalArgumentException ex) {
            showErrorDialog(mRes.getString(R.string.send_error_dust));
            return;
		} catch (InsufficientMoneyException ex) {
            // An InsufficientMoneyException here means the send amount
            // is too large for the recommended fee.
            mLogger.info("insufficient funds for recommended fee");
            // FIXME - Add fee-too-small dialog.
		}
        

        /*
        try {
            mLogger.info(String.format
                         ("send from %d, to %s, amount %f, fee %f starting",
                          mCheckedFromId, addrString, amount, fee));

            mWalletService.sendCoinsFromAccount(mCheckedFromId,
                                                addrString,
                                                amount,
                                                fee);

            mLogger.info("send finished");

            // Head to the transaction view for this account ...
            Intent intent = new Intent(this, ViewTransactionsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("accountId", mCheckedFromId);
            intent.putExtras(bundle);
            startActivity(intent);
            
            // We're done here ...
            finish();

        } catch (RuntimeException ex) {
            mLogger.error(ex.toString());
            showErrorDialog(ex.getMessage());
            return;
        }
        */
    }

    public static class SendConfirmDialogFragment extends DialogFragment {
        int		mAcctId;
        String	mAcctStr;
        String	mAddr;
        double	mAmount;
        double	mFee;
        double	mRate;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            mAcctId = getArguments().getInt("acctId");
            mAcctStr = getArguments().getString("acctStr");
            mAddr = getArguments().getString("addr");
            mAmount = getArguments().getDouble("amount");
            mFee = getArguments().getDouble("fee");
            mRate = getArguments().getDouble("rate");

            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.send_confirm_title);

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dv = inflater.inflate(R.layout.dialog_confirm_send, null);

            {
                TextView tv = (TextView) dv.findViewById(R.id.to_addr);
                tv.setText(mAddr);
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.from_account);
                tv.setText(mAcctStr);
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.amount_btc);
                tv.setText(String.format("%f", mAmount));
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.amount_fiat);
                tv.setText(String.format("%.2f", mAmount * mRate));
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.fee_btc);
                tv.setText(String.format("%f", mFee));
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.fee_fiat);
                tv.setText(String.format("%.2f", mFee * mRate));
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.total_btc);
                tv.setText(String.format("%f", mAmount + mFee));
            }
            {
                TextView tv = (TextView) dv.findViewById(R.id.total_fiat);
                tv.setText(String.format("%.2f", (mAmount + mFee) * mRate));
            }
            
            builder.setView(dv);

            /*
            // FIXME - replace this with custom dialog.
            String msg = String.format("send %f BTC with %f fee to %s?",
                                       mAmount, mFee, mAddr);
            builder.setMessage(msg);
            */

            builder.setPositiveButton
                (R.string.send_confirm_send,
                 new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface di, int id) {
                         mLogger.info("send confirmed");
                     }
                 });

            builder.setNegativeButton
                (R.string.send_confirm_cancel,
                 new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface di, int id) {
                         mLogger.info("send canceled");
                     }
                 });

            return builder.create();
        }
    }

    private DialogFragment showSendConfirmDialog(int acctId,
                                                 String acctStr,
                                                 String addrStr,
                                                 double amount,
                                                 double fee,
                                                 double rate) {
        DialogFragment df = new SendConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putInt("acctId", acctId);
        args.putString("acctStr", acctStr);
        args.putString("addr", addrStr);
        args.putDouble("amount", amount);
        args.putDouble("fee", fee);
        args.putDouble("rate", rate);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "sendconfirm");
        return df;
    }

    private static double parseNumberWorkaround(String numstr)
        throws NumberFormatException {
        // Some countries use comma as the decimal separator.
        // Android's numberDecimal EditText fields don't handle this
        // correctly (https://code.google.com/p/android/issues/detail?id=2626).
        // As a workaround we substitute ',' -> '.' manually ...
        return Double.parseDouble(numstr.toString().replace(',', '.'));
    }
}
