package com.osarmod.omparts.receivers;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import android.util.Log;

import com.osarmod.omparts.Blx;
import com.osarmod.omparts.OMParts;
import com.osarmod.omparts.R;
import com.osarmod.omparts.UpdateInfo;

public class UpdateReceiver extends BroadcastReceiver {

	private static final String TAG = "OMParts.UpdateReceiver";
	private static final long INTERVAL = AlarmManager.INTERVAL_DAY;
	private static final long START_DELAY = DateUtils.MINUTE_IN_MILLIS * 2;
	private static final int NOTIFICATION_UPD_ID = 1;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			SharedPreferences prefs = ctx.getSharedPreferences("osarmod",
					Context.MODE_PRIVATE);
			boolean notifyme = prefs.getInt(OMParts.KEY_NOTIFICATION, 1) == 1;
			if (notifyme) {
				Log.d(TAG, "Boot complete: starting update checks.");
				startUpdateChecks(ctx);
			}
			if (Blx.isSupported()) {
				Blx.setChargingLimit(prefs.getString(OMParts.KEY_BLX, "96"));
			}
		} else {
			ConnectivityManager cm = (ConnectivityManager) ctx
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if (null != ni && ni.isConnected()) {
				checkForUpdate(ctx);
			} else {
				Log.d(TAG, "No network connection, update check skipped.");
				// receive connectivity state changes
				ComponentName receiver = new ComponentName(ctx,
						ConnectivityChangeReceiver.class);
				PackageManager pm = ctx.getPackageManager();
				pm.setComponentEnabledSetting(receiver,
						PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP);
			}
		}
	}

	private class UpdateCheckRunnable implements Runnable {
		Context m_ctx = null;

		public UpdateCheckRunnable(Context ctx) {
			m_ctx = ctx;
		}

		private void showNotification(String version) {
			Intent i = new Intent(m_ctx, OMParts.class);
			PendingIntent pi = PendingIntent.getActivity(m_ctx, 0, i,
					PendingIntent.FLAG_CANCEL_CURRENT);

			Notification n = new Notification.Builder(m_ctx)
					.setContentTitle(
							m_ctx.getString(R.string.notification_title))
					.setContentText(
							m_ctx.getString(R.string.update_available) + ": "
									+ version).setContentIntent(pi)
					.setSmallIcon(R.drawable.ic_osarmod)
					.setWhen(System.currentTimeMillis()).setAutoCancel(true)
					.build();

			NotificationManager nm = (NotificationManager) m_ctx
					.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(NOTIFICATION_UPD_ID, n);
		}

		public void run() {
			UpdateInfo ui = new UpdateInfo(m_ctx);
			String updVersion = ui.getUpdateVersion();
			if (null != updVersion) {
				showNotification(updVersion);
			}
		}
	}

	private void checkForUpdate(Context ctx) {
		Log.d(TAG, "Checking for ROM update...");
		Thread t = new Thread(new UpdateCheckRunnable(ctx));
		t.start();
	}

	public static void startUpdateChecks(Context ctx) {
		Intent intent = new Intent(ctx, UpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ START_DELAY, INTERVAL, pi);
	}

	public static void stopUpdateChecks(Context ctx) {
		Intent intent = new Intent(ctx, UpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);
	}
}
