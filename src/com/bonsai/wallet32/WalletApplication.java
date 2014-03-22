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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import android.app.Application;

import com.google.bitcoin.crypto.KeyCrypter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class WalletApplication extends Application {

    private static Logger mLogger =
        LoggerFactory.getLogger(WalletApplication.class);

    public String			mPasscode;
    public KeyCrypter		mKeyCrypter;
    public KeyParameter		mAesKey;

    public String			mIntentURI = null;

	@Override
	public void onCreate()
	{
        // Apply PRNGFixes.
        PRNGFixes.apply();

        initLogging();

        super.onCreate();

        // Log the About contents so we have the version string.
        mLogger.info(getResources().getString(R.string.about_contents));

        mLogger.info("WalletApplication created");
    }

    public void setIntentURI(String uri) {
        mIntentURI = uri;
    }

    public String getIntentURI() {
        return mIntentURI;
    }

	private void initLogging()
	{
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
