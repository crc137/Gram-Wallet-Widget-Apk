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
import android.content.pm.PackageInfo;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;

final class Notifications {
    static final String CHANNEL_ID = "transactions";
    static final String CHANNEL_IN = "tx_in_v2";
    static final String CHANNEL_OUT = "tx_out_v2";
    static final String CHANNEL_GIFTS = "gifts_nft_v2";
    static final String CHANNEL_UPDATES = "app_new_version_v2";
    static final String ACTION_POLL = "xyz.gramapple.wallet.action.POLL";
    private static final int ID_PAYMENTS = 1001;
    private static final int ID_GIFTS = 1002;
    private static final int ID_UPDATES = 1003;
    private static final long INTERVAL = 3L * 60L * 1000L;
    private Notifications() {}

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        deleteLegacyChannels(manager);
        createSoundChannel(context, manager, CHANNEL_IN, context.getString(R.string.notif_channel_in), context.getString(R.string.notif_channel_in_desc), "received");
        createSoundChannel(context, manager, CHANNEL_OUT, context.getString(R.string.notif_channel_out), context.getString(R.string.notif_channel_out_desc), "lost");
        createSoundChannel(context, manager, CHANNEL_GIFTS, context.getString(R.string.notif_channel_gifts), context.getString(R.string.notif_channel_gifts_desc), "nft");
        createSoundChannel(context, manager, CHANNEL_UPDATES, context.getString(R.string.notif_channel_updates), context.getString(R.string.notif_channel_updates_desc), "new_version");
    }

    private static void deleteLegacyChannels(NotificationManager manager) {
        String[] legacy = {"transactions", "transactions_received", "transactions_lost", "gifts_nft", "app_new_version"};
        for (String id : legacy) {try {if (manager.getNotificationChannel(id) != null) {manager.deleteNotificationChannel(id);}} catch (Exception ignored) {}}
    }

    private static void createSoundChannel(Context context, NotificationManager manager, String id, String name, String desc, String rawName) {
        if (manager.getNotificationChannel(id) != null) {return;}
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(desc);
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/" + rawName);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        channel.setSound(sound, attrs);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 280, 140, 280});
        channel.enableLights(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) {return;}
        PendingIntent intent = pollIntent(context);
        if (!Store.wantsAnyNotify(context)) {
            manager.cancel(intent);
            return;
        }
        boolean needWallet = Store.isNotifyPayments(context) || Store.isNotifyGifts(context);
        if (needWallet && Store.getAddress(context) == null && !Store.isNotifyUpdates(context)) {
            manager.cancel(intent);
            return;
        }
        ensureChannel(context);
        long trigger = SystemClock.elapsedRealtime() + INTERVAL;
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, intent);
    }

    static void cancel(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) {manager.cancel(pollIntent(context));}
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (notifications != null) {
            notifications.cancel(ID_PAYMENTS);
            notifications.cancel(ID_GIFTS);
            notifications.cancel(ID_UPDATES);
        }
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
            finally {
                try {if (Store.wantsAnyNotify(appContext)) {schedule(appContext);}} catch (Exception ignored) {}
                if (result != null) {result.finish();}
            }
        }, "gram-notify").start();
    }

    private static void poll(Context context) throws Api.ApiException {
        if (!Store.isNotifyEnabled(context)) {return;}
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null && !nm.areNotificationsEnabled()) {return;}
        if (Store.isNotifyUpdates(context)) {pollUpdates(context);}
        if (!Store.isNotifyPayments(context) && !Store.isNotifyGifts(context)) {return;}
        String address = Store.getAddress(context);
        if (address == null) {return;}

        Api.ActionsPage page = Api.fetchActions(address, null);
        long newest = 0L;
        for (Tx tx : page.items) {if (tx.time > newest) {newest = tx.time;}}

        if (!Store.hasNotifyBaseline(context)) {
            Store.setNotifyBaseline(context, newest > 0 ? newest : System.currentTimeMillis() / 1000L);
            return;
        }

        long baseline = Store.getNotifyBaseline(context);
        double incoming = 0d;
        double outgoing = 0d;
        int incomingCount = 0;
        int outgoingCount = 0;
        Tx focusPay = null;
        List<Tx> gifts = new ArrayList<>();
        for (Tx tx : page.items) {
            if (!tx.success || tx.time <= baseline) {continue;}
            if (tx.isNft()) {
                if (Store.isNotifyGifts(context) && tx.incoming) {gifts.add(tx);}
                continue;
            }
            if (!Store.isNotifyPayments(context)) {continue;}
            if (!Tx.TYPE_TON.equals(tx.type) && tx.gram <= 0d) {continue;}
            if (tx.incoming) {incoming += tx.gram; incomingCount++;}
            else {outgoing += tx.gram; outgoingCount++;}
            if (focusPay == null || tx.time > focusPay.time) {focusPay = tx;}
        }
        if (newest > baseline) {Store.setNotifyBaseline(context, newest);}
        if (Store.isNotifyPayments(context) && (incomingCount > 0 || outgoingCount > 0)) {postPayment(context, address, incoming, outgoing, incomingCount, outgoingCount, focusPay);}
        if (Store.isNotifyGifts(context) && !gifts.isEmpty()) {postGifts(context, gifts);}
    }

    private static void pollUpdates(Context context) {
        try {
            Api.AppRelease release = Api.fetchLatestRelease();
            if (release == null || release.apkUrl.isEmpty()) {return;}
            long theirs = Api.releaseRank(release);
            long installed = currentVersionRank(context);
            if (theirs <= 0 || theirs <= installed) {return;}
            if (theirs <= Store.getNotifyUpdateCode(context)) {return;}
            Store.setNotifyUpdateCode(context, theirs);
            String ver = release.tag != null && !release.tag.isEmpty() ? release.tag : (release.name == null || release.name.isEmpty() ? Long.toString(release.code) : release.name);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {return;}
            ensureChannel(context);
            Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent content = PendingIntent.getActivity(context, 3, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            String title = context.getString(R.string.notif_update_title);
            String text = context.getString(R.string.notif_update_text, ver);
            Notification.Builder builder = new Notification.Builder(context, CHANNEL_UPDATES).setSmallIcon(R.drawable.ic_stat_notify).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(content).setCategory(Notification.CATEGORY_STATUS);
            manager.notify(ID_UPDATES, builder.build());
        } catch (Exception ignored) {}
    }

    private static long currentVersionRank(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return Api.versionRank(info.versionName);
        } catch (Exception error) {return 0L;}
    }

    private static void postPayment(Context context, String address, double incoming, double outgoing, int incomingCount, int outgoingCount, Tx focus) {
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
            text = balanceText.isEmpty() ? context.getString(R.string.notif_spam_text, Format.shortGram(incoming)) : context.getString(R.string.notif_spam_balance, Format.shortGram(incoming), balanceText);
        } else if (incomingCount > 0 && outgoingCount > 0) {
            title = context.getString(R.string.notif_mixed_title);
            text = balanceText.isEmpty() ? context.getString(R.string.notif_mixed_text, Format.shortGram(incoming), Format.shortGram(outgoing)) : context.getString(R.string.notif_mixed_balance, Format.shortGram(incoming), Format.shortGram(outgoing), balanceText);
        } else if (incomingCount > 0) {
            title = context.getString(R.string.notif_incoming_title);
            String who = focus == null ? "" : shortName(focus.counterparty);
            if (balanceText.isEmpty()) {text = who.isEmpty() ? context.getString(R.string.notif_incoming_text, Format.shortGram(incoming)) : context.getString(R.string.notif_incoming_from, Format.shortGram(incoming), who);}
            else {text = who.isEmpty() ? context.getString(R.string.notif_incoming_balance, Format.shortGram(incoming), balanceText) : context.getString(R.string.notif_incoming_from_balance, Format.shortGram(incoming), who, balanceText);}
        } else {
            title = context.getString(R.string.notif_outgoing_title);
            String who = focus == null ? "" : shortName(focus.counterparty);
            if (balanceText.isEmpty()) {text = who.isEmpty() ? context.getString(R.string.notif_outgoing_text, Format.shortGram(outgoing)) : context.getString(R.string.notif_outgoing_to, Format.shortGram(outgoing), who);}
            else {text = who.isEmpty() ? context.getString(R.string.notif_outgoing_balance, Format.shortGram(outgoing), balanceText) : context.getString(R.string.notif_outgoing_to_balance, Format.shortGram(outgoing), who, balanceText);}
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
        Notification.Builder builder = new Notification.Builder(context, channelId).setSmallIcon(R.drawable.ic_stat_notify).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(content).setCategory(Notification.CATEGORY_MESSAGE).setVisibility(Notification.VISIBILITY_PUBLIC).addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_notify), context.getString(R.string.history_title), historyIntent).build());
        manager.notify(ID_PAYMENTS, builder.build());
    }

    private static void postGifts(Context context, List<Tx> gifts) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {return;}
        ensureChannel(context);
        Tx focus = gifts.get(0);
        for (Tx tx : gifts) {if (tx.time > focus.time) {focus = tx;}}
        String title = context.getString(R.string.notif_gift_title);
        String text = gifts.size() == 1
            ? context.getString(R.string.notif_gift_text, focus.nftName.isEmpty() ? context.getString(R.string.tx_nft) : focus.nftName)
            : context.getString(R.string.notif_gift_multi, gifts.size());
        Intent history = new Intent(context, HistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        history.putExtra(HistoryActivity.EXTRA_TX_ID, focus.id);
        PendingIntent content = PendingIntent.getActivity(context, 2, history, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(context, CHANNEL_GIFTS).setSmallIcon(R.drawable.ic_stat_notify).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(content).setCategory(Notification.CATEGORY_MESSAGE).setVisibility(Notification.VISIBILITY_PUBLIC);
        manager.notify(ID_GIFTS, builder.build());
    }

    private static String shortName(String value) {
        if (value == null || value.isEmpty()) {return "";}
        if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
        return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
    }
}
