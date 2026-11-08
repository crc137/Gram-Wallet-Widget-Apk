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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class Notifications {
    static final String CHANNEL_ID = "transactions";
    static final String ACTION_POLL = "xyz.gramapple.wallet.action.POLL";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private Notifications() {}

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notif_channel_description));
        manager.createNotificationChannel(channel);
    }

    static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) {return;}
        if (!Store.isNotifyEnabled(context) || Store.getAddress(context) == null) {
            manager.cancel(pollIntent(context));
            return;
        }
        ensureChannel(context);
        long now = android.os.SystemClock.elapsedRealtime();
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, now + INTERVAL, INTERVAL, pollIntent(context));
    }

    static void cancel(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) {manager.cancel(pollIntent(context));}
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (notifications != null) {notifications.cancel(NOTIFICATION_ID);}
    }

    private static PendingIntent pollIntent(Context context) {
        Intent intent = new Intent(context, NotificationReceiver.class).setAction(ACTION_POLL);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void pollAsync(Context context) {pollAsync(context, null);}

    static void pollAsync(Context context, BroadcastReceiver.PendingResult result) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {poll(appContext);}
            catch (Exception error) {android.util.Log.w("GramWallet", "Уведомления: " + error.getMessage());}
            finally {if (result != null) {result.finish();}}
        }, "gram-notify").start();
    }

    private static void poll(Context context) throws Api.ApiException {
        if (!Store.isNotifyEnabled(context)) {return;}
        String address = Store.getAddress(context);
        if (address == null) {return;}

        Api.ActionsPage page = Api.fetchActions(address, null);
        long newest = 0L;
        for (Tx tx : page.items) {
            if (tx.isNft()) {continue;}
            if (tx.time > newest) {newest = tx.time;}
        }

        if (!Store.hasNotifyBaseline(context)) {
            Store.setNotifyBaseline(context, newest > 0 ? newest : System.currentTimeMillis() / 1000L);
            return;
        }

        long baseline = Store.getNotifyBaseline(context);
        double incoming = 0d;
        double outgoing = 0d;
        int incomingCount = 0;
        int outgoingCount = 0;
        String lastCounterparty = "";
        for (Tx tx : page.items) {
            if (tx.isNft() || !tx.success || tx.time <= baseline) {continue;}
            if (tx.incoming) {
                incoming += tx.gram;
                incomingCount++;
            } else {
                outgoing += tx.gram;
                outgoingCount++;
            }
            if (!tx.counterparty.isEmpty()) {lastCounterparty = tx.counterparty;}
        }
        if (newest > baseline) {Store.setNotifyBaseline(context, newest);}
        if (incomingCount == 0 && outgoingCount == 0) {return;}

        String title;
        String text;
        if (incomingCount > 0 && outgoingCount > 0) {
            title = context.getString(R.string.notif_mixed_title);
            text = context.getString(R.string.notif_mixed_text, Format.shortGram(incoming), Format.shortGram(outgoing));
        } else if (incomingCount > 0) {
            title = context.getString(R.string.notif_incoming_title);
            text = lastCounterparty.isEmpty()? context.getString(R.string.notif_incoming_text, Format.shortGram(incoming)): context.getString(R.string.notif_incoming_from, Format.shortGram(incoming), shortName(lastCounterparty));
        } else {
            title = context.getString(R.string.notif_outgoing_title);
            text = lastCounterparty.isEmpty()? context.getString(R.string.notif_outgoing_text, Format.shortGram(outgoing)): context.getString(R.string.notif_outgoing_to, Format.shortGram(outgoing), shortName(lastCounterparty));
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent history = new Intent(context, HistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent historyIntent = PendingIntent.getActivity(context, 1, history, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(content)
                .addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_notify), context.getString(R.string.history_title), historyIntent).build());
        if (Build.VERSION.SDK_INT >= 26) {builder.setChannelId(CHANNEL_ID);}
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private static String shortName(String value) {
        if (value == null || value.isEmpty()) {return "";}
        if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
        return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
    }
}
