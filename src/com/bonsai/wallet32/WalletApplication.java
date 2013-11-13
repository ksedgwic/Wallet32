package com.bonsai.wallet32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Intent;

public class WalletApplication extends Application {

    protected Logger mLogger;

	@Override
	public void onCreate()
	{
        mLogger = LoggerFactory.getLogger(WalletApplication.class);

        startService(new Intent(this, WalletService.class));

        super.onCreate();

        mLogger.info("WalletApplication created");
    }
}
