package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.script.Script;

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

    public void applyAllTransactions(Iterable<WalletTransaction> iwt) {
        // Clear the balance and tx counters.
        for (HDAccount acct : mAccounts)
            acct.clearBalance();

        for (WalletTransaction wtx : iwt) {
            // WalletTransaction.Pool pool = wtx.getPool();
            Transaction tx = wtx.getTransaction();

            // Traverse the HDAccounts with all outputs.
            List<TransactionOutput> lto = tx.getOutputs();
            for (TransactionOutput to : lto) {
                BigInteger value = to.getValue();
				try {
                    byte[] pubkey = null;
                    byte[] pubkeyhash = null;
                    Script script = to.getScriptPubKey();
                    if (script.isSentToRawPubKey())
                        pubkey = script.getPubKey();
                    else
                        pubkeyhash = script.getPubKeyHash();
                    for (HDAccount hda : mAccounts)
                        hda.applyOutput(pubkey, pubkeyhash, value);
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            // Traverse the HDAccounts with all inputs.
            List<TransactionInput> lti = tx.getInputs();
            for (TransactionInput ti : lti) {
                // Get the connected TransactionOutput to see value.
                TransactionOutput cto = ti.getConnectedOutput();
                if (cto == null) {
                    // It appears we land here when processing transactions
                    // where we handled the output above.
                    //
                    // mLogger.warn("couldn't find connected output for input");
                    continue;
                }
                BigInteger value = cto.getValue();
				try {
                    byte[] pubkey = ti.getScriptSig().getPubKey();
                    for (HDAccount hda : mAccounts)
                        hda.applyInput(pubkey, value);
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }

        // Log balance summary.
        for (HDAccount acct : mAccounts)
            acct.logBalance();
    }

    public void getBalances(List<Balance> balances) {
        for (HDAccount acct : mAccounts)
            balances.add(new Balance(acct.getId(),
                                     acct.getName(),
                                     acct.balance().doubleValue() / 1e8));
    }

    public void sendAccountCoins(Wallet wallet,
                                 int acctnum,
                                 Address dest,
                                 BigInteger value,
                                 BigInteger fee) {

        // Which account are we using for this send?
        HDAccount acct = mAccounts.get(acctnum);

        SendRequest req = SendRequest.to(dest, value);
        req.fee = fee;
        req.changeAddress = acct.nextChangeAddress();
        req.coinSelector = acct.coinSelector();

        SendResult result = wallet.sendCoins(req);
    }
}
