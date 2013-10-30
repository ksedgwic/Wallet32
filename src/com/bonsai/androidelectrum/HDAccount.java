package com.bonsai.androidelectrum;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

public class HDAccount {

    private Logger mLogger;

    private NetworkParameters	mParams;
    private DeterministicKey	mAccountKey;
    private String				mAccountName;

    private HDChain				mReceiveChain;
    private HDChain				mChangeChain;

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     String accountName,
                     int acctnum) {

        mLogger = LoggerFactory.getLogger(HDAccount.class);

        mParams = params;
        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, acctnum);
        mAccountName = accountName;

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
}
