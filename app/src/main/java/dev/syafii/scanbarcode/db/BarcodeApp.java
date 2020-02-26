package dev.syafii.scanbarcode.db;

import android.content.Context;
import android.content.SharedPreferences;

public class BarcodeApp {
    private static final String PREF_NAME = "UserPref";
    private static SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private static String SAVE_BARCODE= "saveBarcode";

    public BarcodeApp(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public void saveBarcode(String barcode){
        editor = preferences.edit();
        editor.putString(SAVE_BARCODE, barcode);
        editor.apply();
    }
    public static String getBarcode(){
        return preferences.getString(SAVE_BARCODE, "");
    }

    public void clearSharedPreference(){
        editor = preferences.edit();
        editor.putString(SAVE_BARCODE, "");
        editor.clear();
        editor.apply();
    }
}
