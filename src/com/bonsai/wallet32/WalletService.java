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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.KeyParameter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.params.MainNetParams;

public class WalletService extends Service
    implements OnSharedPreferenceChangeListener {

    public static boolean mIsRunning = false;

    private static Logger mLogger =
        LoggerFactory.getLogger(WalletService.class);

    public enum State {
        SETUP,
        START,
        SYNCING,
        READY,
        ERROR
    }

    private int NOTIFICATION = R.string.wallet_service_started;

    private NotificationManager		mNM;
    private LocalBroadcastManager	mLBM;

    private final IBinder mBinder = new WalletServiceBinder();

    private State				mState;
    private MyWalletAppKit		mKit;
    private NetworkParameters	mParams;
    private SetupWalletTask		mTask;
    private Context				mContext;
    private Resources			mRes;
    private double				mPercentDone = 0.0;
    private int					mBlocksToGo;
    private Date				mScanDate;

    private KeyCrypter			mKeyCrypter;
    private KeyParameter		mAesKey;
    private HDWallet			mHDWallet = null;

    private RateUpdater			mRateUpdater;

    private static final String mFilePrefix = "wallet32";

    private DownloadListener mDownloadListener =
        new DownloadListener() {
            protected void progress(double pct, int blocksToGo, Date date) {
                mLogger.info(String.format("CHAIN DOWNLOAD %d%% DONE WITH %d BLOCKS TO GO", (int) pct, blocksToGo));
                mBlocksToGo = blocksToGo;
                mScanDate = date;
                if (mPercentDone != pct) {
                    mPercentDone = pct;
                    setState(State.SYNCING);
                }
            }
        };

    private AbstractWalletEventListener mWalletListener =
        new AbstractWalletEventListener() {
            @Override
            public void onWalletChanged(Wallet wallet) {
                // Compute balances and transaction counts.
                Iterable<WalletTransaction> iwt =
                    mKit.wallet().getWalletTransactions();
                mHDWallet.applyAllTransactions(iwt);

                // Check to make sure we have sufficient margins.
                mHDWallet.ensureMargins(mKit.wallet());

                // Persist the new state.
                mHDWallet.persist();

                Intent intent = new Intent("wallet-state-changed");
                mLBM.sendBroadcast(intent);
            }
        };

    private class SetupWalletTask extends AsyncTask<Boolean, Void, Void> {
		@Override
		protected Void doInBackground(Boolean... params)
        {
			Boolean useCheckpoint = params[0];
            WalletApplication wallapp = (WalletApplication) mContext;

            mLogger.info("getting network parameters");

            mParams = MainNetParams.get();

            // Try to restore existing wallet.
            mHDWallet = null;
            try {
				mHDWallet = HDWallet.restore(mParams,
				                             mContext.getFilesDir(),
				                             mFilePrefix, mKeyCrypter, mAesKey);
			} catch (InvalidCipherTextException ex) {
                mLogger.error("wallet restore failed: " + ex.toString());
			} catch (IOException ex) {
                mLogger.error("wallet restore failed: " + ex.toString());
			}

            if (mHDWallet == null) {

                mLogger.error("WalletService started with bad HDWallet");
                System.exit(0);
            }

            mLogger.info("creating new wallet app kit");

            // Checkpointing fails on rescan because the earliest
            // create time is earlier then the genesis block time.
            //
            InputStream chkpntis = null;
            if (useCheckpoint) {
                try {
                    chkpntis = getAssets().open("checkpoints");
                } catch (IOException e) {
                    chkpntis = null;
                }
            }

            mKit = new MyWalletAppKit(mParams,
                                      mContext.getFilesDir(),
                                      mFilePrefix,
                                      chkpntis,
                                      wallapp.mKeyCrypter,
                                      mDownloadListener)
                {
                    @Override
                    protected void onSetupCompleted() {
                        mLogger.info("adding keys");

                        // Add all the existing keys, they'll be
                        // ignored if they are already in the
                        // WalletAppKit.
                        //
                        mHDWallet.addAllKeys(wallet());

                        // Do we have enough margin on all our chains?
                        // Add keys to chains which don't have enough
                        // unused addresses at the end.
                        //
                        mHDWallet.ensureMargins(wallet());
                    }
                };

            setState(State.START);

            mLogger.info("waiting for blockchain setup");

            // Download the block chain and wait until it's done.
            mKit.startAndWait();

            mLogger.info("blockchain setup finished");

            BigInteger bal0 = mKit.wallet().getBalance(BalanceType.AVAILABLE);
            BigInteger bal1 = mKit.wallet().getBalance(BalanceType.ESTIMATED);

            mLogger.info("avail balance = " + bal0.toString());
            mLogger.info("estim balance = " + bal1.toString());

            // Compute balances and transaction counts.
            Iterable<WalletTransaction> iwt =
                mKit.wallet().getWalletTransactions();
            mHDWallet.applyAllTransactions(iwt);

            // Check the margins again, since transactions may have arrived.
            mHDWallet.ensureMargins(mKit.wallet());

            // Persist the new state.
            mHDWallet.persist();

            // Listen for future wallet changes.
            mKit.wallet().addEventListener(mWalletListener);

            // Reset the rescan pref to cancel so we can use again.
            SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(SettingsActivity.KEY_RESCAN_BLOCKCHAIN, "CANCEL");
            editor.commit();

            setState(State.READY);

			return null;
		}
    }

    public WalletService() {
        mState = State.SETUP;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mLBM = LocalBroadcastManager.getInstance(this);

        mLogger.info("WalletService created");

        mContext = getApplicationContext();
        mRes = mContext.getResources();

        SharedPreferences sharedPref =
            PreferenceManager.getDefaultSharedPreferences(this);
        String fiatRateSource =
            sharedPref.getString(SettingsActivity.KEY_FIAT_RATE_SOURCE, "");
        setFiatRateSource(fiatRateSource);

        // Register for future preference changes.
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        WalletApplication wallapp = (WalletApplication) getApplicationContext();

        mKeyCrypter = wallapp.mKeyCrypter;
        mAesKey = wallapp.mAesKey;

        mTask = new SetupWalletTask();
        mTask.execute(true);

        mLogger.info("WalletService started");

        showNotification();

        mIsRunning = true;

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;

        // FIXME - Where does this go?  Anywhere?
        // stopForeground(true);

        mNM.cancel(NOTIFICATION);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(SettingsActivity.KEY_FIAT_RATE_SOURCE)) {
            SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
            String fiatRateSource =
                sharedPref.getString(SettingsActivity.KEY_FIAT_RATE_SOURCE, "");
            setFiatRateSource(fiatRateSource);
        }
        else if (key.equals(SettingsActivity.KEY_RESCAN_BLOCKCHAIN)) {
            SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
            String rescan =
                sharedPref.getString(SettingsActivity.KEY_RESCAN_BLOCKCHAIN, "");
            if (rescan.equals("RESCAN"))
                rescanBlockchain();
        }
    }

    // Show a notification while this service is running.
    //
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.wallet_service_started);

        Notification note = new Notification(R.drawable.ic_stat_notify,
                                             text, System.currentTimeMillis());

        Intent intent = new Intent(this, MainActivity.class);
    
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
      
        // Set the info for the views that show in the notification panel.
        note.setLatestEventInfo(this, getText(R.string.wallet_service_label),
                                text, contentIntent);

        note.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(NOTIFICATION, note);
    }

    public void persist() {
        mHDWallet.persist();
    }

    public byte[] getSeed() {
        return mHDWallet == null ? null : mHDWallet.getSeed();
    }

    private void setFiatRateSource(String src) {

        if (mRateUpdater != null) {
            mRateUpdater.stopUpdater();
            mRateUpdater = null;
        }

        if (src.equals("MTGOXUSD")) {
            mLogger.info("Switching to MtGox USD");
            mRateUpdater = new MtGoxRateUpdater(getApplicationContext());
        }
        else if (src.equals("BITSTAMPUSD")) {
            mLogger.info("Switching to BitStamp USD");
            mRateUpdater = new BitStampRateUpdater(getApplicationContext());
        }
        else {
            mLogger.warn("Unknown fiat rate source " + src);
            return;
        }

        mRateUpdater.startUpdater();
    }

    private void rescanBlockchain(){
        mLogger.info("RESCAN!");

        // Make sure we are in a good state for this.
        if (mState != State.READY) {
            mLogger.warn("can't rescan until the wallet is ready");
            return;
        }

        // Remove our wallet event listener.
        mKit.wallet().removeEventListener(mWalletListener);

        // Persist and remove our HDWallet.
        //
        // NOTE - It's best not to clear the balances here.  When the
        // transactions are filling in on the transactions screen it's
        // disturbing to see negative historical balances.  They'll
        // get completely refigured when the sync is done anyway ...
        //
        mHDWallet.persist();
        mHDWallet = null;

        mLogger.info("resetting wallet state");
        mKit.wallet().clearTransactions(0); 
        mKit.wallet().setLastBlockSeenHeight(-1); // magic value 
        mKit.wallet().setLastBlockSeenHash(null); 

        mLogger.info("shutting kit down");
        try {
			mKit.shutDown();
            mKit = null;
		} catch (Exception ex) {
            mLogger.error("kit shutdown failed: " + ex.toString());
            return;
		}

        mLogger.info("removing spvchain file");
        File chainFile =
            new File(mContext.getFilesDir(), mFilePrefix + ".spvchain");
        if (!chainFile.delete())
            mLogger.error("delete of spvchain file failed");

        mLogger.info("restarting wallet");
        WalletApplication wallapp = (WalletApplication) getApplicationContext();

        mKeyCrypter = wallapp.mKeyCrypter;
        mAesKey = wallapp.mAesKey;

        setState(State.SETUP);
        mTask = new SetupWalletTask();
        mTask.execute(false);
    }

    public State getState() {
        return mState;
    }

    public double getPercentDone() {
        return mPercentDone;
    }

    public int getBlocksToGo() {
        return mBlocksToGo;
    }

    public Date getScanDate() {
        return mScanDate;
    }

    public String getStateString() {
        switch (mState) {
        case SETUP:
            return mRes.getString(R.string.network_status_setup);
        case START:
            return mRes.getString(R.string.network_status_start);
        case SYNCING:
            return mRes.getString(R.string.network_status_sync,
                                  (int) mPercentDone);
        case READY:
            return mRes.getString(R.string.network_status_ready);
        case ERROR:
            return mRes.getString(R.string.network_status_error);
        default:
            return mRes.getString(R.string.network_status_unknown);
        }
    }

    public class WalletServiceBinder extends Binder {
        WalletService getService() {
            return WalletService.this;
        }
    }

    public NetworkParameters getParams() {
        return mParams;
    }

    public double getRate() {
        return mRateUpdater == null ? 0.0 : mRateUpdater.getRate();
    }

    public String getCode() {
        return mRateUpdater == null ? "???" : mRateUpdater.getCode();
    }

    static public double getDefaultFee() {
        final BigInteger dmtf = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
        return dmtf.doubleValue() / 1e8;
    }

    public List<HDAccount> getAccounts() {
        if (mHDWallet == null)
            return null;
        return mHDWallet.getAccounts();
    }

    public HDAccount getAccount(int accountId) {
        if (mHDWallet == null)
            return null;
        return mHDWallet.getAccount(accountId);
    }        

    public List<Balance> getBalances() {
        if (mHDWallet == null)
            return null;

        List<Balance> balances = new LinkedList<Balance>();
        mHDWallet.getBalances(balances);
        return balances;
    }

    public Iterable<WalletTransaction> getTransactions() {
        if (mHDWallet == null)
            return null;

        return mKit.wallet().getWalletTransactions();
    }

    public Address nextReceiveAddress(int acctnum){
        if (mHDWallet == null)
            return null;

        return mHDWallet.nextReceiveAddress(acctnum);
    }

    public void sendCoinsFromAccount(int acctnum,
                                     String address,
                                     double amount,
                                     double fee) throws RuntimeException {
        if (mHDWallet == null)
            return;

        try {
            Address dest = new Address(mParams, address);
            BigInteger vv = BigInteger.valueOf((int)(amount * 1e8));
            BigInteger ff = BigInteger.valueOf((int)(fee * 1e8));

            mLogger.info(String
                         .format("send coins: acct=%d, dest=%s, val=%s, fee=%s",
                                 acctnum, address, vv, ff));
            
            mHDWallet.sendAccountCoins(mKit.wallet(), acctnum, dest, vv, ff);

        } catch (WrongNetworkException ex) {
            String msg = "Address for wrong network: " + ex.getMessage();
            throw new RuntimeException(msg);
        } catch (AddressFormatException ex) {
            String msg = "Malformed bitcoin address: " + ex.getMessage();
            throw new RuntimeException(msg);
        }
    }

    public double amountForAccount(WalletTransaction wtx, int acctnum) {
        return mHDWallet.amountForAccount(wtx, acctnum);
    }

    public double balanceForAccount(int acctnum) {
        return mHDWallet.balanceForAccount(acctnum);
    }

    private void setState(State newstate) {
        mLogger.info("setState " + getStateString());
        mState = newstate;
        sendStateChanged();
    }

    private void sendStateChanged() {
        Intent intent = new Intent("wallet-state-changed");
        mLBM.sendBroadcast(intent);
    }
}
