// Copyright (C) 2013-2014  Bonsai Software, Inc.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;

public class HDAddress {

    // Tue Oct 15 11:18:03 PDT 2013
    public static final long EPOCH = 1381861127;

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
    private long			mBalance;
    private long			mAvailable;		// Available for spending.

    public HDAddress(NetworkParameters params,
                     DeterministicKey chainKey,
                     JSONObject addrNode)
        throws RuntimeException, JSONException {

        mLogger.info("CTOR starting");

        mParams = params;

        mAddrNum = addrNode.getInt("addrNum");

        mLogger.info("got addr num");

        mAddrKey = HDKeyDerivation.deriveChildKey(chainKey, mAddrNum);

        mLogger.info("derived key");

        // Derive ECKey.
        byte[] prvBytes = mAddrKey.getPrivKeyBytes();
        mLogger.info("got private bytes");

        try {
            mPubBytes = Base58.decode(addrNode.getString("pubBytes"));
        } catch (AddressFormatException ex) {
            throw new RuntimeException("failed to decode pubBytes");
        }
        
        mLogger.info("got decoded public");

        mECKey = new ECKey(prvBytes, mPubBytes);

        mLogger.info("created key");

        // Set creation time to Wallet32 epoch.
        mECKey.setCreationTimeSeconds(EPOCH);

        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(mParams);

        mLogger.info("derived stuff");

        // Initialize transaction count and balance.  If we don't have
        // a persisted available amount, presume it is all available.
        mNumTrans = addrNode.getInt("numTrans");
        mBalance = addrNode.getLong("balance");
        mAvailable = addrNode.has("available") ?
            addrNode.getLong("available") : mBalance;

        mLogger.info("read address " + mAddrKey.getPath() + ": " +
                     mAddress.toString());

        mLogger.info("CTOR finished");
    }

    public JSONObject dumps() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("addrNum", mAddrNum);
            obj.put("pubBytes", Base58.encode(mPubBytes));
            obj.put("numTrans", mNumTrans);
            obj.put("balance", mBalance);
            obj.put("available", mAvailable);

            return obj;
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);	// Shouldn't happen.
        }
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

        // Set creation time to now.
        long now = Utils.now().getTime() / 1000;
        mECKey.setCreationTimeSeconds(now);

        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(mParams);

        // Initialize transaction count and balance.
        mNumTrans = 0;
        mBalance = 0;
        mAvailable = 0;

        mLogger.info("created address " + mAddrKey.getPath() + ": " +
                     mAddress.toString());
    }

    public void gatherKey(KeyCrypter keyCrypter,
                          KeyParameter aesKey,
                          long creationTime,
                          List<ECKey> keys) {
        mECKey.setCreationTimeSeconds(creationTime);
        keys.add(mECKey.encrypt(keyCrypter, aesKey));
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
                            long value,
                            boolean avail) {

        // Does this output apply to this address?
        if (!isMatch(pubkey, pubkeyhash))
            return;

        ++mNumTrans;
        mBalance += value;

        if (avail)
            mAvailable += value;

        mLogger.debug(mAddrKey.getPath() + " matched output of " +
                      Long.toString(value));
    }

    public void applyInput(byte[] pubkey, long value) {
        // Does this input apply to this address?
        if (!Arrays.equals(pubkey, mPubKey))
            return;

        ++mNumTrans;
        mBalance -= value;
        mAvailable -= value;

        mLogger.debug(mAddrKey.getPath() + " matched input of " +
                      Long.toString(value));
    }

    public String getPath() {
        return mAddrKey.getPath();
    }

    public long getBalance() {
        return mBalance;
    }
    
    public long getAvailable() {
        return mAvailable;
    }
    
    public String getAddressString() {
        return mAddress.toString();
    }

    public String getAbbrev() {
        return mAddress.toString().substring(0, 8) + "...";
    }

    public String getPrivateKeyString() {
        return mECKey.getPrivateKeyEncoded(mParams).toString();
    }

    public int numTrans() {
        return mNumTrans;
    }

    public void clearBalance() {
        mNumTrans = 0;
        mBalance = 0;
        mAvailable = 0;
    }

    public void logBalance() {
        if (mNumTrans > 0) {
            mLogger.info(mAddrKey.getPath() + " " +
                         Integer.toString(mNumTrans) + " " +
                         Long.toString(mBalance) + " " +
                         Long.toString(mAvailable));
        }
    }

    public boolean isUnused() {
        return mNumTrans == 0;
    }

    public Address getAddress() {
        return mAddress;
    }

    public boolean matchAddress(Address addr) {
        return mAddress.toString().equals(addr.toString());
    }
}
