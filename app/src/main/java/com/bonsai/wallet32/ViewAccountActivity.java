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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ViewAccountActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewAccountActivity.class);

    private int mAccountId = -1;
    private HDAccount mAccount = null;

    private ViewSwitcher mAccountNameSwitcher;
    private TextView mAccountNameTextView;
    private EditText mAccountNameEditText;
    private Button mAccountNameSubmitButton;

    private enum NameEditState {
        INIT,		// Transient state ...
        UNSET,		// Button disabled, says Edit, text not editable.
        SET,		// Button enabled, says "Edit", text not editable.
        CLEAN,		// Button disabled, says "Submit", text editable.
        DIRTY		// Button enabled, says "Submit", text editable.
    }

    private NameEditState	mNameEditState;

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
                setNameState(NameEditState.DIRTY);
            }
        };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_account);

        Intent intent = getIntent();
        mAccountId = intent.getExtras().getInt("accountId");

        mAccountNameSwitcher =
            (ViewSwitcher) findViewById(R.id.account_name_switcher);
        mAccountNameTextView =
            (TextView) findViewById(R.id.account_name_textview);
        mAccountNameEditText =
            (EditText) findViewById(R.id.account_name_edittext);
        mAccountNameSubmitButton =
            (Button) findViewById(R.id.submit_account_name);

        mAccountNameEditText.addTextChangedListener(mAccountNameWatcher);

        mNameEditState = NameEditState.INIT;
        setNameState(NameEditState.UNSET);

        mLogger.info("ViewAccountActivity created");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.account_actions, menu);
        return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent intent;
        switch (item.getItemId()) {
        case R.id.action_export_xpub:
            intent = new Intent(this, ShowXPubActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("xpubstr", mAccount.xpubstr());
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

	@Override
    protected void onWalletServiceBound() {
        // Update our HDAccount.
        mAccount = mWalletService.getAccount(mAccountId);

        // Update the account name field.
        mAccountNameTextView.setText(mAccount.getName());
        mAccountNameEditText.setText(mAccount.getName());
        setNameState(NameEditState.SET);
    }

	@Override
    protected void onWalletStateChanged() {
        updateChains();
    }

    public void setNameState(NameEditState state) {
        // Bail if there hasn't been a change.
        if (state == mNameEditState)
            return;

        String buttonText = "???";
        switch (state) {
        case INIT:
            // Shouldn't get here.
            break;
        case UNSET:
            // Button disabled, says "Edit", text not editable.
            mAccountNameSubmitButton.setEnabled(false);
            buttonText = mRes.getString(R.string.account_name_edit);
            mAccountNameSwitcher.setDisplayedChild(0);
            break;
        case SET:
            // Button enabled, says "Edit", text not editable.
            mAccountNameSubmitButton.setEnabled(true);
            buttonText = mRes.getString(R.string.account_name_edit);
            mAccountNameSwitcher.setDisplayedChild(0);
            break;
        case CLEAN:
            // Button disabled, says "Submit", text editable.
            mAccountNameSubmitButton.setEnabled(false);
            buttonText = mRes.getString(R.string.account_name_submit);
            mAccountNameSwitcher.setDisplayedChild(1);
            break;
        case DIRTY:
            // Button enabled, says "Submit", text editable.
            mAccountNameSubmitButton.setEnabled(true);
            buttonText = mRes.getString(R.string.account_name_submit);
            mAccountNameSwitcher.setDisplayedChild(1);
            break;
        }
        mAccountNameSubmitButton.setText(buttonText);
        mNameEditState = state;
    }

    public void submitAccountName(View view) {
        switch (mNameEditState) {
        case SET:
            // This is an edit action.
            setNameState(NameEditState.CLEAN);
            mAccountNameEditText.requestFocus();
            mAccountNameEditText.setSelection
                (mAccountNameEditText.getText().length());
            break;
        case DIRTY:
            // This is a submit action.
            String name = mAccountNameEditText.getText().toString();
            mLogger.info(String.format("Changing name of account %d to %s",
                                       mAccountId, name));
            mAccount.setName(name);
            mWalletService.persist();
            mAccountNameTextView.setText(name);
            setNameState(NameEditState.SET);
            break;
        }

        // Toggle the keyboard presence ...
        InputMethodManager inputMethodManager =
            (InputMethodManager) getSystemService
            (Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow
            (mAccountNameEditText.getApplicationWindowToken(),
             InputMethodManager.SHOW_FORCED, 0);
    }

    private void updateChains() {
        updateChain(R.id.receive_table, mAccount.getReceiveChain());
        updateChain(R.id.change_table, mAccount.getChangeChain());
    }

    private void addAddressHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.address_table_header, table, false);

        TextView tv = (TextView) row.findViewById(R.id.header_btc);
        tv.setText(mBTCFmt.unitStr());

        table.addView(row);
    }

    private void addAddressRow(int tableId,
                               int index,
                               TableLayout table,
                               String path,
                               String addr,
                               String ntrans,
                               String btcstr,
                               String fiatstr) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.address_table_row, table, false);

        row.setTag(tableId);
        row.setId(index);

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

    public void handleRowClick(View view) {
        int tableId = (Integer) view.getTag();
        int index = view.getId();
        viewAddress(tableId, index);
    }

    public void viewAddress(int tableId, int index) {
        HDChain chain = null;
        switch (tableId) {
        case R.id.receive_table:
            mLogger.info(String.format("receive row %d clicked", index));
            chain = mAccount.getReceiveChain();
            break;
        case R.id.change_table:
            mLogger.info(String.format("change row %d clicked", index));
            chain = mAccount.getChangeChain();
            break;
        }

        List<HDAddress> addrs = chain.getAddresses();
        HDAddress addr = addrs.get(index);
        String addrstr = addr.getAddressString();
        
        // Dispatch to the address viewer.
        Intent intent = new Intent(this, ViewAddressActivity.class);
        intent.putExtra("key", addr.getPrivateKeyString());
        intent.putExtra("address", addrstr);
        startActivity(intent);
    }

    private void updateChain(int tableId, HDChain chain) {
        if (mWalletService == null)
            return;

        TableLayout table = (TableLayout) findViewById(tableId);

        // Clear any existing table content.
        table.removeAllViews();

        addAddressHeader(table);

        // Read all of the addresses.  Presume order is correct ...
        int ndx = 0;
        List<HDAddress> addrs = chain.getAddresses();
        for (HDAddress addr : addrs) {
            String path = addr.getPath();
            String addrstr = addr.getAbbrev();
            String ntrans = String.format("%d", addr.numTrans());
            String bal = mBTCFmt.formatCol(addr.getBalance(), 0, true, true);
            String fiat = String.format
                ("%.02f", mBTCFmt.fiatAtRate(addr.getBalance(), mFiatPerBTC));
            addAddressRow(tableId, ndx++, table, path,
                          addrstr, ntrans, bal, fiat);
        }
    }
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
