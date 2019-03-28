package com.zer0.possessor;

import android.content.Context;
import android.telephony.TelephonyManager;

public class PhoneManager
{
    private final Context _context;
    private TelephonyManager _telephonyMgr;


    public PhoneManager(Context context)
    {
        _context = context;
        _telephonyMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public TelephonyManager getTelephonyManager()
    {
        return _telephonyMgr;
    }

    public String getNetworkOperatorName()
    {
        String sVal = "unknown";
        try {
            String val = _telephonyMgr.getNetworkOperatorName();
            if (val != null && !val.equals("null") && !val.equals("null,null")) {
                sVal = val;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return sVal;
    }

    public String getNetworkType()
    {
        String sVal = "unknown";
        try {
            int val = _telephonyMgr.getNetworkType();
            switch (val) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    sVal = "1xRTT";
                    break;
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    sVal = "CDMA";
                    break;
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    sVal = "EDGE";
                    break;
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    sVal = "eHRPD";
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    sVal = "EVDO revision 0";
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    sVal = "EVDO revision A";
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    sVal = "EVDO revision B";
                    break;
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    sVal = "GPRS";
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    sVal = "HSDPA";
                    break;
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    sVal = "HSPA";
                    break;
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    sVal = "HSPA+";
                    break;
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    sVal = "HSUPA";
                    break;
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    sVal = "iDen";
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    sVal = "LTE";
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    sVal = "UMTS";
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return sVal;
    }

    public String getNetworkCountryIso()
    {
        String sVal = "unknown";
        try {
            final String val = _telephonyMgr.getNetworkCountryIso();
            if (val != null && !val.equals("null")) {
                sVal = val;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return sVal;
    }

    public String getSIMState()
    {
        String sVal = "unknown";
        try {
            int val = _telephonyMgr.getSimState();
            switch (val) {
                case TelephonyManager.SIM_STATE_ABSENT:
                    sVal = "absent";
                    break;
                case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                    sVal = "PIN required";
                    break;
                case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                    sVal = "PUK required";
                    break;
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                    sVal = "Network locked";
                    break;
                case TelephonyManager.SIM_STATE_READY:
                    sVal = "ready";
                    break;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return sVal;
    }

    public String getLine1Number()
    {
        return _telephonyMgr.getLine1Number();
    }
}