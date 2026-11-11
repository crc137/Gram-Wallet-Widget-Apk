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
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class Store {
    private static final String PREFS = "gram_wallet";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_DATA_ADDRESS = "data_address";
    private static final String KEY_GRAM = "gram";
    private static final String KEY_USD = "total_usd";
    private static final String KEY_HAS_USD = "has_usd";
    private static final String KEY_CHANGE = "change";
    private static final String KEY_UPDATED = "updated_at";
    private static final String KEY_ERROR = "error_kind";
    private static final String KEY_CARD = "card_json";
    private static final String KEY_CARD_ADDRESS = "card_address";
    private static final String KEY_MODE = "wallet_mode";
    private static final String KEY_NOTIFY = "notify_enabled";
    private static final String KEY_NOTIFY_BASELINE = "notify_baseline";
    private static final String KEY_NOTIFY_BASELINE_SET = "notify_baseline_set";
    private static final String KEY_DONOR = "donor";
    private static final String KEY_DONOR_ADDRESS = "donor_address";
    private static final String KEY_HIDE_SPAM = "hide_spam_on";
    private static final String KEY_HIST_DIR = "hist_dir";
    private static final String KEY_HIST_TYPE = "hist_type";
    private static final String KEY_HIST_PERIOD = "hist_period";
    private Store() {}

    static String getAddress(Context context) {
        String address = preferences(context).getString(KEY_ADDRESS, "");
        if (address == null) {return null;}
        address = address.trim();
        return address.isEmpty() ? null : address;
    }

    static void setAddress(Context context, String address) {preferences(context).edit().putString(KEY_ADDRESS, address).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();}
    static void clearAddress(Context context) {preferences(context).edit().remove(KEY_ADDRESS).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();}

    static String getMode(Context context) {
        String mode = preferences(context).getString(KEY_MODE, "own");
        return "other".equals(mode) ? "other" : "own";
    }

    static void setMode(Context context, String mode) {preferences(context).edit().putString(KEY_MODE, "other".equals(mode) ? "other" : "own").apply();}
    static boolean isNotifyEnabled(Context context) {return preferences(context).getBoolean(KEY_NOTIFY, false);}
    static void setNotifyEnabled(Context context, boolean enabled) {preferences(context).edit().putBoolean(KEY_NOTIFY, enabled).apply();}
    static boolean hasNotifyBaseline(Context context) {return preferences(context).getBoolean(KEY_NOTIFY_BASELINE_SET, false);}
    static long getNotifyBaseline(Context context) {return preferences(context).getLong(KEY_NOTIFY_BASELINE, 0L);}
    static void setNotifyBaseline(Context context, long time) {preferences(context).edit().putLong(KEY_NOTIFY_BASELINE, time).putBoolean(KEY_NOTIFY_BASELINE_SET, true).apply();}

    static void save(Context context, String address, WalletData data) {
        if (!address.equals(getAddress(context))) {return;}
        preferences(context).edit().putString(KEY_DATA_ADDRESS, address).putString(KEY_GRAM, Double.toString(data.gram)).putString(KEY_USD, Double.toString(data.totalUsd)).putBoolean(KEY_HAS_USD, data.hasUsd).putString(KEY_CHANGE, data.change).putLong(KEY_UPDATED, data.updatedAt).remove(KEY_ERROR).apply();
    }

    static void saveCard(Context context, String address, String cardJson) {
        if (!address.equals(getAddress(context))) {return;}
        preferences(context).edit().putString(KEY_CARD_ADDRESS, address).putString(KEY_CARD, cardJson == null ? "" : cardJson).apply();
    }

    static void setError(Context context, String address, String kind) {
        if (address == null || !address.equals(getAddress(context))) {return;}
        preferences(context).edit().putString(KEY_ERROR, kind).apply();
    }

    static String getError(Context context) {return preferences(context).getString(KEY_ERROR, null);}

    static WalletData load(Context context) {
        String address = getAddress(context);
        if (address == null) {return null;}
        SharedPreferences prefs = preferences(context);
        if (!address.equals(prefs.getString(KEY_DATA_ADDRESS, null))) {return null;}
        String gram = prefs.getString(KEY_GRAM, null);
        if (gram == null) {return null;}
        try {
            double totalUsd = Double.parseDouble(prefs.getString(KEY_USD, "0"));
            return new WalletData(Double.parseDouble(gram),totalUsd,prefs.getString(KEY_CHANGE, ""),prefs.getBoolean(KEY_HAS_USD, totalUsd > 0),prefs.getLong(KEY_UPDATED, 0L));
        } catch (NumberFormatException e) {return null;}
    }

    static CardDecor loadCard(Context context) {
        String address = getAddress(context);
        if (address == null) {return CardDecor.EMPTY;}
        SharedPreferences prefs = preferences(context);
        if (!address.equals(prefs.getString(KEY_CARD_ADDRESS, null))) {return CardDecor.EMPTY;}
        String json = prefs.getString(KEY_CARD, null);
        if (json == null || json.isEmpty()) {return CardDecor.EMPTY;}
        try {return Api.parseCard(new JSONObject(json));}
        catch (Exception error) {return CardDecor.EMPTY;}
    }

    static boolean isDonor(Context context) {
        String address = getAddress(context);
        if (address == null) {return false;}
        SharedPreferences prefs = preferences(context);
        return address.equals(prefs.getString(KEY_DONOR_ADDRESS, null)) && prefs.getBoolean(KEY_DONOR, false);
    }

    static void setDonor(Context context, String address, boolean donor) {
        if (address == null || !address.equals(getAddress(context))) {return;}
        preferences(context).edit().putString(KEY_DONOR_ADDRESS, address).putBoolean(KEY_DONOR, donor).apply();
    }

    static boolean isHideSpam(Context context) {return preferences(context).getBoolean(KEY_HIDE_SPAM, true);}
    static void setHideSpam(Context context, boolean hide) {preferences(context).edit().putBoolean(KEY_HIDE_SPAM, hide).apply();}
    static String getHistDir(Context context) {return preferences(context).getString(KEY_HIST_DIR, "all");}
    static void setHistDir(Context context, String dir) {preferences(context).edit().putString(KEY_HIST_DIR, dir == null ? "all" : dir).apply();}
    static String getHistType(Context context) {return preferences(context).getString(KEY_HIST_TYPE, "all");}
    static void setHistType(Context context, String type) {preferences(context).edit().putString(KEY_HIST_TYPE, type == null ? "all" : type).apply();}
    static String getHistPeriod(Context context) {return preferences(context).getString(KEY_HIST_PERIOD, "all");}
    static void setHistPeriod(Context context, String period) {preferences(context).edit().putString(KEY_HIST_PERIOD, period == null ? "all" : period).apply();}
    private static SharedPreferences preferences(Context context) {return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);}
}
