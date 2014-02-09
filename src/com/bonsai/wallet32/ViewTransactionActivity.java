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
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ViewTransactionActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewTransactionActivity.class);

    private String mHash;

    private ArrayList<Address>	mInputAddrs;
    private ArrayList<Address>	mOutputAddrs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_transaction);

        Intent intent = getIntent();
        mHash = intent.getExtras().getString("hash");

        {
            TextView tv = (TextView) findViewById(R.id.hash);
            tv.setText(mHash);
        }

        mLogger.info("ViewTransactionActivity created");
	}

	@Override
    protected void onWalletStateChanged() {
        NetworkParameters params = mWalletService.getParams();

        // Find the transaction in the wallet.
        Transaction tx = mWalletService.getTransaction(mHash);

        SimpleDateFormat dateFormater =
            new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        String datestr = dateFormater.format(tx.getUpdateTime());
        {
            TextView tv = (TextView) findViewById(R.id.date);
            tv.setText(datestr);
        }
        
        TransactionConfidence conf = tx.getConfidence();
        ConfidenceType ct = conf.getConfidenceType();
        String confstr;
        switch (ct) {
        case UNKNOWN: confstr = "Unknown"; break;
        case BUILDING:
            int depth = conf.getDepthInBlocks();
            confstr = String.format("%d Confirmations", depth);
            break;
        case PENDING: confstr = "Pending"; break;
        case DEAD: confstr = "Dead"; break;
        default: confstr = "?"; break;
        }
        {
            TextView tv = (TextView) findViewById(R.id.confidence);
            tv.setText(confstr);
        }

        // These nbsp string defaults keep the layout sane ...
        String acctStrDef =
            "\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000";
        String chainCodeDef =
            "\u2000";
        String pathDef =
            "\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000\u2000";

        TableLayout inputsTable =
            (TableLayout) findViewById(R.id.inputs_table);
        inputsTable.removeAllViews(); // Clear any existing table content.
        // addTransputsHeader(inputsTable);

        double totalInputBalance = 0.0;

        mInputAddrs = new ArrayList<Address>();

        // Enumerate inputs.
        int ndx = 0;
        for (TransactionInput txIn : tx.getInputs()) {
            // See if this address is in our HD wallet.
            HDAddressDescription descr = null;
            Address addr = null;
            try {
                addr = txIn.getFromAddress();
                descr = mWalletService.findAddress(addr);
            }
            catch (ScriptException ex) {
                // Just leave things blank if we can't determine
                // an address ...
            }

            mInputAddrs.add(addr);

            // What is the value of this input?
            double value = 0.0;
            TransactionOutput cto = txIn.getConnectedOutput();
            if (cto != null)
                value = cto.getValue().doubleValue() / 1e8;

            totalInputBalance += value;

            // We can fill wallet-specific fields if we found the
            // address in the wallet.
            String acctStr = acctStrDef;
            String chainCode = chainCodeDef;
            String path = pathDef;
            if (descr != null) {
                acctStr = descr.hdAccount.getName();
                chainCode = descr.hdChain.isReceive() ? "R" : "C";
                path = descr.hdAddress.getPath();
            }

            String addrStr = "";
            if (addr != null)
                addrStr = addr.toString().substring(0, 8) + "...";

            mLogger.info(String.format("input:  %12s %1s %8s %11s %f",
                                       acctStr, chainCode, path,
                                       addrStr, value));

            String valStr = String.format("%.05f", value);

            addTransputsRow(R.id.inputs_table, ndx++, inputsTable,
                            acctStr, chainCode, path, addrStr, valStr);
        }
        addTransputsSum(inputsTable, totalInputBalance);

        TableLayout outputsTable =
            (TableLayout) findViewById(R.id.outputs_table);
        outputsTable.removeAllViews(); // Clear any existing table content.
        // addTransputsHeader(outputsTable);

        double totalOutputBalance = 0.0;

        mOutputAddrs = new ArrayList<Address>();

        // Enumerate outputs.
        ndx = 0;
        for (TransactionOutput txOut : tx.getOutputs()) {
            // See if this address is in our HD wallet.
            HDAddressDescription descr = null;
            Address addr = null;
            try {
                addr = txOut.getScriptPubKey().getToAddress(params);
                descr = mWalletService.findAddress(addr);
            }
            catch (ScriptException ex) {
                // Just leave things blank if we can't determine
                // an address ...
            }

            mOutputAddrs.add(addr);

            // What is the value of this input?
            double value = txOut.getValue().doubleValue() / 1e8;

            totalOutputBalance += value;

            // We can fill wallet-specific fields if we found the
            // address in the wallet.
            String acctStr = acctStrDef;
            String chainCode = chainCodeDef;
            String path = pathDef;
            if (descr != null) {
                acctStr = descr.hdAccount.getName();
                chainCode = descr.hdChain.isReceive() ? "R" : "C";
                path = descr.hdAddress.getPath();
            }

            String addrStr = "";
            if (addr != null)
                addrStr = addr.toString().substring(0, 8) + "...";

            mLogger.info(String.format("output: %12s %1s %8s %11s %f",
                                       acctStr, chainCode, path,
                                       addrStr, value));
            String valStr = String.format("%.05f", value);

            addTransputsRow(R.id.outputs_table, ndx++, outputsTable,
                            acctStr, chainCode, path, addrStr, valStr);
        }
        addTransputsSum(outputsTable, totalOutputBalance);

        double fee = totalInputBalance - totalOutputBalance;
        {
            String valStr = String.format("%.05f", fee);
            TextView tv = (TextView) findViewById(R.id.fee);
            tv.setText(valStr);
        }
        
        mLogger.info(String.format(" Total Inputs: %f", totalInputBalance));
        mLogger.info(String.format("Total Outputs: %f", totalOutputBalance));
        mLogger.info(String.format("   Miners Fee: %f", fee));
    }

    private void addTransputsHeader(TableLayout table) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transputs_table_header, table, false);
        table.addView(row);
    }

    private void addTransputsRow(int tableId,
                                 int index,
                                 TableLayout table,
                                 String accountName,
                                 String chainCode,
                                 String path,
                                 String addr,
                                 String btcstr) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transputs_table_row, table, false);

        row.setTag(tableId);
        row.setId(index);

        {
            TextView tv = (TextView) row.findViewById(R.id.row_account);
            tv.setText(accountName);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_chain);
            tv.setText(chainCode);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_path);
            tv.setText(path);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_addr);
            tv.setText(addr);
        }

        {
            TextView tv = (TextView) row.findViewById(R.id.row_btc);
            tv.setText(btcstr);
        }

        table.addView(row);
    }

    private void addTransputsSum(TableLayout table, double btc) {
        TableRow row =
            (TableRow) LayoutInflater.from(this)
            .inflate(R.layout.transputs_table_sum, table, false);

        TextView tv1 = (TextView) row.findViewById(R.id.row_btc);
        tv1.setText(String.format("%.05f", btc));

        table.addView(row);
    }

    public void handleRowClick(View view) {
        int tableId = (Integer) view.getTag();
        int index = view.getId();
        viewAddress(tableId, index);
    }

    public void viewAddress(int tableId, int index) {
        Address addr = null;
        switch (tableId) {
        case R.id.inputs_table:
            mLogger.info(String.format("inputs row %d clicked", index));
            addr = mInputAddrs.get(index);
            break;
        case R.id.outputs_table:
            mLogger.info(String.format("outputs row %d clicked", index));
            addr = mOutputAddrs.get(index);
            break;
        }

        String addrstr = addr.toString();
        
        String url = "https://blockchain.info/address/" + addrstr;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        // I think it is useful to come back here after going to
        // blockchain.info, so we don't call finish ...
        // Dispatch to the address viewer.
    }

    public void viewBlockchain(View view) {
        String url = "https://blockchain.info/tx/" + mHash;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        // I think it is useful to come back here after going to
        // blockchain.info, so we don't call finish ...
    }
}
