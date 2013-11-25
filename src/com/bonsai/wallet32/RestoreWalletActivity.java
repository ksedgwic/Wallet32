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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.params.MainNetParams;

public class RestoreWalletActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(RestoreWalletActivity.class);

    private Resources			mRes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        mRes = getApplicationContext().getResources();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_restore_wallet);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.restore_wallet, menu);
		return true;
	}

    public void restoreWallet(View view) {
        mLogger.info("restore wallet");

        NetworkParameters params = MainNetParams.get();

        String filePrefix = "wallet32";

        EditText hextxt = (EditText) findViewById(R.id.seed);
        EditText mnemonictxt = (EditText) findViewById(R.id.mnemonic);

        String hexseedstr = hextxt.getText().toString();
        String mnemonicstr = mnemonictxt.getText().toString();

        // Did the user specify *both* hex seed and mnemonic?
        if (hexseedstr.length() > 0 && mnemonicstr.length() > 0) {
            showErrorDialog(mRes.getString(R.string.restore_bothbad));
            return;
        }

        byte[] seed;

        // Did we have a hex seed?
        if (hexseedstr.length() > 0) {
            try {
                seed = Hex.decode(hexseedstr);
            } catch (Exception ex) {
                showErrorDialog(mRes.getString(R.string.restore_badhexvalue));
                return;
            }
            if (seed.length != 16) {
                showErrorDialog(mRes.getString(R.string.restore_badhexlength));
                return;
            }
        }

        // How about a mnemonic string?
        else if (mnemonicstr.length() > 0) {
            MnemonicCoder coder;
			try {
				coder = new MnemonicCoder(this);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
            List<String> words =
                new ArrayList<String>(Arrays.asList
                                      (mnemonicstr.trim().split("\\s+")));
            try {
                seed = coder.decode(words);
            }
            catch (AddressFormatException ex) {
                showErrorDialog(mRes.getString(R.string.restore_badlength));
                return;
            }
            catch (IllegalArgumentException ex) {
                String msg = mRes.getString(R.string.restore_badword,
                                            ex.getMessage());
                showErrorDialog(msg);
                return;
            }
            catch (VerificationException ex) {
                showErrorDialog(mRes.getString(R.string.restore_badchecksum));
                return;
            }
        }

        // Hmm, nothing specified?
        else {
            showErrorDialog(mRes.getString(R.string.restore_noseed));
            return;
        }

        WalletApplication wallapp = (WalletApplication) getApplicationContext();

        int numAccounts = 3;

        // Setup a wallet with the restore seed.
        HDWallet hdwallet = new HDWallet(params,
                                         getApplicationContext().getFilesDir(),
                                         filePrefix,
                                         wallapp.mKeyCrypter,
                                         wallapp.mAesKey,
                                         seed,
                                         numAccounts);
        hdwallet.persist();

        // Spin up the WalletService.
        startService(new Intent(this, WalletService.class));

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        // Prevent the user from coming back here.
        finish();
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

    private void showErrorDialog(String msg) {
        DialogFragment df = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
    }
}
