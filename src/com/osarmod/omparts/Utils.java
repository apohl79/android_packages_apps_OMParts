package com.osarmod.omparts;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.SystemProperties;

public class Utils {

	public static String getVersion(String def) {
		String ver = SystemProperties.get("ro.modversion", "NO");
		if (ver.equals("NO")) {
			ver = def;
		} else {
			String[] parts = ver.split("-");
			ver = parts[parts.length - 1];
		}
		return ver;
	}

	public static String getVersionFromServer() throws Exception {
        BufferedReader in = null;
		String ver = "";
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://android.diepohls.com/galaxysmtd-cm7/VERSION"));
            HttpResponse response = client.execute(request);
            in = new BufferedReader
				(new InputStreamReader(response.getEntity().getContent()));
            ver = in.readLine();
            in.close();
		} finally {
            if (in != null) {
                try {
                    in.close();
				} catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		return ver;
	}

}
