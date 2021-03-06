package com.osarmod.omparts;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class UpdatePreference extends DialogPreference {

	// private static final String TAG = "OMParts.UpdatePreference";

	UpdateManager m_um = null;
	
	public UpdatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogMessage(R.string.update_inst);
		setPositiveButtonText(R.string.update_yes);
		setNegativeButtonText(R.string.update_no);
	}

	public void setUpdateManager(UpdateManager um) {
		m_um = um;
	}

	public void setWipeMessage() {
		setDialogMessage(R.string.update_inst_wipe);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (DialogInterface.BUTTON_POSITIVE == which) {
			m_um.startUpdate();
		}
	}

}
