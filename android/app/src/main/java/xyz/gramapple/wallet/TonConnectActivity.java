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
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import org.json.JSONObject;

public class TonConnectActivity extends Activity {
    static final String EXTRA_ADDRESS = "address";
    private static final String PAGE_BASE = "https://gram.org/";
    private static final String CHAIN_MAINNET = "-239";
    private WebView webView;
    private boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.app_bg);
        webView = new WebView(this);
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {WebView.setWebContentsDebuggingEnabled(true);}
        webView.setBackgroundColor(Color.parseColor("#0066DD"));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " GramWallet");
        webView.addJavascriptInterface(new Bridge(), "GramWallet");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {return openExternally(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {return openExternally(url == null ? null : Uri.parse(url));}
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final WebView popup = new WebView(TonConnectActivity.this);
                popup.getSettings().setJavaScriptEnabled(true);
                popup.setWebViewClient(new WebViewClient() {
                    private boolean handled;
                    private void handle(String url) {
                        if (handled) {return;}
                        handled = true;
                        openExternally(url == null ? null : Uri.parse(url));
                        popup.destroy();
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        handle(request.getUrl().toString());
                        return true;
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        handle(url);
                        return true;
                    }
                    @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {if (url != null && !url.startsWith("about:")) {handle(url);}}
                });
                if (resultMsg == null || !(resultMsg.obj instanceof WebView.WebViewTransport)) {return false;}
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        setContentView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.loadDataWithBaseURL(PAGE_BASE, readAsset("tonconnect.html"), "text/html", "UTF-8", null);
    }

    private boolean openExternally(Uri uri) {
        if (uri == null) {return false;}
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if (scheme.isEmpty() || "about".equals(scheme) || "data".equals(scheme) || "javascript".equals(scheme)) {return false;}
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, withBackReturn(uri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception error) {Toast.makeText(this, R.string.tonconnect_wallet_missing, Toast.LENGTH_LONG).show();}
        return true;
    }

    private Uri withBackReturn(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {return uri;}
        if (!uri.getQueryParameterNames().contains("ret")) {return uri;}
        Uri.Builder builder = uri.buildUpon().clearQuery();
        for (String name : uri.getQueryParameterNames()) {
            if ("ret".equals(name)) {builder.appendQueryParameter(name, "back");} else {for (String value : uri.getQueryParameters(name)) {builder.appendQueryParameter(name, value);}}
        }
        return builder.build();
    }

    private String readAsset(String name) {
        try (InputStream stream = getAssets().open(name)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {buffer.write(chunk, 0, read);}
            return buffer.toString("UTF-8");
        } catch (Exception error) {return "<html><body style=\"background:#0066DD;color:#fff;font-family:sans-serif;padding:24px\">"+ "Не удалось загрузить TON Connect.</body></html>";}
    }

    private final class Bridge {@JavascriptInterface public void onEvent(String json) {runOnUiThread(() -> handleEvent(json));}}

    private void handleEvent(String json) {
        if (finished || json == null) {return;}
        JSONObject event;
        try {event = new JSONObject(json);} catch (Exception error) {return;}
        String type = event.optString("type", "");
        if ("connected".equals(type)) {
            String address = TonAddress.normalize(event.optString("address", ""));
            if (address == null) {
                Toast.makeText(this, R.string.tonconnect_invalid, Toast.LENGTH_LONG).show();
                return;
            }
            String chain = event.optString("chain", "");
            if (!chain.isEmpty() && !CHAIN_MAINNET.equals(chain)) {
                Toast.makeText(this, R.string.tonconnect_testnet, Toast.LENGTH_LONG).show();
                return;
            }
            finished = true;
            Intent result = new Intent();
            result.putExtra(EXTRA_ADDRESS, address);
            setResult(RESULT_OK, result);
            finish();
        } else if ("closed".equals(type)) {
            finished = true;
            setResult(RESULT_CANCELED);
            finish();
        } else if ("error".equals(type)) {
            finished = true;
            Toast.makeText(this, R.string.tonconnect_error, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("GramWallet");
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
