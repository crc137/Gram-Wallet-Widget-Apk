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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

final class Notifications {
    static final String CHANNEL_ID = "transactions";
    static final String CHANNEL_IN = "transactions_received";
    static final String CHANNEL_OUT = "transactions_lost";
    static final String ACTION_POLL = "xyz.gramapple.wallet.action.POLL";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private Notifications() {}

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        createSoundChannel(context, manager, CHANNEL_IN, context.getString(R.string.notif_channel_in), context.getString(R.string.notif_channel_in_desc), R.raw.received);
        createSoundChannel(context, manager, CHANNEL_OUT, context.getString(R.string.notif_channel_out), context.getString(R.string.notif_channel_out_desc), R.raw.lost);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel legacy = new NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            legacy.setDescription(context.getString(R.string.notif_channel_description));
            manager.createNotificationChannel(legacy);
        }
    }

    private static void createSoundChannel(Context context, NotificationManager manager, String id, String name, String desc, int rawSound) {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(desc);
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + rawSound);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        channel.setSound(sound, attrs);
        channel.enableVibration(true);
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
        Tx focus = null;
        for (Tx tx : page.items) {
            if (tx.isNft() || !tx.success || tx.time <= baseline) {continue;}
            if (tx.incoming) {
                incoming += tx.gram;
                incomingCount++;
            } else {
                outgoing += tx.gram;
                outgoingCount++;
            }
            if (focus == null || tx.time > focus.time) {focus = tx;}
        }
        if (newest > baseline) {Store.setNotifyBaseline(context, newest);}
        if (incomingCount == 0 && outgoingCount == 0) {return;}

        String balanceText = "";
        try {
            WalletData data = Api.fetch(address);
            Store.save(context, address, data);
            balanceText = Format.shortGram(data.gram);
        } catch (Exception ignored) {
            WalletData cached = Store.load(context);
            if (cached != null) {balanceText = Format.shortGram(cached.gram);}
        }

        boolean spamOnly = incomingCount > 0 && outgoingCount == 0 && focus != null && focus.isSpam() && incomingCount == 1;
        boolean outgoingOnly = incomingCount == 0 && outgoingCount > 0;
        String channelId = outgoingOnly ? CHANNEL_OUT : CHANNEL_IN;
        String title;
        String text;
        if (spamOnly) {
            title = context.getString(R.string.notif_spam_title);
            text = balanceText.isEmpty()? context.getString(R.string.notif_spam_text, Format.shortGram(incoming)): context.getString(R.string.notif_spam_balance, Format.shortGram(incoming), balanceText);
        } else if (incomingCount > 0 && outgoingCount > 0) {
            title = context.getString(R.string.notif_mixed_title);
            text = balanceText.isEmpty()? context.getString(R.string.notif_mixed_text, Format.shortGram(incoming), Format.shortGram(outgoing)): context.getString(R.string.notif_mixed_balance, Format.shortGram(incoming), Format.shortGram(outgoing), balanceText);
        } else if (incomingCount > 0) {
            title = context.getString(R.string.notif_incoming_title);
            String who = focus == null ? "" : shortName(focus.counterparty);
            if (balanceText.isEmpty()) {text = who.isEmpty()? context.getString(R.string.notif_incoming_text, Format.shortGram(incoming)): context.getString(R.string.notif_incoming_from, Format.shortGram(incoming), who);} else {text = who.isEmpty()? context.getString(R.string.notif_incoming_balance, Format.shortGram(incoming), balanceText): context.getString(R.string.notif_incoming_from_balance, Format.shortGram(incoming), who, balanceText);}
        } else {
            title = context.getString(R.string.notif_outgoing_title);
            String who = focus == null ? "" : shortName(focus.counterparty);
            if (balanceText.isEmpty()) {text = who.isEmpty()? context.getString(R.string.notif_outgoing_text, Format.shortGram(outgoing)): context.getString(R.string.notif_outgoing_to, Format.shortGram(outgoing), who);} else {text = who.isEmpty()? context.getString(R.string.notif_outgoing_balance, Format.shortGram(outgoing), balanceText): context.getString(R.string.notif_outgoing_to_balance, Format.shortGram(outgoing), who, balanceText);}
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        ensureChannel(context);
        Intent history = new Intent(context, HistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (focus != null) {
            history.putExtra(HistoryActivity.EXTRA_TX_ID, focus.id);
            if (focus.isSpam()) {
                history.putExtra(HistoryActivity.EXTRA_SPAM, true);
                history.putExtra(HistoryActivity.EXTRA_SPAM_AMOUNT, focus.gram);
            }
        }
        PendingIntent content = PendingIntent.getActivity(context, 0, history, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent historyIntent = PendingIntent.getActivity(context, 1, new Intent(history), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(context, channelId).setSmallIcon(R.drawable.ic_stat_notify).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(content).addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_notify), context.getString(R.string.history_title), historyIntent).build());
        if (Build.VERSION.SDK_INT >= 26) {builder.setChannelId(channelId);}
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private static String shortName(String value) {
        if (value == null || value.isEmpty()) {return "";}
        if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
        return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
    }
}
