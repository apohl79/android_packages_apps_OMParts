package com.osarmod.omparts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
	public static final String SERVER = "http://android.diepohls.com/";
	public static final String REMOTE_FILE = "latest";
	public static final String REMOTE_FILE_DEV = "latest_dev";
	public static final String LOCAL_FILE = "osarmod-ota.zip";

	private static UpdateManager m_singleton = null;

	private Context m_ctx = null;
	private ProgressDialog m_pdlg = null;
	private WakeLock m_wl = null;
	private int m_lastProgress = 0;
	private boolean m_updateStarted = false;
	private DownloadThread m_worker = null;

	// Version strings
	String m_vinstalled = null;
	String m_vserver = null;
	String m_vserverDev = null;
	String m_vserverDevBase = null;

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
				if (!f.flashOtaPackage()) {
					if (null != m_pdlg) {
						m_pdlg.hide();
					}
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

			SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
			boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
			String srvPath = SERVER + OMProperties.getOsarmodType() + "/"
				+ (isDevVersion(getUpdateAvailable())? REMOTE_FILE_DEV : REMOTE_FILE);
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

	public String getChangelogUrl() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
		return SERVER + "tools/changelog.cgi?osarmod_type=" + OMProperties.getOsarmodType() + "&version1="
				+ m_vinstalled + "&version2=" + (devbuilds ? m_vserverDev : m_vserver);
	}

	public boolean versionsInitialized() {
		return null != m_vinstalled;
	}

	public void initVersions() {
		m_vinstalled = OMProperties.getVersion("");
		m_vserver = getVersionFromServer(false);
		m_vserverDev = getVersionFromServer(true);
		if (null != m_vserverDev) {
			m_vserverDevBase = getBaseVersion(m_vserverDev);
		}
	}

	public String getUpdateAvailable() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;

		if (null == m_vserver || (devbuilds && null == m_vserverDev)) {
			return null;
		}

		String checkVersion = m_vserver;
		if (devbuilds) {
			if (isNewerVersion(m_vserver, m_vserverDevBase)) {
				return m_vserver;
			} else {
				checkVersion = m_vserverDev;
			}
		}

		if (isNewerVersion(checkVersion, m_vinstalled)) {
			return checkVersion;
		}

		return null;
	}

	private static String getBaseVersion(String v) {
		String parts[] = v.split("-");
		return parts[0];
	}

	private static boolean isNewerVersion(String ver1, String ver2) {
		ver1 = ver1.replace(".", "");
		ver1 = ver1.replace("-dev", "");
		ver2 = ver2.replace(".", "");
		ver2 = ver2.replace("-dev", "");
		Log.d(TAG, "ver1=" + ver1 + " ver2=" + ver2);
		return new Integer(ver1) > new Integer(ver2);
	}

	private static boolean isDevVersion(String v) {
		return v.contains("-dev");
	}

	public String getVersionFromServer(boolean devbuilds) {
		URL url;
		String serverVersion = null;
		try {
			if (devbuilds) {
				url = new URL(SERVER + OMProperties.getOsarmodType() + "/VERSION_DEV");
			} else {
				url = new URL(SERVER + OMProperties.getOsarmodType() + "/VERSION");
			}
			Log.d(TAG, "getVersionFromServer: Reading version from " + url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			serverVersion = in.readLine();
			in.close();
		} catch (Exception e) {
			Log.e(TAG, "getVersionFromServer failed: " + e.getMessage());
		}
		return serverVersion;
	}

}
