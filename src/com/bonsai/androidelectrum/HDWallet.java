package com.bonsai.androidelectrum;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.script.Script;

public class HDWallet {

    private static Logger mLogger = LoggerFactory.getLogger(HDWallet.class);

    private final NetworkParameters	mParams;
    private final File				mDirectory;
    private final String			mFilePrefix;
    private final byte[]			mSeed;
    private final DeterministicKey	mMasterKey;

    private ArrayList<HDAccount>	mAccounts;

    public static String persistPath(String filePrefix) {
        return filePrefix + ".hdwallet";
    }

    // Create an HDWallet from persisted file data.
    public static HDWallet restore(NetworkParameters params,
                                   File directory,
                                   String filePrefix) {

        String path = persistPath(filePrefix);
        mLogger.info("restoring HDWallet from " + path);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(new File(directory, path));
            return new HDWallet(params, directory, filePrefix, node);
        } catch (JsonProcessingException ex) {
            mLogger.warn("trouble parsing JSON: " + ex.toString());
        } catch (IOException ex) {
            mLogger.warn("trouble reading " + path + ": " + ex.toString());
        } catch (RuntimeException ex) {
            mLogger.warn("trouble restoring wallet: " + ex.toString());
        }
        return null;
    }

    public HDWallet(NetworkParameters params,
                    File directory,
                    String filePrefix,
                    JsonNode walletNode)
        throws RuntimeException {

        mParams = params;
        mDirectory = directory;
        mFilePrefix = filePrefix;

        try {
			mSeed = Base58.decode(walletNode.path("seed").textValue());
		} catch (AddressFormatException e) {
            throw new RuntimeException("couldn't decode seed value");
		}

        mMasterKey = HDKeyDerivation.createMasterPrivateKey(mSeed);

        mLogger.info("restored HDWallet " + mMasterKey.getPath());

        mAccounts = new ArrayList<HDAccount>();
        Iterator<JsonNode> it = walletNode.path("accounts").iterator();
        while (it.hasNext()) {
            JsonNode acctNode = it.next();
            mAccounts.add(new HDAccount(mParams, mMasterKey, acctNode));
        }
    }

    public HDWallet(NetworkParameters params,
                    File directory,
                    String filePrefix,
                    byte[] seed) {
        
        mParams = params;
        mDirectory = directory;
        mFilePrefix = filePrefix;
        mSeed = seed;

        mMasterKey = HDKeyDerivation.createMasterPrivateKey(seed);

        mLogger.info("created HDWallet " + mMasterKey.getPath());

        // Add some accounts.
        mAccounts = new ArrayList<HDAccount>();
        mAccounts.add(new HDAccount(mParams, mMasterKey, "Account0", 0));
        mAccounts.add(new HDAccount(mParams, mMasterKey, "Account1", 1));
        mAccounts.add(new HDAccount(mParams, mMasterKey, "Account2", 2));
    }

    public void addAllKeys(Wallet wallet) {
        for (HDAccount acct : mAccounts)
            acct.addAllKeys(wallet);
    }

    public void applyAllTransactions(Iterable<WalletTransaction> iwt) {
        // Clear the balance and tx counters.
        for (HDAccount acct : mAccounts)
            acct.clearBalance();

        for (WalletTransaction wtx : iwt) {
            // WalletTransaction.Pool pool = wtx.getPool();
            Transaction tx = wtx.getTransaction();

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
                        hda.applyOutput(pubkey, pubkeyhash, value);
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

        // Log balance summary.
        for (HDAccount acct : mAccounts)
            acct.logBalance();
    }

    public void getBalances(List<Balance> balances) {
        for (HDAccount acct : mAccounts)
            balances.add(new Balance(acct.getId(),
                                     acct.getName(),
                                     acct.balance().doubleValue() / 1e8));
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
        req.changeAddress = acct.nextChangeAddress();
        req.coinSelector = acct.coinSelector();

        SendResult result = wallet.sendCoins(req);
        if (result == null)
            throw new RuntimeException("Not enough BTC in account");
    }

    public void persist() {
        String path = persistPath(mFilePrefix);
        String tmpPath = path + ".tmp";
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object obj = dumps();
            File tmpFile = new File(mDirectory, tmpPath);
            if (tmpFile.exists())
                tmpFile.delete();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(tmpFile, obj);
            File newFile = new File(mDirectory, path);
            if (!tmpFile.renameTo(newFile))
                mLogger.warn("failed to rename to " + newFile);
            else
                mLogger.info("persisted to " + path);
        } catch (JsonProcessingException ex) {
            mLogger.warn("failed generating JSON: " + ex.toString());
        } catch (IOException ex) {
            mLogger.warn("failed to write to " + tmpPath + ": " + ex.toString());
		}
    }

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("seed", Base58.encode(mSeed));

        List<Object> acctList = new ArrayList<Object>();
        for (HDAccount acct : mAccounts)
            acctList.add(acct.dumps());
        obj.put("accounts", acctList);

        return obj;
    }

    // Ensure that there are enough spare addresses on all chains.
    public void ensureMargins(Wallet wallet) {
        for (HDAccount acct : mAccounts)
            acct.ensureMargins(wallet);
    }
}
