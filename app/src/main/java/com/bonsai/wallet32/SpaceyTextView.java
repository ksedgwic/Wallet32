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

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ScaleXSpan;
import android.util.AttributeSet;
import android.widget.TextView;


public class SpaceyTextView extends TextView {
    private CharSequence originalText = "";


    public SpaceyTextView(Context context) {
        super(context);
    }

    public SpaceyTextView(Context context, AttributeSet attrs){
        super(context, attrs);
        originalText = super.getText();
        applySpacey();
        this.invalidate();
    }

    public SpaceyTextView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        originalText = text;
        applySpacey();
    }

    @Override
    public CharSequence getText() {
        return originalText;
    }

    private void applySpacey() {
        StringBuilder builder = new StringBuilder(originalText);
        SpannableString finalText = new SpannableString(builder.toString());
        if (builder.toString().length() > 1) {
            for (int ii = 1; ii < builder.toString().length(); ++ii) {
                if (builder.charAt(ii) == '\u202F')
                    finalText.setSpan(new ScaleXSpan((float) 0.35),
                                      ii, ii+1,
                                      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        super.setText(finalText, BufferType.SPANNABLE);
    }
}
