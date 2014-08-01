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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.List;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Protos.ScryptParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.bitcoin.crypto.MnemonicCodeX;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class WalletUtil {

    private static Logger mLogger =
        LoggerFactory.getLogger(WalletUtil.class);

    private static final String filePrefix = "wallet32";

	private final static QRCodeWriter sQRCodeWriter = new QRCodeWriter();

    @SuppressLint("TrulyRandom")
	public static void setPasscode(Context context,
                                   WalletService walletService,
                                   String passcode,
                                   boolean isChange) {

        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        KeyParameter oldAesKey = wallapp.mAesKey;

        mLogger.info("setPasscode starting");

        byte[] salt;
        if (isChange) {
            // Reuse our salt (better chance of recovering if
            // we are between passcode values).
            salt = readSalt(context);
        } else {
            // Create salt and write to file.
            SecureRandom secureRandom = new SecureRandom();
            salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
            secureRandom.nextBytes(salt);
            writeSalt(context, salt);
        }

        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(passcode);

        if (isChange) {
            walletService.changePasscode(oldAesKey, keyCrypter, aesKey);
        }

        // Set up the application context with credentials.
        wallapp.mPasscode = passcode;
        wallapp.mKeyCrypter = keyCrypter;
        wallapp.mAesKey = aesKey;

        mLogger.info("setPasscode finished");
    }

    public static void createWallet(Context context) {

        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        NetworkParameters params = Constants.getNetworkParameters(context);

        // Generate a new seed.
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[16];
        random.nextBytes(seed);

        // We currently don't use BIP-0039 passphrases.
        String passphrase = "";

        int numAccounts = 2;

        // New wallets are version V0_6.
        MnemonicCodeX.Version bip39version = MnemonicCodeX.Version.V0_6;

        // Setup a wallet with the seed.
        HDWallet hdwallet = new HDWallet(wallapp,
        								 params,
                                         wallapp.mKeyCrypter,
                                         wallapp.mAesKey,
                                         seed,
                                         passphrase,
                                         numAccounts,
                                         bip39version,
                                         HDWallet.HDStructVersion.HDSV_STDV1);
        hdwallet.persist(wallapp);
    }

    public static boolean passcodeValid(Context context, String passcode) {
        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        byte[] salt = readSalt(context);
        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(passcode);

        // Can we parse our wallet file?
        try {
            HDWallet.deserialize(wallapp, keyCrypter, aesKey);
        } catch (Exception ex) {
            mLogger.warn("passcode didn't deserialize wallet");
            return false;
        }

        // It worked so we consider it valid ...

        // Set up the application context with credentials.
        wallapp.mPasscode = passcode;
        wallapp.mKeyCrypter = keyCrypter;
        wallapp.mAesKey = aesKey;

        return true;
    }

    public static void writeSalt(Context context, byte[] salt) {
        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        mLogger.info("writing salt " + new String(Hex.encode(salt)));

        File saltFile = new File(wallapp.getWalletDir(), "salt");
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
        WalletApplication wallapp =
            (WalletApplication) context.getApplicationContext();

        File saltFile = new File(wallapp.getWalletDir(), "salt");

        byte[] salt = new byte[(int) saltFile.length()];
        DataInputStream dis;
		try {
			dis = new DataInputStream(new FileInputStream(saltFile));
			dis.readFully(salt);
			dis.close();
            // mLogger.info("read salt " + new String(Hex.encode(salt)));
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

    public static byte[] msgHexToBytes(String hexstr) {
        byte[] msgbytes = Hex.decode(hexstr);
        return Utils.reverseBytes(msgbytes);
    }

    public static Bitmap createBitmap(String content, final int size) {
        final Hashtable<EncodeHintType, Object> hints =
            new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix result;
		try {
			result = sQRCodeWriter.encode(content,
                                          BarcodeFormat.QR_CODE,
                                          size,
                                          size,
                                          hints);
		} catch (WriterException ex) {
            mLogger.warn("qr encoder failed: " + ex.toString());
            return null;
		}

        final int width = result.getWidth();
        final int height = result.getHeight();
        final int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++)
        {
            final int offset = y * width;
            for (int x = 0; x < width; x++)
            {
                pixels[offset + x] =
                    result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
            }
        }

        final Bitmap bitmap =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    // Thanks to devrandom!
    public static void signTransactionInputs(Transaction tx,
                                             SigHash hashType,
                                             ECKey key,
                                             List<Script> inputScripts) throws ScriptException {

        List<TransactionInput> inputs = tx.getInputs();
        List<TransactionOutput> outputs = tx.getOutputs();
        
        checkState(inputs.size() > 0);
        checkState(outputs.size() > 0);

        checkArgument(hashType == SigHash.ALL, "Only SIGHASH_ALL is currently supported");

        // The transaction is signed with the input scripts empty
        // except for the input we are signing. In the case where
        // addInput has been used to set up a new transaction, they
        // are already all empty. The input being signed has to have
        // the connected OUTPUT program in it when the hash is
        // calculated!
        //
        // Note that each input may be claiming an output sent to a
        // different key. So we have to look at the outputs to figure
        // out which key to sign with.

        TransactionSignature[] signatures = new TransactionSignature[inputs.size()];
        ECKey[] signingKeys = new ECKey[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = inputs.get(i);
            // We don't have the connected output, we assume it was
            // signed already and move on
            if (input.getScriptBytes().length != 0)
                mLogger.warn("Re-signing an already signed transaction! Be sure this is what you want.");

            // This assert should never fire. If it does, it means the wallet is inconsistent.
            checkNotNull(key, "Transaction exists in wallet that we cannot redeem: %s",
                         input.getOutpoint().getHash());

            // Keep the key around for the script creation step below.
            signingKeys[i] = key;

            // The anyoneCanPay feature isn't used at the moment.
            boolean anyoneCanPay = false;
            signatures[i] = tx.calculateSignature(i, key, inputScripts.get(i), hashType, anyoneCanPay);
        }

        // Now we have calculated each signature, go through and
        // create the scripts. Reminder: the script consists:
        // 
        // 1) For pay-to-address outputs: a signature (over a hash of
        // the simplified transaction) and the complete public key
        // needed to sign for the connected output. The output script
        // checks the provided pubkey hashes to the address and then
        // checks the signature.
        //
        // 2) For pay-to-key outputs: just a signature.
        //
        for (int i = 0; i < inputs.size(); i++) {
                if (signatures[i] == null)
                    continue;
                TransactionInput input = inputs.get(i);
                Script scriptPubKey = inputScripts.get(i);
                if (scriptPubKey.isSentToAddress()) {

                    input.setScriptSig(ScriptBuilder.createInputScript(signatures[i],
                                                                       signingKeys[i]));
                } else if (scriptPubKey.isSentToRawPubKey()) {

                    input.setScriptSig(ScriptBuilder.createInputScript(signatures[i]));
                } else {
                    // Should be unreachable - if we don't recognize
                    // the type of script we're trying to sign for,
                    // then we should have failed above when fetching
                    // the key to sign with.
                    throw new RuntimeException("Do not understand script type: " + scriptPubKey);
                }
        }

        // Every input is now complete.
    }

    public static DeterministicKey createMasterPubKeyFromPubB58(String xpubstr)
        throws AddressFormatException
    {
        byte[] data = Base58.decodeChecked(xpubstr);
        ByteBuffer ser = ByteBuffer.wrap(data);
        if (ser.getInt() != 0x0488B21E)
            throw new AddressFormatException("bad xpub version");
        ser.get();		// depth
        ser.getInt();	// parent fingerprint
        ser.getInt();	// child number
        byte[] chainCode = new byte[32];
        ser.get(chainCode);
        byte[] pubBytes = new byte[33];
        ser.get(pubBytes);
        return HDKeyDerivation.createMasterPubKeyFromBytes(pubBytes, chainCode);
    }
}
