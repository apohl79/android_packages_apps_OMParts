package com.osarmod.omparts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.osarmod.omparts.receivers.UpdateReceiver;

public class OMParts extends PreferenceActivity {
	private static final String TAG = "OMParts";
	public static final String KEY_UPDATE = "update";
	public static final String KEY_VERSION = "version";
	public static final String KEY_CHANGELOG = "changelog";
	public static final String KEY_NOTIFICATION = "notification";
	public static final String KEY_SDCARD = "sdcard";
	public static final String KEY_DEVBUILDS = "devbuilds";
	public static final String KEY_BLX = "blx";

	private UpdatePreference m_updatePref = null;
	private UpdateManager m_um = null;
	private String m_newVersion = null;
	private boolean m_wipe = false;

	final Handler m_handler = new Handler() {
		public void handleMessage(Message m) {
			if (m.arg1 == 1) {
				m_updatePref.setEnabled(true);
				m_updatePref.setSummary(getString(R.string.update_new_version)
						+ " " + m_newVersion);
			} else {
				m_updatePref.setEnabled(false);
				m_updatePref.setSummary(R.string.update_not_found);
			}
		}
	};

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause called");
		super.onPause();
		m_um.dismissProgress();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume called");
		super.onResume();
		m_um.showProgress();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.main);

		m_um = UpdateManager.getInstance(this);

		m_updatePref = (UpdatePreference) findPreference(KEY_UPDATE);
		m_updatePref.setUpdateManager(m_um);

		if (!m_um.updateCheckDone()) {
			checkForUpdate();
		} else {
			if (m_um.isUpdateRunning()) {
				m_um.showProgress();
			}
			m_newVersion = m_um.getUpdateInfo().getUpdateVersion();
			if (null != m_newVersion) {
				m_updatePref.setEnabled(true);
				m_updatePref.setSummary(getString(R.string.update_new_version)
						+ " " + m_newVersion);
			} else {
				m_updatePref.setEnabled(false);
				m_updatePref.setSummary(R.string.update_not_found);
			}
		}

		Preference p = findPreference(KEY_VERSION);
		p.setSummary(OMProperties
				.getVersion(getString(R.string.version_unknown)));
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				checkForUpdate();
				return false;
			}
		});

		p = findPreference(KEY_CHANGELOG);
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(m_um
						.getUpdateInfo().getChangelogUrl()));
				startActivity(i);
				return true;
			}
		});

		final Context ctx = this;

		SharedPreferences prefs = getSharedPreferences("osarmod",
				Context.MODE_PRIVATE);

		boolean notifyme = prefs.getInt(KEY_NOTIFICATION, 1) == 1;
		final CheckBoxPreference cbpNotify = (CheckBoxPreference) findPreference(KEY_NOTIFICATION);
		cbpNotify.setChecked(notifyme);
		cbpNotify
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Boolean b = (Boolean) newValue;
						int val = b ? 1 : 0;
						SharedPreferences prefs = getSharedPreferences(
								"osarmod", Context.MODE_PRIVATE);
						Editor e = prefs.edit();
						e.putInt(KEY_NOTIFICATION, val);
						e.commit();
						if (b) {
							Log.e(TAG, "Starting update checks.");
							UpdateReceiver.startUpdateChecks(ctx);
						} else {
							Log.e(TAG, "Stopping update checks.");
							UpdateReceiver.stopUpdateChecks(ctx);
						}
						cbpNotify.setChecked(b);
						return false;
					}
				});

		boolean devbuilds = prefs.getInt(KEY_DEVBUILDS, 0) == 1;
		final CheckBoxPreference cbpDevbuilds = (CheckBoxPreference) findPreference(KEY_DEVBUILDS);
		cbpDevbuilds.setChecked(devbuilds);
		cbpDevbuilds
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Boolean b = (Boolean) newValue;
						cbpDevbuilds.setChecked(b);
						int val = b ? 1 : 0;
						SharedPreferences prefs = getSharedPreferences(
								"osarmod", Context.MODE_PRIVATE);
						Editor e = prefs.edit();
						e.putInt(KEY_DEVBUILDS, val);
						e.commit();
						return false;
					}
				});

		ListPreference blxPref = (ListPreference) findPreference(KEY_BLX);
		blxPref.setEnabled(Blx.isSupported());
		if (Blx.isSupported()) {
			blxPref.setValue(prefs.getString(KEY_BLX, "96"));
			blxPref.setOnPreferenceChangeListener(new Blx(this));
		}
	}

	private void checkForUpdate() {
		m_updatePref.setEnabled(false);
		m_updatePref.setSummary(R.string.update_check);
		Thread t = new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "Checking for update...");
				m_um.checkForUpdate();
				m_newVersion = m_um.getUpdateInfo().getUpdateVersion();
				m_wipe = m_um.getUpdateInfo().isWipeUpdate();
				// set pref text to wipe msg if um detects wipe update
				if (m_wipe) {
					m_updatePref.setWipeMessage();
				}
				Message m = new Message();
				m.arg1 = (null == m_newVersion) ? 0 : 1;
				m_handler.sendMessage(m);
			}
		});
		Log.d(TAG, "Starting thread");
		t.start();
	}
}
