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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ViewAccountActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewAccountActivity.class);

    private int mAccountId = -1;
    private HDAccount mAccount = null;

    private EditText mAccountNameEditText;
    private Button mAccountNameSubmitButton;

    private final TextWatcher mAccountNameWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}

			@Override
            public void afterTextChanged(Editable ss) {
                mAccountNameSubmitButton.setEnabled(true);
            }
        };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_account);

        Intent intent = getIntent();
        mAccountId = intent.getExtras().getInt("accountId");

        mAccountNameEditText = (EditText) findViewById(R.id.account_name);
        mAccountNameSubmitButton =
            (Button) findViewById(R.id.submit_account_name);

        // Listen for changes to the account name.
        mAccountNameEditText.addTextChangedListener(mAccountNameWatcher);

        mLogger.info("ViewAccountActivity created");
	}

	@Override
    protected void onWalletServiceBound() {
        // Update our HDAccount.
        mAccount = mWalletService.getAccount(mAccountId);

        // Update the account name field.
        mAccountNameEditText.setText(mAccount.getName());
        mAccountNameSubmitButton.setEnabled(false);
    }

	@Override
    protected void onWalletStateChanged() {
        updateChains();
    }

    public void submitAccountName(View view) {
        String name = mAccountNameEditText.getText().toString();
        mLogger.info(String.format("Changing name of account %d to %s",
                                   mAccountId, name));
        mAccount.setName(name);
        mWalletService.persist();
        mAccountNameSubmitButton.setEnabled(false);
    }

    private void updateChains() {
        updateChain(R.id.receive_table, mAccount.getReceiveChain());
        updateChain(R.id.change_table, mAccount.getChangeChain());
    }

    private void addAddressHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.address_table_header, table, false);
        table.addView(row);
    }

    private void addAddressRow(TableLayout table,
                                   String path,
                                   String addr,
                                   String ntrans,
                                   String btcstr,
                                   String fiatstr) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.address_table_row, table, false);

        {
            TextView tv = (TextView) row.findViewById(R.id.row_path);
            tv.setText(path);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_addr);
            tv.setText(addr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_ntrans);
            tv.setText(ntrans);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_btc);
            tv.setText(btcstr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_fiat);
            tv.setText(fiatstr);
        }

        table.addView(row);
    }

    private void updateChain(int tableId, HDChain chain) {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(tableId);

        // Clear any existing table content.
        table.removeAllViews();

        addAddressHeader(table);

        // Read all of the addresses.  Presume order is correct ...
        List<HDAddress> addrs = chain.getAddresses();
        for (HDAddress addr : addrs) {
            String path = addr.getPath();
            String addrstr =
                String.format("%s...",
                              addr.getAddressString().substring(0, 8));
            String ntrans = String.format("%d", addr.numTrans());
            String bal = String.format("%.05f", addr.getBalance());
            String fiat =
                String.format("%.02f", addr.getBalance() * mFiatPerBTC);
            addAddressRow(table, path, addrstr, ntrans, bal, fiat);
        }
    }
}
