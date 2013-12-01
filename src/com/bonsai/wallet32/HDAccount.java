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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.CoinSelection;
import com.google.bitcoin.core.Wallet.CoinSelector;
import com.google.bitcoin.core.Wallet.DefaultCoinSelector;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.script.Script;

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
                     JsonNode acctNode)
        throws RuntimeException {

        mParams = params;

        mAccountName = acctNode.path("name").textValue();
        mAccountId = acctNode.path("id").intValue();

        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, mAccountId);

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        mReceiveChain =
            new HDChain(mParams, mAccountKey, acctNode.path("receive"));
        mChangeChain =
            new HDChain(mParams, mAccountKey, acctNode.path("change"));
    }

    public HDAccount(NetworkParameters params,
                     DeterministicKey masterKey,
                     String accountName,
                     int acctnum) {

        mParams = params;
        mAccountKey = HDKeyDerivation.deriveChildKey(masterKey, acctnum);
        mAccountName = accountName;
        mAccountId = acctnum;

        mLogger.info("created HDAccount " + mAccountName + ": " +
                     mAccountKey.getPath());

        mReceiveChain = new HDChain(mParams, mAccountKey, true, "Receive", 0);
        mChangeChain = new HDChain(mParams, mAccountKey, false, "Change", 0);
    }

    public void addAllKeys(Wallet wallet,
                           KeyCrypter keyCrypter,
                           KeyParameter aesKey) {
        mReceiveChain.addAllKeys(wallet, keyCrypter, aesKey);
        mChangeChain.addAllKeys(wallet, keyCrypter, aesKey);
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

    public boolean hasPubKey(byte[] pubkey, byte[] pubkeyhash) {
        if (mReceiveChain.hasPubKey(pubkey, pubkeyhash))
            return true;

        return mChangeChain.hasPubKey(pubkey, pubkeyhash);
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

    public Address nextReceiveAddress() {
        return mReceiveChain.nextUnusedAddress();
    }

    public Address nextChangeAddress() {
        return mChangeChain.nextUnusedAddress();
    }

    public CoinSelector coinSelector() {
        return new AccountCoinSelector();
    }

    public class AccountCoinSelector implements CoinSelector {

        private DefaultCoinSelector mDefaultCoinSelector;

        public AccountCoinSelector() {
            mDefaultCoinSelector = new DefaultCoinSelector();
        }

        public CoinSelection select(BigInteger biTarget,
                                    LinkedList<TransactionOutput> candidates) {
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

    public Object dumps() {
        Map<String,Object> obj = new HashMap<String,Object>();

        obj.put("name", mAccountName);
        obj.put("id", mAccountId);
        obj.put("receive", mReceiveChain.dumps());
        obj.put("change", mChangeChain.dumps());

        return obj;
    }

    public void ensureMargins(Wallet wallet,
                              KeyCrypter keyCrypter,
                              KeyParameter aesKey) {
        mReceiveChain.ensureMargins(wallet, keyCrypter, aesKey);
        mChangeChain.ensureMargins(wallet, keyCrypter, aesKey);
    }
}
