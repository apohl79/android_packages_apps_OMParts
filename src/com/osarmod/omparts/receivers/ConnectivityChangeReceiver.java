package com.osarmod.omparts.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	private static final String TAG = "OMParts.ConnectivityChangeReceiver";

	@Override
	public void onReceive(Context ctx, Intent intent) {
		boolean hasConnection = !intent.getBooleanExtra(
				ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
		if (hasConnection) {			
			// deactivate receiver after successful check to safe battery
			ComponentName receiver = new ComponentName(ctx,
					ConnectivityChangeReceiver.class);
			PackageManager pm = ctx.getPackageManager();
			pm.setComponentEnabledSetting(receiver,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
			
			Intent i = new Intent("com.osarmod.omparts.UPDATE_CHECK");
			ctx.sendBroadcast(i);
		}
	}

}
