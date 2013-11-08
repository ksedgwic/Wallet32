package com.bonsai.androidelectrum;

import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;

public class WalletService extends Service
{
    public enum State {
        SETUP,
        START,
        SYNCING,
        READY,
        ERROR
    }

    private Logger mLogger;
    private LocalBroadcastManager mLBM;

    private final IBinder mBinder = new WalletServiceBinder();

    private State				mState;
    private MyWalletAppKit		mKit;
    private NetworkParameters	mParams;
    private SetupWalletTask		mTask;
    private Context				mContext;
    private Resources			mRes;
    private int					mPercentDone = 0;

    private HDWallet			mHDWallet = null;

    private RateUpdater			mRateUpdater;

    private DownloadListener mDownloadListener =
        new DownloadListener() {
            protected void progress(double pct, int blocksToGo, Date date) {
                mLogger.info(String.format("CHAIN DOWNLOAD %d%% DONE WITH %d BLOCKS TO GO", (int) pct, blocksToGo));
                if (mPercentDone != (int) pct) {
                    mPercentDone = (int) pct;
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

                Intent intent = new Intent("wallet-state-changed");
                mLBM.sendBroadcast(intent);
            }
        };

    private class SetupWalletTask extends AsyncTask<Void, Void, Void>
    {
		@Override
		protected Void doInBackground(Void... arg0)
        {
            mLogger.info("getting network parameters");

            mParams = MainNetParams.get();

            String filePrefix = "android-electrum";

            mLogger.info("creating new wallet app kit");

            byte[] seed = Hex.decode("4a34f8fe74f81723ab07ff1d73af91e2");
            mHDWallet = new HDWallet(mParams, seed);

            mKit =
                new MyWalletAppKit(mParams,
                                   mContext.getFilesDir(),
                                   filePrefix,
                                   mDownloadListener)
                {
                    @Override
                    protected void onSetupCompleted() {
                        mLogger.info("adding keys");
                        mHDWallet.addAllKeys(wallet());
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

            // Listen for future wallet changes.
            mKit.wallet().addEventListener(mWalletListener);

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
        mLogger = LoggerFactory.getLogger(WalletService.class);
        mLBM = LocalBroadcastManager.getInstance(this);

        mLogger.info("WalletService created");

        mContext = getApplicationContext();
        mRes = mContext.getResources();

        BitStampRateUpdater bsru =
            new BitStampRateUpdater(getApplicationContext());
        bsru.start();
        mRateUpdater = bsru;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mTask = new SetupWalletTask();
        mTask.execute();

        mLogger.info("WalletService started");

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
    }

    public State getState() {
        return mState;
    }

    public String getStateString() {
        switch (mState) {
        case SETUP:
            return mRes.getString(R.string.network_status_setup);
        case START:
            return mRes.getString(R.string.network_status_start);
        case SYNCING:
            return mRes.getString(R.string.network_status_sync,
                                        mPercentDone);
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

    public double getRate() {
        return mRateUpdater.getRate();
    }

    public String getCode() {
        return mRateUpdater.getCode();
    }

    static public double getDefaultFee() {
        final BigInteger dmtf = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
        return dmtf.doubleValue() / 1e8;
    }

    public List<Balance> getBalances() {
        if (mHDWallet == null)
            return null;

        List<Balance> balances = new LinkedList<Balance>();
        mHDWallet.getBalances(balances);
        return balances;
    }

    public void sendCoinsFromAccount(int acctnum,
                                     String address,
                                     double amount,
                                     double fee) throws RuntimeException {
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

    private void setState(State newstate) {
        mState = newstate;
        sendStateChanged();
    }

    private void sendStateChanged() {
        Intent intent = new Intent("wallet-state-changed");
        mLBM.sendBroadcast(intent);
    }
}
