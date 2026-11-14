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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

final class GiftImages {
    private static final String API = "https://api.changes.tg/";
    private static final String CDN = "https://cdn.changes.tg/gifts/models/";
    private static final String CDN_PATTERNS = "https://cdn.changes.tg/gifts/patterns/";
    private static final String TONAPI_NFT = "https://tonapi.io/v2/nfts/";
    private static final String TGS_FALLBACK = "https://t.me/i/nft/";
    private static final int RENDER_SIZE = 512;
    private static final int CONNECT_MS = 6000;
    private static final int READ_MS = 12000;
    private static final long CACHE_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final Map<String, String> SPECIAL_NAMES = new HashMap<>();
    private static final Map<String, Bitmap> MEMORY = new HashMap<>();
    private static final Map<String, String> TRAITS = new HashMap<>();
    private static final Map<String, TgPage> PAGES = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static final Set<String> IN_FLIGHT = new HashSet<>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Pattern OG_DESC = Pattern.compile("property=\"og:description\"\\s+content=\"([^\"]*)\"|content=\"([^\"]*)\"\\s+property=\"og:description\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern GIFT_PATTERN = Pattern.compile("id=\"giftPattern\"[^>]*xlink:href=\"([^\"]+)\"|xlink:href=\"([^\"]+)\"[^>]*id=\"giftPattern\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIFT_TGS = Pattern.compile("type=\"application/x-tgsticker\"[^>]*srcset=\"([^\"]+)\"|srcset=\"([^\"]+)\"[^>]*type=\"application/x-tgsticker\"", Pattern.CASE_INSENSITIVE);

    private static final class TgPage {
        static final TgPage EMPTY = new TgPage(null, null, null, null, null);
        final String model;
        final String symbol;
        final String backdrop;
        final String patternUrl;
        final String tgsUrl;
        TgPage(String model, String symbol, String backdrop, String patternUrl, String tgsUrl) {
            this.model = model;
            this.symbol = symbol;
            this.backdrop = backdrop;
            this.patternUrl = patternUrl;
            this.tgsUrl = tgsUrl;
        }
        String trait(String type) {
            if ("Model".equalsIgnoreCase(type)) {return model;}
            if ("Symbol".equalsIgnoreCase(type)) {return symbol;}
            if ("Backdrop".equalsIgnoreCase(type)) {return backdrop;}
            return null;
        }
    }

    static {
        SPECIAL_NAMES.put("DurovsCap", "Durov's Cap");
        SPECIAL_NAMES.put("DurovsGlasses", "Durov's Glasses");
        SPECIAL_NAMES.put("XmasStocking", "Xmas Stocking");
        SPECIAL_NAMES.put("BDayCandle", "B-Day Candle");
        SPECIAL_NAMES.put("HomemadeCake", "Homemade Cake");
    }

    private GiftImages() {}

    static boolean isReady(Context context, CardDecor decor) {
        if (context == null || decor == null || decor.isEmpty()) {return true;}
        File dir = cacheDir(context);
        for (CardDecor.Gift gift : decor.gifts) {
            if (gift == null || gift.id == null || gift.id.isEmpty()) {continue;}
            if (!isFreshCache(new File(dir, safeName(gift.id) + ".png"), new File(dir, safeName(gift.id) + ".model8"))) {return false;}
        }
        if (decor.background != null && decor.background.giftId != null && !decor.background.giftId.isEmpty()) {
            String id = decor.background.giftId;
            if (!isFreshCache(new File(dir, "pat_" + safeName(id) + ".png"), new File(dir, "pat_" + safeName(id) + ".sym"))) {return false;}
        }
        return true;
    }

    private static boolean isFreshCache(File file, File marker) {
        if (file == null || marker == null || !file.isFile() || file.length() < 64 || !marker.isFile()) {return false;}
        long age = System.currentTimeMillis() - Math.max(file.lastModified(), marker.lastModified());
        return age >= 0L && age < CACHE_TTL_MS;
    }

    static void preload(Context context, CardDecor decor) {
        if (decor == null || decor.isEmpty() || context == null) {return;}
        final Context app = context.getApplicationContext();
        final File dir = cacheDir(app);
        List<Future<?>> jobs = new ArrayList<>();
        String patternNft = null;
        for (CardDecor.Gift gift : decor.gifts) {
            if (decor.background != null && decor.background.giftId != null && decor.background.giftId.equals(gift.id)) {patternNft = gift.nft;}
            else if (gift.background && patternNft == null) {patternNft = gift.nft;}
            final CardDecor.Gift g = gift;
            jobs.add(POOL.submit(() -> ensureGift(dir, g)));
        }
        final String patNft = patternNft;
        if (decor.background != null && decor.background.giftId != null) {
            final String giftId = decor.background.giftId;
            jobs.add(POOL.submit(() -> ensurePattern(dir, giftId, patNft)));
        }
        for (Future<?> job : jobs) {try {job.get();} catch (Exception ignored) {}}
    }

    static void preloadAsync(Context context, CardDecor decor, Runnable onDone) {
        if (decor == null || decor.isEmpty() || context == null) {
            if (onDone != null) {MAIN.post(onDone);}
            return;
        }
        final Context app = context.getApplicationContext();
        final File dir = cacheDir(app);
        if (isReady(app, decor)) {
            POOL.execute(() -> {
                for (CardDecor.Gift gift : decor.gifts) {ensureGift(dir, gift);}
                if (decor.background != null && decor.background.giftId != null) {
                    String patternNft = null;
                    for (CardDecor.Gift gift : decor.gifts) {
                        if (decor.background.giftId.equals(gift.id)) {patternNft = gift.nft; break;}
                        if (gift.background && patternNft == null) {patternNft = gift.nft;}
                    }
                    ensurePattern(dir, decor.background.giftId, patternNft);
                }
                if (onDone != null) {MAIN.post(onDone);}
            });
            return;
        }
        List<CardDecor.Gift> gifts = new ArrayList<>(decor.gifts);
        String patternNft = null;
        String patternId = decor.background == null ? null : decor.background.giftId;
        for (CardDecor.Gift gift : gifts) {
            if (patternId != null && patternId.equals(gift.id)) {patternNft = gift.nft;}
            else if (gift.background && patternNft == null) {patternNft = gift.nft;}
        }
        final String patNft = patternNft;
        final String patId = patternId;
        final int total = gifts.size() + (patId != null ? 1 : 0);
        if (total == 0) {
            if (onDone != null) {MAIN.post(onDone);}
            return;
        }
        final AtomicInteger left = new AtomicInteger(total);
        Runnable finishOne = () -> {if (left.decrementAndGet() == 0 && onDone != null) {MAIN.post(onDone);}};
        for (CardDecor.Gift gift : gifts) {
            final CardDecor.Gift g = gift;
            POOL.execute(() -> {try {ensureGift(dir, g);} finally {finishOne.run();}});
        }
        if (patId != null) {POOL.execute(() -> {try {ensurePattern(dir, patId, patNft);} finally {finishOne.run();}});}
    }

    static Bitmap get(Context context, CardDecor.Gift gift) {
        if (context == null || gift == null || gift.id == null || gift.id.isEmpty()) {return null;}
        synchronized (MEMORY) {
            if (FAILED.contains(gift.id)) {return null;}
            Bitmap cached = MEMORY.get(gift.id);
            if (cached != null && !cached.isRecycled()) {return cached;}
        }
        File file = new File(cacheDir(context), safeName(gift.id) + ".png");
        Bitmap bitmap = decodeFile(file);
        if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(gift.id, bitmap);}}
        return bitmap;
    }

    static Bitmap getPattern(Context context, String giftId, String nft) {
        if (giftId == null || giftId.isEmpty() || context == null) {return null;}
        String key = "pat:" + giftId;
        synchronized (MEMORY) {
            Bitmap cached = MEMORY.get(key);
            if (cached != null && !cached.isRecycled()) {return cached;}
        }
        File file = new File(cacheDir(context), "pat_" + safeName(giftId) + ".png");
        Bitmap bitmap = decodeFile(file);
        if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
        return bitmap;
    }

    private static void ensureGift(File dir, CardDecor.Gift gift) {
        if (gift == null || gift.id == null || gift.id.isEmpty()) {return;}
        String giftId = gift.id;
        synchronized (IN_FLIGHT) {if (!IN_FLIGHT.add(giftId)) {return;}}
        try {
            File file = new File(dir, safeName(giftId) + ".png");
            File marker = new File(dir, safeName(giftId) + ".model8");
            synchronized (MEMORY) {
                Bitmap cached = MEMORY.get(giftId);
                if (cached != null && !cached.isRecycled()) {return;}
            }
            if (isFreshCache(file, marker)) {
                Bitmap bitmap = decodeFile(file);
                if (bitmap != null) {
                    synchronized (MEMORY) {MEMORY.put(giftId, bitmap); FAILED.remove(giftId);}
                    return;
                }
            }
            synchronized (MEMORY) {if (FAILED.contains(giftId)) {return;}}
            String wanted = desiredPngMarker(giftId, gift.nft);
            String have = readText(marker);
            boolean stale = file.isFile() && (!marker.isFile() || (wanted != null && !wanted.equals(have)));
            if (stale) {
                file.delete();
                marker.delete();
                synchronized (MEMORY) {MEMORY.remove(giftId); FAILED.remove(giftId);}
            } else if (file.isFile() && file.length() >= 64) {
                Bitmap bitmap = decodeFile(file);
                if (bitmap != null) {
                    synchronized (MEMORY) {MEMORY.put(giftId, bitmap); FAILED.remove(giftId);}
                    touch(file);
                    touch(marker);
                    return;
                }
            }
            String tgs = gift.tgs == null || gift.tgs.isEmpty() ? tgsUrl(giftId) : gift.tgs;
            if (!downloadSticker(giftId, tgs, gift.nft, file, marker)) {
                synchronized (MEMORY) {FAILED.add(giftId);}
                return;
            }
            Bitmap bitmap = decodeFile(file);
            if (bitmap == null) {
                file.delete();
                marker.delete();
                synchronized (MEMORY) {FAILED.add(giftId);}
                return;
            }
            synchronized (MEMORY) {MEMORY.put(giftId, bitmap); FAILED.remove(giftId);}
        } finally {synchronized (IN_FLIGHT) {IN_FLIGHT.remove(giftId);}}
    }

    private static void ensurePattern(File dir, String giftId, String nft) {
        String key = "pat:" + giftId;
        File file = new File(dir, "pat_" + safeName(giftId) + ".png");
        File marker = new File(dir, "pat_" + safeName(giftId) + ".sym");
        synchronized (MEMORY) {
            Bitmap cached = MEMORY.get(key);
            if (cached != null && !cached.isRecycled()) {return;}
        }
        if (isFreshCache(file, marker)) {
            Bitmap bitmap = decodeFile(file);
            if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);} return;}
        }
        TgPage page = pageFor(giftId);
        String symbol = page.trait("Symbol");
        if (symbol == null || symbol.isEmpty()) {symbol = fetchTraitForGift(giftId, nft, "Symbol");}
        String want = symbol == null || symbol.isEmpty() ? "tgpat" : symbol;
        String have = readText(marker);
        if (file.isFile() && file.length() > 32 && want.equals(have)) {
            Bitmap bitmap = decodeFile(file);
            if (bitmap != null) {
                synchronized (MEMORY) {MEMORY.put(key, bitmap);}
                touch(file);
                touch(marker);
            }
            return;
        }
        if (file.isFile() && have != null && !want.equals(have)) {
            file.delete();
            marker.delete();
            synchronized (MEMORY) {MEMORY.remove(key);}
        }
        byte[] bytes = null;
        if (page.patternUrl != null && !page.patternUrl.isEmpty()) {bytes = downloadBytes(page.patternUrl);}
        if (bytes == null || bytes.length < 32) {
            String slug = giftKey(giftId);
            if (slug != null && symbol != null && !symbol.isEmpty()) {
                String encSym = Uri.encode(symbol).replace("+", "%20");
                bytes = downloadBytes(API + "pattern/" + Uri.encode(slug).replace("+", "%20") + "/" + encSym + ".png?size=64");
                String coll = collectionName(giftId);
                if ((bytes == null || bytes.length < 32) && coll != null && !coll.equals(slug)) {bytes = downloadBytes(API + "pattern/" + Uri.encode(coll).replace("+", "%20") + "/" + encSym + ".png?size=64");}
                if ((bytes == null || bytes.length < 32) && coll != null) {bytes = downloadBytes(CDN_PATTERNS + Uri.encode(coll).replace("+", "%20") + "/png/" + encSym + ".png");}
            }
        }
        if (bytes == null || bytes.length < 32) {return;}
        if (!writeBytes(file, bytes)) {return;}
        writeText(marker, want);
        Bitmap bitmap = decodeFile(file);
        if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
    }

    private static void touch(File file) {if (file != null && file.isFile()) {file.setLastModified(System.currentTimeMillis());}}

    private static String desiredPngMarker(String giftId, String nft) {
        String variant = fetchTraitForGift(giftId, nft, "Model");
        if (variant == null || variant.isEmpty()) {return null;}
        return "png:" + variant;
    }

    private static boolean downloadSticker(String giftId, String tgs, String nft, File file, File marker) {
        TgPage page = pageFor(giftId);
        String variant = page.trait("Model");
        if (variant == null || variant.isEmpty()) {variant = fetchTrait(nft, "Model");}
        boolean known = variant != null && !variant.isEmpty();
        if (!known) {variant = "Original";}
        if (downloadPng(giftId, variant, file)) {
            writeText(marker, "png:" + variant);
            return true;
        }
        if (!known && downloadPng(giftId, "Original", file)) {
            writeText(marker, "png:Original");
            return true;
        }
        if (renderAndSaveTgs(tgs, giftId, file)) {
            writeText(marker, "tgs");
            return true;
        }
        if (page.tgsUrl != null && !page.tgsUrl.isEmpty() && renderAndSaveTgs(page.tgsUrl, giftId, file)) {
            writeText(marker, "tgs");
            return true;
        }
        return false;
    }

    private static boolean renderAndSaveTgs(String tgs, String giftId, File file) {
        String url = tgs == null || tgs.isEmpty() ? tgsUrl(giftId) : tgs;
        byte[] gzipped = downloadBytes(url);
        if (gzipped == null || gzipped.length < 32) {return false;}
        Bitmap bitmap = renderTgs(gzipped, RENDER_SIZE);
        if (bitmap == null) {return false;}
        try {return savePng(bitmap, file);} finally {bitmap.recycle();}
    }

    private static Bitmap renderTgs(byte[] gzipped, int size) {
        try {
            String json = new String(gunzip(gzipped), StandardCharsets.UTF_8);
            LottieResult<LottieComposition> result = LottieCompositionFactory.fromJsonStringSync(json, null);
            LottieComposition composition = result == null ? null : result.getValue();
            if (composition == null) {return null;}
            LottieDrawable drawable = new LottieDrawable();
            drawable.setComposition(composition);
            drawable.setBounds(0, 0, size, size);
            drawable.setProgress(0.2f);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception error) {
            android.util.Log.w("GramWallet", "TGS render: " + error.getMessage());
            return null;
        }
    }

    private static byte[] gunzip(byte[] gzipped) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped));
                ByteArrayOutputStream out = new ByteArrayOutputStream(gzipped.length * 4)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {out.write(buf, 0, n);}
            return out.toByteArray();
        }
    }

    private static boolean savePng(Bitmap bitmap, File file) {
        File tmp = new File(file.getAbsolutePath() + ".part");
        try (FileOutputStream out = new FileOutputStream(tmp)) {if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {return false;}} catch (Exception error) {return false;}
        if (file.exists()) {file.delete();}
        return tmp.renameTo(file) && file.isFile() && file.length() > 64;
    }

    private static boolean downloadPng(String giftId, String variant, File file) {
        String key = giftKey(giftId);
        if (key == null || variant == null) {return false;}
        String encModel = Uri.encode(variant).replace("+", "%20");
        byte[] bytes = downloadBytes(API + "model/" + Uri.encode(key).replace("+", "%20") + "/" + encModel + ".png?size=512");
        String coll = collectionName(giftId);
        if ((bytes == null || bytes.length < 64) && coll != null && !coll.equals(key)) {bytes = downloadBytes(API + "model/" + Uri.encode(coll).replace("+", "%20") + "/" + encModel + ".png?size=512");}
        if ((bytes == null || bytes.length < 64) && coll != null) {bytes = downloadBytes(CDN + Uri.encode(coll).replace("+", "%20") + "/png/" + encModel + ".png");}
        if (bytes == null || bytes.length < 64) {return false;}
        return writeBytes(file, bytes);
    }

    private static String giftKey(String giftId) {
        if (giftId == null || giftId.isEmpty()) {return null;}
        int dash = giftId.lastIndexOf('-');
        return dash > 0 ? giftId.substring(0, dash) : giftId;
    }

    private static boolean writeBytes(File file, byte[] bytes) {
        File tmp = new File(file.getAbsolutePath() + ".part");
        try (FileOutputStream out = new FileOutputStream(tmp)) {out.write(bytes);}
        catch (Exception error) {return false;}
        if (file.exists()) {file.delete();}
        return tmp.renameTo(file) && file.isFile() && file.length() > 32;
    }

    private static Bitmap decodeFile(File file) {
        if (file == null || !file.isFile() || file.length() < 32) {return null;}
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inDither = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bitmap == null) {return null;}
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {return bitmap;}
        Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        if (copy != null && copy != bitmap) {bitmap.recycle();}
        return copy != null ? copy : bitmap;
    }

    private static String fetchTraitForGift(String giftId, String nft, String traitType) {
        TgPage page = pageFor(giftId);
        String fromPage = page.trait(traitType);
        if (fromPage != null && !fromPage.isEmpty()) {return fromPage;}
        return fetchTrait(nft, traitType);
    }

    private static TgPage pageFor(String giftId) {
        if (giftId == null || giftId.isEmpty()) {return TgPage.EMPTY;}
        synchronized (PAGES) {
            TgPage cached = PAGES.get(giftId);
            if (cached != null) {return cached;}
            TgPage page = fetchTgPage(giftId);
            PAGES.put(giftId, page);
            synchronized (TRAITS) {
                if (page.model != null) {TRAITS.put("tg|" + giftId + "|Model", page.model);}
                if (page.symbol != null) {TRAITS.put("tg|" + giftId + "|Symbol", page.symbol);}
                if (page.backdrop != null) {TRAITS.put("tg|" + giftId + "|Backdrop", page.backdrop);}
            }
            return page;
        }
    }

    private static TgPage fetchTgPage(String giftId) {
        byte[] body = downloadBytes("https://t.me/nft/" + giftId);
        if (body == null || body.length == 0) {return TgPage.EMPTY;}
        String html = new String(body, StandardCharsets.UTF_8);
        String model = null, symbol = null, backdrop = null;
        Matcher og = OG_DESC.matcher(html);
        if (og.find()) {
            String desc = (og.group(1) != null ? og.group(1) : og.group(2));
            if (desc != null) {
                desc = desc.replace("&#10;", "\n").replace("&#13;", "\n").replace("&amp;", "&");
                for (String line : desc.split("[\r\n]+")) {
                    String trimmed = line.trim();
                    int colon = trimmed.indexOf(':');
                    if (colon <= 0) {continue;}
                    String label = trimmed.substring(0, colon).trim();
                    String raw = trimmed.substring(colon + 1).trim();
                    if (raw.isEmpty()) {continue;}
                    if ("Model".equalsIgnoreCase(label)) {model = raw;}
                    else if ("Symbol".equalsIgnoreCase(label)) {symbol = raw;}
                    else if ("Backdrop".equalsIgnoreCase(label)) {backdrop = raw;}
                }
            }
        }
        String patternUrl = matchGroup(GIFT_PATTERN.matcher(html));
        String tgsUrl = matchGroup(GIFT_TGS.matcher(html));
        return new TgPage(model, symbol, backdrop, patternUrl, tgsUrl);
    }

    private static String matchGroup(Matcher m) {
        if (!m.find()) {return null;}
        String a = m.group(1);
        if (a != null && !a.isEmpty()) {return a;}
        String b = m.groupCount() >= 2 ? m.group(2) : null;
        return b == null || b.isEmpty() ? null : b;
    }

    private static String fetchTrait(String nft, String traitType) {
        if (nft == null || nft.isEmpty() || traitType == null) {return null;}
        String cacheKey = nft + "|" + traitType;
        synchronized (TRAITS) {if (TRAITS.containsKey(cacheKey)) {return TRAITS.get(cacheKey);}}
        String value = null;
        boolean fetched = false;
        try {
            byte[] body = downloadBytes(TONAPI_NFT + Uri.encode(nft));
            if (body != null) {
                fetched = true;
                JSONObject json = new JSONObject(new String(body, StandardCharsets.UTF_8));
                JSONObject metadata = json.optJSONObject("metadata");
                JSONArray attrs = metadata == null ? null : metadata.optJSONArray("attributes");
                if (attrs != null) {
                    for (int i = 0; i < attrs.length(); i++) {
                        JSONObject item = attrs.optJSONObject(i);
                        if (item == null) {continue;}
                        if (traitType.equalsIgnoreCase(item.optString("trait_type", ""))) {
                            String raw = item.optString("value", "").trim();
                            value = raw.isEmpty() ? null : raw;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (fetched) {synchronized (TRAITS) {TRAITS.put(cacheKey, value);}}
        return value;
    }

    private static byte[] downloadBytes(String url) {
        if (url == null || url.isEmpty()) {return null;}
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(CONNECT_MS);
            connection.setReadTimeout(READ_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "GramWallet/26.1.7 (Android)");
            connection.setRequestProperty("Accept", "*/*");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {return null;}
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {out.write(buf, 0, n);}
                return out.toByteArray();
            }
        } catch (Exception error) {return null;}
        finally {if (connection != null) {connection.disconnect();}}
    }

    private static String tgsUrl(String giftId) {return giftId == null || giftId.isEmpty() ? null : TGS_FALLBACK + giftId + ".tgs";}

    static String collectionName(String giftId) {
        if (giftId == null || giftId.isEmpty()) {return null;}
        int dash = giftId.lastIndexOf('-');
        String camel = dash > 0 ? giftId.substring(0, dash) : giftId;
        String special = SPECIAL_NAMES.get(camel);
        if (special != null) {return special;}
        StringBuilder out = new StringBuilder(camel.length() + 8);
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && (Character.isLowerCase(camel.charAt(i - 1)) || (i + 1 < camel.length() && Character.isLowerCase(camel.charAt(i + 1))))) {out.append(' ');}
            out.append(c);
        }
        return out.toString();
    }

    private static File cacheDir(Context context) {
        File dir = new File(context.getCacheDir(), "gifts-model");
        if (!dir.isDirectory()) {dir.mkdirs();}
        return dir;
    }

    private static String safeName(String giftId) {return giftId.replaceAll("[^A-Za-z0-9._-]", "_");}
    private static void writeText(File file, String text) {try (FileOutputStream out = new FileOutputStream(file)) {out.write(text.getBytes(StandardCharsets.UTF_8));} catch (Exception ignored) {}}

    private static String readText(File file) {
        if (file == null || !file.isFile() || file.length() == 0 || file.length() > 512) {return null;}
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length())) {
            byte[] buf = new byte[256];
            int n;
            while ((n = in.read(buf)) > 0) {out.write(buf, 0, n);}
            String text = out.toString(StandardCharsets.UTF_8.name()).trim();
            return text.isEmpty() ? null : text;
        } catch (Exception error) {return null;}
    }
}
