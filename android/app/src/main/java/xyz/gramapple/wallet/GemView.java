/*
✨ CoonDev • https://dev.coonlink.com/

 ▄█▄    ████▄ ████▄    ▄   ██▄   ▄███▄      ▄
 █▀ ▀▄  █   █ █   █     █  █  █  █▀   ▀      █
 █   ▀  █   █ █   █ ██   █ █   █ ██▄▄   █     █
 █▄  ▄▀ ▀████ ▀████ █ █  █ █  █  █▄   ▄▀ █    █
 ▀███▀              █  █ █ ███▀  ▀███▀    █  █
                    █   ██                 █▐
                                           ▐
*/

package xyz.gramapple.wallet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

public final class GemView extends FrameLayout {
    private final ImageView preview;
    private WebView web;
    private boolean loaded;
    private boolean wanted;

    public GemView(Context context) {this(context, null);}
    public GemView(Context context, AttributeSet attrs) {this(context, attrs, 0);}
    public GemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        preview = new ImageView(context);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setImageResource(R.drawable.blue_diamond);
        addView(preview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));
    }

    public void wake() {
        wanted = true;
        ensureWeb();
        if (web == null) {return;}
        if (!loaded) {
            loaded = true;
            web.loadUrl("file:///android_asset/gem/index.html");
        } else {web.evaluateJavascript("window.GemControl&&window.GemControl.wake()", null);}
    }

    public void sleep() {
        wanted = false;
        if (web != null) {web.evaluateJavascript("window.GemControl&&window.GemControl.sleep()", null);}
        preview.setVisibility(VISIBLE);
        if (web != null) {web.setVisibility(INVISIBLE);}
    }

    public void destroyGem() {
        wanted = false;
        if (web != null) {
            try {web.evaluateJavascript("window.GemControl&&window.GemControl.sleep()", null);} catch (Exception ignored) {}
            removeView(web);
            web.destroy();
            web = null;
        }
        loaded = false;
        preview.setVisibility(VISIBLE);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void ensureWeb() {
        if (web != null) {return;}
        web = new WebView(getContext());
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setVisibility(INVISIBLE);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 26) {settings.setSafeBrowsingEnabled(false);}
        try {settings.setAllowFileAccessFromFileURLs(true); settings.setAllowUniversalAccessFromFileURLs(true);} catch (Exception ignored) {}
        web.addJavascriptInterface(new Bridge(), "GemBridge");
        web.setWebViewClient(new WebViewClient() {@Override public void onPageFinished(WebView view, String url) {if (wanted) {view.evaluateJavascript("window.GemControl&&window.GemControl.wake()", null);}}});
        addView(web, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));
    }

    private final class Bridge {
        @JavascriptInterface public void onReady() {post(() -> {if (web != null) {web.setVisibility(VISIBLE); preview.setVisibility(INVISIBLE);}});}
        @JavascriptInterface public void onSleep() {post(() -> {preview.setVisibility(VISIBLE); if (web != null) {web.setVisibility(INVISIBLE);}});}
        @JavascriptInterface public void onError() {post(() -> {preview.setVisibility(VISIBLE); if (web != null) {web.setVisibility(INVISIBLE);}});}
    }
}
