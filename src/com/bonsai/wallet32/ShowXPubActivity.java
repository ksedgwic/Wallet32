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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class ShowXPubActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(ShowXPubActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mLogger.info("ShowXPubActivity created");

		setContentView(R.layout.activity_show_xpub);

        Bundle bundle = getIntent().getExtras();

        String xpubstr = bundle.getString("xpubstr");

        TextView tv = (TextView) findViewById(R.id.xpubstr);
        tv.setText(xpubstr);

        final int size =
            (int) (240 * getResources().getDisplayMetrics().density);

        Bitmap bm = WalletUtil.createBitmap(xpubstr, size);
        if (bm != null) {
            ImageView iv = (ImageView) findViewById(R.id.xpub_qr_view);
            iv.setImageBitmap(bm);
        }
	}
}
