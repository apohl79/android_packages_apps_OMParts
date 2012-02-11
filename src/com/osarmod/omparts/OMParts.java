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

	private UpdatePreference m_updatePref = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Allow a network connection to be established from the main thread. We
		// need this from HC and ICS on.
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(policy);

		addPreferencesFromResource(R.xml.main);

		m_updatePref = (UpdatePreference) findPreference(KEY_UPDATE);
		checkForUpdate();

		Preference p = findPreference(KEY_VERSION);
		p.setSummary(Utils.getVersion(getString(R.string.version_unknown)));
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
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getChangelogUrl()));
				startActivity(i);
				return true;
			}
		});

		SharedPreferences prefs = getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean notifyme = prefs.getInt(KEY_NOTIFICATION, 1) == 1;
		final CheckBoxPreference cbp = (CheckBoxPreference) findPreference(KEY_NOTIFICATION);
		final Context ctx = this;
		cbp.setChecked(notifyme);
		cbp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
				cbp.setChecked(b);
				return false;
			}
		});
	}

	private void checkForUpdate() {
		if (Utils.isUpdateAvailable()) {
			m_updatePref.setEnabled(true);
			m_updatePref.setSummary(getString(R.string.update_new_version) + " "
					+ Utils.getVersionFromServer());
		} else {
			m_updatePref.setEnabled(false);
			m_updatePref.setSummary(R.string.update_not_found);
		}
	}
}
