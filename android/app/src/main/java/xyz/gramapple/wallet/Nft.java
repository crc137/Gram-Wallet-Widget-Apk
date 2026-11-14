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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Nft {
    private static final Pattern TG_GIFT = Pattern.compile("^(.+?)\\s+#(\\d+)$");
    final String address;
    final String name;
    final String collection;
    final String image;
    final boolean scam;

    Nft(String address, String name, String collection, String image, boolean scam) {
        this.address = address == null ? "" : address;
        this.name = name == null ? "" : name;
        this.collection = collection == null ? "" : collection;
        this.image = image == null ? "" : image;
        this.scam = scam;
    }

    String openUrl() {
        String slug = telegramSlug();
        if (slug != null) {return "https://t.me/nft/" + slug;}
        if (!address.isEmpty()) {return "https://tonviewer.com/" + address;}
        return null;
    }

    String telegramSlug() {
        Matcher m = TG_GIFT.matcher(name.trim());
        if (!m.matches()) {return null;}
        String base = m.group(1).replace("'", "").replace("\u2019", "").replace(" ", "").replace(".", "");
        if (base.isEmpty()) {return null;}
        return base + "-" + m.group(2);
    }
}
