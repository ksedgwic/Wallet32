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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ViewSeedActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewSeedActivity.class);

    private LocalBroadcastManager mLBM;
    // private Resources mRes;

    private WalletService	mWalletService;

    private MnemonicCoder	mCoder;

    private boolean			mSeedFetched;

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateWalletStatus();
                updateSeed();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLBM = LocalBroadcastManager.getInstance(this);
        // mRes = getResources();

        try {
			mCoder = new MnemonicCoder(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        mSeedFetched = false;

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_seed);

        mLogger.info("ViewSeedActivity created");
	}

    @SuppressLint("InlinedApi")
	@Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLBM.registerReceiver(mWalletStateChangedReceiver,
                              new IntentFilter("wallet-state-changed"));

        mLogger.info("ViewSeedActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);

        mLogger.info("ViewSeedActivity paused");
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_actions, menu);
        return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.action_settings:
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void openSettings()
    {
        // FIXME - Implement this.
    }

    private BroadcastReceiver mWalletStateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWalletStatus();
            }
        };

    private void updateWalletStatus() {
        if (mWalletService == null)
            return;

        String state = mWalletService.getStateString();
        TextView tv = (TextView) findViewById(R.id.network_status);
        tv.setText(state);

        updateSeed();
    }

    private void updateSeed() {
        if (mWalletService == null)
            return;

        // Don't need further updating after we fetched it once.
        if (mSeedFetched)
            return;

        byte[] seed = mWalletService.getSeed();
        if (seed == null)
            return;

        mLogger.info("saw seed " + new String(Hex.encode(seed)));

        mSeedFetched = true;

        TextView hextv = (TextView) findViewById(R.id.seed_hex);
        hextv.setText(new String(Hex.encode(seed)));

        StringBuilder builder = new StringBuilder();
        List<String> words = mCoder.encode(seed);
        for (int ii = 0; ii < words.size(); ii += 3) {
            if (ii != 0)
                builder.append("\n");

            builder.append(String.format("%-8s %-8s %-8s",
                                         words.get(ii),
                                         words.get(ii+1),
                                         words.get(ii+2)));
        }
        TextView mnetv = (TextView) findViewById(R.id.seed_mnemonic);
        mnetv.setText(builder.toString());
    }

    public void seedDone(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();	// All done here ...
    }
}
