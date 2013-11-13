package com.bonsai.wallet32;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class BitStampRateUpdater extends Thread implements RateUpdater {

    private Logger mLogger;

    private double mRate = 0.0;
    private String mCode = "USD";
    private LocalBroadcastManager mLBM;

    public BitStampRateUpdater(Context context) {
        mLogger = LoggerFactory.getLogger(BitStampRateUpdater.class);
        mLBM = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {
        while (true) {
            double rate = fetchLatestRate();

            // Did the rate change?
            if (mRate != rate) {
                mLogger.info(String.format("rate changed to %f", rate));
                mRate = rate;
                Intent intent = new Intent("rate-changed");
                mLBM.sendBroadcast(intent);
            }

            // Wait a while before doing it again.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected final String url = "https://www.bitstamp.net/api/ticker/";

    protected double fetchLatestRate() {
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
 
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream is = httpEntity.getContent();           
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            String json = sb.toString();
            JSONObject jObj = new JSONObject(json);
            double rate = jObj.getDouble("last");
            return rate;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
		return 0;
    }

    public double getRate() {
        return mRate;
    }

    public String getCode() {
        return mCode;
    }
}
