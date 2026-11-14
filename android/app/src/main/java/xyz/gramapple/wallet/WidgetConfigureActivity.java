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
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras != null) {appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);}
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {finish(); return;}
        setupEdgeToEdge();
        setContentView(R.layout.activity_widget_configure);
        applySystemBarInsets(findViewById(R.id.widget_configure_root));
        findViewById(R.id.widget_configure_close).setOnClickListener(v -> finish());
        bindWallets();
    }

    private void bindWallets() {
        LinearLayout list = findViewById(R.id.widget_configure_list);
        list.removeAllViews();
        List<Store.WalletEntry> wallets = Store.getWallets(this);
        float d = getResources().getDisplayMetrics().density;
        if (wallets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.save_address_first);
            empty.setTextColor(0x99FFFFFF);
            empty.setPadding(Math.round(16 * d), Math.round(16 * d), Math.round(16 * d), Math.round(16 * d));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < wallets.size(); i++) {
            Store.WalletEntry e = wallets.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Math.round(16 * d), Math.round(14 * d), Math.round(16 * d), Math.round(14 * d));
            row.setMinimumHeight(Math.round(56 * d));
            TextView title = new TextView(this);
            title.setText(e.label);
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(17f);
            TextView sub = new TextView(this);
            String shortAddr = e.address.length() > 16 ? e.address.substring(0, 4) + "…" + e.address.substring(e.address.length() - 4) : e.address;
            sub.setText(shortAddr);
            sub.setTextColor(0x99FFFFFF);
            sub.setTextSize(13f);
            row.addView(title);
            row.addView(sub);
            final String addr = e.address;
            row.setOnClickListener(v -> finishWithWallet(addr));
            list.addView(row);
            if (i < wallets.size() - 1) {
                View line = new View(this);
                line.setBackgroundColor(0x33FFFFFF);
                list.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(0.5f * d))));
            }
        }
    }

    private void finishWithWallet(String address) {
        final int widgetId = appWidgetId;
        Store.setWidgetAddress(this, widgetId, address);
        GramWidgetProvider.renderOne(getApplicationContext(), widgetId);
        final android.content.Context appContext = getApplicationContext();
        new Thread(() -> {
            try {Store.save(appContext, address, Api.fetch(address));}
            catch (Exception ignored) {}
            try {
                String cardRaw = Api.fetchCardRaw(address);
                if (cardRaw != null) {
                    Store.saveCard(appContext, address, cardRaw);
                    GiftImages.preload(appContext, Store.loadCardCache(appContext, address));
                }
            } catch (Exception ignored) {}
            GramWidgetProvider.renderOne(appContext, widgetId);
        }, "gram-widget-configure-refresh").start();
        Intent result = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        Toast.makeText(this, R.string.widget_configured, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {window.setDecorFitsSystemWindows(false);}
        else {window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);}
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {window.setNavigationBarContrastEnforced(false);}
    }

    private void applySystemBarInsets(View root) {
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left; int top; int right; int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }
}
