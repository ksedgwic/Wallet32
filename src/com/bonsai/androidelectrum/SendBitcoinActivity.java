package com.bonsai.androidelectrum;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class SendBitcoinActivity extends ActionBarActivity {

    protected EditText mBTCAmountEditText;
    protected EditText mFiatAmountEditText;

    protected double mFiatPerBTC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_bitcoin);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFiatPerBTC = 0.0;

        mBTCAmountEditText = (EditText) findViewById(R.id.bitcoin_amount);
        mFiatAmountEditText = (EditText) findViewById(R.id.fiat_amount);

        mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);
        mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_bitcoin_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    private final TextWatcher mBTCAmountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {}

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

            @SuppressLint("DefaultLocale")
			@Override
            public void afterTextChanged(Editable ss) {
                // Avoid recursion by removing the other fields listener.
                mFiatAmountEditText.removeTextChangedListener
                    (mFiatAmountWatcher);

                String ffs;
                try {
                    double bb = Double.parseDouble(ss.toString());
                    double ff = bb * mFiatPerBTC;
                    ffs = String.format("%.2f", ff);
                } catch (final NumberFormatException ex) {
                    ffs = "";
                }
                mFiatAmountEditText.setText(ffs, TextView.BufferType.EDITABLE);

                // Restore the other fields listener.
                mFiatAmountEditText.addTextChangedListener(mFiatAmountWatcher);
            }

        };

    private final TextWatcher mFiatAmountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss,
                                          int start,
                                          int count,
                                          int after) {}

            @Override
            public void onTextChanged(CharSequence ss,
                                      int start,
                                      int before,
                                      int count) {}

            @SuppressLint("DefaultLocale")
			@Override
            public void afterTextChanged(Editable ss) {
                // Avoid recursion by removing the other fields listener.
                mBTCAmountEditText.removeTextChangedListener
                    (mBTCAmountWatcher);

                String bbs;
                try {
                    double ff = Double.parseDouble(ss.toString());
                    double bb;
                    if (mFiatPerBTC == 0.0) {
                        bbs = "";
                    }
                    else {
                        bb = ff / mFiatPerBTC;
                        bbs = String.format("%.4f", bb);
                    }
                } catch (final NumberFormatException ex) {
                    bbs = "";
                }
                mBTCAmountEditText.setText(bbs, TextView.BufferType.EDITABLE);

                // Restore the other fields listener.
                mBTCAmountEditText.addTextChangedListener(mBTCAmountWatcher);
            }
        };
}
