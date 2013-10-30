package com.bonsai.androidelectrum;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

public class HDWallet {

    private Logger mLogger;

    private NetworkParameters	mParams;
    private DeterministicKey	mMasterKey;

    private ArrayList<HDAccount>	mAccounts;

    public HDWallet(NetworkParameters params, byte[] seed) {
        
        mLogger = LoggerFactory.getLogger(HDWallet.class);

        mParams = params;
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
}
