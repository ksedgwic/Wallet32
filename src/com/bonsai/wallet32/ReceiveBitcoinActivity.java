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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.bitcoin.core.Address;

public class ReceiveBitcoinActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ReceiveBitcoinActivity.class);

    protected EditText mBTCAmountEditText;
    protected EditText mFiatAmountEditText;

    protected boolean mUserSetAmountFiat;

	@Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_bitcoin);

        // Start off presuming the user set the BTC amount.
        mUserSetAmountFiat = false;

        mBTCAmountEditText = (EditText) findViewById(R.id.amount_btc);
        mFiatAmountEditText = (EditText) findViewById(R.id.amount_fiat);

        mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);
        mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);

        mLogger.info("ReceiveBitcoinActivity created");
    }

	@Override
    protected void onWalletStateChanged() {
        updateAccounts();
    }

	@Override
    protected void onRateChanged() {
        updateAccounts();
        updateAmountFields();
    }

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
                long bb;
                if (mFiatPerBTC == 0.0) {
                    bbs = "";
                }
                else {
                    bb = mBTCFmt.btcAtRate(ff, mFiatPerBTC);
                    bbs = mBTCFmt.format(bb);
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
                long bb = mBTCFmt.parse(ss.toString());
                double ff = mBTCFmt.fiatAtRate(bb, mFiatPerBTC);
                ffs = String.format("%.2f", ff);
            } catch (final NumberFormatException ex) {
                ffs = "";
            }
            mFiatAmountEditText.setText(ffs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);
        }
    }

    private List<Integer> mAccountIds;
    private int mCheckedToId = -1;

    private OnCheckedChangeListener mReceiveToListener =
        new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton cb,
                                         boolean isChecked) {
                if (cb.isChecked()) {
                    TableLayout table =
                        (TableLayout) findViewById(R.id.to_choices);
                    mCheckedToId = cb.getId();
                    for (Integer acctid : mAccountIds) {
                        int rbid = acctid.intValue();
                        if (rbid != mCheckedToId) {
                            RadioButton rb =
                                (RadioButton) table.findViewById(rbid);
                            rb.setChecked(false);
                        }
                    }
                }
			}
        };

    private void addAccountHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.receive_to_header, table, false);
        table.addView(row);
    }

    private void addAccountRow(TableLayout table,
                               int acctId,
                               String acctName,
                               long btc,
                               double fiat) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.receive_to_row, table, false);

        RadioButton tv0 = (RadioButton) row.findViewById(R.id.to_account);
        tv0.setId(acctId);		// Change id to the acctId.
        tv0.setText(acctName);
        tv0.setOnCheckedChangeListener(mReceiveToListener);
        if (acctId == mCheckedToId)
            tv0.setChecked(true);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%s", mBTCFmt.formatCol(btc, 0, true)));

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.02f", fiat));

        table.addView(row);
    }

    private void updateAccounts() {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(R.id.to_choices);

        // Clear any existing table content.
        table.removeAllViews();

        addAccountHeader(table);

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
                              mBTCFmt.fiatAtRate(bal.balance, mFiatPerBTC));
                mAccountIds.add(bal.accountId);
            }
        }
    }

    public void receiveBitcoin(View view) {
        if (mWalletService == null) {
            showErrorDialog(mRes.getString(R.string.receive_error_nowallet));
            return;
        }

        // Which account was selected?
        if (mCheckedToId == -1) {
            showErrorDialog(mRes.getString(R.string.receive_error_noaccount));
            return;
        }

        mLogger.info(String.format("receiving to account %d", mCheckedToId));

        Address addr = mWalletService.nextReceiveAddress(mCheckedToId);
        String addrstr = addr.toString();

        mLogger.info(String.format("picked %s to receive", addrstr));

        // Was the amount specified?
        EditText amountEditText = (EditText) findViewById(R.id.amount_btc);
        String amountString = amountEditText.getText().toString();
        long amount = 0;
        if (amountString.length() != 0) {
            try {
                amount = mBTCFmt.parse(amountString);
            } catch (NumberFormatException ex) {
                showErrorDialog(mRes
                                .getString(R.string.receive_error_badamount));
                return;
            }
        }

        // Dispatch to the address viewer.
        Intent intent = new Intent(this, ViewAddressActivity.class);
        intent.putExtra("address", addrstr);
        intent.putExtra("amount", amount);
        startActivity(intent);
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
