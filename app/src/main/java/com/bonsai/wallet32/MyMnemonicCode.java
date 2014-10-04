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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Joiner;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.PBKDF2SHA512;

// This derived version of MnemonicCode supports older wallets that
// used a different number of rounds.

public class MyMnemonicCode extends MnemonicCode {

    public enum Version { V0_5, V0_6 }

    private static final int PBKDF2_ROUNDS_V0_5 = 4096;
    private static final int PBKDF2_ROUNDS_V0_6 = 2048;


    public MyMnemonicCode() throws IOException {
        super();
    }

    public MyMnemonicCode(InputStream wordstream,
                          String wordListDigest)
        throws IOException, IllegalArgumentException {
        super(wordstream, wordListDigest);
    }

    /**
     * Convert mnemonic word list to seed.
     */
    public static byte[] toSeed(List<String> words, String passphrase) {
        return toSeed(words, passphrase, Version.V0_6);
    }

    public static byte[] toSeed(List<String> words, String passphrase, Version version) {

        // To create binary seed from mnemonic, we use PBKDF2 function
        // with mnemonic sentence (in UTF-8) used as a password and
        // string "mnemonic" + passphrase (again in UTF-8) used as a
        // salt. Iteration count is set to 4096 and HMAC-SHA512 is
        // used as a pseudo-random function. Desired length of the
        // derived key is 512 bits (= 64 bytes).
        //
        String pass = Joiner.on(' ').join(words);
        String salt = "mnemonic" + passphrase;

        int rounds = (version == Version.V0_5) ? PBKDF2_ROUNDS_V0_5 : PBKDF2_ROUNDS_V0_6;

        return PBKDF2SHA512.derive(pass, salt, rounds, 64);
    }
}
