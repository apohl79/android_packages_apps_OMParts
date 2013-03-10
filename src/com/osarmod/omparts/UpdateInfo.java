package com.osarmod.omparts;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.util.Log;

public class UpdateInfo {

	private static final String TAG = "OMParts.UpdateInfo";
	public static String SERVER = "http://android.diepohls.com/";
	public static String REMOTE_FILE = "latest";
	public static String REMOTE_FILE_DEV = "latest_dev";
	public static String REMOTE_FILE_INC = "incremental.zip";
	public static String REMOTE_FILE_DEV_INC = "incremental-dev.zip";
	public static String VERSION_FILE = "version";
	public static String VERSION_FILE_DEV = "version_dev";
	public static String VERSION_FILE_PREV = "version_prev";
	public static String VERSION_FILE_DEV_PREV = "version_dev_prev";
	public static String WIPE_FILE = "wipe";
	public static String WIPE_FILE_DEV = "wipe_dev";
	
	enum VersionType {
		STABLE, DEV, STABLE_PREVIOUS, DEV_PREVIOUS
	}

	private Context m_ctx = null;

	// Version strings
	private String m_vinstalled = null;
	private String m_vserver = null;
	private String m_vserverDev = null;
	private String m_vserverPrev = null;
	private String m_vserverDevPrev = null;
	private boolean m_wipe = false;
	private boolean m_wipeDev = false;

	public UpdateInfo(Context ctx) {
		m_ctx = ctx;
		SERVER = SystemProperties.get("ro.osarmod.ota.server", SERVER);
		REMOTE_FILE = SystemProperties.get("ro.osarmod.ota.remote_file", REMOTE_FILE);
		REMOTE_FILE_DEV = SystemProperties.get("ro.osarmod.ota.remote_file_dev", REMOTE_FILE_DEV);
		VERSION_FILE = SystemProperties.get("ro.osarmod.ota.version_file", VERSION_FILE);
		VERSION_FILE_DEV = SystemProperties.get("ro.osarmod.ota.version_file_dev", VERSION_FILE_DEV);
		WIPE_FILE = SystemProperties.get("ro.osarmod.ota.wipe_file", WIPE_FILE);
		WIPE_FILE_DEV = SystemProperties.get("ro.osarmod.ota.wipe_file_dev", WIPE_FILE_DEV);
		initVersions();
	}

	private void initVersions() {
		m_vinstalled = OMProperties.getVersion("");
		m_vserver = getVersionRemote(VersionType.STABLE);
		m_vserverDev = getVersionRemote(VersionType.DEV);
		m_vserverPrev = getVersionRemote(VersionType.STABLE_PREVIOUS);
		m_vserverDevPrev = getVersionRemote(VersionType.DEV_PREVIOUS);
		m_wipe = isWipeUpdateRemote(false);
		m_wipeDev = isWipeUpdateRemote(true);
	}

	public String getUpdateVersion() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
		String vserver = devbuilds ? m_vserverDev : m_vserver;

		if (null != vserver && !m_vinstalled.equals(vserver)) {
			return vserver;
		}

		return null;
	}

	public String getRemoteFile() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
		String path = SERVER + OMProperties.getOsarmodType() + "/";
		if (devbuilds) {
			// if the last version is installed, we can do an incremental update
			if (m_vinstalled.equals(m_vserverDevPrev)) {
				path += REMOTE_FILE_DEV_INC;
			} else {
				path += REMOTE_FILE_DEV;
			}
		} else {
			// if the last version is installed, we can do an incremental update
			if (m_vinstalled.equals(m_vserverPrev)) {
				path += REMOTE_FILE_INC;
			} else {
				path += REMOTE_FILE;
			}
		}
		return path;
	}

	public boolean isWipeUpdate() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
		return (devbuilds ? m_wipeDev : m_wipe);
	}

	private boolean isWipeUpdateRemote(boolean devbuilds) {
		String wipe_url = SERVER + OMProperties.getOsarmodType() + "/" +
			(devbuilds ? WIPE_FILE_DEV : WIPE_FILE);
		String wipe = getFileFromServer(wipe_url);
		return (null != wipe ? wipe.equals("1") : false);
	}

	private String getVersionRemote(VersionType t) {
		String vers_url = SERVER + OMProperties.getOsarmodType() + "/";
		switch (t) {
		case STABLE:
			vers_url += VERSION_FILE;
			break;
		case DEV:
			vers_url += VERSION_FILE_DEV;
			break;
		case STABLE_PREVIOUS:
			vers_url += VERSION_FILE_PREV;
			break;
		case DEV_PREVIOUS:
			vers_url += VERSION_FILE_DEV_PREV;
			break;
		}
		String serverVersion = getFileFromServer(vers_url);
		return serverVersion;
	}

	public String getChangelogUrl() {
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		boolean devbuilds = prefs.getInt(OMParts.KEY_DEVBUILDS, 0) == 1;
		return SERVER + "tools/changelog.cgi?osarmod_type=" + OMProperties.getOsarmodType() + "&version1="
				+ m_vinstalled + "&version2=" + (devbuilds ? m_vserverDev : m_vserver);
	}

	private String getFileFromServer(String purl) {
		Log.d(TAG, "getFileFromServer: Reading file " + purl);
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet req = new HttpGet(purl);
			req.addHeader("Cache-Control", "no-cache");
			HttpResponse res = client.execute(req);
			int sc = res.getStatusLine().getStatusCode();
			if (HttpStatus.SC_OK != sc) {
				throw new Exception("Got status code " + sc);
			}
			HttpEntity entity = res.getEntity();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					entity.getContent()));
			String content = in.readLine();
			in.close();
			Log.d(TAG, " => " + content);
			return content;
		} catch (Exception e) {
			Log.e(TAG, "getFileFromServer failed: " + e);
		}
		return null;
	}

}