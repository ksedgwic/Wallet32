package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewAddressActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewAddressActivity.class);

    private LocalBroadcastManager mLBM;
    private Resources mRes;

    private WalletService	mWalletService;

    private double mFiatPerBTC = 0.0;
    private double mAmount = 0.0;

    private String mURI;

	private ClipboardManager mClipboardManager;

	private final static QRCodeWriter sQRCodeWriter = new QRCodeWriter();

    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                mWalletService =
                    ((WalletService.WalletServiceBinder) binder).getService();
                mLogger.info("WalletService bound");
                updateWalletStatus(); // calls updateBalances() ...
                updateRate();
            }

            public void onServiceDisconnected(ComponentName className) {
                mWalletService = null;
                mLogger.info("WalletService unbound");
            }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mLBM = LocalBroadcastManager.getInstance(this);
        mRes = getResources();

		mClipboardManager =
            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_address);

        Intent intent = getIntent();
        String address = intent.getExtras().getString("address");
        mAmount = intent.getExtras().getDouble("amount");

        BigInteger amt =
            mAmount == 0.0 ? null : BigInteger.valueOf((int) (mAmount * 1e8));

        mURI = BitcoinURI.convertToBitcoinURI(address, amt, null, null);

        mLogger.info("uri=" + mURI);

        final int size =
            (int) (256 * getResources().getDisplayMetrics().density);

        Bitmap bm = createBitmap(mURI, size);
        if (bm != null) {
            ImageView iv = (ImageView) findViewById(R.id.address_qr_view);
            iv.setImageBitmap(bm);
        }

        TextView idtv = (TextView) findViewById(R.id.address);
        idtv.setText(address);

        updateAmount();

        mLogger.info("ViewAddressActivity created");
	}

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WalletService.class), mConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY);

        mLBM.registerReceiver(mWalletStateChangedReceiver,
                              new IntentFilter("wallet-state-changed"));
        mLBM.registerReceiver(mRateChangedReceiver,
                              new IntentFilter("rate-changed"));

        mLogger.info("ViewAddressActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        mLBM.unregisterReceiver(mWalletStateChangedReceiver);
        mLBM.unregisterReceiver(mRateChangedReceiver);

        mLogger.info("ViewAddressActivity paused");
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

    private BroadcastReceiver mWalletStateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWalletStatus();
            }
        };

    private BroadcastReceiver mRateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateRate();
            }
        };

    private void updateWalletStatus() {
        if (mWalletService != null) {
            String state = mWalletService.getStateString();
            TextView tv = (TextView) findViewById(R.id.network_status);
            tv.setText(state);
        }
    }

    private void updateRate() {
        if (mWalletService != null) {
            mFiatPerBTC = mWalletService.getRate();
        }
        updateAmount();
    }

    private void updateAmount() {
        // Is the amount set?
        if (mAmount == 0.0)
            return;

        String amtstr = String.format("Amount: %.04f BTC = %.02f USD",
                                      mAmount, mAmount * mFiatPerBTC);
        TextView amttv = (TextView) findViewById(R.id.amount);
        amttv.setText(amtstr);
        
    }

    private Bitmap createBitmap(String content, final int size) {
        final Hashtable<EncodeHintType, Object> hints =
            new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix result;
		try {
			result = sQRCodeWriter.encode(content,
                                          BarcodeFormat.QR_CODE,
                                          size,
                                          size,
                                          hints);
		} catch (WriterException ex) {
            mLogger.warn("qr encoder failed: " + ex.toString());
            return null;
		}

        final int width = result.getWidth();
        final int height = result.getHeight();
        final int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++)
        {
            final int offset = y * width;
            for (int x = 0; x < width; x++)
            {
                pixels[offset + x] =
                    result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
            }
        }

        final Bitmap bitmap =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    protected void openSettings()
    {
        // FIXME - Implement this.
    }

    public void copyAddress(View view) {
		mClipboardManager.setText(mURI);
		Toast.makeText(this,
                       R.string.view_clipboard_copy,
                       Toast.LENGTH_SHORT).show();
        finish();
    }

    public void dismissView(View view) {
        finish();
    }
}
