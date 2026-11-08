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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class GramWidgetProvider extends AppWidgetProvider {
    static final String ACTION_REFRESH = "xyz.gramapple.wallet.action.REFRESH";
    private static final long REFRESH_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        schedule(context);
        render(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)|| AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {refreshAsync(context, goAsync());}
    }

    @Override
    public void onEnabled(Context context) {schedule(context);}

    @Override
    public void onDisabled(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) {manager.cancel(refreshIntent(context));}
    }

    static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) {return;}
        long now = SystemClock.elapsedRealtime();
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,now + REFRESH_INTERVAL,REFRESH_INTERVAL,refreshIntent(context));
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent intent = new Intent(context, GramWidgetProvider.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void refreshAsync(Context context, final PendingResult pendingResult) {
        final Context appContext = context.getApplicationContext();
        final String address = Store.getAddress(appContext);
        Thread thread = new Thread(() -> {
            if (address != null) {
                try {Store.save(appContext, address, Api.fetch(address));}
                catch (Api.ApiException error) {Store.setError(appContext, address, error.kind);}
                catch (Exception error) {Store.setError(appContext, address, Api.ERROR_NETWORK);}
                try {
                    String cardRaw = Api.fetchCardRaw(address);
                    if (cardRaw != null) {
                        Store.saveCard(appContext, address, cardRaw);
                        GiftImages.preload(appContext, Store.loadCard(appContext));
                    }
                } catch (Exception ignored) {}
            }
            try {renderAll(appContext);} finally {if (pendingResult != null) {pendingResult.finish();}}
        }, "gram-widget-refresh");
        thread.start();
    }

    static void renderAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, GramWidgetProvider.class);
        render(context, manager, manager.getAppWidgetIds(provider));
    }

    private static void render(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) {return;}
        String address = Store.getAddress(context);
        WalletData data = Store.load(context);
        RemoteViews views = build(context, address, data);
        for (int id : ids) {manager.updateAppWidget(id, views);}
    }

    private static RemoteViews build(Context context, String address, WalletData data) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_gram);
        String balance = "—";
        String usd = "—";
        if (data != null) {
            balance = Format.shortGram(data.gram);
            if (data.hasUsd) {usd = Format.usd(data.totalUsd);}
        }

        int width = Math.min(972, context.getResources().getDisplayMetrics().widthPixels);
        width = Math.max(320, width);
        views.setImageViewBitmap(R.id.widget_image,CardArt.render(context, width, balance, usd, address, Store.loadCard(context)));
        views.setContentDescription(R.id.widget_image,context.getString(R.string.widget_description));
        Intent open = new Intent(context,address != null && !address.isEmpty() ? ReceiveActivity.class : MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        return views;
    }
}
