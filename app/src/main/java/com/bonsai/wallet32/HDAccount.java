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

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.AllowUnconfirmedCoinSelector;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.DefaultCoinSelector;

public class HDAccount {

    private static Logger mLogger =
        LoggerFactory.getLogger(HDAccount.class);

    private NetworkParameters	mParams;
    private DeterministicKey	mAccountKey;
    private String				mAccountName;
    private int					mAccountId;

    private HDChain				mReceiveChain;
    private HDChain				mChangeChain;

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     JSONObject acctNode,
                     boolean isPairing,
                     HDWallet.HDStructVersion hdsv)
        throws RuntimeException, JSONException {

        mParams = params;

        mAccountName = acctNode.getString("name");
        mAccountId = acctNode.getInt("id");

        int childnum = mAccountId;
        switch (hdsv) {
        case HDSV_L0PUB:
            // Old level 0 public just uses the child number.
            break;
        case HDSV_L0PRV:
        case HDSV_STDV0:
        case HDSV_STDV1:
            // Both L0PRV and STDVx use private derivation.
            childnum |= ChildNumber.HARDENED_BIT;
            break;
        }

        mAccountKey =
            HDKeyDerivation.deriveChildKey(masterKey, childnum);

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        if (isPairing) {
            int numReceive = acctNode.getInt("nrcv");
            int numChange = acctNode.getInt("nchg");
            mReceiveChain = new HDChain(mParams, mAccountKey,
                                        true, "Receive", numReceive);
            mChangeChain = new HDChain(mParams, mAccountKey,
                                       false, "Change", numChange);
        } else {
            mReceiveChain =
                new HDChain(mParams, mAccountKey,
                            acctNode.getJSONObject("receive"));
            mChangeChain =
                new HDChain(mParams, mAccountKey,
                            acctNode.getJSONObject("change"));
        }
    }

    public JSONObject dumps(boolean isPairing) {
        try {
            JSONObject obj = new JSONObject();

            obj.put("name", mAccountName);
            obj.put("id", mAccountId);
            if (isPairing) {
                obj.put("nrcv", mReceiveChain.numAddrs());
                obj.put("nchg", mChangeChain.numAddrs());
            } else {
                obj.put("receive", mReceiveChain.dumps());
                obj.put("change", mChangeChain.dumps());
            }

            return obj;
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);	// Shouldn't happen.
        }
    }

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     String accountName,
                     int acctnum,
                     HDWallet.HDStructVersion hdsv) {

        mParams = params;
        int childnum = acctnum;
        switch (hdsv) {
        case HDSV_L0PUB:
            // Old level 0 public just uses the child number.
            break;
        case HDSV_L0PRV:
        case HDSV_STDV0:
        case HDSV_STDV1:
            // Both L0PRV and STDVx use private derivation.
            childnum |= ChildNumber.HARDENED_BIT;
            break;
        }
        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, childnum);
        mAccountName = accountName;
        mAccountId = acctnum;

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        mReceiveChain = new HDChain(mParams, mAccountKey, true, "Receive", 0);
        mChangeChain = new HDChain(mParams, mAccountKey, false, "Change", 0);
    }

    public void gatherAllKeys(KeyCrypter keyCrypter,
                              KeyParameter aesKey,
                              long creationTime,
                              List<ECKey> keys) {
        mReceiveChain.gatherAllKeys(keyCrypter, aesKey, creationTime, keys);
        mChangeChain.gatherAllKeys(keyCrypter, aesKey, creationTime, keys);
    }

    public void applyOutput(byte[] pubkey,
                            byte[] pubkeyhash,
                            long value,
                            boolean avail) {
        mReceiveChain.applyOutput(pubkey, pubkeyhash, value, avail);
        mChangeChain.applyOutput(pubkey, pubkeyhash, value, avail);
    }

    public void applyInput(byte[] pubkey, long value) {
        mReceiveChain.applyInput(pubkey, value);
        mChangeChain.applyInput(pubkey, value);
    }

    public void clearBalance() {
        mReceiveChain.clearBalance();
        mChangeChain.clearBalance();
    }

    public boolean hasPubKey(byte[] pubkey, byte[] pubkeyhash) {
        if (mReceiveChain.hasPubKey(pubkey, pubkeyhash))
            return true;

        return mChangeChain.hasPubKey(pubkey, pubkeyhash);
    }

    public String xpubstr() {
        return mAccountKey.serializePubB58();
    }

    public String getName() {
        return mAccountName;
    }

    public void setName(String name) {
        mAccountName = name;
    }

    public int getId() {
        return mAccountId;
    }

    public HDChain getReceiveChain() {
        return mReceiveChain;
    }

    public HDChain getChangeChain() {
        return mChangeChain;
    }

    public long balance() {
        long balance = 0;
        balance += mReceiveChain.balance();
        balance += mChangeChain.balance();
        return balance;
    }

    public long available() {
        long available = 0;
        available += mReceiveChain.available();
        available += mChangeChain.available();
        return available;
    }

    public void logBalance() {
        mLogger.info(mAccountName + " balance " + Long.toString(balance()) +
                     ", " + "available " + Long.toString(available()));

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

    public CoinSelector coinSelector(boolean spendUnconfirmed) {
        return new AccountCoinSelector(spendUnconfirmed);
    }

    public class AccountCoinSelector implements CoinSelector {

        private DefaultCoinSelector mDefaultCoinSelector;

        public AccountCoinSelector(boolean spendUnconfirmed) {
            mDefaultCoinSelector = spendUnconfirmed ?
                new AllowUnconfirmedCoinSelector() :
                new DefaultCoinSelector();
        }

        public CoinSelection select(Coin biTarget,
                                    List<TransactionOutput> candidates) {
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

                    if (mReceiveChain.hasPubKey(pubkey, pubkeyhash))
                        filtered.add(to);
                    else if (mChangeChain.hasPubKey(pubkey, pubkeyhash))
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

    // Returns the largest number of addresses added to a chain.
    public int ensureMargins(Wallet wallet,
                              KeyCrypter keyCrypter,
                              KeyParameter aesKey) {
        int receiveAdded =
            mReceiveChain.ensureMargins(wallet, keyCrypter, aesKey);
        int changeAdded =
            mChangeChain.ensureMargins(wallet, keyCrypter, aesKey);

        return (receiveAdded > changeAdded) ? receiveAdded : changeAdded;
    }

    // Finds an address (if present) and returns a description
    // of it's wallet location.
    public HDAddressDescription findAddress(Address addr) {
        HDAddressDescription retval;

        // Try the receive chain first.
        retval = mReceiveChain.findAddress(addr);

        // If it wasn't there try the change chain.
        if (retval == null)
            retval = mChangeChain.findAddress(addr);

        // If we found this address update the hdAccount.
        if (retval != null)
            retval.setHDAccount(this);

        return retval;
    }
}
