package com.bonsai.androidelectrum;

import android.os.Handler;
import android.os.Message;

public class BitStampRateUpdater extends Thread {

    protected Handler mRateUpdateHandler;

    public BitStampRateUpdater(Handler rateUpdateHandler) {
        mRateUpdateHandler = rateUpdateHandler;
    }

    @Override
    public void run() {
        double rate = 120.00;

        while (true) {
            // FIXME - Determine the real rate here.
            rate += 0.10;

            // Update the rate in the activity.
            Message msg = mRateUpdateHandler.obtainMessage();
            msg.obj = Double.valueOf(rate);
            mRateUpdateHandler.sendMessage(msg);

            // Wait a while before doing it again.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
