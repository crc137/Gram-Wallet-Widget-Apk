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
import android.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TonAddress {
    private static final Pattern RAW = Pattern.compile("^(-?\\d{1,3}):([0-9a-fA-F]{64})$");
    private static final Pattern FRIENDLY = Pattern.compile("^[A-Za-z0-9_-]{48}$");
    private static final int FRIENDLY_BYTES = 36;

    private TonAddress() {}

    static String normalize(String input) {
        if (input == null) {return null;}
        String value = input.trim().replaceAll("\\s+", "");
        if (value.isEmpty()) {return null;}
        int addressParam = value.indexOf("address=");
        if (addressParam >= 0) {value = value.substring(addressParam + "address=".length());}
        int query = value.indexOf('?');
        if (query >= 0) {value = value.substring(0, query);}
        int fragment = value.indexOf('#');
        if (fragment >= 0) {value = value.substring(0, fragment);}
        if (value.startsWith("ton://")) {value = value.substring("ton://".length());}
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {value = value.substring(slash + 1);}
        value = value.replaceAll("[,.;:]+$", "");
        if (isFriendly(value)) {return value;}
        Matcher raw = RAW.matcher(value);
        if (raw.matches()) {return rawToFriendly(raw.group(1), raw.group(2));}
        return null;
    }

    static boolean isValid(String input) {return normalize(input) != null;}

    private static boolean isFriendly(String value) {
        if (!FRIENDLY.matcher(value).matches()) {return false;}
        byte[] decoded;
        try {decoded = Base64.decode(value, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);} catch (IllegalArgumentException error) {return false;}
        if (decoded.length != FRIENDLY_BYTES) {return false;}
        int expected = ((decoded[34] & 0xff) << 8) | (decoded[35] & 0xff);
        return crc16(decoded, 34) == expected;
    }

    private static String rawToFriendly(String workchain, String hex) {
        int chain;
        try {chain = Integer.parseInt(workchain);} catch (NumberFormatException error) {return null;}
        if (chain < -128 || chain > 127) {return null;}
        byte[] body = new byte[FRIENDLY_BYTES];
        body[0] = (byte) 0x11;
        body[1] = (byte) chain;
        for (int i = 0; i < 32; i++) {body[2 + i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);}
        int crc = crc16(body, 34);
        body[34] = (byte) (crc >> 8);
        body[35] = (byte) crc;
        return Base64.encodeToString(body, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private static int crc16(byte[] data, int length) {
        int crc = 0;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xff) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xffff;
            }
        }
        return crc;
    }

    static String cardAddress(String address) {
        if (address == null || address.isEmpty()) {return "";}
        int groups = (address.length() + 3) / 4;
        int firstLine = (groups + 1) / 2;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < groups; i++) {
            if (i == firstLine) {out.append('\n');} else if (i > 0) {out.append(' ');}
            out.append(address, i * 4, Math.min(i * 4 + 4, address.length()));
        }
        return out.toString();
    }

    static boolean isTestnet(String address) {
        if (address == null || address.isEmpty()) {return false;}
        try {
            byte[] decoded = Base64.decode(address, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return decoded.length == FRIENDLY_BYTES && (decoded[0] & 0x80) != 0;
        } catch (IllegalArgumentException error) {return false;}
    }
}
