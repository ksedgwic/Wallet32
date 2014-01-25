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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;

public class HDChain {

    private static Logger mLogger =
        LoggerFactory.getLogger(HDChain.class);

    private NetworkParameters	mParams;
    private DeterministicKey	mChainKey;
    private boolean				mIsReceive;
    private String				mChainName;

    private ArrayList<HDAddress>	mAddrs;

    private int					mMarginSize = 8;

    public HDChain(NetworkParameters params,
                   DeterministicKey accountKey,
                   JsonNode chainNode)
        throws RuntimeException {

        mParams = params;

        mChainName = chainNode.path("name").textValue();
        mIsReceive = chainNode.path("isReceive").booleanValue();

        int chainnum = mIsReceive ? 0 : 1;

        mChainKey = HDKeyDerivation.deriveChildKey(accountKey, chainnum);

        mLogger.info("created HDChain " + mChainName + ": " +
                     mChainKey.getPath());
        
        mAddrs = new ArrayList<HDAddress>();
        Iterator<JsonNode> it = chainNode.path("addrs").iterator();
        while (it.hasNext()) {
            JsonNode addrNode = it.next();
            mAddrs.add(new HDAddress(mParams, mChainKey, addrNode));
        }
    }

    public HDChain(NetworkParameters params,
                   DeterministicKey accountKey,
                   boolean isReceive,
                   String chainName,
                   int numAddrs) {

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

    public List<HDAddress> getAddresses() {
        return mAddrs;
    }

    public void gatherAllKeys(KeyCrypter keyCrypter,
                              KeyParameter aesKey,
                              long creationTime,
                              List<ECKey> keys) {
        for (HDAddress hda : mAddrs)
            hda.gatherKey(keyCrypter, aesKey, creationTime, keys);
    }

    public void applyOutput(byte[] pubkey,
                            byte[] pubkeyhash,
                            BigInteger value,
                            boolean avail) {
        for (HDAddress hda : mAddrs)
            hda.applyOutput(pubkey, pubkeyhash, value, avail);
    }

    public void applyInput(byte[] pubkey, BigInteger value) {
        for (HDAddress hda : mAddrs)
            hda.applyInput(pubkey, value);
    }

    public void clearBalance() {
        for (HDAddress hda : mAddrs)
            hda.clearBalance();
    }

    public BigInteger balance() {
        BigInteger balance = BigInteger.ZERO;
        for (HDAddress hda : mAddrs)
            balance = balance.add(hda.balance());
        return balance;
    }

    public BigInteger available() {
        BigInteger available = BigInteger.ZERO;
        for (HDAddress hda : mAddrs)
            available = available.add(hda.available());
        return available;
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
        throw new RuntimeException("no unused address available");
    }

    public boolean hasPubKey(byte[] pubkey, byte[] pubkeyhash) {
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

    private int marginSize() {
        int count = 0;
        ListIterator li = mAddrs.listIterator(mAddrs.size());
        while (li.hasPrevious()) {
            HDAddress hda = (HDAddress) li.previous();
            if (!hda.isUnused())
                return count;
            ++count;
        }
        return count;
    }

    public void ensureMargins(Wallet wallet,
                              KeyCrypter keyCrypter,
                              KeyParameter aesKey) {
        // How many unused addresses do we have at the end of the chain?
        int numUnused = marginSize();

        // Do we have an ample margin?
        if (numUnused < mMarginSize) {

            // How many addresses do we need to add?
            int numAdd = mMarginSize - numUnused;

            mLogger.info(String.format("%s expanding margin, adding %d addrs",
                                       mChainKey.getPath(), numAdd));

            // Set the new keys creation time to now.
            long now = Utils.now().getTime() / 1000;

            // Add the addresses ...
            int newSize = mAddrs.size() + numAdd;
            ArrayList<ECKey> keys = new ArrayList<ECKey>();
            for (int ii = mAddrs.size(); ii < newSize; ++ii) {
                HDAddress hda = new HDAddress(mParams, mChainKey, ii);
                mAddrs.add(hda);
                hda.gatherKey(keyCrypter, aesKey, now, keys);
            }
            mLogger.info(String.format("adding %d keys", keys.size()));
            wallet.addKeys(keys);
        }
    }
}
