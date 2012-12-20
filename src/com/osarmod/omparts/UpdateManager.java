package com.osarmod.omparts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class UpdateManager {
	private static final String TAG = "OMParts.UpdateManager";
	public static String LOCAL_FILE = "osarmod-ota.zip";

	private static UpdateManager m_singleton = null;

	private Context m_ctx = null;
	private ProgressDialog m_pdlg = null;
	private WakeLock m_wl = null;
	private int m_lastProgress = 0;
	private boolean m_updateStarted = false;
	private DownloadThread m_worker = null;
	private UpdateInfo m_updateInfo = null;

	public UpdateManager(Context context) {
		Log.d(TAG, "UpdateManager created");
		m_ctx = context;
	}

	public static UpdateManager getInstance(Context context) {
		if (null == m_singleton) {
			m_singleton = new UpdateManager(context);
		} else {
			m_singleton.m_ctx = context;
		}
		return m_singleton;
	}

	final Handler m_handler = new Handler() {
		public void handleMessage(Message m) {
			AlertDialog.Builder builder = null;
			switch (m.what) {
			case DownloadThread.PROGRESS:
				if (null != m_pdlg) {
					m_pdlg.setProgress(m.arg1);
				}
				m_lastProgress = m.arg1;
				break;
			case DownloadThread.FAILED:
				if (null != m_pdlg) {
					m_pdlg.hide();
				}
				builder = new AlertDialog.Builder(m_ctx);
				builder.setMessage(m_ctx.getString(R.string.update_dl_failed)).setNeutralButton("OK", null)
						.setCancelable(false);
				builder.create();
				m_wl.release();
				break;
			case DownloadThread.FINISHED:
				if (null != m_pdlg) {
					m_pdlg.setMessage("Flashing...");
				}
				Flasher f = new Flasher(LOCAL_FILE);
				if (!f.flashOtaPackage(m_ctx)) {
					if (null != m_pdlg) {
						m_pdlg.hide();
					}
					builder = new AlertDialog.Builder(m_ctx);
					builder.setMessage(m_ctx.getString(R.string.update_flash_failed))
							.setNeutralButton("OK", null).setCancelable(false);
					builder.create();
					m_wl.release();
				}
				break;
			default:
				break;
			}
		}
	};

	private void initProgress() {
		m_pdlg = new ProgressDialog(m_ctx);
		m_pdlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_pdlg.setMessage(m_ctx.getString(R.string.update_downloading));
		m_pdlg.setCancelable(false);
	}

	public void showProgress() {
		if (null != m_pdlg) {
			if (!m_pdlg.isShowing()) {
				m_pdlg.show();
			}
		} else if (m_updateStarted) {
			initProgress();
			m_pdlg.show();
			m_pdlg.setProgress(m_lastProgress);
		}
	}

	public void dismissProgress() {
		if (null != m_pdlg) {
			m_pdlg.dismiss();
			m_pdlg = null;
		}
	}

	public void startUpdate() {
		if (m_ctx != null) {
			initProgress();

			// prevent the device from sleeping
			PowerManager pm = (PowerManager) m_ctx.getSystemService(Context.POWER_SERVICE);
			m_wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "OMParts");
			m_wl.acquire();

			// Download new version
			m_pdlg.setProgress(0);
			m_pdlg.show();

			String srvPath = m_updateInfo.getRemoteFile();
			String locPath = OMProperties.getSdCard() + "/" + LOCAL_FILE;
			m_worker = new DownloadThread(m_handler, srvPath, locPath);
			Thread t = new Thread(m_worker);
			t.start();
			m_updateStarted = true;
		}
	}

	public void stopUpdate() {
		if (m_updateStarted && null != m_worker) {
			m_worker.terminate();
			m_worker = null;
			m_updateStarted = false;
			dismissProgress();
		}
	}

	public boolean isUpdateRunning() {
		return m_updateStarted;
	}
	
	public boolean updateCheckDone() {
		return null != m_updateInfo;
	}
	
	public void checkForUpdate() {
		if (null == m_updateInfo) {
			m_updateInfo = new UpdateInfo(m_ctx);
		}
	}
	
	public UpdateInfo getUpdateInfo() {
		return m_updateInfo;
	}
}
