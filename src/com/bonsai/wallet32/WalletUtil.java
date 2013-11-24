// Copyright (C) 2013  Bonsai Software, Inc.
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

import java.security.SecureRandom;

import android.content.Context;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;

public class WalletUtil {

    public static void createWallet(Context context) {
        NetworkParameters params = MainNetParams.get();

        String filePrefix = "wallet32";

        // Generate a new seed.
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[16];
        random.nextBytes(seed);

        int numAccounts = 2;

        // Setup a wallet with the seed.
        HDWallet hdwallet = new HDWallet(params,
                                         context.getFilesDir(),
                                         filePrefix,
                                         seed,
                                         numAccounts);
        hdwallet.persist();
    }
}
