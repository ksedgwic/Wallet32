package com.bonsai.androidelectrum;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends ActionBarActivity {

    private Logger mLogger;

    private WalletService	mWalletService;

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateWalletStatus();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLogger = LoggerFactory.getLogger(MainActivity.class);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

        mLogger.info("MainActivity created");
	}

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(mMessageReceiver,
                              new IntentFilter("wallet-state-changed"));

        mLogger.info("MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(mMessageReceiver);

        mLogger.info("MainActivity paused");
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
            openSettings();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWalletStatus();
            }
        };

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
    }

    protected void openSettings()
    {
        // FIXME - Implement this.
    }

    public void sendBitcoin(View view) {
        Intent intent = new Intent(this, SendBitcoinActivity.class);
        startActivity(intent);
    }

    public void receiveBitcoin(View view) {
        // Do something in response to button
    }

    public void exitApp(View view) {
        mLogger.info("Application Exiting");
        stopService(new Intent(this, WalletService.class));
        finish();
        System.exit(0);
    }
}
