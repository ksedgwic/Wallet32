package com.bonsai.androidelectrum;

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

import android.os.Handler;
import android.os.Message;

public class BitStampRateUpdater extends Thread {

    protected Handler mRateUpdateHandler;

    public BitStampRateUpdater(Handler rateUpdateHandler) {
        mRateUpdateHandler = rateUpdateHandler;
    }

    @Override
    public void run() {
        while (true) {
            double rate = fetchLatestRate();

            // Update the rate in the activity.
            Message msg = mRateUpdateHandler.obtainMessage();
            msg.obj = Double.valueOf(rate);
            mRateUpdateHandler.sendMessage(msg);

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
}
