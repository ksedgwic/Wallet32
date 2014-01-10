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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.script.Script;

public class HDWallet {

    private static Logger mLogger = LoggerFactory.getLogger(HDWallet.class);

    private static final transient SecureRandom secureRandom = new SecureRandom();

    private final NetworkParameters	mParams;
    private final File				mDirectory;
    private final String			mFilePrefix;
    private KeyCrypter				mKeyCrypter;
    private KeyParameter			mAesKey;

    private final DeterministicKey	mMasterKey;

    private final byte[]			mWalletSeed;

    private ArrayList<HDAccount>	mAccounts;

    public static String persistPath(String filePrefix) {
        return filePrefix + ".hdwallet";
    }

    // Create an HDWallet from persisted file data.
    public static HDWallet restore(Context ctxt,
    							   NetworkParameters params,
                                   File directory,
                                   String filePrefix,
                                   KeyCrypter keyCrypter,
                                   KeyParameter aesKey) throws InvalidCipherTextException, IOException {

        JsonNode node = deserialize(directory, filePrefix,
                                    keyCrypter, aesKey);

        return new HDWallet(ctxt, params, directory, filePrefix,
                            keyCrypter, aesKey, node);
    }

    // Deserialize the wallet data.
    public static JsonNode deserialize(File directory,
                                       String filePrefix,
                                       KeyCrypter keyCrypter,
                                       KeyParameter aesKey) throws IOException, InvalidCipherTextException {

        String path = persistPath(filePrefix);
        mLogger.info("restoring HDWallet from " + path);
        try {
            File file = new File(directory, path);
            int len = (int) file.length();

            // Open persisted file.
            DataInputStream dis =
                new DataInputStream(new FileInputStream(file));

            // Read IV from file.
            byte[] iv = new byte[KeyCrypterScrypt.BLOCK_LENGTH];
			dis.readFully(iv);

            // Read the ciphertext from the file.
            byte[] cipherBytes = new byte[len - iv.length];
            dis.readFully(cipherBytes);
            dis.close();

            // Decrypt the ciphertext.
            ParametersWithIV keyWithIv =
                new ParametersWithIV(new KeyParameter(aesKey.getKey()), iv);
            BufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher
                (new CBCBlockCipher(new AESFastEngine()));
            cipher.init(false, keyWithIv);
            int minimumSize = cipher.getOutputSize(cipherBytes.length);
            byte[] outputBuffer = new byte[minimumSize];
            int length1 = cipher.processBytes(cipherBytes,
                                              0, cipherBytes.length,
                                              outputBuffer, 0);
            int length2 = cipher.doFinal(outputBuffer, length1);
            int actualLength = length1 + length2;
            byte[] decryptedBytes = new byte[actualLength];
            System.arraycopy(outputBuffer, 0, decryptedBytes, 0, actualLength);
            
            // Parse the decryptedBytes.
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(decryptedBytes);
            return node;

        } catch (JsonProcessingException ex) {
            mLogger.warn("trouble parsing JSON: " + ex.toString());
            throw ex;
        } catch (IOException ex) {
            mLogger.warn("trouble reading " + path + ": " + ex.toString());
            throw ex;
        } catch (RuntimeException ex) {
            mLogger.warn("trouble restoring wallet: " + ex.toString());
            throw ex;
        } catch (InvalidCipherTextException ex) {
            mLogger.warn("wallet decrypt failed: " + ex.toString());
            throw ex;
		}
    }

    public HDWallet(Context ctxt,
                    NetworkParameters params,
                    File directory,
                    String filePrefix,
                    KeyCrypter keyCrypter,
                    KeyParameter aesKey,
                    JsonNode walletNode) {
        mParams = params;
        mDirectory = directory;
        mFilePrefix = filePrefix;
        mKeyCrypter = keyCrypter;
        mAesKey = aesKey;

        try {
			mWalletSeed = Base58.decode(walletNode.path("seed").textValue());
		} catch (AddressFormatException e) {
            throw new RuntimeException("couldn't decode wallet seed");
		}

        byte[] hdseed;
        try {
            InputStream wis = ctxt.getAssets().open("wordlist/english.txt");
            MnemonicCode mc =
                new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);
            List<String> wordlist = mc.toMnemonic(mWalletSeed);
            hdseed = MnemonicCode.toSeed(wordlist, "");
        } catch (Exception ex) {
            throw new RuntimeException("trouble decoding seed");
        }

        mMasterKey = HDKeyDerivation.createMasterPrivateKey(hdseed);

        mLogger.info("restored HDWallet " + mMasterKey.getPath());

        mAccounts = new ArrayList<HDAccount>();
        Iterator<JsonNode> it = walletNode.path("accounts").iterator();
        while (it.hasNext()) {
            JsonNode acctNode = it.next();
            mAccounts.add(new HDAccount(mParams, mMasterKey, acctNode));
        }
    }

    public HDWallet(Context ctxt,
                    NetworkParameters params,
                    File directory,
                    String filePrefix,
                    KeyCrypter keyCrypter,
                    KeyParameter aesKey,
                    byte[] walletSeed,
                    int numAccounts) {
        mParams = params;
        mDirectory = directory;
        mFilePrefix = filePrefix;
        mKeyCrypter = keyCrypter;
        mAesKey = aesKey;
        mWalletSeed = walletSeed;

        byte[] hdseed;
        try {
            InputStream wis = ctxt.getAssets().open("wordlist/english.txt");
            MnemonicCode mc =
                new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);
            List<String> wordlist = mc.toMnemonic(mWalletSeed);
            hdseed = MnemonicCode.toSeed(wordlist, "");
        } catch (Exception ex) {
            throw new RuntimeException("trouble decoding seed");
        }

        mMasterKey = HDKeyDerivation.createMasterPrivateKey(hdseed);

        mLogger.info("created HDWallet " + mMasterKey.getPath());

        // Add some accounts.
        mAccounts = new ArrayList<HDAccount>();
        for (int ii = 0; ii < numAccounts; ++ii) {
            String acctName = String.format("Account %d", ii);
            mAccounts.add(new HDAccount(mParams, mMasterKey, acctName, ii));
        }
    }

    public void setPersistCrypter(KeyCrypter keyCrypter, KeyParameter aesKey) {
        mKeyCrypter = keyCrypter;
        mAesKey = aesKey;
    }

    public byte[] getWalletSeed() {
        return mWalletSeed;
    }

    public List<HDAccount> getAccounts() {
        return mAccounts;
    }

    public HDAccount getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    public void addAccount() {
        int ndx = mAccounts.size();
        String acctName = String.format("Account %d", ndx);
        mAccounts.add(new HDAccount(mParams, mMasterKey, acctName, ndx));
    }

    public void gatherAllKeys(boolean isRestore, List<ECKey> keys) {
        for (HDAccount acct : mAccounts)
            acct.gatherAllKeys(mKeyCrypter, mAesKey, isRestore, keys);
    }

    public void clearBalances() {
        // Clears the balance and tx counters.
        for (HDAccount acct : mAccounts)
            acct.clearBalance();
    }

    public void applyAllTransactions(Iterable<WalletTransaction> iwt) {
        // Clear the balance and tx counters.
        clearBalances();

        for (WalletTransaction wtx : iwt) {
            // WalletTransaction.Pool pool = wtx.getPool();
            Transaction tx = wtx.getTransaction();
            boolean avail = !tx.isPending();
            TransactionConfidence conf = tx.getConfidence();
            ConfidenceType ct = conf.getConfidenceType();

            // Skip dead transactions.
            if (ct != ConfidenceType.DEAD) {

                // Traverse the HDAccounts with all outputs.
                List<TransactionOutput> lto = tx.getOutputs();
                for (TransactionOutput to : lto) {
                    BigInteger value = to.getValue();
                    try {
                        byte[] pubkey = null;
                        byte[] pubkeyhash = null;
                        Script script = to.getScriptPubKey();
                        if (script.isSentToRawPubKey())
                            pubkey = script.getPubKey();
                        else
                            pubkeyhash = script.getPubKeyHash();
                        for (HDAccount hda : mAccounts)
                            hda.applyOutput(pubkey, pubkeyhash, value, avail);
                    } catch (ScriptException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                // Traverse the HDAccounts with all inputs.
                List<TransactionInput> lti = tx.getInputs();
                for (TransactionInput ti : lti) {
                    // Get the connected TransactionOutput to see value.
                    TransactionOutput cto = ti.getConnectedOutput();
                    if (cto == null) {
                        // It appears we land here when processing transactions
                        // where we handled the output above.
                        //
                        // mLogger.warn("couldn't find connected output for input");
                        continue;
                    }
                    BigInteger value = cto.getValue();
                    try {
                        byte[] pubkey = ti.getScriptSig().getPubKey();
                        for (HDAccount hda : mAccounts)
                            hda.applyInput(pubkey, value);
                    } catch (ScriptException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        // Log balance summary.
        for (HDAccount acct : mAccounts)
            acct.logBalance();
    }

    public double balanceForAccount(int acctnum) {
        // Which accounts are we considering?  (-1 means all)
        if (acctnum != -1) {
            return mAccounts.get(acctnum).balance().doubleValue() / 1e8;
        } else {
            BigInteger sum = BigInteger.ZERO;
            for (HDAccount hda : mAccounts)
                sum = sum.add(hda.balance());
            return sum.doubleValue() / 1e8;
        }
    }

    public double availableForAccount(int acctnum) {
        // Which accounts are we considering?  (-1 means all)
        if (acctnum != -1) {
            return mAccounts.get(acctnum).available().doubleValue() / 1e8;
        } else {
            BigInteger sum = BigInteger.ZERO;
            for (HDAccount hda : mAccounts)
                sum = sum.add(hda.available());
            return sum.doubleValue() / 1e8;
        }
    }

    public double amountForAccount(WalletTransaction wtx, int acctnum) {

        // This routine is only called from the View Transactions
        // activity, so it is OK if it uses all balance and not
        // available balance (since the confirmation count is shown).

        BigInteger credits = BigInteger.ZERO;
        BigInteger debits = BigInteger.ZERO;

        // Which accounts are we considering?  (-1 means all)
        ArrayList<HDAccount> accts = new ArrayList<HDAccount>();
        if (acctnum != -1) {
            accts.add(mAccounts.get(acctnum));
        } else {
            for (HDAccount hda : mAccounts)
                accts.add(hda);
        }
        
        Transaction tx = wtx.getTransaction();

        // Consider credits.
        List<TransactionOutput> lto = tx.getOutputs();
        for (TransactionOutput to : lto) {
            BigInteger value = to.getValue();
            try {
                byte[] pubkey = null;
                byte[] pubkeyhash = null;
                Script script = to.getScriptPubKey();
                if (script.isSentToRawPubKey())
                    pubkey = script.getPubKey();
                else
                    pubkeyhash = script.getPubKeyHash();
                for (HDAccount hda : accts) {
                    if (hda.hasPubKey(pubkey, pubkeyhash))
                        credits = credits.add(value);
                }
            } catch (ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Traverse the HDAccounts with all inputs.
        List<TransactionInput> lti = tx.getInputs();
        for (TransactionInput ti : lti) {
            // Get the connected TransactionOutput to see value.
            TransactionOutput cto = ti.getConnectedOutput();
            if (cto == null) {
                // It appears we land here when processing transactions
                // where we handled the output above.
                //
                // mLogger.warn("couldn't find connected output for input");
                continue;
            }
            BigInteger value = cto.getValue();
            try {
                byte[] pubkey = ti.getScriptSig().getPubKey();
                for (HDAccount hda : accts)
                    if (hda.hasPubKey(pubkey, null))
                        debits = debits.add(value);
            } catch (ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return credits.subtract(debits).doubleValue() / 1e8;
    }

    public void getBalances(List<Balance> balances) {
        for (HDAccount acct : mAccounts)
            balances.add(new Balance(acct.getId(),
                                     acct.getName(),
                                     acct.balance().doubleValue() / 1e8,
                                     acct.available().doubleValue() / 1e8));
    }

    public Address nextReceiveAddress(int acctnum) {
        // Which account are we using for this receive?
        HDAccount acct = mAccounts.get(acctnum);
        return acct.nextReceiveAddress();
    }

    public void sendAccountCoins(Wallet wallet,
                                 int acctnum,
                                 Address dest,
                                 BigInteger value,
                                 BigInteger fee) throws RuntimeException {

        // Which account are we using for this send?
        HDAccount acct = mAccounts.get(acctnum);

        SendRequest req = SendRequest.to(dest, value);
        req.fee = fee;
        req.feePerKb = BigInteger.ZERO;
        req.ensureMinRequiredFee = false;
        req.changeAddress = acct.nextChangeAddress();
        req.coinSelector = acct.coinSelector();
        req.aesKey = mAesKey;

        SendResult result = wallet.sendCoins(req);
        if (result == null)
            throw new RuntimeException("Not enough BTC in account");
    }

    public void persist() {
        String path = persistPath(mFilePrefix);
        String tmpPath = path + ".tmp";
        try {
            // Serialize into a byte array.
            ObjectMapper mapper = new ObjectMapper();
            Object obj = dumps();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            byte[] plainBytes = mapper.writeValueAsBytes(obj);

            // Generate an IV.
            byte[] iv = new byte[KeyCrypterScrypt.BLOCK_LENGTH];
            secureRandom.nextBytes(iv);

            // Encrypt the serialized data.
            ParametersWithIV keyWithIv = new ParametersWithIV(mAesKey, iv);
            BufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher
                (new CBCBlockCipher(new AESFastEngine()));
            cipher.init(true, keyWithIv);
            byte[] encryptedBytes =
                new byte[cipher.getOutputSize(plainBytes.length)];
            int length = cipher.processBytes(plainBytes, 0, plainBytes.length,
                                             encryptedBytes, 0);
            cipher.doFinal(encryptedBytes, length);

            // Ready a tmp file.
            File tmpFile = new File(mDirectory, tmpPath);
            if (tmpFile.exists())
                tmpFile.delete();

            // Write the IV followed by the data.
			FileOutputStream ostrm = new FileOutputStream(tmpFile);
			ostrm.write(iv);
            ostrm.write(encryptedBytes);
			ostrm.close();

            // Swap the tmp file into place.
            File newFile = new File(mDirectory, path);
            if (!tmpFile.renameTo(newFile))
                mLogger.warn("failed to rename to " + newFile);
            else
                mLogger.info("persisted to " + path);

        } catch (JsonProcessingException ex) {
            mLogger.warn("failed generating JSON: " + ex.toString());
        } catch (IOException ex) {
            mLogger.warn("failed to write to " + tmpPath + ": " + ex.toString());
		} catch (DataLengthException ex) {
            mLogger.warn("encryption failed: " + ex.toString());
		} catch (IllegalStateException ex) {
            mLogger.warn("encryption failed: " + ex.toString());
		} catch (InvalidCipherTextException ex) {
            mLogger.warn("encryption failed: " + ex.toString());
		}
    }

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("seed", Base58.encode(mWalletSeed));

        List<Object> acctList = new ArrayList<Object>();
        for (HDAccount acct : mAccounts)
            acctList.add(acct.dumps());
        obj.put("accounts", acctList);

        return obj;
    }

    // Ensure that there are enough spare addresses on all chains.
    public void ensureMargins(Wallet wallet) {
        for (HDAccount acct : mAccounts)
            acct.ensureMargins(wallet, mKeyCrypter, mAesKey);
    }
}
