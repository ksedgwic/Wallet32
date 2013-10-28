package com.bonsai.androidelectrum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;

public class WalletApplication extends Application {
	@Override
	public void onCreate()
	{
        super.onCreate();

        Logger logger =
            LoggerFactory.getLogger(WalletApplication.class);
        logger.info("WalletApplication started");
    }
}
