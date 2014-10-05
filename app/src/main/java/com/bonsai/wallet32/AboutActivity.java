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

import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends ActionBarActivity {

    private static Logger mLogger =
        LoggerFactory.getLogger(AboutActivity.class);

    private WalletApplication mApp;

    private int aboutClicks = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mApp = (WalletApplication) getApplicationContext();

        // Turn off "up" navigation since we can be called in
        // the lobby activities.
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		setContentView(R.layout.activity_about);

        TextView tv = (TextView) findViewById(R.id.about_contents);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        // Catch touches on the walrus image.
        ImageView aboutImage = (ImageView) findViewById(R.id.about_image);
        aboutImage.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        ++aboutClicks;
                        mLogger.info(String.format("%d clicks", aboutClicks));
                        if (aboutClicks == 10) {
                            toggleExperimental();
                            aboutClicks = 0;
                        }
                        
                        return true;
                    }
                    return false;
                }
            });

        mLogger.info("AboutActivity created");
	}

	@Override
    protected void onResume() {
        super.onResume();

        mLogger.info("AboutActivity resumed");

        mApp.cancelBackgroundTimeout();

        aboutClicks = 0;
    }

    @Override
    protected void onPause() {
        mLogger.info("AboutActivity paused");

        mApp.startBackgroundTimeout();

        super.onPause();
    }

    private void toggleExperimental() {
        SharedPreferences sharedPref =
            PreferenceManager.getDefaultSharedPreferences(this);

        // Fetch the experimental preference.
        Boolean isExperimental =
            sharedPref.getBoolean(SettingsActivity.KEY_EXPERIMENTAL, false);

        // Toggle the value.
        isExperimental = !isExperimental;

        // Store the modified value.
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SettingsActivity.KEY_EXPERIMENTAL, isExperimental);
        editor.commit();

        // Let the user know what happened
        Resources res = getApplicationContext().getResources();
        if (isExperimental) {
            mLogger.info("toggled experimental mode on");
            String msg = res.getString(R.string.about_exper_on);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
        else {
            mLogger.info("toggled experimental mode off");
            String msg = res.getString(R.string.about_exper_off);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
