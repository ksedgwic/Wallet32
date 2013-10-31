package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

public class HDAddress {

    private Logger mLogger;

    private NetworkParameters	mParams;
    private DeterministicKey	mAddrKey;
    private ECKey				mECKey;
    private byte[]				mPubKey;
    private byte[]				mPubKeyHash;
    private Address				mAddress;

    private int				mNumTrans;
    private BigInteger		mBalance;

    public HDAddress(NetworkParameters params,
                     DeterministicKey chainKey,
                     int addrnum) {

        mLogger = LoggerFactory.getLogger(HDAddress.class);

        mParams = params;
        mAddrKey = HDKeyDerivation.deriveChildKey(chainKey, addrnum);
        mECKey = mAddrKey.toECKey();
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(mParams);

        mNumTrans = 0;
        mBalance = BigInteger.ZERO;

        mLogger.info("created address " + mAddrKey.getPath() + ": " +
                     mAddress.toString() + " " +
                     mECKey.getPrivateKeyEncoded(mParams).toString());
    }

    public void addKey(Wallet wallet) {

        // Set the creation time to Tue Oct 15 11:18:03 PDT 2013
        mECKey.setCreationTimeSeconds(1381861127);

        wallet.addKey(mECKey);
    }

    public boolean isMatch(byte[] pubkey, byte[] pubkeyhash) {
        if (pubkey != null)
            return Arrays.equals(pubkey, mPubKey);
        else if (pubkeyhash != null)
            return Arrays.equals(pubkeyhash, mPubKeyHash);
        else
            return false;
    }

    public void applyOutput(byte[] pubkey,
                            byte[] pubkeyhash,
                            BigInteger value) {

        // Does this output apply to this address?
        if (!isMatch(pubkey, pubkeyhash))
            return;

        ++mNumTrans;
        mBalance = mBalance.add(value);

        mLogger.info(mAddrKey.getPath() + " matched output of " +
                     value.toString());
    }

    public void applyInput(byte[] pubkey, BigInteger value) {
        // Does this input apply to this address?
        if (!Arrays.equals(pubkey, mPubKey))
            return;

        ++mNumTrans;
        mBalance = mBalance.subtract(value);

        mLogger.info(mAddrKey.getPath() + " matched input of " +
                     value.toString());
    }

    public void clearBalance() {
        mNumTrans = 0;
        mBalance = BigInteger.ZERO;
    }

    public BigInteger balance() {
        return mBalance;
    }

    public void logBalance() {
        if (mNumTrans > 0) {
            mLogger.info(mAddrKey.getPath() + " " +
                         Integer.toString(mNumTrans) + " " +
                         mBalance.toString());
        }
    }

    public boolean isUnused() {
        return mNumTrans == 0;
    }

    public Address getAddress() {
        return mAddress;
    }
}
