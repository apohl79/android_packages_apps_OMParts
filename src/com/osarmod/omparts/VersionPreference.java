package com.osarmod.omparts;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.preference.Preference;
import android.preference.DialogPreference;
import android.app.AlertDialog;

public class VersionPreference extends DialogPreference {

	private static final String TAG = "OSARMOD_Settings";
	private boolean updateFound = false;

    public VersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
		
		// Check if there is an update available
		try {
			String instVer = Utils.getVersion("");
			String serverVer = Utils.getVersionFromServer();
			if (!instVer.equals(serverVer)) {
				updateFound = true;
			}
			if (updateFound) { // Update found
				setDialogTitle(R.string.update_found);
				String msg = "Version: " + serverVer;
				setDialogMessage(msg);
			} else {
				setDialogMessage(R.string.update_not_found);
			}
		} catch(Exception e) {
			setDialogMessage(R.string.update_server_error);
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (updateFound && which == DialogInterface.BUTTON_POSITIVE) {
			// Download new version
		}
	}

}
