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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Intent;

public class WalletApplication extends Application {

    protected Logger mLogger;

	@Override
	public void onCreate()
	{
        mLogger = LoggerFactory.getLogger(WalletApplication.class);

        // Apply PRNGFixes.
        PRNGFixes.apply();

        super.onCreate();

        mLogger.info("WalletApplication created");
    }
}
