package com.osarmod.omparts;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.SystemProperties;
import android.util.Log;

public class Utils {

	private static final String TAG = "OMParts.Utils";
	public static final String SERVER = "http://android.diepohls.com/";
	public static final String REMOTE_FILE = "latest";
	public static final String LOCAL_FILE = "osarmod-ota.zip";

	private static String m_serverVersion = null;

	public static String getVersion(String def) {
		return SystemProperties.get("ro.osarmod.version", def);
	}

	public static String getOSType() {
		return SystemProperties.get("ro.osarmod.ostype", "cm7");
	}

	public static String getDevice() {
		return SystemProperties.get("ro.osarmod.device", "galaxysmtd");
	}

	public static String getOsarmodType() {
		return getDevice() + "-" + getOSType();
	}

	public static String getLocalPath() {
		if (SystemProperties.getInt("persist.sys.vold.switchexternal", 1) == 1) {
			return "/mnt/sdcard/" + LOCAL_FILE;
		} else {
			return "/mnt/emmc/" + LOCAL_FILE;
		}
	}

	public static String getFlashPath() {
		String path = null;
		String device = getDevice();
		if (device.equals("galaxysmtd")) {
			path = "/sdcard/" + LOCAL_FILE;
		} else if (device.equals("wingray")) {
			path = "/data/media/" + LOCAL_FILE;
		}
		return path;
	}

	public static String getServerPath() {
		return SERVER + getOsarmodType() + "/" + REMOTE_FILE;
	}

	public static String getChangelogUrl() {
		return Utils.SERVER + "tools/changelog.cgi?osarmod_type=" + getOsarmodType() + "&version1="
				+ getVersion("") + "&version2=" + getVersionFromServer();
	}

	public static boolean isUpdateAvailable() {
		// Check if there is an update available
		String instVer = getVersion("");
		String serverVer = getVersionFromServer(true);
		Log.v(TAG, "isUpdateAvailable: Installed: " + instVer + ", Server: " + serverVer);
		return null != serverVer && !instVer.equals(serverVer);
	}

	public static String getVersionFromServer() {
		return getVersionFromServer(false);
	}

	public static String getVersionFromServer(boolean force) {
		if (!force && null != m_serverVersion) {
			return m_serverVersion;
		} else {
			URL url;
			try {
				url = new URL(SERVER + getOsarmodType() + "/VERSION");
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

	public static boolean flashOtaPackage() {
		// we need root permissions now
		Process p;
		boolean success = false;
		try {
			p = Runtime.getRuntime().exec("su");
			Log.v(TAG, "Got root access");
			// create command file for recovery
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.writeBytes("echo \"--wipe_cache\">/cache/recovery/command\n");
			out.writeBytes("echo \"--update_package=" + getFlashPath() + "\">>/cache/recovery/command\n");
			Log.v(TAG, "Created /cache/recovery/command");
			Log.v(TAG, "Rebooting into recovery");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
			}
			out.writeBytes("reboot recovery\n");
			// out.writeBytes("exit\n");
			out.flush();
			success = true;
		} catch (IOException e1) {
			Log.e(TAG, "su failed: " + e1.getMessage());
		}
		return success;
	}

}
