// Copyright (C) 2014  Bonsai Software, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.widget.ImageView;

public class ShowPairingActivity extends BaseWalletActivity {
    private NfcManager nfcManager;

    private static Logger mLogger =
        LoggerFactory.getLogger(ShowPairingActivity.class);

    private boolean			mSeedFetched;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        mSeedFetched = false;

	super.onCreate(savedInstanceState);
	this.nfcManager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);

        mLogger.info("ShowPairingActivity created");

		setContentView(R.layout.activity_show_pairing);
	}

    public void onPause() {
	Nfc.unpublish(nfcManager, this);
	super.onPause();
    }


	@Override
    protected void onWalletServiceBound() {
        String pairingCode = mWalletService.getPairingCode();

        final int size =
            (int) (240 * getResources().getDisplayMetrics().density);

        Bitmap bm = WalletUtil.createBitmap(pairingCode, size);
        if (bm != null) {
            ImageView iv = (ImageView) findViewById(R.id.pairing_qr_view);
            iv.setImageBitmap(bm);
        }
	final boolean nfcSuccess = Nfc.publishMimeObject(nfcManager, this, Nfc.MIMETYPE_WALLET32PAIRING, pairingCode.getBytes(Nfc.UTF_8));
    }
}
