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

import java.io.File;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar.LayoutParams;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class LobbyActivity extends Activity {

    private static Logger mLogger =
        LoggerFactory.getLogger(LobbyActivity.class);

    private WalletApplication mApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);

        mLogger.info("Lobby starting");

        // If this is the first time set preferences to default values.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Always set the rescan value to CANCEL.
        SharedPreferences settings =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SettingsActivity.KEY_RESCAN_BLOCKCHAIN, "CANCEL");
        editor.commit();

        mApp = (WalletApplication) getApplicationContext();

        // Were we called with VIEW intent URI (another app wants to send)?
        {
            final Intent intent = getIntent();
            final String action = intent.getAction();
            final Uri intentUri = intent.getData();
            final String scheme =
                intentUri != null ? intentUri.getScheme() : null;
			if (Intent.ACTION_VIEW.equals(action)
                && intentUri != null
                && "bitcoin".equals(scheme))
            {
                mLogger.info("saw URI " + intentUri.toString());
                mApp.setIntentURI(intentUri.toString());
            }
        }

        // Is the wallet already open?
        if (WalletService.mIsRunning) {
            mLogger.info("Wallet already open");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }

        else {
            List<WalletApplication.WalletEntry> walletList = mApp.listWallets();

            if (walletList.size() == 1) {
                // If there is one wallet, open it by default.
                doOpenWallet(walletList.get(0).mPath);
                finish();
            }
            else {
                updateWalletTable(walletList);
            }
        }
	}

	@Override
    protected void onResume() {
        super.onResume();

        List<WalletApplication.WalletEntry> walletList = mApp.listWallets();
        updateWalletTable(walletList);

        mLogger.info("LobbyActivity resumed");
    }

    public void updateWalletTable(List<WalletApplication.WalletEntry> walletList) {
        TableLayout table =
            (TableLayout) findViewById(R.id.lobby_table);

        table.removeAllViews();
                
        for (WalletApplication.WalletEntry entry : walletList) {
            TableRow row =
                (TableRow) LayoutInflater.from(this)
                .inflate(R.layout.lobby_table_row, table, false);

            // Setup the wallet button.
            Button wb = (Button) row.findViewById(R.id.wallet_button);
            wb.setText(entry.mName);
            wb.setTag(entry.mPath);

            // Setup the wallet entry edit button.
            Button eb = (Button) row.findViewById(R.id.edit_button);
            eb.setTag(entry.mPath);

            table.addView(row);
        }
    }

    public void openWallet(View view) {
        String path = (String) view.getTag();
        mApp.makeWalletDirectory(path);
        doOpenWallet(path);
    }

    public void editWallet(View view) {
        final String path = (String) view.getTag();
        mLogger.info(String.format("edit %s", path));

        AlertDialog.Builder alertDialogBuilder =
            new AlertDialog.Builder(this);

        LayoutInflater li = LayoutInflater.from(this);
        View editDialog = li.inflate(R.layout.dialog_edit_wallet, null);

        alertDialogBuilder.setView(editDialog);

        final EditText editName =
            (EditText) editDialog.findViewById(R.id.edit_name);

        String name = mApp.walletName(path);
        editName.setText(name);
        editName.setSelection(editName.getText().length());

        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton(R.string.lobby_edit_apply,
                               new DialogInterface.OnClickListener() {
                                   public void onClick
                                       (DialogInterface dialog,int id) {
                                       String newName =
                                           editName.getText().toString();
                                       doRenameWallet(path, newName);
                                   }
                               })
            .setNegativeButton(R.string.lobby_edit_cancel,
                               new DialogInterface.OnClickListener() {
                                   public void onClick
                                       (DialogInterface dialog,int id) {
                                       // Do nothing.
                                   }
                               });
 
        // If this isn't the last wallet we can delete it.
        List<WalletApplication.WalletEntry> walletList = mApp.listWallets();
        if (walletList.size() > 1) {
            alertDialogBuilder
                .setNeutralButton(R.string.lobby_edit_delete,
                                  new DialogInterface.OnClickListener() {
                                      public void onClick
                                          (DialogInterface dialog,int id) {
                                          doConfirmDelete(path);
                                      }
                                  });
        }

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
 
        // show it
        alertDialog.show();
    }

    public void doRenameWallet(String path, String newName) {
        mLogger.info("doRenameRallet " + path + " to " + newName);
        mApp.renameWallet(path, newName);
        List<WalletApplication.WalletEntry> walletList = mApp.listWallets();
        updateWalletTable(walletList);
    }

    public void doConfirmDelete(final String path) {
        mLogger.info("doConfirmDelete " + path);

        Resources res = getApplicationContext().getResources();

        List<WalletApplication.WalletEntry> walletList = mApp.listWallets();
        if (walletList.size() == 1) {
            String msg = res.getString(R.string.lobby_nodelete_last);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        String name = mApp.walletName(path);

        AlertDialog.Builder alertDialogBuilder =
            new AlertDialog.Builder(this);

        String msg = res.getString(R.string.lobby_confirm_delete, name);

        alertDialogBuilder
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(R.string.lobby_edit_delete,
                               new DialogInterface.OnClickListener() {
                                   public void onClick
                                       (DialogInterface dialog,int id) {
                                       doDeleteWallet(path);
                                   }
                               })
            .setNegativeButton(R.string.lobby_edit_cancel,
                               new DialogInterface.OnClickListener() {
                                   public void onClick
                                       (DialogInterface dialog,int id) {
                                       // Do nothing.
                                   }
                               });
 
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
 
        // show it
        alertDialog.show();
    }

    public void doDeleteWallet(String path) {
        mLogger.info("doDeleteWallet " + path);

        // Make sure we aren't deleting the last wallet.
        List<WalletApplication.WalletEntry> walletList = mApp.listWallets();
        if (walletList.size() == 1) {
            Resources res = getApplicationContext().getResources();
            String msg = res.getString(R.string.lobby_nodelete_last);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
        else {
            mApp.deleteWallet(path);
            walletList = mApp.listWallets();
            updateWalletTable(walletList);
        }
    }

    public void doOpenWallet(String walletPath) {

        mApp.setWalletDirName(walletPath);

        mApp.setEntered();

        File walletFile = mApp.getHDWalletFile(null);
        if (walletFile.exists()) {

            mLogger.info("Existing wallet found");

            Intent intent = new Intent(this, PasscodeActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("action", "login");
            intent.putExtras(bundle);
            startActivity(intent);

        } else {

            mLogger.info("No existing wallet");

            Intent intent = new Intent(this, CreateRestoreActivity.class);
            startActivity(intent);
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby_actions, menu);
		return true;
	}
}
