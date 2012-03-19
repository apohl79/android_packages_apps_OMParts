package com.osarmod.omparts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class OMParts extends PreferenceActivity {
	private static final String TAG = "OMParts";
	public static final String KEY_UPDATE = "update";
	public static final String KEY_VERSION = "version";
	public static final String KEY_CHANGELOG = "changelog";
	public static final String KEY_NOTIFICATION = "notification";
	public static final String KEY_SDCARD = "sdcard";
	public static final String KEY_DEVBUILDS = "devbuilds";

	private UpdatePreference m_updatePref = null;
	private UpdateManager m_um = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Allow a network connection to be established from the main thread. We
		// need this from HC and ICS on.
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(policy);

		addPreferencesFromResource(R.xml.main);

		m_updatePref = (UpdatePreference) findPreference(KEY_UPDATE);
		m_um = new UpdateManager(this);
		checkForUpdate();

		Preference p = findPreference(KEY_VERSION);
		p.setSummary(OMProperties.getVersion(getString(R.string.version_unknown)));
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				checkForUpdate();
				return false;
			}
		});

		p = findPreference(KEY_CHANGELOG);
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(m_um.getChangelogUrl()));
				startActivity(i);
				return true;
			}
		});

		final Context ctx = this;

		SharedPreferences prefs = getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean notifyme = prefs.getInt(KEY_NOTIFICATION, 1) == 1;
		final CheckBoxPreference cbpNotify = (CheckBoxPreference) findPreference(KEY_NOTIFICATION);
		cbpNotify.setChecked(notifyme);
		cbpNotify.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean b = (Boolean) newValue;
				int val = b ? 1 : 0;
				SharedPreferences prefs = getSharedPreferences("osarmod", Context.MODE_PRIVATE);
				Editor e = prefs.edit();
				e.putInt(KEY_NOTIFICATION, val);
				e.commit();
				if (b) {
					Log.e(TAG, "Starting update checks.");
					EventManager.startUpdateChecks(ctx);
				} else {
					Log.e(TAG, "Stopping update checks.");
					EventManager.stopUpdateChecks(ctx);
				}
				cbpNotify.setChecked(b);
				return false;
			}
		});

		boolean devbuilds = prefs.getInt(KEY_DEVBUILDS, 0) == 1;
		final CheckBoxPreference cbpDevbuilds = (CheckBoxPreference) findPreference(KEY_DEVBUILDS);
		cbpDevbuilds.setChecked(devbuilds);
		cbpDevbuilds.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean b = (Boolean) newValue;
				cbpDevbuilds.setChecked(b);
				int val = b ? 1 : 0;
				SharedPreferences prefs = getSharedPreferences("osarmod", Context.MODE_PRIVATE);
				Editor e = prefs.edit();
				e.putInt(KEY_DEVBUILDS, val);
				e.commit();
				return false;
			}
		});

		final CheckBoxPreference cbpSdcard = (CheckBoxPreference) findPreference(KEY_SDCARD);
		if (OMProperties.getOsarmodType().equals("galaxysmtd-cm9")) {
			cbpSdcard.setChecked(OMProperties.getSwitchSdCard());
			cbpSdcard.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean b = (Boolean) newValue;
					OMProperties.setSwitchSdCard(b);
					cbpSdcard.setChecked(b);
					return false;
				}
			});
		} else {
			cbpSdcard.setEnabled(false);
		}
	}

	private void checkForUpdate() {
		String newVersion = m_um.getUpdateAvailable();
		if (null != newVersion) {
			m_updatePref.setEnabled(true);
			m_updatePref.setSummary(getString(R.string.update_new_version) + " " + newVersion);
		} else {
			m_updatePref.setEnabled(false);
			m_updatePref.setSummary(R.string.update_not_found);
		}
	}
}
