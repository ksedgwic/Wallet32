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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import com.google.bitcoin.crypto.KeyCrypter;

public class WalletApplication
    extends Application
    implements OnSharedPreferenceChangeListener {

    private static Logger mLogger =
        LoggerFactory.getLogger(WalletApplication.class);

    public String			mPasscode;
    public KeyCrypter		mKeyCrypter;
    public KeyParameter		mAesKey;

    public String			mIntentURI = null;

    private BTCFmt			mBTCFmt = null;

    private String			mWalletDirName;

    private WalletService	mWalletService = null;

    private long		    mPasscodeValidTimestamp = 0L;

    private boolean			mEntered = false;	// Came through lobby?
    private boolean			mLoggedIn = false;	// Past the passcode?

	@Override
	public void onCreate()
	{
        // Apply PRNGFixes.
        PRNGFixes.apply();

        initLogging();

        super.onCreate();

        // Log the About contents so we have the version string.
        mLogger.info(getResources().getString(R.string.about_contents));

        SharedPreferences sharedPref =
            PreferenceManager.getDefaultSharedPreferences(this);
        String btcUnits =
            sharedPref.getString(SettingsActivity.KEY_BTC_UNITS, "");
        setBTCUnits(btcUnits);

        // Register for future preference changes.
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        mLogger.info("WalletApplication created");
    }

    public void doExit() {
        mLogger.info("Application exiting");

        if (mWalletService != null)
            mWalletService.shutdown();

        mLogger.info("Stopping WalletService");
        stopService(new Intent(this, WalletService.class));

        // Cancel any remaining notifications.
        NotificationManager nm =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();

        // Kill everything
        mLogger.info("Exiting");
        System.exit(0);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        mLogger.info("saw pref key " + key);
        if (key.equals(SettingsActivity.KEY_BTC_UNITS)) {
            SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
            String btcUnits =
                sharedPref.getString(SettingsActivity.KEY_BTC_UNITS, "");
            setBTCUnits(btcUnits);
        }
    }

    public void setWalletService(WalletService walletService) {
        mWalletService = walletService;
    }

    public WalletService getWalletService() {
        return mWalletService;
    }

    public void startBackgroundTimeout() {
        if (mWalletService != null)
            mWalletService.startBackgroundTimeout();
    }

    public void cancelBackgroundTimeout() {
        if (mWalletService != null)
            mWalletService.cancelBackgroundTimeout();
    }

    public void setPasscodeValidTimestamp() {
        mPasscodeValidTimestamp = System.currentTimeMillis();
    }

    public boolean passcodeFreshlyEntered() {
        long delta = System.currentTimeMillis() - mPasscodeValidTimestamp;
        return delta < (5 * 60 * 1000);
    }

    public void setEntered() {
        mEntered = true;
    }

    public boolean hasEntered() {
        return mEntered;
    }
        
    public void setLoggedIn() {
        mLoggedIn = true;
    }

    public boolean isLoggedIn() {
        return mLoggedIn;
    }
        
    private void setBTCUnits(String src) {
        if (src.equals("UBTC")) {
            mLogger.info("Setting BTC units to uBTC");
            mBTCFmt = new BTCFmt(BTCFmt.SCALE_UBTC, this);
        }
        else if (src.equals("MBTC")) {
            mLogger.info("Setting BTC units to MBTC");
            mBTCFmt = new BTCFmt(BTCFmt.SCALE_MBTC, this);
        }
        else if (src.equals("BTC")) {
            mLogger.info("Setting BTC units to BTC");
            mBTCFmt = new BTCFmt(BTCFmt.SCALE_BTC, this);
        }
        else if (src.equals("")) {
            mLogger.info("Defaulting BTC units to MBTC");
            mBTCFmt = new BTCFmt(BTCFmt.SCALE_MBTC, this);
        }
        else {
            mLogger.warn("Unknown btc units " + src);
            return;
        }
    }

    public BTCFmt getBTCFmt() {
        return mBTCFmt;
    }

    public void setIntentURI(String uri) {
        mIntentURI = uri;
    }

    public String getIntentURI() {
        return mIntentURI;
    }

    public static class WalletEntry {
        public String	mName;		// User friendly name.
        public String	mPath;		// Wallet directory path name.
        public WalletEntry(String name, String path) {
            mName = name;
            mPath = path;
        }
        public JSONObject dumpJSON() throws JSONException {
            JSONObject entobj = new JSONObject();
            entobj.put("name", mName);
            entobj.put("path", mPath);
            return entobj;
        }
		public static WalletEntry loadJSON(JSONObject entobj)
            throws JSONException {
            return new WalletEntry(entobj.getString("name"),
                                   entobj.getString("path"));
        }
    }

    public List<WalletEntry> listWallets() {
        List<WalletEntry> wallets = new ArrayList<WalletEntry>();

        File walletDirFile =
            new File(getFilesDir(), getWalletPrefix() + ".walletdir");

        // Does the directory not exist?
        if (!walletDirFile.exists()) {
            // Yes, we don't have a directory file yet.  Create a
            // default one with a single entry for default wallet.
            mLogger.info("creating default wallet directory in " +
                         walletDirFile.getPath());
            wallets.add(new WalletEntry("Default", "."));
            persistWalletDirectory(wallets);
        }
        else {
            mLogger.info("reading wallet directory in " +
                         walletDirFile.getPath());
            wallets = loadWalletDirectory();
        }

        return wallets;
    }

    public int nextAvailableWallet() {
        // Find the first available wallet directory.
        for (int ndx = 1; ndx < 64; ++ndx) {
            String path = String.format("wallet%03d", ndx);
            File dir = new File(getFilesDir(), path);
            if (!dir.exists()) {
                mLogger.info("nextAvailableWallet " + path);
                return ndx;
            }
        }
        throw new RuntimeException("no wallet space available");
    }

    public void renameWallet(String path, String newName) {
        mLogger.info("renmaing wallet " + path + " to " + newName);
        List<WalletEntry> wallets = loadWalletDirectory();
        for (WalletEntry entry : wallets) {
            if (entry.mPath.equals(path)) {
                entry.mName = newName;
                persistWalletDirectory(wallets);
                return;
            }
        }
        throw new RuntimeException("wallet " + path + " not found");
    }

    public void deleteWallet(String path) {
        mLogger.info("deleting wallet " + path);

        File dir = new File(getFilesDir(), path);

        // Special case, the root wallet is not in a subdirectory.
        File child = null;
        if (path.equals(".")) {
            try {
                child = new File(dir, "salt");
                child.delete();
                child = new File(dir, "wallet32.spvchain");
                child.delete();
                child = new File(dir, "wallet32.hdwallet");
                child.delete();
                child = new File(dir, "wallet32.wallet");
                child.delete();
            }
            catch (Exception ex) {
                mLogger.error("delete of " + child.toString() + " failed");
            }
        } else {
            File[] directoryListing = dir.listFiles();
            for (File child2 : directoryListing) {
                try {
                    mLogger.info("deleting " + child2.toString());
                    child2.delete();
                }
                catch (Exception ex) {
                    mLogger.error("delete of " + child2.toString() + " failed");
                }
            }
            try {
                dir.delete();
            }
            catch (Exception ex) {
                mLogger.error("delete of " + path + " failed");
            }
        }

        // Re-persist the wallet list without the deleted entry.
        List<WalletEntry> newWalletList = new ArrayList<WalletEntry>();
        List<WalletEntry> walletList = loadWalletDirectory();
        for (WalletEntry entry : walletList)
            if (!entry.mPath.equals(path))
                newWalletList.add(entry);
        persistWalletDirectory(newWalletList);
    }

    public String addWallet() {
        int ndx = nextAvailableWallet();
        List<WalletEntry> wallets = loadWalletDirectory();
        String name = String.format("Wallet %d", ndx);
        String path = String.format("wallet%03d", ndx);
        wallets.add(new WalletEntry(name, path));
        makeWalletDirectory(path);
        persistWalletDirectory(wallets);
        return name;
    }

    public String walletName(String walletpath) {
        List<WalletEntry> walletList = loadWalletDirectory();
        for (WalletEntry entry : walletList) {
            if (entry.mPath.equals(walletpath))
                return entry.mName;
        }
        throw new RuntimeException("wallet path " + walletpath + " not found");
    }

    public void persistWalletDirectory(List<WalletEntry> wallets) {
        File walletDirFile =
            new File(getFilesDir(), getWalletPrefix() + ".walletdir");
        try {
            File tmpFile = new File(walletDirFile.getPath() + ".tmp");
            if (tmpFile.exists()) {
                mLogger.info("deleting existing at " + tmpFile.getPath());
                tmpFile.delete();
            }
        
            JSONArray listobj = new JSONArray();
            for (WalletEntry entry : wallets)
                listobj.put(entry.dumpJSON());
            JSONObject rootobj = new JSONObject();
            rootobj.put("wallets", listobj);
            String jsonstr = rootobj.toString(4);	// indentation
            byte[] jsondata = jsonstr.getBytes(Charset.forName("UTF-8"));

            mLogger.info("persisting: " + jsonstr);

            FileOutputStream ostrm = new FileOutputStream(tmpFile);
            ostrm.write(jsondata);
            ostrm.close();

            // Swap the tmp file into place.
            if (!tmpFile.renameTo(walletDirFile))
                mLogger.warn("failed to rename to " + walletDirFile.getPath());
            else
                mLogger.info("persisted to " + walletDirFile.getPath());
        }
        catch (Exception ex) {
            mLogger.error("persistWalletDirectory failed: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    public List<WalletEntry> loadWalletDirectory() {
        File walletDirFile =
            new File(getFilesDir(), getWalletPrefix() + ".walletdir");

        try {
            int len = (int) walletDirFile.length();
            DataInputStream dis =
                new DataInputStream(new FileInputStream(walletDirFile));
            byte[] jsondata = new byte[len];
            dis.readFully(jsondata);
            String jsonstr = new String(jsondata);
            mLogger.info("loading: " + jsonstr);
            JSONObject rootobj = new JSONObject(jsonstr);
            JSONArray listobj = rootobj.getJSONArray("wallets");
            ArrayList<WalletEntry> wallets = new ArrayList<WalletEntry>();
            for (int ii = 0; ii < listobj.length(); ++ii)
                wallets.add(WalletEntry.loadJSON(listobj.getJSONObject(ii)));
            return wallets;
        }
        catch (Exception ex) {
            mLogger.error("loadWalletDirectory failed: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    public void makeWalletDirectory(String walletDirName) {
        File dir = new File(getFilesDir(), walletDirName);
        boolean success = dir.mkdir();
    }

    public void setWalletDirName(String walletDirName) {
        mWalletDirName = walletDirName;
    }

    public File getWalletDir() {
        return new File(getFilesDir(), mWalletDirName);
    }

    public String getWalletPrefix() {
        return "wallet32";
    }

    public File getHDWalletFile(String suffix) {
        String filename = getWalletPrefix() + ".hdwallet";
        if (suffix != null)
            filename = filename + suffix;
        return new File(getWalletDir(), filename);
    }

	private void initLogging()
	{
        // We can't log into the wallet specific directories because
        // logging is initialized well before we've selected one.

		final File logDir = getDir("log", MODE_PRIVATE); // Context.MODE_WORLD_READABLE
		final File logFile = new File(logDir, "wallet32.log");

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
		filePattern.setContext(context);
		filePattern.setPattern("%d{HH:mm:ss.SSS} [%thread] %logger{0} - %msg%n");
		filePattern.start();

		final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();
		fileAppender.setContext(context);
		fileAppender.setFile(logFile.getAbsolutePath());

		final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
		rollingPolicy.setContext(context);
		rollingPolicy.setParent(fileAppender);
		rollingPolicy.setFileNamePattern(logDir.getAbsolutePath() + "/wallet32.%d.log.gz");
		rollingPolicy.setMaxHistory(7);
		rollingPolicy.start();

		fileAppender.setEncoder(filePattern);
		fileAppender.setRollingPolicy(rollingPolicy);
		fileAppender.start();

		final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
		logcatTagPattern.setContext(context);
		logcatTagPattern.setPattern("%logger{0}");
		logcatTagPattern.start();

		final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
		logcatPattern.setContext(context);
		logcatPattern.setPattern("[%thread] %msg%n");
		logcatPattern.start();

		final LogcatAppender logcatAppender = new LogcatAppender();
		logcatAppender.setContext(context);
		logcatAppender.setTagEncoder(logcatTagPattern);
		logcatAppender.setEncoder(logcatPattern);
		logcatAppender.start();

		final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);
		log.addAppender(fileAppender);
		log.addAppender(logcatAppender);
		log.setLevel(Level.INFO);
	}
}
