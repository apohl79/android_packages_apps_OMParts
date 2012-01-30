package com.osarmod.omparts;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.EditTextPreference;

public class OMParts extends PreferenceActivity  {

    public static final String KEY_UPDATE = "update";
    public static final String KEY_VERSION = "version";

    private VersionPreference m_updatePref;
    private EditTextPreference m_versionPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main);

        m_updatePref = (VersionPreference) findPreference(KEY_UPDATE);
        m_updatePref.setEnabled(true);

        m_versionPref = (EditTextPreference) findPreference(KEY_VERSION);
        m_versionPref.setEnabled(false);
		m_versionPref.setSummary(Utils.getVersion(getString(R.string.version_unknown)));
    }

}
