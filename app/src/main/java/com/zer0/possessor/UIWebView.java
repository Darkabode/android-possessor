package com.zer0.possessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class UIWebView extends WebView
{
    private com.zer0.possessor.Reflect _zOwnerModule;

    private BroadcastReceiver _receiver;

    /** Activities and other important classes **/
    private UIWebViewClient _viewClient;
    private UIChromeClient _chromeClient;

    // Flag to track that a loadUrl timeout occurred
    int loadUrlTimeout = 0;

    public UIBridge _bridge;

    // The URL passed to loadUrl(), not necessarily the URL of the current page.
    String loadedUrl;

    class ActivityResult {
        
        int request;
        int result;
        Intent incoming;
        
        public ActivityResult(int req, int res, Intent intent) {
            request = req;
            result = res;
            incoming = intent;
        }
    }
    
    static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
    
    public UIWebView(Context context)
    {
    	super(context, null);
    	_zOwnerModule = null;
    }

    // Use two-phase init so that the control will work with XML layouts.
    public void init(Reflect zModule, UIWebViewClient webViewClient, UIChromeClient webChromeClient)
    {
    	_zOwnerModule = zModule;
        this._viewClient = webViewClient;
        this._chromeClient = webChromeClient;
        super.setWebChromeClient(webChromeClient);
        super.setWebViewClient(webViewClient);
        _bridge = new UIBridge(this, new NativeToJsMessageQueue(this), LockActivity.getInstance().getPackageName());
        initWebViewSettings();
        exposeJsInterface();

        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    public void setOwnerModule(Reflect zModule)
    {
        _zOwnerModule = zModule;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void initWebViewSettings() {
        this.setInitialScale(0);
        this.setVerticalScrollBarEnabled(false);

		// Enable JavaScript
        WebSettings settings = this.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

        // Enable third-party cookies if on Lolipop.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(this, true);
        }

        // Set the nav dump for HTC 2.x devices (disabling for ICS, deprecated entirely for Jellybean 4.2)
        try {
            Method gingerbread_getMethod =  WebSettings.class.getMethod("setNavDump", new Class[] { boolean.class });
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB && android.os.Build.MANUFACTURER.contains("HTC")) {
                gingerbread_getMethod.invoke(settings, true);
            }
        }
        catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        //We don't save any form data in the application
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        
        // Jellybean rightfully tried to lock this down. Too bad they didn't give us a whitelist
        // while we do this
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Level16Apis.enableUniversalAccess(settings);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Level17Apis.setMediaPlaybackRequiresUserGesture(settings, false);
        }
        // Enable database
        // We keep this disabled because we use or shim to get around DOM_EXCEPTION_ERROR_16
        String databasePath = getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(databasePath);
        
        /*
        //Determine whether we're in debug or release mode, and turn on Debugging!
        ApplicationInfo appInfo = getContext().getApplicationContext().getApplicationInfo();
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            enableRemoteDebugging();
        }
        */
        settings.setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        settings.setGeolocationEnabled(true);
        
        // Enable AppCache
        // Fix for CB-2282
        settings.setAppCacheMaxSize(5 * 1048576);
        settings.setAppCachePath(databasePath);
        settings.setAppCacheEnabled(true);
        
        // Fix for CB-1405
        // Google issue 4641
        settings.getUserAgentString();
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        if (this._receiver == null) {
            this._receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    getSettings().getUserAgentString();
                }
            };
            getContext().registerReceiver(this._receiver, intentFilter);
        }
        // end CB-1405
    }

    public UIChromeClient makeWebChromeClient()
    {
        return new UIChromeClient(this);
    }

    public UIWebViewClient makeWebViewClient()
    {
        return new UIWebViewClient(this);
    }

    private void exposeJsInterface()
    {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            // Bug being that Java Strings do not get converted to JS strings automatically.
            // This isn't hard to work-around on the JS side, but it's easier to just
            // use the prompt bridge instead.
            return;            
        } 
        this.addJavascriptInterface(new ExposedJsApi(_bridge), "_cordovaNative");
    }

    @Override
    public void setWebViewClient(WebViewClient client)
    {
        this._viewClient = (UIWebViewClient)client;
        super.setWebViewClient(client);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client)
    {
        this._chromeClient = (UIChromeClient)client;
        super.setWebChromeClient(client);
    }
    
    public UIChromeClient getWebChromeClient()
    {
        return this._chromeClient;
    }

    /**
     * Load the url into the webview.
     *
     * @param url
     */
    @Override
    public void loadUrl(String url)
    {
        if (url.equals("about:blank") || url.startsWith("javascript:")) {
            loadUrlNow(url);
        }
        else {
            loadUrlIntoView(url);
        }
    }

    public void loadUrlIntoView(final String url)
    {
        this.loadedUrl = url;

        // Create a timeout timer for loadUrl
        final UIWebView me = this;
        final int currentLoadUrlTimeout = me.loadUrlTimeout;
        final int loadUrlTimeoutValue = 20000;

        // Timeout error method
        final Runnable loadError = new Runnable() {
            public void run() {
                me.stopLoading();
                if (_viewClient != null) {
                    _viewClient.onReceivedError(me, -6, "The connection to the server was unsuccessful.", url);
                }
            }
        };

        // Timeout timer method
        final Runnable timeoutCheck = new Runnable() {
            public void run() {
                try {
                    synchronized (this) {
                        wait(loadUrlTimeoutValue);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // If timeout, then stop loading and handle error
                    if (me.loadUrlTimeout == currentLoadUrlTimeout) {
                        LockActivity a = LockActivity.getInstance();
                        if (a != null) {
                            a.runOnUiThread(loadError);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Load url
        LockActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                ZRuntime.getInstance(null).getThreadPool().execute(timeoutCheck);
                me.loadUrlNow(url);
            }
        });
    }

    void loadUrlNow(String url)
    {
        HashMap<String, String> hdrs = new HashMap<>();
        hdrs.put("device", "android");
        super.loadUrl(url, hdrs);
    }

    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     *
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    public void loadUrlIntoView(final String url, final int time)
    {
        // Load url
        this.loadUrlIntoView(url);
    }
    
    @Override
    public void stopLoading()
    {
        _viewClient.isCurrentlyLoading = false;
        super.stopLoading();
    }
    
    public void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    /**
     * Send a plugin result back to JavaScript.
     * (This is a convenience method)
     *
     * @param result
     * @param callbackId
     */
    public void sendModuleResult(ModuleResult result, String callbackId)
    {
        this._bridge.getMessageQueue().addPluginResult(result, callbackId);
    }

    /**
     * Send a message to all plugins.
     *
     * @param id            The message id
     * @param data          The message data
     */
    public void postMessage(String id, Object data)
    {
    	if (_zOwnerModule != null && _zOwnerModule.get() != null) {
//            _zOwnerModule.call("onMessage", id, data);
	        //_zOwnerModule.onMessage(id, data);
        }
    }


    /**
     * Go to previous page in history.  (We manage our own history)
     *
     * @return true if we went back, false if we are already at top
     */
    public boolean backHistory()
    {
        // Check webview first to see if there is a history
        // This is needed to support curPage#diffLink, since they are added to appView's history, but not our history url array (JQMobile behavior)
        if (super.canGoBack()) {
            super.goBack();
            return true;
        }
        return false;
    }

    public void handlePause()
    {
        // Send pause event to JavaScript
        //loadUrl("javascript:try{cordova.fireDocumentEvent('pause');}catch(e){console.log('exception firing pause event from native: ' + e);};");
        //onPause();
    }
    
    public void handleResume()
    {
        //loadUrl("javascript:try{cordova.fireDocumentEvent('resume');}catch(e){console.log('exception firing resume event from native:' + e);};");
        //onResume();
    }
    
    public void handleDestroy()
    {
        // Cancel pending timeout timer.
        loadUrlTimeout++;

        // Load blank page so that JavaScript onunload is called
        loadUrl("about:blank");

        //Remove last AlertDialog
        _chromeClient.destroyLastDialog();

        // unregister the receiver
        if (_receiver != null) {
            try {
                getContext().unregisterReceiver(_receiver);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Wrapping these functions in their own class prevents warnings in adb like:
    // VFY: unable to resolve virtual method 285: Landroid/webkit/WebSettings;.setAllowUniversalAccessFromFileURLs
    @TargetApi(16)
    private static final class Level16Apis {
        static void enableUniversalAccess(WebSettings settings) {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
    }

    @TargetApi(17)
    private static final class Level17Apis {
        static void setMediaPlaybackRequiresUserGesture(WebSettings settings, boolean value) {
            settings.setMediaPlaybackRequiresUserGesture(value);
        }
    }
    
    //Can Go Back is BROKEN!
    public boolean startOfHistory()
    {
        WebBackForwardList currentList = this.copyBackForwardList();
        WebHistoryItem item = currentList.getItemAtIndex(0);
        if( item!=null){	// Null-fence in case they haven't called loadUrl yet (CB-2458)
	        String url = item.getUrl();
	        String currentUrl = this.getUrl();
	        return currentUrl.equals(url);
        }
        return false;
    }
    
    public WebBackForwardList restoreState(Bundle savedInstanceState)
    {
        WebBackForwardList myList = super.restoreState(savedInstanceState);
        return myList;
    }

    /**
     * Receives a request for execution and fulfills it by finding the appropriate
     * Java class and calling it's execute method.
     *
     * PluginManager.exec can be used either synchronously or async. In either case, a JSON encoded
     * string is returned that will indicate if any errors have occurred when trying to find
     * or execute the class denoted by the clazz argument.
     *
     * @param action        String containing the action that the class is supposed to perform. This is
     *                      passed to the plugin execute method and it is up to the plugin developer
     *                      how to deal with it.
     * @param callbackId    String containing the id of the callback that is execute in JavaScript if
     *                      this is an async plugin call.
     * @param rawArgs       An Array literal string containing any arguments needed in the
     *                      plugin execute method.
     */
    public void exec(final String action, final String callbackId, final String rawArgs)
    {
        if (_zOwnerModule == null || _zOwnerModule.get() == null) {
            sendModuleResult(new ModuleResult(ModuleResult.Status.CLASS_NOT_FOUND_EXCEPTION), callbackId);
            return;
        }
        try {
            boolean wasValidAction = _zOwnerModule.call("uiExecute", action, rawArgs, callbackId).get();

            if (wasValidAction) {
            	sendModuleResult(new ModuleResult(ModuleResult.Status.OK), callbackId);
            }
            else {
            	sendModuleResult(new ModuleResult(ModuleResult.Status.INVALID_ACTION), callbackId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e.getCause() instanceof JSONException) {
                sendModuleResult(new ModuleResult(ModuleResult.Status.JSON_EXCEPTION), callbackId);
            }
            else {
                sendModuleResult(new ModuleResult(ModuleResult.Status.ERROR, e.getMessage()), callbackId);
            }
        }
    }
}
