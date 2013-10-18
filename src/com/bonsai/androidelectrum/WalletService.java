package com.bonsai.androidelectrum;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.params.MainNetParams;

public class WalletService extends Service {

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
    }

    @Override
    public void onDestroy() {
    }
}
