package com.bonsai.androidelectrum;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;

public class WalletService extends Service
{
    private Logger mLogger;

    public WalletAppKit mKit;
    public NetworkParameters mParams;
    public SetupWalletTask mTask;

    private class SetupWalletTask extends AsyncTask<Void, Void, Void>
    {
		@Override
		protected Void doInBackground(Void... arg0)
        {
            mLogger.info("getting network parameters");

            mParams = MainNetParams.get();

            String filePrefix = "android-electrum";

            Context context = getApplicationContext();

            mLogger.info("creating new wallet app kit");

            mKit = new WalletAppKit(mParams, context.getFilesDir(), filePrefix)
                {
                    @Override
                    protected void onSetupCompleted() {
                        // This is called in a background thread after
                        // startAndWait is called, as setting up various
                        // objects can do disk and network IO that may
                        // cause UI jank/stuttering in wallet apps if it
                        // were to be done on the main thread.
                        if (wallet().getKeychainSize() < 1)
                            wallet().addKey(new ECKey());
                    }
                };

            if (mParams == RegTestParams.get()) {
                // Regression test mode is designed for testing and
                // development only, so there's no public network for it.
                // If you pick this mode, you're expected to be running a
                // local "bitcoind -regtest" instance.
                mKit.connectToLocalHost();
            }

            mLogger.info("waiting for blockchain setup");

            // Download the block chain and wait until it's done.
            mKit.startAndWait();

            mLogger.info("blockchain setup finished");

            // FIXME - Need to add Progress and Result.
			return null;
		}
    }

    public WalletService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate()
    {
        mLogger = LoggerFactory.getLogger(WalletService.class);

        mLogger.info("WalletService created");
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        mTask = new SetupWalletTask();
        mTask.execute();

        mLogger.info("WalletService started");
    }

    @Override
    public void onDestroy() {
    }
}
