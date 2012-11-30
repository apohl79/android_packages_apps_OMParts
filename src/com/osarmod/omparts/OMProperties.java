package com.osarmod.omparts;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.os.SystemProperties;
import android.util.Log;

public class OMProperties {
	private static final String TAG = "OMParts.OMProperties";

	public static String getVersion(String def) {
		return SystemProperties.get("ro.osarmod.version", def);
	}

	public static String getOSType() {
		return SystemProperties.get("ro.osarmod.ostype", "unknown");
	}

	public static String getDevice() {
		return SystemProperties.get("ro.osarmod.device", "unknown");
	}

	public static String getOsarmodType() {
		return getDevice() + "-" + getOSType();
	}

	public static String getSdCard() {
		return SystemProperties.get("ro.osarmod.ota.download", "/mnt/sdcard");
	}
}
