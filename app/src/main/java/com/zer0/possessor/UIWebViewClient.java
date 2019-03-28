package com.zer0.possessor;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This class is the WebViewClient that implements callbacks for our web view.
 * The kind of callbacks that happen here are regarding the rendering of the
 * document instead of the chrome surrounding it, such as onPageStarted(), 
 * shouldOverrideUrlLoading(), etc. Related to but different than
 * CordovaChromeClient.
 *
 * @see <a href="http://developer.android.com/reference/android/webkit/WebViewClient.html">WebViewClient</a>
 * @see <a href="http://developer.android.com/guide/webapps/webview.html">WebView guide</a>
 * @see UIChromeClient
 * @see UIWebView
 */
public class UIWebViewClient extends WebViewClient
{
    UIWebView _appView;
    private boolean doClearHistory = false;
    boolean isCurrentlyLoading;

    /**
     * Constructor.
     *
     * @param cordova
     * @param view
     */
    public UIWebViewClient(UIWebView view)
    {
        _appView = view;
    }

    /**
     * Notify the host application that a page has started loading.
     * This method is called once for each main frame load so a page with iframes or framesets will call onPageStarted
     * one time for the main frame. This also means that onPageStarted will not be called when the contents of an
     * embedded frame changes, i.e. clicking a link whose target is an iframe.
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        super.onPageStarted(view, url, favicon);
        isCurrentlyLoading = true;
        // Flush stale messages.
        _appView._bridge.reset(url);

        // Broadcast message that page has loaded
        _appView.postMessage("onPageStarted", url);
    }

    /**
     * Notify the host application that a page has finished loading.
     * This method is called only for main frame. When onPageFinished() is called, the rendering picture may not be updated yet.
     *
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageFinished(WebView view, String url)
    {
        super.onPageFinished(view, url);
        // Ignore excessive calls.
        if (!isCurrentlyLoading && !url.startsWith("about:")) {
            return;
        }
        isCurrentlyLoading = false;

        /**
         * Because of a timing issue we need to clear this history in onPageFinished as well as
         * onPageStarted. However we only want to do this if the doClearHistory boolean is set to
         * true. You see when you load a url with a # in it which is common in jQuery applications
         * onPageStared is not called. Clearing the history at that point would break jQuery apps.
         */
        if (this.doClearHistory) {
            view.clearHistory();
            this.doClearHistory = false;
        }

        // Clear timeout flag
        _appView.loadUrlTimeout++;

        // Broadcast message that page has loaded
        _appView.postMessage("onPageFinished", url);
        /*
        // Make app visible after 2 sec in case there was a JS error and Cordova JS never initialized correctly
        if (_appView.getVisibility() == View.INVISIBLE) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                        MainActivity.getInstance().runOnUiThread(new Runnable() {
                            public void run() {
                            	_appView.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (InterruptedException e) {
                    }
                }
            });
            t.start();
        }       
*/
        // Shutdown if blank loaded
        if (url.equals("about:blank")) {
            _appView.postMessage("exit", null);
        }
    }

    /**
     * Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable).
     * The errorCode parameter corresponds to one of the ERROR_* constants.
     *
     * @param view          The WebView that is initiating the callback.
     * @param errorCode     The error code corresponding to an ERROR_* value.
     * @param description   A String describing the error.
     * @param failingUrl    The url that failed to load.
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // Ignore error due to stopLoading().
        if (!isCurrentlyLoading) {
            return;
        }

        // Clear timeout flag
        _appView.loadUrlTimeout++;

        // If this is a "Protocol Not Supported" error, then revert to the previous
        // page. If there was no previous page, then punt. The application's config
        // is likely incorrect (start page set to sms: or something like that)
        if (errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
            if (view.canGoBack()) {
                view.goBack();
                return;
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        // Handle other errors by passing them to the webview in JS
        JSONObject data = new JSONObject();
        try {
            data.put("errorCode", errorCode);
            data.put("description", description);
            data.put("url", failingUrl);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        _appView.postMessage("onReceivedError", data);
    }
}
