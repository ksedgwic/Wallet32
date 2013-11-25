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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;

public class HDAddress {

    private static Logger mLogger = 
        LoggerFactory.getLogger(HDAddress.class);

    private NetworkParameters	mParams;
    private int					mAddrNum;
    private DeterministicKey	mAddrKey;
    private byte[]				mPubBytes;
    private ECKey				mECKey;
    private byte[]				mPubKey;
    private byte[]				mPubKeyHash;
    private Address				mAddress;

    private int				mNumTrans;
    private BigInteger		mBalance;

    public HDAddress(NetworkParameters params,
                     DeterministicKey chainKey,
                     JsonNode addrNode)
        throws RuntimeException {

        mParams = params;

        mAddrNum = addrNode.path("addrNum").intValue();

        mAddrKey = HDKeyDerivation.deriveChildKey(chainKey, mAddrNum);

        // Derive ECKey.
        byte[] prvBytes = mAddrKey.getPrivKeyBytes();
        try {
            mPubBytes = Base58.decode(addrNode.path("pubBytes").textValue());
        } catch (AddressFormatException ex) {
            throw new RuntimeException("failed to decode pubByts");
        }
        
        mECKey = new ECKey(prvBytes, mPubBytes);

        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(mParams);

        // Initialize transaction count and balance.
        mNumTrans = addrNode.path("numTrans").intValue();
        mBalance = addrNode.path("balance").bigIntegerValue();

        mLogger.info("created address " + mAddrKey.getPath() + ": " +
                     mAddress.toString() + " " +
                     mECKey.getPrivateKeyEncoded(mParams).toString());
    }

    public HDAddress(NetworkParameters params,
                     DeterministicKey chainKey,
                     int addrnum) {

        mParams = params;
        mAddrNum = addrnum;

        mAddrKey = HDKeyDerivation.deriveChildKey(chainKey, addrnum);

        // Derive ECKey.
        byte[] prvBytes = mAddrKey.getPrivKeyBytes();
        mPubBytes = mAddrKey.getPubKeyBytes(); // Expensive, save.
        mECKey = new ECKey(prvBytes, mPubBytes);

        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(mParams);

        // Initialize transaction count and balance.
        mNumTrans = 0;
        mBalance = BigInteger.ZERO;

        mLogger.info("created address " + mAddrKey.getPath() + ": " +
                     mAddress.toString() + " " +
                     mECKey.getPrivateKeyEncoded(mParams).toString());
    }

    public void addKey(Wallet wallet,
                       KeyCrypter keyCrypter,
                       KeyParameter aesKey) {

        // NOTE - we can do much better here.  There are two cases:
        //
        // 1) When we are creating a new key for the first time the
        //    current time is the right value.
        //
        // 2) If we are scanning to restore a wallet from seed then
        //    we need to search from the earliest possible wallet
        //    existence time.
        //
        // For now, set the creation time to Tue Oct 15 11:18:03 PDT 2013
        //
        mECKey.setCreationTimeSeconds(1381861127);
        wallet.addKey(mECKey.encrypt(keyCrypter, aesKey));
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

        mLogger.debug(mAddrKey.getPath() + " matched output of " +
                      value.toString());
    }

    public void applyInput(byte[] pubkey, BigInteger value) {
        // Does this input apply to this address?
        if (!Arrays.equals(pubkey, mPubKey))
            return;

        ++mNumTrans;
        mBalance = mBalance.subtract(value);

        mLogger.debug(mAddrKey.getPath() + " matched input of " +
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

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("addrNum", mAddrNum);
        obj.put("pubBytes", Base58.encode(mPubBytes));
        obj.put("numTrans", Integer.valueOf(mNumTrans));
        obj.put("balance", mBalance);

        return obj;
    }
}
