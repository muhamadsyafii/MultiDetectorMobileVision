package dev.syafii.scanbarcode.db;

import android.content.Context;
import android.content.SharedPreferences;

public class BarcodeApp {
    private static final String PREF_NAME = "UserPref";
    private static SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public BarcodeApp(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public void saveString(String key, String value) {
        editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    public void clearSharedPreference(){
        editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}
