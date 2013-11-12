package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.CoinSelection;
import com.google.bitcoin.core.Wallet.CoinSelector;
import com.google.bitcoin.core.Wallet.DefaultCoinSelector;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.script.Script;

public class HDAccount {

    private Logger mLogger;

    private NetworkParameters	mParams;
    private DeterministicKey	mAccountKey;
    private String				mAccountName;
    private int					mAccountId;

    private HDChain				mReceiveChain;
    private HDChain				mChangeChain;

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     JsonNode acctNode)
        throws RuntimeException {

        mLogger = LoggerFactory.getLogger(HDAccount.class);

        mParams = params;

        mAccountName = acctNode.path("name").textValue();
        mAccountId = acctNode.path("id").intValue();

        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, mAccountId);

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        mReceiveChain =
            new HDChain(mParams, mAccountKey, acctNode.path("receive"));
        mChangeChain =
            new HDChain(mParams, mAccountKey, acctNode.path("change"));
    }

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     String accountName,
                     int acctnum) {

        mLogger = LoggerFactory.getLogger(HDAccount.class);

        mParams = params;
        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, acctnum);
        mAccountName = accountName;
        mAccountId = acctnum;

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        mReceiveChain = new HDChain(mParams, mAccountKey, true, "Receive", 8);
        mChangeChain = new HDChain(mParams, mAccountKey, false, "Change", 8);
    }

    public void addAllKeys(Wallet wallet) {
        mReceiveChain.addAllKeys(wallet);
        mChangeChain.addAllKeys(wallet);
    }

    public void applyOutput(byte[] pubkey,
                            byte[] pubkeyhash,
                            BigInteger value) {
        mReceiveChain.applyOutput(pubkey, pubkeyhash, value);
        mChangeChain.applyOutput(pubkey, pubkeyhash, value);
    }

    public void applyInput(byte[] pubkey, BigInteger value) {
        mReceiveChain.applyInput(pubkey, value);
        mChangeChain.applyInput(pubkey, value);
    }

    public void clearBalance() {
        mReceiveChain.clearBalance();
        mChangeChain.clearBalance();
    }

    public String getName() {
        return mAccountName;
    }

    public int getId() {
        return mAccountId;
    }

    public BigInteger balance() {
        BigInteger balance = BigInteger.ZERO;
        balance = balance.add(mReceiveChain.balance());
        balance = balance.add(mChangeChain.balance());
        return balance;
    }

    public void logBalance() {
        mLogger.info(mAccountName + " balance " + balance().toString());

        // Now log any active addresses in this account.
        mReceiveChain.logBalance();
        mChangeChain.logBalance();
    }

    public Address nextReceiveAddress() {
        return mReceiveChain.nextUnusedAddress();
    }

    public Address nextChangeAddress() {
        return mChangeChain.nextUnusedAddress();
    }

    public CoinSelector coinSelector() {
        return new AccountCoinSelector();
    }

    public class AccountCoinSelector implements CoinSelector {

        private DefaultCoinSelector mDefaultCoinSelector;

        public AccountCoinSelector() {
            mDefaultCoinSelector = new DefaultCoinSelector();
        }

        public CoinSelection select(BigInteger biTarget,
                                    LinkedList<TransactionOutput> candidates) {
            // Filter the candidates so only coins from this account
            // are considered.  Let the Wallet.DefaultCoinSelector do
            // all the remaining work.
            LinkedList<TransactionOutput> filtered =
                new LinkedList<TransactionOutput>();
            for (TransactionOutput to : candidates) {
				try {
                    byte[] pubkey = null;
                    byte[] pubkeyhash = null;
                    Script script = to.getScriptPubKey();
                    if (script.isSentToRawPubKey())
                        pubkey = script.getPubKey();
                    else
                        pubkeyhash = script.getPubKeyHash();

                    if (mReceiveChain.isInChain(pubkey, pubkeyhash))
                        filtered.add(to);
                    else if (mChangeChain.isInChain(pubkey, pubkeyhash))
                        filtered.add(to);
                    else
                        // Not in this account ...
                        continue;

				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            // Does all the real work ...
            return mDefaultCoinSelector.select(biTarget, filtered);
        }
    }

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("name", mAccountName);
        obj.put("id", mAccountId);
        obj.put("receive", mReceiveChain.dumps());
        obj.put("change", mChangeChain.dumps());

        return obj;
    }
}
