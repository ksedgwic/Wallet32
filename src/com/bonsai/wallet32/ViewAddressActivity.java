// Copyright (C) 2013-2014  Bonsai Software, Inc.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bonsai.wallet32;

import java.math.BigInteger;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ViewAddressActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ViewAddressActivity.class);

    private double mAmount = 0.0;

    private String mPrivateKey;
    private String mAddress;

    private String mURI;

	private ClipboardManager mClipboardManager;

	private final static QRCodeWriter sQRCodeWriter = new QRCodeWriter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		mClipboardManager =
            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_address);

        Intent intent = getIntent();
        mPrivateKey = intent.getExtras().getString("key");
        mAddress = intent.getExtras().getString("address");
        mAmount = intent.getExtras().getDouble("amount");

        BigInteger amt =
            mAmount == 0.0 ? null : BigInteger.valueOf((int) (mAmount * 1e8));

        mURI = BitcoinURI.convertToBitcoinURI(mAddress, amt, null, null);

        mLogger.info("view address uri=" + mURI);

        final int size =
            (int) (240 * getResources().getDisplayMetrics().density);

        Bitmap bm = createBitmap(mURI, size);
        if (bm != null) {
            ImageView iv = (ImageView) findViewById(R.id.address_qr_view);
            iv.setImageBitmap(bm);
        }

        TextView idtv = (TextView) findViewById(R.id.address);
        idtv.setText(mAddress);

        // When this activity is called from the receive bitcoin
        // activity we should show a simplified version; remove
        // some actions.
        //
        if (mPrivateKey == null) {
            Button keybutt = (Button) findViewById(R.id.copy_key);
            keybutt.setVisibility(View.GONE);

            Button bcibutt = (Button) findViewById(R.id.blockchain);
            bcibutt.setVisibility(View.GONE);
        }

        updateAmount();

        mLogger.info("ViewAddressActivity created");
	}

	@Override
    protected void onRateChanged() {
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

    public void copyAddress(View view) {
        mLogger.info(String.format("copying %s to the clipboard", mURI));
		mClipboardManager.setText(mURI);
		Toast.makeText(this,
                       R.string.view_clipboard_copy,
                       Toast.LENGTH_SHORT).show();
        finish();
    }

    public void copyKey(View view) {
        mLogger.info("copying private key to the clipboard");
        mClipboardManager.setText(mPrivateKey);
		Toast.makeText(this,
                       R.string.view_clipboard_copy_key,
                       Toast.LENGTH_SHORT).show();
        finish();
    }

    public void viewBlockchain(View view) {
        String url = "https://blockchain.info/address/" + mAddress;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        // I think it is useful to come back here after going to
        // blockchain.info, so we don't call finish ...
    }

    public void dismissView(View view) {
        finish();
    }
}
