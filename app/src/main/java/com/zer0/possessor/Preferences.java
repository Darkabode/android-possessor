package com.zer0.possessor;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences
{
    final private Context _context;

    public Preferences(Context context)
    {
        _context = context;
    }

    private SharedPreferences getDefaultSharedPreferences()
    {
        return _context.getSharedPreferences("sp", Context.MODE_PRIVATE);
    }

    public void saveString(String key, String value)
    {
        SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }
    public String loadString(String key, String defaultValue)
    {
        SharedPreferences settings = getDefaultSharedPreferences();
        return settings.getString(key, defaultValue);
    }
    public void saveInt(String key, int value)
    {
        SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }
    public int loadInt(String key, int defaultValue)
    {
        SharedPreferences settings = getDefaultSharedPreferences();
        return settings.getInt(key, defaultValue);
    }
    public void saveLong(String key, long value)
    {
        SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
        editor.putLong(key, value);
        editor.apply();
    }
    public long loadLong(String key, long defaultValue)
    {
        SharedPreferences settings = getDefaultSharedPreferences();
        return settings.getLong(key, defaultValue);
    }
    public void saveBoolean(String key, boolean value)
    {
        SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    public boolean loadBoolean(String key, boolean defaultValue)
    {
        SharedPreferences settings = _context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        return settings.getBoolean(key, defaultValue);
    }
}