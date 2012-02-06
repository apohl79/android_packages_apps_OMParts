package com.osarmod.omparts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class OMParts extends PreferenceActivity {

	public static final String KEY_UPDATE = "update";
	public static final String KEY_VERSION = "version";
	public static final String KEY_CHANGELOG = "changelog";

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
		if (Utils.isUpdateAvailable()) {
			m_updatePref.setEnabled(true);
			m_updatePref.setSummary(getString(R.string.update_new_version) + " " + Utils.getVersionFromServer());
		} else {
			m_updatePref.setEnabled(false);
			m_updatePref.setSummary(R.string.update_not_found);
		}

		Preference p = findPreference(KEY_VERSION);
		p.setSummary(Utils.getVersion(getString(R.string.version_unknown)));

		p = findPreference(KEY_CHANGELOG);
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getChangelogUrl()));
				startActivity(i);
				return true;
			}
		});
	}

}
