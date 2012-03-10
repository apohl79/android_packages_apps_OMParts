package com.osarmod.omparts;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadThread implements Runnable {
	private static final String TAG = "OMParts.DownloadThread";
	public static final int PROGRESS = 0;
	public static final int FINISHED = 1;
	public static final int FAILED = 2;
	
	Handler m_handler = null;
	String m_serverPath = null;
	String m_localPath = null;
	
	public DownloadThread(Handler h, String serverPath, String localPath) {
		m_handler = h;
		m_serverPath = serverPath;
		m_localPath = localPath;
	}

	/**
	 * Download the ota package to the external sdcard.
	 */
	public void run() {
		File file = null;
		try {
			URL url = new URL(m_serverPath);
			file = new File(m_localPath);

			URLConnection con = url.openConnection();
			int len = con.getContentLength();

			Log.d(TAG, "Downloading " + url + " to " + file);
			Log.d(TAG, len + " bytes to download");

			if (len > 0) {
				BufferedInputStream in = new BufferedInputStream(con.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
				byte[] buf = new byte[1024];
				int bytes = 0;
				long total = 0;
				long last_perc = -1;
				do {
					bytes = in.read(buf, 0, 1024);
					out.write(buf, 0, bytes);
					total += bytes;
					long perc = total * 100 / len;
					if (perc != last_perc) {
						Message m = new Message();
						m.what = PROGRESS;
						m.arg1 = (int) perc;
						m_handler.sendMessage(m);
						last_perc = perc;
					}
				} while (total < len);
				in.close();
				out.close();
				Log.d(TAG, len + " bytes downloaded");
			}
			m_handler.sendEmptyMessage(FINISHED);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			if (null != file && file.exists()) {
				file.delete();
			}
			m_handler.sendEmptyMessage(FAILED);
		}
	}
}
