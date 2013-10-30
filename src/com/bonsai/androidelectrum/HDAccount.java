package com.bonsai.androidelectrum;

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

        mReceiveChain = new HDChain(params, mAccountKey, true, "Receive", 8);
        mChangeChain = new HDChain(params, mAccountKey, false, "Change", 8);
    }

    public void addAllKeys(Wallet wallet) {
        mReceiveChain.addAllKeys(wallet);
        mChangeChain.addAllKeys(wallet);
    }
}
