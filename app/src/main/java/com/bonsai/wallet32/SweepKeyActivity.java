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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;

import eu.livotov.zxscan.ZXScanHelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class SweepKeyActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(SweepKeyActivity.class);

    private EditText mPrivateKeyEditText;

    private TextView mBalanceBTCText;
    private TextView mBalanceFiatText;

    private EditText mBTCFeeEditText;
    private EditText mFiatFeeEditText;

    private boolean mUserSetFeeFiat;

    private JSONArray	mUnspentOutputs;

    private ECKey		mKey = null;
    private Address		mAddr = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweep_key);

        // Start off presuming the user set the BTC amount.
        mUserSetFeeFiat = false;

        mPrivateKeyEditText = (EditText) findViewById(R.id.private_key);
        mPrivateKeyEditText.addTextChangedListener(mPrivateKeyWatcher);

        mBalanceBTCText = (TextView) findViewById(R.id.balance_btc);
        mBalanceFiatText = (TextView) findViewById(R.id.balance_fiat);

        mBTCFeeEditText = (EditText) findViewById(R.id.fee_btc);
        mBTCFeeEditText.addTextChangedListener(mBTCFeeWatcher);

        mFiatFeeEditText = (EditText) findViewById(R.id.fee_fiat);
        mFiatFeeEditText.addTextChangedListener(mFiatFeeWatcher);

        // Set the default fee value.
        long defaultFee = WalletService.getDefaultFee();
        String defaultFeeString = mBTCFmt.format(defaultFee);
        mBTCFeeEditText.setText(defaultFeeString);

        mUnspentOutputs = null;

        mLogger.info("SweepKeyActivity created");
    }

	@Override
    protected void onResume() {
        super.onResume();
        // Set these each time we resume in case we've visited the
        // Settings and they've changed.
        {
            TextView tv = (TextView) findViewById(R.id.balance_btc_label);
            tv.setText(mBTCFmt.unitStr());
        }
        {
            TextView tv = (TextView) findViewById(R.id.fee_btc_label);
            tv.setText(mBTCFmt.unitStr());
        }
        mLogger.info("SweepKeyActivity resumed");
    }

	@Override
    protected void onWalletStateChanged() {
        updateAccounts();
    }

	@Override
    protected void onRateChanged() {
        updateAccounts();
        updateBalance();
        updateFeeFields();
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
                long bb = mBTCFmt.parse(ss.toString());
                double ff = mBTCFmt.fiatAtRate(bb, mFiatPerBTC);
                ffs = String.format("%.2f", ff);
            } catch (final NumberFormatException ex) {
                ffs = "";
            }
            mFiatFeeEditText.setText(ffs, TextView.BufferType.EDITABLE);

            // Restore the other fields listener.
            mFiatFeeEditText.addTextChangedListener(mFiatFeeWatcher);
        }
    }

    private List<Integer> mAccountIds;
    private int mAccountId = -1;

    private OnCheckedChangeListener mReceiveToListener =
        new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton cb,
                                         boolean isChecked) {
                if (cb.isChecked()) {
                    TableLayout table =
                        (TableLayout) findViewById(R.id.to_choices);
                    mAccountId = cb.getId();
                    for (Integer acctid : mAccountIds) {
                        int rbid = acctid.intValue();
                        if (rbid != mAccountId) {
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

        TextView tv = (TextView) row.findViewById(R.id.header_btc);
        tv.setText(mBTCFmt.unitStr());

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
        if (acctId == mAccountId)
            tv0.setChecked(true);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%s", mBTCFmt.formatCol(btc, 0, true, true)));

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

    private final TextWatcher mPrivateKeyWatcher = new TextWatcher() {
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
                String val = mPrivateKeyEditText.getText().toString();
                updatePrivateKey(val);
            }

        };

    private void updateBalance() {
        String ss = mBalanceBTCText.getText().toString();
        String ffs;
        try {
            long bb = mBTCFmt.parse(ss.toString());
            double ff = mBTCFmt.fiatAtRate(bb, mFiatPerBTC);
            ffs = String.format("%.2f", ff);
        } catch (final NumberFormatException ex) {
            ffs = "";
        }
        mBalanceFiatText.setText(ffs, TextView.BufferType.NORMAL);
    }

    public static class MyDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");
            String title = getArguments().getString("title");
            boolean hasOK = getArguments().getBoolean("hasOK");
            AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
            builder.setTitle(title);
            builder.setMessage(msg);
            if (hasOK) {
                builder
                    .setPositiveButton(R.string.base_error_ok,
                                       new DialogInterface.OnClickListener() {
                                           public void onClick(DialogInterface di,
                                                               int id) {
                                              }
                                          });
            }
            return builder.create();
        }
    }

    protected DialogFragment showModalDialog(String title, String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", msg);
        args.putBoolean("hasOK", false);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "wait");
        return df;
    }

    private class FetchUnspentTask extends AsyncTask<String, Void, String> {

        String baseUrl = "https://blockchain.info/unspent?active=";

        DialogFragment		mDF = null;

        @Override
        protected void onPreExecute() {
            // Show the wait dialog ...
            mDF = showModalDialog(mRes.getString(R.string.sweep_wait_title),
                                  mRes.getString(R.string.sweep_wait_fetching));
        }

		@Override
		protected String doInBackground(String... params)
        {
            final String addr = params[0];

            mLogger.info("fetching unspent outputs for " + addr);
            
            String url = baseUrl + addr;

            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                InputStream is = httpEntity.getContent();           
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                return sb.toString();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
			return null;
		}

        @Override
        protected void onPostExecute(String jsonstr) {
            mDF.dismissAllowingStateLoss();

            if (jsonstr == null) {
                showErrorDialog(mRes.getString(R.string.sweep_error_blockchain));
                mBalanceBTCText.setText("0.0", TextView.BufferType.NORMAL);
                updateBalance();
                mUnspentOutputs = null;
                return;
            }
                
            else if (jsonstr.contains("No free outputs to spend")) {
                showErrorDialog(mRes.getString(R.string.sweep_no_unspent));
                mBalanceBTCText.setText("0.0", TextView.BufferType.NORMAL);
                updateBalance();
                mUnspentOutputs = null;
                return;
            }
                
            else try {
                JSONObject jsonobj = new JSONObject(jsonstr);
                JSONArray outputs = jsonobj.getJSONArray("unspent_outputs");
                long balance = 0;
                for (int ii = 0; ii < outputs.length(); ++ii) {
                    JSONObject output = outputs.getJSONObject(ii);
                    balance += output.getLong("value");
                }

                mLogger.info(String.format("key balance %d", balance));
                
                String ffs = String.format("%s", mBTCFmt.format(balance));
                mBalanceBTCText.setText(ffs, TextView.BufferType.NORMAL);
                updateBalance();
                mUnspentOutputs = outputs;

                return;
                
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            showErrorDialog(mRes.getString(R.string.sweep_error_parse));
            return;
        }
    }

    private final Handler mHandleKeyChanged = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Fetch unspent outputs from service.
                FetchUnspentTask task = new FetchUnspentTask();
                task.execute(mAddr.toString());
            }
        };

    private void updatePrivateKey(String privstr) {

        // Sets mKey to the private key, null otherwise.

        // Avoid recursion by removing the field listener while
        // we possibly update the field value.
        mPrivateKeyEditText.removeTextChangedListener(mPrivateKeyWatcher);

        NetworkParameters params =
            mWalletService == null ? null : mWalletService.getParams();

        mKey = null;
        mAddr = null;

		try {
            // If we can decode a private key we're set.
            mKey = new DumpedPrivateKey(params, privstr).getKey();

            mAddr = mKey.toAddress(params);

            mPrivateKeyEditText.setText(privstr, TextView.BufferType.EDITABLE);

		} catch (AddressFormatException e) {

            // Is this a bitcoin URI?
            try {
                BitcoinURI uri = new BitcoinURI(params, privstr);
                mAddr = uri.getAddress();

            } catch (BitcoinURIParseException ex) {

                // Is it just a plain address?
                try {
                    mAddr = new Address(params, privstr);
                } catch (WrongNetworkException ex2) {
                    String msg = mRes.getString(R.string.sweep_error_wrongnw);
                    mLogger.warn(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } catch (AddressFormatException ex2) {
                    String msg = mRes.getString(R.string.sweep_error_badqr);
                    mLogger.warn(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            if (mAddr != null) {
                mPrivateKeyEditText.setText(privstr,
                                            TextView.BufferType.EDITABLE);

                String msg = mRes.getString(R.string.sweep_needprivate);
                mLogger.warn(msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
		}

        // Clear any existing outputs.
        mUnspentOutputs = null;

        if (mAddr != null) {
            // Send a message to update the unspent outputs.  Can't do it
            // directly here because we are in a bad context ...
            Message msgObj = mHandleKeyChanged.obtainMessage();
            mHandleKeyChanged.sendMessage(msgObj);
        }

        // Restore the field changed listener.
        mPrivateKeyEditText.addTextChangedListener(mPrivateKeyWatcher);
    }

    @SuppressLint("DefaultLocale")
	protected void onActivityResult(final int requestCode,
                                    final int resultCode,
                                    final Intent data)
    {
        if (resultCode == RESULT_OK && requestCode == 12346)
        {
            String scannedCode = ZXScanHelper.getScannedCode(data);
            mLogger.info("saw scannedCode " + scannedCode);
            updatePrivateKey(scannedCode);
        }
    }

    public void scanQR(View view) {
        // CaptureActivity
        // ZXScanHelper.setCustomScanSound(R.raw.quiet_beep);
        ZXScanHelper.setPlaySoundOnRead(false);
        ZXScanHelper.setCustomScanLayout(R.layout.scanner_layout);
        ZXScanHelper.scan(this, 12346);
    }

    public void sweepKey(View view) {
        if (mWalletService == null) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nowallet));
            return;
        }

        // Fetch the private key.
        if (mKey == null) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nokey));
            return;
        }

        // Make sure we have fetched the unspent outputs.
        if (mUnspentOutputs == null) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nooutputs));
            return;
        }

        // Fetch the fee amount.
        long fee = 0;
        EditText feeEditText = (EditText) findViewById(R.id.fee_btc);
        String feeString = feeEditText.getText().toString();
        if (feeString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nofee));
            return;
        }
        try {
            fee = mBTCFmt.parse(feeString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.sweep_error_badfee));
            return;
        }

        // Which account was selected?
        if (mAccountId == -1) {
            showErrorDialog(mRes.getString(R.string.sweep_error_noaccount));
            return;
        }

        // Sweep!
        mWalletService.sweepKey(mKey, fee, mAccountId, mUnspentOutputs);

        // Head to the transaction view for this account ...
        Intent intent = new Intent(this, ViewTransactionsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("accountId", mAccountId);
        intent.putExtras(bundle);
        startActivity(intent);
            
        // We're done here ...
        finish();
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

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
