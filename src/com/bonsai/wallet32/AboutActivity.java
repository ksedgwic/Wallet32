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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends BaseWalletActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(AboutActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Turn off "up" navigation since we can be called in
        // the lobby activities.
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		setContentView(R.layout.activity_about);

        TextView tv = (TextView) findViewById(R.id.about_contents);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        mLogger.info("AboutActivity created");
	}
}
