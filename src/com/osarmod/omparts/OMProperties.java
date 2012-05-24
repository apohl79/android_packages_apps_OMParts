package com.osarmod.omparts;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.os.SystemProperties;
import android.util.Log;

public class OMProperties {
	private static final String TAG = "OMParts.OMProperties";
	private static String SDCARD_SWITCH_FILE = "/data/local/switch_sdcard";

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

	public static String getSdCard() {
		String path = "/mnt/sdcard";
		if (SystemProperties.getInt("persist.sys.vold.switchexternal", 1) != 1) {
			path = "/mnt/emmc";
		}
		return path;
	}

	public static boolean getSwitchSdCard() {
		File f = new File(SDCARD_SWITCH_FILE);
		return f.exists();
	}

	public static void setSwitchSdCard(boolean b) {
		// we need root permissions now
		Process p;
		try {
			p = Runtime.getRuntime().exec("su");
			Log.v(TAG, "Got root access");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			if (b) {
				out.writeBytes("touch " + SDCARD_SWITCH_FILE + "\n");
				Log.v(TAG, "Created " + SDCARD_SWITCH_FILE);
			} else {
				out.writeBytes("rm " + SDCARD_SWITCH_FILE + "\n");
				Log.v(TAG, "Removed " + SDCARD_SWITCH_FILE);				
			}
			out.flush();
		} catch (IOException e1) {
			Log.e(TAG, "su failed: " + e1.getMessage());
		}
	}
}
