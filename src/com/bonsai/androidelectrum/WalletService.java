package com.bonsai.androidelectrum;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.bitcoin.kits.WalletAppKit;

public class WalletService extends Service {

    public WalletAppKit mKit;

    public WalletService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onStart(Intent intent, int startId) {

        /*
        NetworkParameters params = MainNetParams.get();

        String filePrefix = "android-electrum";

        Context context = getApplicationContext();

        mKit = new WalletAppKit(params, context.getFilesDir(), filePrefix) {
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

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and
            // development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a
            // local "bitcoind -regtest" instance.
            mKit.connectToLocalHost();
        }

        // Download the block chain and wait until it's done.
        mKit.startAndWait();
        */


        /*
        NetworkParameters params = MainNetParams.get();
        try {
			Address addr = new Address(params,
			                           "15WXpqRvKBpNgcV9XviHANJmqoguQKthdg");
		} catch (WrongNetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AddressFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
    }

    @Override
    public void onDestroy() {
    }
}
