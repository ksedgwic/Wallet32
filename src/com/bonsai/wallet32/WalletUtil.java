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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Protos.ScryptParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import android.content.Context;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.bitcoin.params.MainNetParams;
import com.google.protobuf.ByteString;

public class WalletUtil {

    private static Logger mLogger =
        LoggerFactory.getLogger(WalletUtil.class);

    private static final String filePrefix = "wallet32";

    public static void setPasscode(Context context, String passcode) {

        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        // Create salt and write to file.
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
        secureRandom.nextBytes(salt);
        writeSalt(context, salt);

        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(passcode);

        // Set up the application context with credentials.
        wallapp.mPasscode = passcode;
        wallapp.mKeyCrypter = keyCrypter;
        wallapp.mAesKey = aesKey;
    }

    public static void createWallet(Context context) {

        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        NetworkParameters params = MainNetParams.get();

        // Generate a new seed.
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[16];
        random.nextBytes(seed);

        int numAccounts = 2;

        // Setup a wallet with the seed.
        HDWallet hdwallet = new HDWallet(params,
                                         context.getFilesDir(),
                                         filePrefix,
                                         wallapp.mKeyCrypter,
                                         wallapp.mAesKey,
                                         seed,
                                         numAccounts);
        hdwallet.persist();
    }

    public static boolean passcodeValid(Context context, String passcode) {
        byte[] salt = readSalt(context);
        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(passcode);

        // Can we parse our wallet file?
        try {
            HDWallet.deserialize(context.getFilesDir(),
                                 filePrefix, keyCrypter, aesKey);
        } catch (Exception ex) {
            mLogger.warn("passcode didn't deserialize wallet");
            return false;
        }

        // It worked so we consider it valid ...

        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        // Set up the application context with credentials.
        wallapp.mPasscode = passcode;
        wallapp.mKeyCrypter = keyCrypter;
        wallapp.mAesKey = aesKey;

        return true;
    }

    public static void writeSalt(Context context, byte[] salt) {
        mLogger.info("writing salt " + Hex.encode(salt));
        File saltFile = new File(context.getFilesDir(), "salt");
        FileOutputStream saltStream;
		try {
			saltStream = new FileOutputStream(saltFile);
			saltStream.write(salt);
			saltStream.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
    }

    public static byte[] readSalt(Context context) {
        File saltFile = new File(context.getFilesDir(), "salt");
        byte[] salt = new byte[(int) saltFile.length()];
        DataInputStream dis;
		try {
			dis = new DataInputStream(new FileInputStream(saltFile));
			dis.readFully(salt);
			dis.close();
            mLogger.info("read salt " + Hex.encode(salt));
            return salt;
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
        return null;
    }

    public static KeyCrypter getKeyCrypter(byte[] salt) {
        Protos.ScryptParameters.Builder scryptParametersBuilder =
            Protos.ScryptParameters.newBuilder()
            .setSalt(ByteString.copyFrom(salt));
        ScryptParameters scryptParameters = scryptParametersBuilder.build();
        return new KeyCrypterScrypt(scryptParameters);
    }
}
