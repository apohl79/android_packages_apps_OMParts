package com.osarmod.omparts;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;

public class EventManager extends BroadcastReceiver {

	private static final String TAG = "OMParts.EventManager";
	private static final long INTERVAL = AlarmManager.INTERVAL_HOUR * 3;
	private static final long START_DELAY = DateUtils.MINUTE_IN_MILLIS * 15;
	private static final int NOTIFICATION_UPD_ID = 1;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		String action = intent.getAction();
		if (null != action && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			SharedPreferences prefs = ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
			boolean notifyme = prefs.getInt(OMParts.KEY_NOTIFICATION, 1) == 1;
			if (notifyme) {
				Log.d(TAG, "Boot complete: starting update checks.");
				startUpdateChecks(ctx);
			}
			if (Blx.isSupported()) {
				Blx.setChargingLimit(prefs.getString(OMParts.KEY_BLX, "96"));
			}
		} else {
			Log.d(TAG, "Checking for ROM update...");
			UpdateManager um = UpdateManager.getInstance(ctx);
			um.initVersions();
			if (null != um.getUpdateAvailable()) {
				showNotification(ctx);
			}
		}
	}

	private void showNotification(Context ctx) {
		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.ic_osarmod, ctx.getString(R.string.update_available),
				System.currentTimeMillis());
		n.flags = Notification.FLAG_AUTO_CANCEL;
		Intent i = new Intent(ctx, OMParts.class);
		PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		n.setLatestEventInfo(ctx, "OSARMOD", ctx.getString(R.string.update_available), pi);
		nm.notify(NOTIFICATION_UPD_ID, n);
	}

	public static void startUpdateChecks(Context ctx) {
		Intent intent = new Intent(ctx, EventManager.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + START_DELAY, INTERVAL,
				pi);
	}

	public static void stopUpdateChecks(Context ctx) {
		Intent intent = new Intent(ctx, EventManager.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);
	}
}
