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
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
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
    private static final String KEY_NOTIFY_PAYMENTS = "notify_payments";
    private static final String KEY_NOTIFY_GIFTS = "notify_gifts";
    private static final String KEY_NOTIFY_UPDATES = "notify_updates";
    private static final String KEY_NOTIFY_BASELINE = "notify_baseline";
    private static final String KEY_NOTIFY_BASELINE_SET = "notify_baseline_set";
    private static final String KEY_NOTIFY_UPDATE_CODE = "notify_update_code";
    private static final String KEY_DONOR = "donor";
    private static final String KEY_DONOR_ADDRESS = "donor_address";
    private static final String KEY_HIDE_SPAM = "hide_spam_on";
    private static final String KEY_HIST_DIR = "hist_dir";
    private static final String KEY_HIST_TYPE = "hist_type";
    private static final String KEY_HIST_PERIOD = "hist_period";
    private static final String KEY_WALLETS = "wallets_json";
    private static final String KEY_CHART_DAYS = "chart_days";
    private static final String KEY_CACHE = "wallet_cache_json";
    private static final String KEY_WIDGET_MAP = "widget_map_json";
    private static final String KEY_TASK_DONE = "task_done_";
    private static final String KEY_TASKS_HIDDEN = "tasks_tab_hidden";
    private static final String KEY_TASK_IDS = "task_ids_json";
    private static final String KEY_TASKS_CACHE = "tasks_cache_json";
    private static final long FRESH_MS = 15L * 60L * 1000L;
    private Store() {}
    static boolean isTaskDone(Context context, String id) {return id != null && preferences(context).getBoolean(KEY_TASK_DONE + id, false);}

    static void setTaskDone(Context context, String id, boolean done) {
        if (id == null || id.isEmpty()) {return;}
        preferences(context).edit().putBoolean(KEY_TASK_DONE + id, done).apply();
    }

    static void saveTasksCache(Context context, String json) {
        if (json == null) {return;}
        preferences(context).edit().putString(KEY_TASKS_CACHE, json).apply();
    }

    static String loadTasksCache(Context context) {return preferences(context).getString(KEY_TASKS_CACHE, null);}

    static void setTaskIds(Context context, java.util.List<String> ids) {
        org.json.JSONArray arr = new org.json.JSONArray();
        if (ids != null) {for (String id : ids) {if (id != null && !id.isEmpty()) {arr.put(id);}}}
        preferences(context).edit().putString(KEY_TASK_IDS, arr.toString()).apply();
    }

    static java.util.List<String> getTaskIds(Context context) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        String raw = preferences(context).getString(KEY_TASK_IDS, null);
        if (raw == null || raw.isEmpty()) {return out;}
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String id = arr.optString(i, "").trim();
                if (!id.isEmpty()) {out.add(id);}
            }
        } catch (Exception ignored) {}
        return out;
    }

    static boolean areAllTasksDone(Context context) {
        java.util.List<String> ids = getTaskIds(context);
        if (ids.isEmpty()) {return false;}
        for (String id : ids) {if (!isTaskDone(context, id)) {return false;}}
        return true;
    }

    static boolean isTasksTabHidden(Context context) {return preferences(context).getBoolean(KEY_TASKS_HIDDEN, false);}
    static void setTasksTabHidden(Context context, boolean hidden) {preferences(context).edit().putBoolean(KEY_TASKS_HIDDEN, hidden).apply();}

    static final class WalletEntry {
        final String address;
        final String label;
        WalletEntry(String address, String label) {
            this.address = address == null ? "" : address;
            this.label = label == null || label.trim().isEmpty() ? shortLabel(this.address) : label.trim();
        }
    }

    static String getAddress(Context context) {
        String address = preferences(context).getString(KEY_ADDRESS, "");
        if (address == null) {return null;}
        address = address.trim();
        return address.isEmpty() ? null : address;
    }

    static final int FREE_WALLET_LIMIT = 3;

    static boolean hasDonorAccess(Context context) {
        if (isDonor(context)) {return true;}
        for (WalletEntry e : getWallets(context)) {if (isDonorFor(context, e.address)) {return true;}}
        return false;
    }

    static boolean canAddWallet(Context context) {
        if (hasDonorAccess(context)) {return true;}
        return readWalletsRaw(context).size() < FREE_WALLET_LIMIT;
    }

    static boolean setAddress(Context context, String address) {
        if (address != null && !address.trim().isEmpty()) {
            String normalized = address.trim();
            if (!isWalletKnown(context, normalized) && !canAddWallet(context)) {return false;}
            preferences(context).edit().putString(KEY_ADDRESS, normalized).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();
            upsertWallet(context, normalized, null);
            return true;
        }
        preferences(context).edit().putString(KEY_ADDRESS, address).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();
        return true;
    }
    static void clearAddress(Context context) {preferences(context).edit().remove(KEY_ADDRESS).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();}

    private static List<WalletEntry> readWalletsRaw(Context context) {
        List<WalletEntry> out = new ArrayList<>();
        String raw = preferences(context).getString(KEY_WALLETS, "[]");
        try {
            JSONArray arr = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {continue;}
                String a = o.optString("address", "").trim();
                if (a.isEmpty()) {continue;}
                out.add(new WalletEntry(a, o.optString("label", "")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static List<WalletEntry> getWallets(Context context) {
        List<WalletEntry> out = readWalletsRaw(context);
        String active = getAddress(context);
        if (active != null) {
            boolean found = false;
            for (WalletEntry e : out) {if (active.equals(e.address)) {found = true; break;}}
            if (!found) {
                out.add(0, new WalletEntry(active, null));
                saveWallets(context, out);
            }
        }
        return out;
    }

    static boolean upsertWallet(Context context, String address, String label) {
        if (address == null || address.trim().isEmpty()) {return false;}
        address = address.trim();
        List<WalletEntry> list = readWalletsRaw(context);
        String keep = label;
        boolean known = false;
        for (WalletEntry e : list) {
            if (address.equals(e.address)) {
                known = true;
                if (keep == null || keep.trim().isEmpty()) {keep = e.label;}
                break;
            }
        }
        if (!known && !hasDonorAccess(context) && list.size() >= FREE_WALLET_LIMIT) {return false;}
        if (keep == null || keep.trim().isEmpty()) {keep = "Wallet " + (list.size() + 1);}
        List<WalletEntry> next = new ArrayList<>();
        next.add(new WalletEntry(address, keep));
        for (WalletEntry e : list) {
            if (!address.equals(e.address)) {next.add(e);}
        }
        saveWallets(context, next);
        return true;
    }

    static void removeWallet(Context context, String address) {
        if (address == null) {return;}
        List<WalletEntry> list = readWalletsRaw(context);
        List<WalletEntry> next = new ArrayList<>();
        for (WalletEntry e : list) {if (!address.equals(e.address)) {next.add(e);}}
        saveWallets(context, next);
        clearCacheForAddress(context, address);
        clearWidgetBindingsForAddress(context, address);
        if (address.equals(getAddress(context))) {
            if (next.isEmpty()) {clearAddress(context);}
            else {preferences(context).edit().putString(KEY_ADDRESS, next.get(0).address).remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_ERROR).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS).apply();}
        }
    }

    private static JSONObject widgetMap(Context context) {
        try {return new JSONObject(preferences(context).getString(KEY_WIDGET_MAP, "{}"));}
        catch (Exception ignored) {return new JSONObject();}
    }

    private static void saveWidgetMap(Context context, JSONObject map) {preferences(context).edit().putString(KEY_WIDGET_MAP, map == null ? "{}" : map.toString()).apply();}

    static String getWidgetAddress(Context context, int appWidgetId) {
        if (appWidgetId <= 0) {return null;}
        String key = Integer.toString(appWidgetId);
        JSONObject map = widgetMap(context);
        if (map.has(key)) {
            String bound = map.optString(key, "").trim();
            return bound.isEmpty() ? null : bound;
        }
        String active = getAddress(context);
        if (active != null) {
            try {map.put(key, active); saveWidgetMap(context, map);} catch (Exception ignored) {}
            return active;
        }
        return null;
    }

    static void setWidgetAddress(Context context, int appWidgetId, String address) {
        if (appWidgetId <= 0 || address == null || address.trim().isEmpty()) {return;}
        JSONObject map = widgetMap(context);
        try {
            map.put(Integer.toString(appWidgetId), address.trim());
            saveWidgetMap(context, map);
        } catch (Exception ignored) {}
    }

    static void clearWidgetAddress(Context context, int appWidgetId) {
        if (appWidgetId <= 0) {return;}
        JSONObject map = widgetMap(context);
        map.remove(Integer.toString(appWidgetId));
        saveWidgetMap(context, map);
    }

    static String peekWidgetAddress(Context context, int appWidgetId) {
        if (appWidgetId <= 0) {return null;}
        JSONObject map = widgetMap(context);
        String key = Integer.toString(appWidgetId);
        if (!map.has(key)) {return null;}
        String bound = map.optString(key, "").trim();
        return bound.isEmpty() ? null : bound;
    }

    static boolean isWidgetMarkedUnbound(Context context, int appWidgetId) {
        if (appWidgetId <= 0) {return false;}
        JSONObject map = widgetMap(context);
        String key = Integer.toString(appWidgetId);
        return map.has(key) && map.optString(key, "").trim().isEmpty();
    }

    static int[] widgetIdsForAddress(Context context, String address, int[] appWidgetIds) {
        if (address == null || address.isEmpty() || appWidgetIds == null || appWidgetIds.length == 0) {return new int[0];}
        boolean deletingActive = address.equals(getAddress(context));
        java.util.ArrayList<Integer> hit = new java.util.ArrayList<>();
        for (int id : appWidgetIds) {
            if (isWidgetMarkedUnbound(context, id)) {continue;}
            String bound = peekWidgetAddress(context, id);
            if (address.equals(bound) || (bound == null && deletingActive)) {hit.add(id);}
        }
        int[] out = new int[hit.size()];
        for (int i = 0; i < hit.size(); i++) {out[i] = hit.get(i);}
        return out;
    }

    static void clearWidgetBindingsForAddress(Context context, String address) {
        if (address == null || address.isEmpty()) {return;}
        JSONObject map = widgetMap(context);
        boolean changed = false;
        java.util.Iterator<String> keys = map.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (address.equals(map.optString(key, ""))) {try {map.put(key, ""); changed = true;} catch (Exception ignored) {}}
        }
        if (changed) {saveWidgetMap(context, map);}
    }

    static void markWidgetsUnbound(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {return;}
        JSONObject map = widgetMap(context);
        boolean changed = false;
        for (int id : appWidgetIds) {
            if (id <= 0) {continue;}
            try {map.put(Integer.toString(id), ""); changed = true;} catch (Exception ignored) {}
        }
        if (changed) {saveWidgetMap(context, map);}
    }

    static void clearCacheForAddress(Context context, String address) {
        if (address == null || address.isEmpty()) {return;}
        try {
            JSONObject all = cacheRoot(context);
            if (!all.has(address)) {return;}
            all.remove(address);
            preferences(context).edit().putString(KEY_CACHE, all.toString()).apply();
        } catch (Exception ignored) {}
    }

    static java.util.LinkedHashSet<String> getWidgetAddresses(Context context, int[] appWidgetIds) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (appWidgetIds == null) {return out;}
        for (int id : appWidgetIds) {
            String address = getWidgetAddress(context, id);
            if (address != null) {out.add(address);}
        }
        return out;
    }

    static boolean isWalletKnown(Context context, String address) {
        if (address == null || address.isEmpty()) {return false;}
        for (WalletEntry e : getWallets(context)) {if (address.equals(e.address)) {return true;}}
        return false;
    }

    static boolean isDonorFor(Context context, String address) {
        if (address == null || address.isEmpty()) {return false;}
        if (address.equals(getAddress(context))) {return isDonor(context);}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            return o != null && o.optBoolean("donor", false);
        } catch (Exception ignored) {return false;}
    }

    static boolean selectWallet(Context context, String address) {
        if (address == null || address.trim().isEmpty()) {return false;}
        address = address.trim();
        upsertWallet(context, address, null);
        SharedPreferences.Editor ed = preferences(context).edit().putString(KEY_ADDRESS, address).remove(KEY_ERROR).remove(KEY_NOTIFY_BASELINE).remove(KEY_NOTIFY_BASELINE_SET);
        restoreActiveFromCache(context, ed, address);
        ed.apply();
        return true;
    }

    static boolean isFresh(Context context, String address) {
        if (address == null) {return false;}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            if (o == null) {return false;}
            long at = o.optLong("at", 0L);
            return at > 0L && System.currentTimeMillis() - at < FRESH_MS;
        } catch (Exception ignored) {return false;}
    }

    static boolean hasCache(Context context, String address) {
        if (address == null) {return false;}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            return o != null && o.has("gram");
        } catch (Exception ignored) {return false;}
    }

    static WalletData loadCache(Context context, String address) {
        if (address == null) {return null;}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            if (o == null || !o.has("gram")) {return null;}
            double gram = Double.parseDouble(o.optString("gram", "0"));
            double usd = Double.parseDouble(o.optString("usd", "0"));
            return new WalletData(gram, usd, o.optString("change", ""), o.optBoolean("has_usd", usd > 0), o.optLong("at", 0L));
        } catch (Exception ignored) {return null;}
    }

    static CardDecor loadCardCache(Context context, String address) {
        if (address == null) {return CardDecor.EMPTY;}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            if (o == null) {return CardDecor.EMPTY;}
            String card = o.optString("card", "");
            if (card == null || card.isEmpty()) {return CardDecor.EMPTY;}
            return Api.parseCard(new JSONObject(card));
        } catch (Exception ignored) {return CardDecor.EMPTY;}
    }

    private static JSONObject cacheRoot(Context context) {
        try {return new JSONObject(preferences(context).getString(KEY_CACHE, "{}"));}
        catch (Exception ignored) {return new JSONObject();}
    }

    private static void putCacheData(Context context, String address, WalletData data) {
        if (address == null || data == null) {return;}
        try {
            JSONObject all = cacheRoot(context);
            JSONObject o = all.optJSONObject(address);
            if (o == null) {o = new JSONObject();}
            o.put("gram", Double.toString(data.gram));
            o.put("usd", Double.toString(data.totalUsd));
            o.put("has_usd", data.hasUsd);
            o.put("change", data.change == null ? "" : data.change);
            o.put("at", data.updatedAt);
            all.put(address, o);
            preferences(context).edit().putString(KEY_CACHE, all.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static void putCacheCard(Context context, String address, String cardJson) {
        if (address == null) {return;}
        try {
            JSONObject all = cacheRoot(context);
            JSONObject o = all.optJSONObject(address);
            if (o == null) {o = new JSONObject();}
            o.put("card", cardJson == null ? "" : cardJson);
            all.put(address, o);
            preferences(context).edit().putString(KEY_CACHE, all.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static void restoreActiveFromCache(Context context, SharedPreferences.Editor ed, String address) {
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            if (o == null || !o.has("gram")) {
                ed.remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED).remove(KEY_CARD).remove(KEY_CARD_ADDRESS).remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS);
                return;
            }
            ed.putString(KEY_DATA_ADDRESS, address).putString(KEY_GRAM, o.optString("gram", "0")).putString(KEY_USD, o.optString("usd", "0")).putBoolean(KEY_HAS_USD, o.optBoolean("has_usd", false)).putString(KEY_CHANGE, o.optString("change", "")).putLong(KEY_UPDATED, o.optLong("at", 0L));
            String card = o.optString("card", "");
            if (card == null || card.isEmpty()) {ed.remove(KEY_CARD).remove(KEY_CARD_ADDRESS);}
            else {ed.putString(KEY_CARD_ADDRESS, address).putString(KEY_CARD, card);}
            if (o.has("donor")) {ed.putString(KEY_DONOR_ADDRESS, address).putBoolean(KEY_DONOR, o.optBoolean("donor", false));}
            else {ed.remove(KEY_DONOR).remove(KEY_DONOR_ADDRESS);}
        } catch (Exception ignored) {
            ed.remove(KEY_DATA_ADDRESS).remove(KEY_GRAM).remove(KEY_USD).remove(KEY_HAS_USD).remove(KEY_CHANGE).remove(KEY_UPDATED);
        }
    }

    private static void saveWallets(Context context, List<WalletEntry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (WalletEntry e : list) {
                JSONObject o = new JSONObject();
                o.put("address", e.address);
                o.put("label", e.label);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        preferences(context).edit().putString(KEY_WALLETS, arr.toString()).apply();
    }

    private static String shortLabel(String address) {
        if (address == null || address.length() < 8) {return "Wallet";}
        return address.substring(0, 4) + "…" + address.substring(address.length() - 4);
    }

    static int getChartDays(Context context) {return preferences(context).getInt(KEY_CHART_DAYS, 7);}
    static void setChartDays(Context context, int days) {preferences(context).edit().putInt(KEY_CHART_DAYS, days <= 7 ? 7 : days <= 30 ? 30 : 90).apply();}

    static String getMode(Context context) {
        String mode = preferences(context).getString(KEY_MODE, "own");
        return "other".equals(mode) ? "other" : "own";
    }

    static void setMode(Context context, String mode) {preferences(context).edit().putString(KEY_MODE, "other".equals(mode) ? "other" : "own").apply();}
    static boolean isNotifyEnabled(Context context) {return preferences(context).getBoolean(KEY_NOTIFY, false);}
    static void setNotifyEnabled(Context context, boolean enabled) {preferences(context).edit().putBoolean(KEY_NOTIFY, enabled).apply();}
    static boolean isNotifyPayments(Context context) {return preferences(context).getBoolean(KEY_NOTIFY_PAYMENTS, true);}
    static void setNotifyPayments(Context context, boolean enabled) {preferences(context).edit().putBoolean(KEY_NOTIFY_PAYMENTS, enabled).apply();}
    static boolean isNotifyGifts(Context context) {return preferences(context).getBoolean(KEY_NOTIFY_GIFTS, true);}
    static void setNotifyGifts(Context context, boolean enabled) {preferences(context).edit().putBoolean(KEY_NOTIFY_GIFTS, enabled).apply();}
    static boolean isNotifyUpdates(Context context) {return preferences(context).getBoolean(KEY_NOTIFY_UPDATES, true);}
    static void setNotifyUpdates(Context context, boolean enabled) {preferences(context).edit().putBoolean(KEY_NOTIFY_UPDATES, enabled).apply();}
    static boolean wantsAnyNotify(Context context) {return isNotifyEnabled(context) && (isNotifyPayments(context) || isNotifyGifts(context) || isNotifyUpdates(context));}
    static boolean hasNotifyBaseline(Context context) {return preferences(context).getBoolean(KEY_NOTIFY_BASELINE_SET, false);}
    static long getNotifyBaseline(Context context) {return preferences(context).getLong(KEY_NOTIFY_BASELINE, 0L);}
    static void setNotifyBaseline(Context context, long time) {preferences(context).edit().putLong(KEY_NOTIFY_BASELINE, time).putBoolean(KEY_NOTIFY_BASELINE_SET, true).apply();}
    static long getNotifyUpdateCode(Context context) {return preferences(context).getLong(KEY_NOTIFY_UPDATE_CODE, 0L);}
    static void setNotifyUpdateCode(Context context, long code) {preferences(context).edit().putLong(KEY_NOTIFY_UPDATE_CODE, code).apply();}

    static void save(Context context, String address, WalletData data) {
        putCacheData(context, address, data);
        if (!address.equals(getAddress(context))) {return;}
        preferences(context).edit().putString(KEY_DATA_ADDRESS, address).putString(KEY_GRAM, Double.toString(data.gram)).putString(KEY_USD, Double.toString(data.totalUsd)).putBoolean(KEY_HAS_USD, data.hasUsd).putString(KEY_CHANGE, data.change).putLong(KEY_UPDATED, data.updatedAt).remove(KEY_ERROR).apply();
    }

    static void saveCard(Context context, String address, String cardJson) {
        putCacheCard(context, address, cardJson);
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

    static boolean hasCard(Context context) {
        String address = getAddress(context);
        if (address == null) {return false;}
        SharedPreferences prefs = preferences(context);
        if (!address.equals(prefs.getString(KEY_CARD_ADDRESS, null))) {return false;}
        String json = prefs.getString(KEY_CARD, null);
        return json != null && !json.isEmpty();
    }

    static boolean hasCardCache(Context context, String address) {
        if (address == null) {return false;}
        try {
            JSONObject o = cacheRoot(context).optJSONObject(address);
            if (o == null) {return false;}
            String card = o.optString("card", "");
            return card != null && !card.isEmpty();
        } catch (Exception ignored) {return false;}
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
        if (address == null) {return;}
        try {
            JSONObject all = cacheRoot(context);
            JSONObject o = all.optJSONObject(address);
            if (o == null) {o = new JSONObject();}
            o.put("donor", donor);
            all.put(address, o);
            preferences(context).edit().putString(KEY_CACHE, all.toString()).apply();
        } catch (Exception ignored) {}
        if (!address.equals(getAddress(context))) {return;}
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
