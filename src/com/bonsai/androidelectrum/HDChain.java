package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

public class HDChain {

    private Logger mLogger;

    private NetworkParameters	mParams;
    private DeterministicKey	mChainKey;
    private boolean				mIsReceive;
    private String				mChainName;

    private ArrayList<HDAddress>	mAddrs;

    public HDChain(NetworkParameters params,
                   DeterministicKey accountKey,
                   boolean isReceive,
                   String chainName,
                   int numAddrs) {

        mLogger = LoggerFactory.getLogger(HDChain.class);

        mParams = params;
        mIsReceive = isReceive;
        int chainnum = mIsReceive ? 0 : 1;
        mChainKey = HDKeyDerivation.deriveChildKey(accountKey, chainnum);
        mChainName = chainName;

        mLogger.info("created HDChain " + mChainName + ": " +
                     mChainKey.getPath());
        
        mAddrs = new ArrayList<HDAddress>();
        for (int ii = 0; ii < numAddrs; ++ii)
            mAddrs.add(new HDAddress(mParams, mChainKey, ii));
    }

    public void addAllKeys(Wallet wallet) {
        for (HDAddress hda : mAddrs)
            hda.addKey(wallet);
    }

    public void applyOutput(byte[] pubkey,
                            byte[] pubkeyhash,
                            BigInteger value) {
        for (HDAddress hda: mAddrs)
            hda.applyOutput(pubkey, pubkeyhash, value);
    }

    public void applyInput(byte[] pubkey, BigInteger value) {
        for (HDAddress hda: mAddrs)
            hda.applyInput(pubkey, value);
    }

    public void clearBalance() {
        for (HDAddress hda: mAddrs)
            hda.clearBalance();
    }

    public BigInteger balance() {
        BigInteger balance = BigInteger.ZERO;
        for (HDAddress hda: mAddrs)
            balance = balance.add(hda.balance());
        return balance;
    }

    public void logBalance() {
        for (HDAddress hda: mAddrs)
            hda.logBalance();
    }

    public Address nextUnusedAddress() {
        for (HDAddress hda : mAddrs) {
            if (hda.isUnused())
                return hda.getAddress();
        }
        return null;	// FIXME - bogus.
    }

    public boolean isInChain(byte[] pubkey, byte[] pubkeyhash) {
        for (HDAddress hda : mAddrs) {
            if (hda.isMatch(pubkey, pubkeyhash))
                return true;
        }
        return false;
    }

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("name", mChainName);
        obj.put("isReceive", Boolean.valueOf(mIsReceive));

        List<Object> addrsList = new ArrayList<Object>();
        for (HDAddress addr : mAddrs)
            addrsList.add(addr.dumps());
        obj.put("addrs", addrsList);

        return obj;
    }
}
