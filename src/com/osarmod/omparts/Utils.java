package com.osarmod.omparts;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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

	public static String getOsarmodType() {
		String type = SystemProperties.get("ro.osarmod.ostype", "cm7");
		String device = SystemProperties.get("ro.osarmod.device", "galaxysmtd");
		return device + "-" + type;
	}
	
	public static String getLocalPath() {
		if (SystemProperties.getInt("persist.sys.vold.switchexternal", 1) == 1) {
			return "/mnt/sdcard/" + LOCAL_FILE;
		} else {
			return "/mnt/emmc/" + LOCAL_FILE;
		}
	}
	
	public static String getServerPath() {
		return SERVER + getOsarmodType() + "/" + REMOTE_FILE;
	}
	
	public static boolean isUpdateAvailable() {
		// Check if there is an update available
		String instVer = Utils.getVersion("");
		String serverVer = Utils.getVersionFromServer();
		Log.v(TAG, "isUpdateAvailable: Installed: " + instVer + ", Server: " + serverVer);
		return !instVer.equals(serverVer);
	}

	public static String getVersionFromServer() {
		if (null != m_serverVersion) {
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

	public static void flashOtaPackage() {
		// we need root permissions now
		Process p;
		try {
			p = Runtime.getRuntime().exec("su");
			Log.v(TAG, "Got root access");
			// create command file for recovery
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.writeBytes("echo \"--wipe_cache\">/cache/recovery/command\n");
			out.writeBytes("echo \"--update_package=/sdcard/" + Utils.LOCAL_FILE
					+ "\">>/cache/recovery/command\n");
			Log.v(TAG, "Created /cache/recovery/command");
			Log.v(TAG, "Rebooting into recovery");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
			}
			out.writeBytes("reboot recovery\n");
			//out.writeBytes("exit\n");
			out.flush();
		} catch (IOException e1) {
			Log.e(TAG, "su failed: " + e1.getMessage());
		}
	}
}
