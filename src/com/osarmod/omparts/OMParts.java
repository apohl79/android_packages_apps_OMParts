package com.osarmod.omparts;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class OMParts extends PreferenceActivity {

	public static final String KEY_UPDATE = "update";
	public static final String KEY_VERSION = "version";

	private UpdatePreference m_updatePref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
	}

}
