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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ViewTransactionActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewTransactionActivity.class);

    private String mHash;

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
    protected void onWalletServiceBound() {
        // Find the transction in the wallet.
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
