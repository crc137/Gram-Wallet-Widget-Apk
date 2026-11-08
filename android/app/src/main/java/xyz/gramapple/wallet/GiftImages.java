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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private static final Map<String, String> SPECIAL_NAMES = new HashMap<>();
    private static final Map<String, Bitmap> MEMORY = new HashMap<>();
    private static final Map<String, String> TRAITS = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    static {
        SPECIAL_NAMES.put("DurovsCap", "Durov's Cap");
        SPECIAL_NAMES.put("DurovsGlasses", "Durov's Glasses");
        SPECIAL_NAMES.put("XmasStocking", "Xmas Stocking");
        SPECIAL_NAMES.put("BDayCandle", "B-Day Candle");
        SPECIAL_NAMES.put("HomemadeCake", "Homemade Cake");
    }

    private GiftImages() {}

    static void preload(Context context, CardDecor decor) {
        if (decor == null || decor.isEmpty() || context == null) {return;}
        synchronized (FAILED) {FAILED.clear();}
        synchronized (MEMORY) {MEMORY.clear();}
        File dir = cacheDir(context);
        String patternNft = null;
        for (CardDecor.Gift gift : decor.gifts) {
            loadGift(dir, gift);
            if (decor.background != null&& decor.background.giftId != null&& decor.background.giftId.equals(gift.id)) {patternNft = gift.nft;} else if (gift.background && patternNft == null) {patternNft = gift.nft;}
        }
        if (decor.background != null && decor.background.giftId != null) {loadPattern(dir, decor.background.giftId, patternNft);}
    }

    static Bitmap get(Context context, CardDecor.Gift gift) {
        if (context == null || gift == null || gift.id == null || gift.id.isEmpty()) {return null;}
        return loadGift(cacheDir(context), gift);
    }

    static Bitmap getPattern(Context context, String giftId, String nft) {
        if (giftId == null || giftId.isEmpty() || context == null) {return null;}
        File dir = cacheDir(context);
        loadPattern(dir, giftId, nft);
        String key = "pat:" + giftId;
        synchronized (MEMORY) {
            Bitmap cached = MEMORY.get(key);
            if (cached != null && !cached.isRecycled()) {return cached;}
        }
        File file = new File(dir, "pat_" + safeName(giftId) + ".png");
        Bitmap bitmap = decodeFile(file);
        if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
        return bitmap;
    }

    private static Bitmap loadGift(File dir, CardDecor.Gift gift) {
        String giftId = gift.id;
        File file = new File(dir, safeName(giftId) + ".png");
        File marker = new File(dir, safeName(giftId) + ".model8");
        String wanted = desiredPngMarker(giftId, gift.nft);
        String have = readText(marker);
        boolean stale = file.isFile()&& (!marker.isFile() || (wanted != null && !wanted.equals(have)));
        if (stale) {
            file.delete();
            marker.delete();
            synchronized (MEMORY) {
                MEMORY.remove(giftId);
                FAILED.remove(giftId);
            }
        } else {
            synchronized (MEMORY) {
                if (FAILED.contains(giftId)) {return null;}
                Bitmap cached = MEMORY.get(giftId);
                if (cached != null && !cached.isRecycled()) {return cached;}
            }
        }
        if (!file.isFile() || file.length() < 64) {
            String tgs = gift.tgs == null || gift.tgs.isEmpty() ? tgsUrl(giftId) : gift.tgs;
            if (!downloadSticker(giftId, tgs, gift.nft, file, marker)) {
                synchronized (MEMORY) {FAILED.add(giftId);}
                return null;
            }
        }
        Bitmap bitmap = decodeFile(file);
        if (bitmap == null) {
            file.delete();
            marker.delete();
            synchronized (MEMORY) {FAILED.add(giftId);}
            return null;
        }
        synchronized (MEMORY) {MEMORY.put(giftId, bitmap);}
        return bitmap;
    }

    private static void loadPattern(File dir, String giftId, String nft) {
        String key = "pat:" + giftId;
        File file = new File(dir, "pat_" + safeName(giftId) + ".png");
        File marker = new File(dir, "pat_" + safeName(giftId) + ".sym");
        String symbol = fetchTraitForGift(giftId, nft, "Symbol");
        String have = readText(marker);
        if (symbol != null && (have == null || !symbol.equals(have))) {
            if (file.isFile() && have != null && !symbol.equals(have)) {
                file.delete();
                marker.delete();
                synchronized (MEMORY) {MEMORY.remove(key);}
            }
        }
        if (file.isFile() && file.length() > 32 && symbol != null && symbol.equals(have)) {
            Bitmap bitmap = decodeFile(file);
            if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
            return;
        }
        if (file.isFile() && file.length() > 32 && symbol == null) {
            Bitmap bitmap = decodeFile(file);
            if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
            return;
        }
        String slug = giftKey(giftId);
        if (slug == null || symbol == null || symbol.isEmpty()) {return;}
        String encSym = Uri.encode(symbol).replace("+", "%20");
        byte[] bytes = downloadBytes(API + "pattern/" + Uri.encode(slug).replace("+", "%20") + "/" + encSym + ".png?size=64");
        if (bytes == null || bytes.length < 32) {
            String coll = collectionName(giftId);
            if (coll != null && !coll.equals(slug)) {bytes = downloadBytes(API + "pattern/" + Uri.encode(coll).replace("+", "%20") + "/" + encSym + ".png?size=64");}
            if ((bytes == null || bytes.length < 32) && coll != null) {bytes = downloadBytes(CDN_PATTERNS + Uri.encode(coll).replace("+", "%20") + "/png/" + encSym + ".png");}
        }
        if (bytes == null || bytes.length < 32) {return;}
        if (!writeBytes(file, bytes)) {return;}
        writeText(marker, symbol);
        Bitmap bitmap = decodeFile(file);
        if (bitmap != null) {synchronized (MEMORY) {MEMORY.put(key, bitmap);}}
    }

    private static String desiredPngMarker(String giftId, String nft) {
        String variant = fetchTraitForGift(giftId, nft, "Model");
        if (variant == null || variant.isEmpty()) {return null;}
        return "png:" + variant;
    }

    private static boolean downloadSticker(String giftId, String tgs, String nft, File file, File marker) {
        String variant = fetchTraitForGift(giftId, nft, "Model");
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
        return false;
    }

    private static void stripBackdrop(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w < 8 || h < 8) {return;}
        int c0 = bitmap.getPixel(2, 2);
        int c1 = bitmap.getPixel(w - 3, 2);
        int c2 = bitmap.getPixel(2, h - 3);
        int c3 = bitmap.getPixel(w - 3, h - 3);
        if (!sameRgb(c0, c1) || !sameRgb(c0, c2) || !sameRgb(c0, c3)) {return;}
        if (((c0 >>> 24) & 0xFF) < 200) {return;}
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        int br = (c0 >> 16) & 0xFF;
        int bg = (c0 >> 8) & 0xFF;
        int bb = c0 & 0xFF;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            if (a < 16) {continue;}
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int dr = r - br;
            int dg = g - bg;
            int db = b - bb;
            if (dr * dr + dg * dg + db * db <= 48 * 48) {pixels[i] = 0;}
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private static boolean sameRgb(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return dr * dr + dg * dg + db * db <= 30 * 30;
    }

    private static Bitmap renderTgs(byte[] gzipped, int size) {
        try {
            String json = new String(gunzip(gzipped), StandardCharsets.UTF_8);
            LottieResult<LottieComposition> result =LottieCompositionFactory.fromJsonStringSync(json, null);
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
        String fromNft = fetchTrait(nft, traitType);
        if (fromNft != null && !fromNft.isEmpty()) {return fromNft;}
        return fetchTraitFromTelegram(giftId, traitType);
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

    private static String fetchTraitFromTelegram(String giftId, String traitType) {
        if (giftId == null || giftId.isEmpty() || traitType == null) {return null;}
        String cacheKey = "tg|" + giftId + "|" + traitType;
        synchronized (TRAITS) {if (TRAITS.containsKey(cacheKey)) {return TRAITS.get(cacheKey);}}
        String value = null;
        boolean fetched = false;
        try {
            byte[] body = downloadBytes("https://t.me/nft/" + giftId);
            if (body != null) {
                fetched = true;
                String html = new String(body, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("property=\"og:description\"\\s+content=\"([^\"]*)\"",Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
                if (!m.find()) {m = Pattern.compile("content=\"([^\"]*)\"\\s+property=\"og:description\"",Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);}
                if (m.find()) {
                    String desc = m.group(1).replace("&#10;", "\n").replace("&#13;", "\n").replace("&amp;", "&");
                    String label = traitType + ":";
                    for (String line : desc.split("[\r\n]+")) {
                        String trimmed = line.trim();
                        if (trimmed.length() > label.length()&& trimmed.regionMatches(true, 0, label, 0, label.length())) {
                            String raw = trimmed.substring(label.length()).trim();
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
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(25000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "GramWallet/26.1.7 (Android)");
            connection.setRequestProperty("Accept", "*/*");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {return null;}
            try (InputStream in = connection.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
            if (i > 0&& Character.isUpperCase(c)&& (Character.isLowerCase(camel.charAt(i - 1))|| (i + 1 < camel.length()&& Character.isLowerCase(camel.charAt(i + 1))))) {out.append(' ');}
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
        try (InputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length())) {
            byte[] buf = new byte[256];
            int n;
            while ((n = in.read(buf)) > 0) {out.write(buf, 0, n);}
            String text = out.toString(StandardCharsets.UTF_8.name()).trim();
            return text.isEmpty() ? null : text;
        } catch (Exception error) {return null;}
    }
}
