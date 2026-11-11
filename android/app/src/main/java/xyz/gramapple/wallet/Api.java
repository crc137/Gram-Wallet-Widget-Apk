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
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Api {
    static final String ERROR_INVALID_ADDRESS = "invalid_address";
    static final String ERROR_NETWORK = "network";
    static final String ERROR_TLS = "tls";
    static final String ERROR_SERVER = "server";
    static final String ERROR_BAD_RESPONSE = "bad_response";
    private static final String USER_AGENT = "GramWallet/26.1.7 (Android)";
    private static final int TIMEOUT_MS = 7000;
    private static final int BALANCE_ATTEMPTS = 2;
    private static final String TONAPI_ACCOUNT = "https://tonapi.io/v2/accounts/";
    private static final String TONAPI_RATES ="https://tonapi.io/v2/rates?tokens=ton&currencies=usd";
    private static final String TONCENTER ="https://toncenter.com/api/v2/getAddressInformation?address=";
    private static final String GRAMAPPLE ="https://gramapple.xyz/api/public/portfolio?address=";
    private static final String COINGECKO ="https://api.coingecko.com/api/v3/simple/price"+ "?ids=the-open-network&vs_currencies=usd&include_24hr_change=true";
    private static final String BINANCE ="https://api.binance.com/api/v3/ticker/24hr?symbol=TONUSDT";
    private static final String COINBASE ="https://api.coinbase.com/v2/prices/TON-USD/spot";
    private static final String GRAM_CARD = "https://gram.org/api/accounts/";
    private static final String GITHUB_TAGS = "https://api.github.com/repos/crc137/Gram-Wallet-Widget-Bot/tags";
    private static final String GITHUB_LATEST = "https://api.github.com/repos/crc137/Gram-Wallet-Widget-Bot/releases/latest";
    private static final String GITHUB_TAG_RELEASE = "https://api.github.com/repos/crc137/Gram-Wallet-Widget-Bot/releases/tags/";
    private static final String DONOR_API = "https://gram-wallet-widget-bot.direct-ban-guard.workers.dev/donor";
    private static final Pattern RELEASE_CODE = Pattern.compile("(?:\\(b|\\.b)(\\d+)\\)?", Pattern.CASE_INSENSITIVE);
    private Api() {}    static String walletUrl(String address) {return "https://gram.org/address/" + android.net.Uri.encode(address);}

    static final class AppRelease {
        final String tag;
        final String name;
        final String apkUrl;
        final String body;
        final long code;
        final long sizeBytes;
        AppRelease(String tag, String name, String apkUrl, String body, long code, long sizeBytes) {
            this.tag = tag == null ? "" : tag;
            this.name = name == null ? "" : name;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
            this.body = body == null ? "" : body;
            this.code = code;
            this.sizeBytes = Math.max(0L, sizeBytes);
        }
    }

    static boolean isDonor(String address) {
        if (address == null || address.isEmpty()) {return false;}
        try {
            JSONObject json = getJson(DONOR_API + "?address=" + encode(address), TIMEOUT_MS);
            return json.optBoolean("donor", false);
        } catch (Exception error) {return false;}
    }

    static boolean claimDonor(String address, String code) {
        if (address == null || address.isEmpty() || code == null || code.trim().isEmpty()) {return false;}
        try {
            JSONObject body = new JSONObject();
            body.put("address", address);
            body.put("code", code.trim());
            JSONObject json = postJson(DONOR_API + "/claim", body.toString(), TIMEOUT_MS);
            return json.optBoolean("donor", false);
        } catch (Exception error) {return false;}
    }

    static AppRelease fetchLatestRelease() throws ApiException {
        JSONObject json = releaseFromTags();
        if (json == null) {json = getJson(GITHUB_LATEST, TIMEOUT_MS);}
        String tag = json.optString("tag_name", "");
        String name = json.optString("name", tag);
        String body = json.optString("body", "");
        String apkUrl = "";
        long sizeBytes = 0L;
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) {continue;}
                String assetName = asset.optString("name", "").toLowerCase(Locale.US);
                if (assetName.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "");
                    sizeBytes = asset.optLong("size", 0L);
                    if (!apkUrl.isEmpty()) {break;}
                }
            }
        }
        long code = parseReleaseCode(tag);
        if (code <= 0) {code = parseReleaseCode(name);}
        if (code <= 0) {code = parseReleaseCode(apkUrl);}
        if (apkUrl.isEmpty() || code <= 0) {return null;}
        return new AppRelease(tag, name, apkUrl, body, code, sizeBytes);
    }

    private static JSONObject releaseFromTags() {
        try {
            JSONArray tags = new JSONArray(getRaw(GITHUB_TAGS, TIMEOUT_MS));
            String tag = "";
            for (int i = 0; i < tags.length(); i++) {
                JSONObject item = tags.optJSONObject(i);
                if (item == null) {continue;}
                tag = item.optString("name", "").trim();
                if (!tag.isEmpty()) {break;}
            }
            if (tag.isEmpty()) {return null;}
            return getJson(GITHUB_TAG_RELEASE + encode(tag), TIMEOUT_MS);
        } catch (Exception error) {return null;}
    }

    private static long parseReleaseCode(String value) {
        if (value == null || value.isEmpty()) {return 0L;}
        Matcher matcher = RELEASE_CODE.matcher(value);
        long best = 0L;
        while (matcher.find()) {try {best = Math.max(best, Long.parseLong(matcher.group(1)));} catch (NumberFormatException ignored) {}}
        return best;
    }

    static String fetchCardRaw(String address) {
        try {
            String raw = getRaw(GRAM_CARD + encode(address) + "/card", TIMEOUT_MS);
            if (raw == null || raw.isEmpty()) {return null;}
            JSONObject json = new JSONObject(raw);
            String domain = fetchAccountDomain(address);
            if (domain != null) {json.put("domain", domain);}
            return json.toString();
        } catch (Exception error) {
            android.util.Log.w("GramWallet", "Карточка: " + error.getMessage());
            return null;
        }
    }

    static CardDecor fetchCard(String address) {
        String raw = fetchCardRaw(address);
        if (raw == null || raw.isEmpty()) {return CardDecor.EMPTY;}
        try {return parseCard(new JSONObject(raw));}
        catch (Exception error) {return CardDecor.EMPTY;}
    }

    private static String fetchAccountDomain(String address) {
        try {
            JSONObject json = getJson(TONAPI_ACCOUNT + encode(address), TIMEOUT_MS);
            return emptyToNull(json.optString("name", null));
        } catch (Exception error) {return null;}
    }

    static CardDecor parseCard(JSONObject json) throws JSONException {
        CardDecor.Background background = null;
        JSONObject bg = json.optJSONObject("background");
        if (bg != null) {background = new CardDecor.Background(parseColor(bg.optString("center", null), 0xFF0088FF),parseColor(bg.optString("edge", null), 0xFF0057C2),parseColor(bg.optString("pattern", null), 0x66000000),emptyToNull(bg.optString("gift", null)),emptyToNull(bg.optString("name", null)));}
        List<CardDecor.Gift> gifts = new ArrayList<>();
        JSONArray arr = json.optJSONArray("gifts");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) {continue;}
                String id = item.optString("id", "");
                if (id.isEmpty()) {continue;}
                gifts.add(new CardDecor.Gift(id,emptyToNull(item.optString("nft", null)),item.optString("tgs", ""),(float) item.optDouble("x", 50d),(float) item.optDouble("y", 50d),(float) item.optDouble("w", 20d),(float) item.optDouble("h", 20d),(float) item.optDouble("rotation", 0d),item.optBoolean("background", false)));
            }
        }
        boolean telegramWallet = json.optBoolean("telegram_wallet", false);
        String domain = emptyToNull(json.optString("domain", null));
        return new CardDecor(background, gifts, telegramWallet, domain);
    }

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.isEmpty()) {return fallback;}
        try {
            int color = Color.parseColor(hex.trim());
            return (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
        } catch (Exception error) {return fallback;}
    }

    private static String emptyToNull(String value) {
        if (value == null) {return null;}
        value = value.trim();
        return value.isEmpty() || "null".equals(value) ? null : value;
    }

    static ActionsPage fetchActions(String address, String cursor) throws ApiException {
        String url = GRAM_CARD + encode(address) + "/actions";
        if (cursor != null && !cursor.isEmpty()) {url = url + "?cursor=" + encode(cursor);}
        JSONObject json = getJson(url, TIMEOUT_MS);
        JSONArray array = json.optJSONArray("actions");
        JSONObject names = json.optJSONObject("names");
        List<Tx> items = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Tx tx = parseAction(array.optJSONObject(i), names, address);
                if (tx != null) {items.add(tx);}
            }
        }
        return new ActionsPage(items, emptyToNull(json.optString("next", null)));
    }

    static TokensPage fetchTokens(String address, String cursor) throws ApiException {
        String url = GRAM_CARD + encode(address) + "/tokens";
        if (cursor != null && !cursor.isEmpty()) {url = url + "?cursor=" + encode(cursor);}
        JSONObject json = getJson(url, TIMEOUT_MS);
        JSONArray array = json.optJSONArray("tokens");
        List<Token> items = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Token token = parseToken(array.optJSONObject(i));
                if (token != null) {items.add(token);}
            }
        }
        return new TokensPage(items, emptyToNull(json.optString("next", null)));
    }

    static NftsPage fetchNfts(String address, String cursor) throws ApiException {
        String url = GRAM_CARD + encode(address) + "/nfts";
        if (cursor != null && !cursor.isEmpty()) {url = url + "?offset=" + encode(cursor);}
        JSONObject json = getJson(url, TIMEOUT_MS);
        JSONArray array = json.optJSONArray("items");
        List<Nft> items = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Nft nft = parseNftItem(array.optJSONObject(i));
                if (nft != null) {items.add(nft);}
            }
        }
        String next = emptyToNull(json.optString("next_offset", null));
        if (next == null) {next = emptyToNull(json.optString("next", null));}
        return new NftsPage(items, next);
    }

    private static Nft parseNftItem(JSONObject item) {
        if (item == null) {return null;}
        String name = item.optString("name", "").trim();
        JSONObject collection = item.optJSONObject("collection");
        String collectionName = collection == null ? "" : collection.optString("name", "").trim();
        String image = nftImage(item);
        if (name.isEmpty() && image.isEmpty()) {return null;}
        return new Nft(name, collectionName, image, item.optBoolean("scam", false));
    }

    static final class NftsPage {
        final List<Nft> items;
        final String next;
        NftsPage(List<Nft> items, String next) {
            this.items = items;
            this.next = next;
        }
    }

    private static Token parseToken(JSONObject item) {
        if (item == null) {return null;}
        String symbol = item.optString("symbol", "").trim();
        String name = item.optString("name", "").trim();
        if (symbol.isEmpty() && name.isEmpty()) {return null;}
        String image = item.optString("image", "");
        if (image == null || image.isEmpty() || "null".equals(image)) {image = item.optString("image_origin", "");}
        if (image == null || "null".equals(image)) {image = "";}
        Double price = null;
        if (!item.isNull("price")) {
            double value = item.optDouble("price", Double.NaN);
            if (!Double.isNaN(value) && !Double.isInfinite(value) && value >= 0) {price = value;}
        }
        return new Token(name, symbol, item.optString("balance", "0"), item.optInt("decimals", 0), price, tokenImage(image), item.optBoolean("scam", false), item.optBoolean("verified", false));
    }

    private static String tokenImage(String raw) {
        if (raw == null || raw.isEmpty() || "null".equals(raw)) {return "";}
        if (raw.startsWith("https://") || raw.startsWith("http://")) {return raw;}
        return imageUrl(raw);
    }

    static final class TokensPage {
        final List<Token> items;
        final String next;
        TokensPage(List<Token> items, String next) {
            this.items = items;
            this.next = next;
        }
    }

    private static Tx parseAction(JSONObject action, JSONObject names, String address) {
        if (action == null) {return null;}
        JSONObject presentation = action.optJSONObject("presentation");
        if (presentation == null) {return null;}
        String type = presentation.optString("type", action.optString("type", ""));
        JSONObject details = presentation.optJSONObject("details");
        if (details == null) {details = action.optJSONObject("details");}
        if (details == null) {return null;}
        boolean success = presentation.optBoolean("success", true);
        long time = action.optLong("time", 0L);
        String id = action.optString("id", "");
        if (Tx.TYPE_TON.equals(type)) {
            String source = details.optString("source", "");
            String destination = details.optString("destination", "");
            boolean incoming = address.equals(destination);
            double gram = parseDouble(details.optString("value", "")) / 1_000_000_000d;
            if (Double.isNaN(gram) || Double.isInfinite(gram) || gram < 0) {gram = 0d;}
            String counterparty = incoming ? source : destination;
            JSONObject actionDetails = action.optJSONObject("details");
            String comment = actionDetails == null ? "" : actionDetails.optString("comment", "");
            return new Tx(id, Tx.TYPE_TON, incoming, success, time, gram,nameFor(names, counterparty),comment,null, null, null);
        }
        if (Tx.TYPE_NFT.equals(type)) {
            JSONObject actionDetails = action.optJSONObject("details");
            JSONObject nft = actionDetails == null ? null : actionDetails.optJSONObject("nft");
            if (nft == null) {nft = details.optJSONObject("nft");}
            if (nft == null) {
                String item = details.optString("nft_item", "");
                JSONObject nfts = presentation.optJSONObject("nfts");
                if (nfts != null && !item.isEmpty()) {nft = nfts.optJSONObject(item);}
            }
            String newOwner = details.optString("new_owner", "");
            boolean incoming = address.equals(newOwner);
            String nftName = nft == null ? "" : nft.optString("name", "");
            JSONObject collection = nft == null ? null : nft.optJSONObject("collection");
            String collectionName = collection == null ? "" : collection.optString("name", "");
            if (collectionName.isEmpty()) {
                String collectionAddress = details.optString("nft_collection", "");
                String named = nameFor(names, collectionAddress);
                collectionName = named.equals(collectionAddress) ? "" : named;
            }
            return new Tx(id, Tx.TYPE_NFT, incoming, success, time, 0d, "", "", nftName, collectionName, nftImage(nft));
        }
        String source = details.optString("source", details.optString("from", ""));
        String destination = details.optString("destination", details.optString("to", ""));
        boolean incoming = address.equals(destination);
        if (!incoming && !address.equals(source) && destination.isEmpty()) {incoming = false;}
        double gram = parseDouble(details.optString("value", details.optString("amount", ""))) / 1_000_000_000d;
        if (Double.isNaN(gram) || Double.isInfinite(gram) || gram < 0) {gram = 0d;}
        String counterparty = incoming ? source : destination;
        if (counterparty.isEmpty()) {counterparty = incoming ? source : destination;}
        JSONObject actionDetails = action.optJSONObject("details");
        String comment = actionDetails == null ? "" : actionDetails.optString("comment", "");
        String kind = type == null || type.isEmpty() ? "other" : type;
        return new Tx(id, kind, incoming, success, time, gram, nameFor(names, counterparty), comment, null, null, null);
    }

    private static String nftImage(JSONObject nft) {
        if (nft == null) {return "";}
        String image = jsonText(nft, "image");
        if (image.isEmpty()) {image = jsonText(nft, "image_origin");}
        if (image.isEmpty()) {
            JSONObject collection = nft.optJSONObject("collection");
            if (collection != null) {
                image = jsonText(collection, "image");
                if (image.isEmpty()) {image = jsonText(collection, "image_origin");}
            }
        }
        return mediaUrl(image);
    }

    private static String jsonText(JSONObject object, String key) {
        if (object == null || key == null || object.isNull(key)) {return "";}
        String value = object.optString(key, "").trim();
        return value.isEmpty() || "null".equalsIgnoreCase(value) ? "" : value;
    }

    private static String mediaUrl(String raw) {
        if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) {return "";}
        if (raw.startsWith("https://") || raw.startsWith("http://")) {return raw;}
        return imageUrl(raw);
    }

    private static String imageUrl(String raw) {
        if (raw == null || raw.isEmpty()) {return "";}
        if (raw.startsWith("https://gram.org/") || raw.startsWith("http://gram.org/")) {return raw;}
        return "https://gram.org/api/img?u=" + android.net.Uri.encode(raw);
    }

    private static String nameFor(JSONObject names, String address) {
        if (address == null) {return "";}
        if (names == null || address.isEmpty()) {return address;}
        JSONObject entry = names.optJSONObject(address);
        if (entry == null) {return address;}
        String domain = emptyToNull(entry.optString("domain", null));
        return domain == null ? address : domain;
    }

    static WalletData fetch(String address) throws ApiException {
        final List<ApiException> failures = new ArrayList<>();
        Double gram = trySource(address, failures, "tonapi.io", Api::fetchTonapiBalance);
        if (gram == null) {
            gram = trySource(address, failures, "toncenter.com", Api::fetchToncenterBalance);
        }
        if (gram == null) {
            WalletData gramapple = tryGramapple(address, failures);
            if (gramapple != null) {return gramapple;}
            throw combine(failures);
        }
        Price price = fetchPrice();
        double totalUsd = price.usd > 0 ? gram * price.usd : 0d;
        return new WalletData(gram, totalUsd, price.change, totalUsd > 0, System.currentTimeMillis());
    }

    private interface BalanceSource {double fetch(String address) throws ApiException;}

    private static Double trySource(
            String address, List<ApiException> failures, String label, BalanceSource source) {
        for (int attempt = 0; attempt < BALANCE_ATTEMPTS; attempt++) {
            try {
                double value = source.fetch(address);
                android.util.Log.i("GramWallet", "Баланс получен: " + label);
                return value;
            } catch (ApiException error) {
                failures.add(new ApiException(error.kind, label + ": " + error.getMessage()));
                if (!isRetryable(error) || attempt == BALANCE_ATTEMPTS - 1) {return null;}
                sleep(400);
            }
        }
        return null;
    }

    private static double fetchTonapiBalance(String address) throws ApiException {
        JSONObject json = getJson(TONAPI_ACCOUNT + encode(address), TIMEOUT_MS);
        String balance = json.optString("balance", null);
        if (balance == null || balance.isEmpty()) {throw new ApiException(ERROR_BAD_RESPONSE, "В ответе нет баланса");}
        return nanoToGram(balance);
    }

    private static double fetchToncenterBalance(String address) throws ApiException {
        JSONObject json = getJson(TONCENTER + encode(address), TIMEOUT_MS);
        if (!json.optBoolean("ok", false)) {throw new ApiException(ERROR_SERVER, "toncenter: " + json.optString("result", ""));}
        JSONObject result = json.optJSONObject("result");
        String balance = result == null ? null : result.optString("balance", null);
        if (balance == null || balance.isEmpty()) {throw new ApiException(ERROR_BAD_RESPONSE, "В ответе нет баланса");}
        return nanoToGram(balance);
    }

    private static WalletData tryGramapple(String address, List<ApiException> failures) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WalletData data = fetchGramapple(address);
                android.util.Log.i("GramWallet", "Баланс получен: gramapple.xyz");
                return data;
            } catch (ApiException error) {
                failures.add(new ApiException(error.kind, "gramapple.xyz: " + error.getMessage()));
                if (!isRetryable(error) || attempt == 2) {return null;}
                sleep(700);
            }
        }
        return null;
    }

    private static WalletData fetchGramapple(String address) throws ApiException {
        JSONObject json = getJson(GRAMAPPLE + encode(address), TIMEOUT_MS);
        if (!json.has("gram") || json.isNull("gram")) {
            String detail = json.optString("error", "").trim();
            if (detail.toLowerCase(Locale.US).contains("invalid ton address")) {throw new ApiException(ERROR_INVALID_ADDRESS, detail);}
            throw new ApiException(ERROR_SERVER, detail.isEmpty() ? "В ответе нет баланса" : detail);
        }
        double gram;
        double totalUsd;
        String change;
        try {
            gram = json.getDouble("gram");
            totalUsd = json.optDouble("totalUsd", 0d);
            change = json.optString("change24h", "");
        } catch (JSONException error) {throw new ApiException(ERROR_BAD_RESPONSE, "Некорректные числа в ответе");}
        if (Double.isNaN(totalUsd) || Double.isInfinite(totalUsd)) {totalUsd = 0d;}
        if ("null".equals(change)) {change = "";}
        return new WalletData(gram, totalUsd, change.trim(), totalUsd > 0, System.currentTimeMillis());
    }

    private static Price fetchPrice() {
        Price price = tryPriceCoingecko();
        if (price == null) {price = tryPriceTonapi();}
        if (price == null) {price = tryPriceBinance();}
        if (price == null) {price = tryPriceCoinbase();}
        return price == null ? Price.NONE : price;
    }

    private static Price tryPriceCoingecko() {
        try {
            JSONObject json = getJson(COINGECKO, TIMEOUT_MS);
            JSONObject ton = json.optJSONObject("the-open-network");
            if (ton == null) {return null;}
            double usd = ton.optDouble("usd", 0d);
            if (!isPositive(usd)) {return null;}
            return new Price(usd, change(ton.optDouble("usd_24h_change", Double.NaN)));
        } catch (ApiException error) {return null;}
    }

    private static Price tryPriceTonapi() {
        try {
            JSONObject json = getJson(TONAPI_RATES, TIMEOUT_MS);
            JSONObject rates = json.optJSONObject("rates");
            JSONObject ton = rates == null ? null : rates.optJSONObject("TON");
            JSONObject prices = ton == null ? null : ton.optJSONObject("prices");
            if (prices == null) {return null;}
            double usd = prices.optDouble("USD", 0d);
            if (!isPositive(usd)) {return null;}
            JSONObject diff = ton.optJSONObject("diff_24h");
            String change = diff == null ? "" : diff.optString("USD", "");
            return new Price(usd, change.trim());
        } catch (ApiException error) {return null;}
    }

    private static Price tryPriceBinance() {
        try {
            JSONObject json = getJson(BINANCE, TIMEOUT_MS);
            double usd = parseDouble(json.optString("lastPrice", ""));
            if (!isPositive(usd)) {return null;}
            return new Price(usd, change(parseDouble(json.optString("priceChangePercent", ""))));
        } catch (ApiException error) {return null;}
    }

    private static Price tryPriceCoinbase() {
        try {
            JSONObject json = getJson(COINBASE, TIMEOUT_MS);
            JSONObject data = json.optJSONObject("data");
            double usd = data == null ? 0d : parseDouble(data.optString("amount", ""));
            return isPositive(usd) ? new Price(usd, "") : null;
        } catch (ApiException error) {return null;}
    }

    private static JSONObject getJson(String url, int timeoutMs) throws ApiException {
        String body = getRaw(url, timeoutMs);
        try {return new JSONObject(body);}
        catch (JSONException error) {throw new ApiException(ERROR_BAD_RESPONSE, "Ответ не является JSON");}
    }

    private static JSONObject postJson(String url, String payload, int timeoutMs) throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {out.write(bytes);}
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (code < 200 || code >= 300) {throw new ApiException(ERROR_SERVER, "HTTP " + code + serverDetail(body));}
            try {return new JSONObject(body);}
            catch (JSONException error) {throw new ApiException(ERROR_BAD_RESPONSE, "Ответ не является JSON");}
        } catch (ApiException error) {throw error;}
        catch (IOException error) {throw new ApiException(isTlsFailure(error) ? ERROR_TLS : ERROR_NETWORK, networkDetail(error));}
        finally {if (connection != null) {connection.disconnect();}}
    }

    private static String getRaw(String url, int timeoutMs) throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (code < 200 || code >= 300) {throw new ApiException(ERROR_SERVER, "HTTP " + code + serverDetail(body));}
            return body;
        } catch (ApiException error) {throw error;}
        catch (IOException error) {throw new ApiException(isTlsFailure(error) ? ERROR_TLS : ERROR_NETWORK, networkDetail(error));} finally {if (connection != null) {connection.disconnect();}}
    }

    private static String encode(String address) {try {return URLEncoder.encode(address, "UTF-8");} catch (java.io.UnsupportedEncodingException error) {return address;}}

    private static double nanoToGram(String nano) throws ApiException {
        double value = parseDouble(nano);
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {throw new ApiException(ERROR_BAD_RESPONSE, "Некорректный баланс");}
        return value / 1_000_000_000d;
    }

    private static double parseDouble(String value) {
        if (value == null || value.isEmpty()) {return Double.NaN;}
        try {return Double.parseDouble(value);} catch (NumberFormatException error) {return Double.NaN;}
    }

    private static boolean isPositive(double value) {return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0;}

    private static String change(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent)) {return "";}
        return String.format(Locale.US, "%s%.2f%%", percent >= 0 ? "+" : "-", Math.abs(percent));
    }

    private static boolean isRetryable(ApiException error) {return ERROR_SERVER.equals(error.kind) || ERROR_BAD_RESPONSE.equals(error.kind);}
    private static void sleep(long millis) {try {Thread.sleep(millis);} catch (InterruptedException interrupted) {Thread.currentThread().interrupt();}}

    private static boolean isTlsFailure(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 12; depth++) {
            if (current instanceof java.security.cert.CertificateException|| current instanceof javax.net.ssl.SSLException|| current.getClass().getName().contains("CertPathValidatorException")) {return true;}
            Throwable cause = current.getCause();
            if (cause == current) {break;}
            current = cause;
        }
        return false;
    }

    private static String serverDetail(String body) {
        if (body == null || body.isEmpty()) {return "";}
        try {
            JSONObject json = new JSONObject(body);
            String detail = json.optString("error", "").trim();
            if (detail.isEmpty()) {detail = json.optString("result", "").trim();}
            return detail.isEmpty() ? "" : ": " + detail;
        } catch (JSONException ignored) {return "";}
    }

    private static String networkDetail(IOException error) {
        String message = error.getMessage();
        if (message == null || message.isEmpty()) {message = error.getClass().getSimpleName();}
        return message;
    }

    private static ApiException combine(List<ApiException> failures) {
        if (failures.isEmpty()) {return new ApiException(ERROR_NETWORK, "Нет ответа от серверов");}
        ApiException best = failures.get(0);
        int bestRank = rank(best.kind);
        StringBuilder details = new StringBuilder();
        for (ApiException failure : failures) {
            if (rank(failure.kind) > bestRank) {
                best = failure;
                bestRank = rank(failure.kind);
            }
            String message = failure.getMessage();
            if (message == null || message.isEmpty() || details.indexOf(message) >= 0) {continue;}
            if (details.length() > 0) {details.append("; ");}
            details.append(message);
        }
        if (details.length() > 400) {details.setLength(400);}
        android.util.Log.w("GramWallet", "Не удалось получить баланс: " + details);
        return new ApiException(best.kind, details.toString());
    }

    private static int rank(String kind) {
        if (ERROR_INVALID_ADDRESS.equals(kind)) {return 5;}
        if (ERROR_TLS.equals(kind)) {return 4;}
        if (ERROR_SERVER.equals(kind)) {return 3;}
        if (ERROR_BAD_RESPONSE.equals(kind)) {return 2;}
        return 1;
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) {return "";}
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader =new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {builder.append(line);}
        }
        return builder.toString();
    }

    static final class ActionsPage {
        final List<Tx> items;
        final String next;
        ActionsPage(List<Tx> items, String next) {
            this.items = items == null ? new ArrayList<>() : items;
            this.next = next;
        }
    }

    private static final class Price {
        static final Price NONE = new Price(0d, "");
        final double usd;
        final String change;
        Price(double usd, String change) {
            this.usd = usd;
            this.change = change == null ? "" : change;
        }
    }

    static final class ApiException extends IOException {
        final String kind;
        ApiException(String kind, String detail) {
            super(detail);
            this.kind = kind;
        }
    }
}
