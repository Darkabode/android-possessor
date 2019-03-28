package com.zer0.possessor;

import org.json.JSONArray;
import org.json.JSONException;
import java.security.SecureRandom;

/**
 * Contains APIs that the JS can call. All functions in here should also have
 * an equivalent entry in CordovaChromeClient.java, and be added to
 * cordova-js/lib/android/plugin/android/promptbasednativeapi.js
 */
public class UIBridge
{
    private UIWebView _appView;
    private NativeToJsMessageQueue _jsMessageQueue;
    private volatile int expectedBridgeSecret = -1; // written by UI thread, read by JS thread.
    private String appContentUrlPrefix;

    public UIBridge(UIWebView appView, NativeToJsMessageQueue jsMessageQueue, String packageName)
    {
        _appView = appView;
        _jsMessageQueue = jsMessageQueue;
        appContentUrlPrefix = "content://" + packageName + ".";
    }
    
    public String jsExec(int bridgeSecret, String action, String callbackId, String arguments) throws JSONException, IllegalAccessException
    {
        if (!verifySecret("exec()", bridgeSecret)) {
            return null;
        }
        // If the arguments weren't received, send a message back to JS.  It will switch bridge modes and try again.  See CB-2666.
        // We send a message meant specifically for this case.  It starts with "@" so no other message can be encoded into the same string.
        if (arguments == null) {
            return "@Null arguments.";
        }

        _jsMessageQueue.setPaused(true);
        try {
            _appView.exec(action, callbackId, arguments);
            return _jsMessageQueue.popAndEncode(false);
        }
        catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
        finally {
            _jsMessageQueue.setPaused(false);
        }
    }

    public void jsSetNativeToJsBridgeMode(int bridgeSecret, int value) throws IllegalAccessException
    {
        if (!verifySecret("setNativeToJsBridgeMode()", bridgeSecret)) {
            return;
        }
        _jsMessageQueue.setBridgeMode(value);
    }

    public String jsRetrieveJsMessages(int bridgeSecret, boolean fromOnlineEvent) throws IllegalAccessException
    {
        if (!verifySecret("retrieveJsMessages()", bridgeSecret)) {
            return null;
        }
        return _jsMessageQueue.popAndEncode(fromOnlineEvent);
    }

    private boolean verifySecret(String action, int bridgeSecret) throws IllegalAccessException
    {
        if (!_jsMessageQueue.isBridgeEnabled()) {
            return false;
        }
        // Bridge secret wrong and bridge not due to it being from the previous page.
        if (expectedBridgeSecret < 0 || bridgeSecret != expectedBridgeSecret) {
            clearBridgeSecret();
            throw new IllegalAccessException();
        }
        return true;
    }

    /** Called on page transitions */
    void clearBridgeSecret()
    {
        expectedBridgeSecret = -1;
    }

    /** Called by cordova.js to initialize the bridge. */
    int generateBridgeSecret()
    {
        SecureRandom randGen = new SecureRandom();
        expectedBridgeSecret = randGen.nextInt(Integer.MAX_VALUE);
        return expectedBridgeSecret;
    }

    public void reset(String loadedUrl)
    {
        _jsMessageQueue.reset();
        clearBridgeSecret();
    }

    public String promptOnJsPrompt(String origin, String message, String defaultValue)
    {
        if (defaultValue != null && defaultValue.length() > 3 && defaultValue.startsWith("gap:")) {
            JSONArray array;
            try {
                array = new JSONArray(defaultValue.substring(4));
                int bridgeSecret = array.getInt(0);
                String action = array.getString(1);
                String callbackId = array.getString(2);
                String r = jsExec(bridgeSecret, action, callbackId, message);
                return r == null ? "" : r;
            }
            catch (JSONException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return "";
        }
        // Sets the native->JS bridge mode. 
        else if (defaultValue != null && defaultValue.startsWith("gap_bridge_mode:")) {
            try {
                int bridgeSecret = Integer.parseInt(defaultValue.substring(16));
                jsSetNativeToJsBridgeMode(bridgeSecret, Integer.parseInt(message));
            }
            catch (NumberFormatException | IllegalAccessException e){
                e.printStackTrace();
            }
            return "";
        }
        // Polling for JavaScript messages 
        else if (defaultValue != null && defaultValue.startsWith("gap_poll:")) {
            int bridgeSecret = Integer.parseInt(defaultValue.substring(9));
            try {
                String r = jsRetrieveJsMessages(bridgeSecret, "1".equals(message));
                return r == null ? "" : r;
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return "";
        }
        else if (defaultValue != null && defaultValue.startsWith("gap_init:")) {
            // Protect against random iframes being able to talk through the bridge.
            if (origin.startsWith("file:") || origin.startsWith(this.appContentUrlPrefix)) {
                // Enable the bridge
                int bridgeMode = Integer.parseInt(defaultValue.substring(9));
                _jsMessageQueue.setBridgeMode(bridgeMode);
                // Tell JS the bridge secret.
                int secret = generateBridgeSecret();
                return ""+secret;
            }
            return "";
        }
        return null;
    }
    
    public NativeToJsMessageQueue getMessageQueue()
    {
        return _jsMessageQueue;
    }
}
