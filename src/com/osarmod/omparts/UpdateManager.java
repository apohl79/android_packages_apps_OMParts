package com.osarmod.omparts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class UpdateManager {
	private static final String TAG = "OMParts.UpdateManager";
	public static final String SERVER = "http://android.diepohls.com/";
	public static final String REMOTE_FILE = "latest";
	public static final String LOCAL_FILE = "osarmod-ota.zip";

	private static String m_serverVersion = null;
	Context m_ctx = null;
	ProgressDialog m_pdlg = null;
	WakeLock m_wl = null;

	public UpdateManager() {
	}

	public UpdateManager(Context context) {
		m_ctx = context;
		m_pdlg = new ProgressDialog(m_ctx);
		m_pdlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_pdlg.setMessage(m_ctx.getString(R.string.update_downloading));
		m_pdlg.setCancelable(false);
	}

	final Handler m_handler = new Handler() {
		public void handleMessage(Message m) {
			AlertDialog.Builder builder = null;
			switch (m.what) {
			case DownloadThread.PROGRESS:
				m_pdlg.setProgress(m.arg1);
				break;
			case DownloadThread.FAILED:
				m_pdlg.hide();
				builder = new AlertDialog.Builder(m_ctx);
				builder.setMessage(m_ctx.getString(R.string.update_dl_failed)).setNeutralButton("OK", null)
						.setCancelable(false);
				builder.create();
				m_wl.release();
				break;
			case DownloadThread.FINISHED:
				m_pdlg.setMessage("Flashing...");
				Flasher f = new Flasher(LOCAL_FILE);
				if (!f.flashOtaPackage()) {
					m_pdlg.hide();
					builder = new AlertDialog.Builder(m_ctx);
					builder.setMessage(m_ctx.getString(R.string.update_su_failed))
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

	public void startUpdate() {
		if (m_ctx != null) {
			// prevent the device from sleeping
			PowerManager pm = (PowerManager) m_ctx.getSystemService(Context.POWER_SERVICE);
			m_wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "OMParts");
			m_wl.acquire();

			// Download new version
			m_pdlg.setProgress(0);
			m_pdlg.show();
			String srvPath = SERVER + OMProperties.getOsarmodType() + "/" + REMOTE_FILE;
			String locPath = OMProperties.getSdCard() + "/" + LOCAL_FILE;
			DownloadThread worker = new DownloadThread(m_handler, srvPath, locPath);
			Thread t = new Thread(worker);
			t.start();
		}
	}

	public String getChangelogUrl() {
		return SERVER + "tools/changelog.cgi?osarmod_type=" + OMProperties.getOsarmodType() + "&version1="
				+ OMProperties.getVersion("") + "&version2=" + getVersionFromServer();
	}

	public boolean isUpdateAvailable() {
		// Check if there is an update available
		String instVer = OMProperties.getVersion("");
		String serverVer = getVersionFromServer(true);
		Log.v(TAG, "isUpdateAvailable: Installed: " + instVer + ", Server: " + serverVer);
		return null != serverVer && !instVer.equals(serverVer);
	}

	public String getVersionFromServer() {
		return getVersionFromServer(false);
	}

	public String getVersionFromServer(boolean force) {
		if (!force && null != m_serverVersion) {
			return m_serverVersion;
		} else {
			URL url;
			try {
				url = new URL(SERVER + OMProperties.getOsarmodType() + "/VERSION");
				Log.d(TAG, "getVersionFromServer: Reading version from " + url);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.connect();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				m_serverVersion = in.readLine();
				in.close();
			} catch (Exception e) {
				Log.e(TAG, "getVersionFromServer failed: " + e.getMessage());
			}
		}
		return m_serverVersion;
	}

}
