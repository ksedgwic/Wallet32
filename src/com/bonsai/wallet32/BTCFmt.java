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

import java.math.BigInteger;

public class BTCFmt {

    public final static int SCALE_BTC = 8;
    public final static int SCALE_MBTC = 5;

    private int mScale;

    public BTCFmt(int scale) {
        mScale = scale;
    }

    // Returns the minimum length scaled string which maintains all
    // necessary precision.
    //
    public String amount(long amt) {

        StringBuilder sb = new StringBuilder();
        sb.append(Long.toString(amt));

        // Add left padding as necessary.
        int leftPadLen = mScale + 1 - sb.length();
        if (leftPadLen > 0)
            for (int ii = 0; ii < leftPadLen; ++ii)
                sb.insert(0, "0");

        // Insert the decimal point.
        int ndx = sb.length() - mScale;
        sb.insert(ndx, ".");

        // Remove trailing '0' characters.
        while (sb.charAt(sb.length() - 1) == '0')
            sb.deleteCharAt(sb.length() - 1);

        // If the last character is the '.', remove it too.
        if (sb.charAt(sb.length() - 1) == '.')
            sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
