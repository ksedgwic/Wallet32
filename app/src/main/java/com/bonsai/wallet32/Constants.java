package com.bonsai.wallet32;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

/**
 * @author Harald Hoyer
 */
public class Constants
{
	private static NetworkParameters networkParameters = null;
	public static String CHECKPOINTS_FILENAME = null;

	public static NetworkParameters getNetworkParameters(Context context)
	{
		if (networkParameters == null) {
			boolean TESTNET = true;

			try {
				PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				TESTNET = pinfo.versionName.contains("-test");
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}

			networkParameters = TESTNET ? TestNet3Params.get() : MainNetParams.get();
			CHECKPOINTS_FILENAME = TESTNET ? "checkpoints-testnet" : "checkpoints";
		}

		return networkParameters;
	}
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:
