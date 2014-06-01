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
import android.content.res.Resources;


public class BTCFmt {

    public final static int SCALE_BTC = 8;
    public final static int SCALE_MBTC = 5;
    public final static int SCALE_UBTC = 2;

    private int mScale;
    private String mUnitStr;

    public BTCFmt(int scale, Context ctxt) {
        mScale = scale;
        Resources res = ctxt.getResources();
        switch (mScale) {
        case SCALE_BTC:
            mUnitStr = res.getString(R.string.app_units_btc);
            break;
        case SCALE_MBTC:
            mUnitStr = res.getString(R.string.app_units_mbtc);
            break;
        case SCALE_UBTC:
            mUnitStr = res.getString(R.string.app_units_ubtc);
            break;
        default:
            throw new RuntimeException(String.format("unknown scale %d", mScale));
        }
    }

    public String unitStr() {
        return mUnitStr;
    }

    // Returns the minimum length scaled string which maintains all
    // necessary precision.
    //
    public String format(long amt) {
        return formatInternal(mScale, amt, 0, false, false);
    }

    // Used for formatting column aligned values.
    //
    public String formatCol(long amt,
                            int reducePrecision,
                            boolean colPad,
                            boolean triSpace) {
        return formatInternal(mScale, amt, reducePrecision, colPad, triSpace);
    }

    // Always formats using the BTC units, for cases where scaled
    // units are not acceptable (eg: request urls)
    //
    public String formatBTC(long amt) {
        return formatInternal(SCALE_BTC, amt, 0, false, false);
    }

    protected String formatInternal(int scale,
                                    long amt,
                                    int reducePrecision,
                                    boolean colPad,
                                    boolean triSpace) {
        // Convert negative numbers to positive, reverse at end.
        boolean isNeg = false;
        if (amt < 0) {
            amt = -amt;
            isNeg = true;
        }

        if (reducePrecision > 0) {
            double factor = Math.pow(10, reducePrecision);
            amt  = Math.round(((double) amt) / factor);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Long.toString(amt));

        // How wide should the fractional field be?
        int fracWidth = scale - reducePrecision;

        // Add left padding as necessary.
        int leftPadLen = fracWidth + 1 - sb.length();
        if (leftPadLen > 0)
            for (int ii = 0; ii < leftPadLen; ++ii)
                sb.insert(0, "0");

        // Insert the decimal point.
        sb.insert(sb.length() - fracWidth, ".");

        // Remove trailing '0' characters.
        if (colPad) {
            // Replace trailing '0' with ' '.
            int ndx;
            for (ndx = sb.length() - 1; ndx >= 0; --ndx) {
                if (sb.charAt(ndx) == '0')
                    sb.replace(ndx, ndx+1, " ");
                else
                    break;
            }

            // If the last character is the '.', replace it too.
            if (sb.charAt(ndx) == '.')
                sb.replace(ndx, ndx+1, " ");

        } else {
            // Just remove the characters.
            while (sb.charAt(sb.length() - 1) == '0')
                sb.deleteCharAt(sb.length() - 1);

            // If the last character is the '.', remove it too.
            if (sb.charAt(sb.length() - 1) == '.')
                sb.deleteCharAt(sb.length() - 1);
        }

        if (triSpace) {
            // Insert leading tri-spaces
            int ndx = sb.indexOf(".");
            if (ndx == -1) {
                // Find the last non-space char.
                for (ndx = sb.length() - 1; ndx >= 0; ndx--)
                    if (sb.charAt(ndx) != ' ')
                        break;
                ++ndx;
            }
            while (ndx - 3 > 0) {
                ndx -= 3;
                sb.insert(ndx, ' ');
            }

            // Insert following tri-spaces
            ndx = sb.indexOf(".");
            if (ndx == -1) {
                // Find the last non-space char.
                for (ndx = sb.length() - 1; ndx >= 0; ndx--)
                    if (sb.charAt(ndx) != ' ')
                        break;
                ++ndx;
            }
            ++ndx;
            while (ndx + 3 < sb.length() - 1) {
                ndx += 3;
                sb.insert(ndx, ' ');
                ++ndx;
            }
        }

        // If we were negative prepend a "-".
        if (isNeg)
            sb.insert(0, '-');

        return sb.toString();
    }

    public long parse(String valstr) throws NumberFormatException {
        // Some countries use comma as the decimal separator.
        // Android's numberDecimal EditText fields don't handle this
        // correctly (https://code.google.com/p/android/issues/detail?id=2626).
        // As a workaround we substitute ',' -> '.' manually ...
        double dval = Double.parseDouble(valstr.toString().replace(',', '.'));
        return (long)(dval * Math.pow(10, mScale));
    }

    public double fiatAtRate(long btc, double fiatPerBTC) {
        // Fiat is always expressed per BTC, not mBTC, for example.
        final int fiatScale = 8;
        double dval = ((double) btc) / Math.pow(10, fiatScale);
        return dval * fiatPerBTC;
    }

    public long btcAtRate(double fiat, double fiatPerBTC) {
        // Fiat is always expressed per BTC, not mBTC, for example.
        final int fiatScale = 8;
        return (long) ((fiat / fiatPerBTC) * Math.pow(10, fiatScale));
    }
}
