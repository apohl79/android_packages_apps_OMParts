package com.osarmod.omparts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class UpdatePreference extends DialogPreference {

	//private static final String TAG = "OMParts.UpdatePreference";

	Context m_ctx = null;
	ProgressDialog m_pdlg = null;
	WakeLock m_wl = null;

	final Handler m_handler = new Handler() {
		public void handleMessage(Message m) {
			switch (m.what) {
			case DownloadThread.PROGRESS:
				m_pdlg.setProgress(m.arg1);
				break;
			case DownloadThread.FAILED:
				m_pdlg.hide();
				AlertDialog.Builder builder = new AlertDialog.Builder(m_ctx);
				builder.setMessage(m_ctx.getString(R.string.update_dl_failed)).setNeutralButton("OK", null)
						.setCancelable(false);
				builder.create();
				break;
			case DownloadThread.FINISHED:
				m_pdlg.setMessage("Flashing...");
				Utils.flashOtaPackage();
				break;
			default:
				break;
			}
		}
	};

	public UpdatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogMessage(R.string.update_inst);
		setPositiveButtonText(R.string.update_yes);
		setNegativeButtonText(R.string.update_no);
		m_ctx = context;
		m_pdlg = new ProgressDialog(m_ctx);
		m_pdlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_pdlg.setMessage(m_ctx.getString(R.string.update_downloading));
		m_pdlg.setCancelable(false);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (DialogInterface.BUTTON_POSITIVE == which) {
			// prevent the device from sleeping
			PowerManager pm = (PowerManager) m_ctx.getSystemService(Context.POWER_SERVICE);
			m_wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "OMParts");
			m_wl.acquire();
			
			// Download new version
			m_pdlg.setProgress(0);
			m_pdlg.show();
			DownloadThread worker = new DownloadThread(m_handler);
			Thread t = new Thread(worker);
			t.start();
		}
	}

}
