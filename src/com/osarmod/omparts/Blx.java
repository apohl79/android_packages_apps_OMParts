package com.osarmod.omparts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class Blx implements OnPreferenceChangeListener {
	private static final String FILE = "/sys/class/misc/batterylifeextender/charging_limit";
	
	private Context m_ctx = null;
	
	public Blx(Context ctx) {
		m_ctx = ctx;
	}
	
	@Override
	public boolean onPreferenceChange(Preference pref, Object oval) {
		String sval = (String) oval;
		SharedPreferences prefs = m_ctx.getSharedPreferences("osarmod", Context.MODE_PRIVATE);
		Editor e = prefs.edit();
		e.putString(OMParts.KEY_BLX, sval);
		e.commit();
		setChargingLimit(sval);
		return true;
	}
	
	public static boolean isSupported() {
		return new File(FILE).exists();
	}
	
    public static void setChargingLimit(String val) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(FILE));
            fos.write(val.getBytes());
            fos.flush();
            //fos.getFD().sync();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
