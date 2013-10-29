package com.bonsai.androidelectrum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

public class HDWallet {

    private Logger mLogger;

    private DeterministicKey	mMasterPrv;
    private DeterministicKey	mMasterPub;

    public HDWallet(NetworkParameters params, byte[] seed) {
        
        mLogger = LoggerFactory.getLogger(WalletService.class);

        mMasterPrv = HDKeyDerivation.createMasterPrivateKey(seed);
        mMasterPub = mMasterPrv.getPubOnly();

        // Derive m/0/0 from master private key.
        DeterministicKey acctPrv =
            HDKeyDerivation.deriveChildKey(mMasterPrv, 0);
        DeterministicKey extPrv =
            HDKeyDerivation.deriveChildKey(acctPrv, 0);

        // Log nodes derived from the master private key.
        mLogger.info("From master private key:");
        logit(params, mMasterPrv);
        logit(params, acctPrv);
        logit(params, extPrv);

        // Derive m/0/0 from master public key.
        DeterministicKey acctPub =
            HDKeyDerivation.deriveChildKey(mMasterPub, 0);
        DeterministicKey extPub =
            HDKeyDerivation.deriveChildKey(acctPub, 0);

        // Log nodes derived from the master public key.
        mLogger.info("From master public key:");
        logit(params, mMasterPub);
        logit(params, acctPub);
        logit(params, extPub);
    }

    private void logit(NetworkParameters params, DeterministicKey dk) {
        mLogger.info(dk.getPath() + ": " + dk.toECKey().toAddress(params).toString());
    }
}
