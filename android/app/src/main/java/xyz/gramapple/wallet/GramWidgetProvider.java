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
import java.util.LinkedHashSet;

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

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null) {return;}
        for (int id : appWidgetIds) {Store.clearWidgetAddress(context, id);}
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
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        ComponentName provider = new ComponentName(appContext, GramWidgetProvider.class);
        final int[] ids = manager.getAppWidgetIds(provider);
        final LinkedHashSet<String> addresses = Store.getWidgetAddresses(appContext, ids);
        Thread thread = new Thread(() -> {
            for (String address : addresses) {
                try {Store.save(appContext, address, Api.fetch(address));}
                catch (Api.ApiException error) {Store.setError(appContext, address, error.kind);}
                catch (Exception error) {Store.setError(appContext, address, Api.ERROR_NETWORK);}
                try {
                    String cardRaw = Api.fetchCardRaw(address);
                    if (cardRaw != null) {
                        Store.saveCard(appContext, address, cardRaw);
                        CardDecor decor = Store.loadCardCache(appContext, address);
                        if (decor != null && !decor.isEmpty() && !GiftImages.isReady(appContext, decor)) {GiftImages.preload(appContext, decor);}
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

    static void renderOne(Context context, int appWidgetId) {
        if (appWidgetId <= 0) {return;}
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        render(context, manager, new int[]{appWidgetId});
    }

    private static void render(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) {return;}
        for (int id : ids) {
            String address = Store.getWidgetAddress(context, id);
            if (address != null && !Store.isWalletKnown(context, address)) {
                Store.clearWidgetBindingsForAddress(context, address);
                address = null;
            }
            WalletData data = address == null ? null : Store.loadCache(context, address);
            CardDecor card = address == null ? CardDecor.EMPTY : Store.loadCardCache(context, address);
            manager.updateAppWidget(id, build(context, id, address, data, card));
        }
    }

    private static RemoteViews build(Context context, int appWidgetId, String address, WalletData data, CardDecor card) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_gram);
        String balance = "—";
        String usd = "—";
        if (address == null) {
            balance = context.getString(R.string.widget_set_address);
            usd = "";
        } else if (data != null) {
            balance = Format.shortGram(data.gram);
            if (data.hasUsd) {usd = Format.usd(data.totalUsd);}
        }

        int width = Math.min(972, context.getResources().getDisplayMetrics().widthPixels);
        width = Math.max(320, width);
        views.setImageViewBitmap(R.id.widget_image,CardArt.render(context, width, balance, usd, address, card == null ? CardDecor.EMPTY : card));
        views.setContentDescription(R.id.widget_image,context.getString(R.string.widget_description));

        Intent open;
        if (address == null || address.isEmpty()) {open = new Intent(context, WidgetConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);} else {open = new Intent(context, ReceiveActivity.class).putExtra(ReceiveActivity.EXTRA_ADDRESS, address).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);}
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        return views;
    }
}
