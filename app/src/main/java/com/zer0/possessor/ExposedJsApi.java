package com.zer0.possessor;

import android.webkit.JavascriptInterface;

import org.json.JSONException;

/**
 * Contains APIs that the JS can call. All functions in here should also have
 * an equivalent entry in CordovaChromeClient.java, and be added to
 * cordova-js/lib/android/plugin/android/promptbasednativeapi.js
 */
class ExposedJsApi
{    
    private UIBridge _bridge;

    public ExposedJsApi(UIBridge bridge)
    {
        _bridge = bridge;
    }

    @JavascriptInterface
    public String exec(int bridgeSecret, String action, String callbackId, String arguments) throws JSONException, IllegalAccessException
    {
        return _bridge.jsExec(bridgeSecret, action, callbackId, arguments);
    }
    
    @JavascriptInterface
    public void setNativeToJsBridgeMode(int bridgeSecret, int value) throws IllegalAccessException
    {
        _bridge.jsSetNativeToJsBridgeMode(bridgeSecret, value);
    }
    
    @JavascriptInterface
    public String retrieveJsMessages(int bridgeSecret, boolean fromOnlineEvent) throws IllegalAccessException
    {
        return _bridge.jsRetrieveJsMessages(bridgeSecret, fromOnlineEvent);
    }
}
