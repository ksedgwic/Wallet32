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

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;

import eu.livotov.zxscan.ZXScanHelper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
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
        double defaultFee = WalletService.getDefaultFee();
        String defaultFeeString = String.format("%.5f", defaultFee);
        mBTCFeeEditText.setText(defaultFeeString);

        mUnspentOutputs = null;

        mLogger.info("SweepKeyActivity created");
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

    private void addAccountRow(TableLayout table,
                               int acctId,
                               String acctName,
                               double btc,
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
        tv1.setText(String.format("%.04f BTC", btc));

        TextView tv2 = (TextView) row.findViewById(R.id.row_fiat);
        tv2.setText(String.format("%.02f USD", fiat));

        table.addView(row);
    }

    private void updateAccounts() {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(R.id.to_choices);

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
            double bb = parseNumberWorkaround(ss);
            double ff = bb * mFiatPerBTC;
            ffs = String.format("%.3f", ff);
        } catch (final NumberFormatException ex) {
            ffs = "";
        }
        mBalanceFiatText.setText(ffs, TextView.BufferType.NORMAL);
    }

    private class FetchUnspentTask extends AsyncTask<String, Void, String> {

        String baseUrl = "https://blockchain.info/unspent?active=";

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
            if (jsonstr == null) {
                showErrorDialog(mRes.getString(R.string.sweep_error_blockchain));
                return;
            }
                
            else if (jsonstr.contains("No free outputs to spend")) {
                showErrorDialog(mRes.getString(R.string.sweep_no_unspent));
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

                mLogger.info("key balance %d", balance);
                
                String ffs = String.format("%.5f", (double) balance / 1e8);
                mBalanceBTCText.setText(ffs, TextView.BufferType.NORMAL);
                updateBalance();

                // Stash the JSONArray of outputs.
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

    private void updatePrivateKey(String privstr) {

        // Avoid recursion by removing the field listener while
        // we possibly update the field value.
        mPrivateKeyEditText.removeTextChangedListener(mPrivateKeyWatcher);

        NetworkParameters params =
            mWalletService == null ? null : mWalletService.getParams();

        ECKey key;
		try {
            key = new DumpedPrivateKey(params, privstr).getKey();

            mPrivateKeyEditText.setText(privstr, TextView.BufferType.EDITABLE);

		} catch (AddressFormatException e) {
            String msg = mRes.getString(R.string.sweep_error_badkey);
            mLogger.warn(msg);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
		}

        String addr = key.toAddress(params).toString();

        // Clear any existing outputs.
        mUnspentOutputs = null;

        // Fetch unspent outputs from service.
        FetchUnspentTask task = new FetchUnspentTask();
        task.execute(addr);

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
        ZXScanHelper.scan(this, 12346);
    }

    public void sweepKey(View view) {
        if (mWalletService == null) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nowallet));
            return;
        }

        // Which account was selected?
        if (mAccountId == -1) {
            showErrorDialog(mRes.getString(R.string.sweep_error_noaccount));
            return;
        }

        // Fetch the private key.
        String keyString = mPrivateKeyEditText.getText().toString();
        if (keyString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nokey));
            return;
        }

        // Make sure we have fetched the unspent outputs.
        if (mUnspentOutputs == null) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nooutputs));
            return;
        }

        // Fetch the fee amount.
        EditText feeEditText = (EditText) findViewById(R.id.fee_btc);
        String feeString = feeEditText.getText().toString();
        if (feeString.length() == 0) {
            showErrorDialog(mRes.getString(R.string.sweep_error_nofee));
            return;
        }
        double fee;
        try {
            fee = parseNumberWorkaround(feeString);
        } catch (NumberFormatException ex) {
            showErrorDialog(mRes.getString(R.string.sweep_error_badfee));
            return;
        }

        // Sweep!
        mWalletService.sweepKey(keyString, fee, mAccountId, mUnspentOutputs);

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
