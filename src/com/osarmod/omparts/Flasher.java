package com.osarmod.omparts;

import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class Flasher {
	private static final String TAG = "OMParts.Flasher";

	private String m_file = null;
	private String m_flashPath = null;
	
	public Flasher(String file) {
		m_file = file;
		m_flashPath = getFlashPath();
	}
	
	private String getFlashPath() {
		String path = null;
		String device = OMProperties.getDevice();
		if (device.equals("galaxysmtd")) {
			path = "/sdcard/" + m_file;
		} else if (device.equals("wingray")) {
			path = "/data/media/" + m_file;
		}
		return path;
	}

	public boolean flashOtaPackage(Context ctx) {
		// we need root permissions now
		Process p;
		boolean success = false;
		try {
			p = Runtime.getRuntime().exec("sh");
			// create command file for recovery
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.writeBytes("echo \"--wipe_cache\">/cache/recovery/command\n");
			out.writeBytes("echo \"--update_package=" + m_flashPath + "\">>/cache/recovery/command\n");
			Log.v(TAG, "Created /cache/recovery/command");
			Log.v(TAG, "Rebooting into recovery");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
			}
			// out.writeBytes("reboot recovery\n");
			// out.writeBytes("exit\n");
			out.flush();                        // Trigger the reboot                                                                                     
            PowerManager powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            powerManager.reboot("recovery");			
			success = true;
		} catch (IOException e1) {
			Log.e(TAG, "sh failed: " + e1.getMessage());
		}
		return success;
	}
}
