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
import java.util.Locale;

final class Tx {
    static final String TYPE_TON = "ton_transfer";
    static final String TYPE_NFT = "nft_transfer";
    final String id;
    final String type;
    final boolean incoming;
    final boolean success;
    final long time;
    final double gram;
    final String counterparty;
    final String comment;
    final String nftName;
    final String collection;
    final String image;

    Tx(String id,String type,boolean incoming,boolean success,long time,double gram,String counterparty,String comment,String nftName,String collection,String image) {
        this.id = id == null ? "" : id;
        this.type = type == null ? "" : type;
        this.incoming = incoming;
        this.success = success;
        this.time = time;
        this.gram = gram;
        this.counterparty = counterparty == null ? "" : counterparty;
        this.comment = comment == null ? "" : comment;
        this.nftName = nftName == null ? "" : nftName;
        this.collection = collection == null ? "" : collection;
        this.image = image == null ? "" : image;
    }

    boolean isNft() {return TYPE_NFT.equals(type);}
    boolean hasHiddenComment() {return !isNft() && incoming && !success && comment != null && !comment.trim().isEmpty();}
    boolean isSpam() {
        if (isNft() || !incoming) {return false;}
        if (!TYPE_TON.equals(type) && !type.isEmpty() && !type.contains("transfer")) {return false;}
        String note = comment == null ? "" : comment.trim();
        if (gram <= 0.000001d) {return true;}
        if (gram <= 0.001d) {return true;}
        if (note.isEmpty()) {return false;}
        if (!success && gram <= 1.0d) {return true;}
        return gram <= 2.0d && looksLikePromo(note);
    }

    boolean matchesTypeFilter(String filter) {
        if (filter == null || "all".equals(filter)) {return true;}
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if ("ton".equals(filter)) {return TYPE_TON.equals(type);}
        if ("nft".equals(filter)) {return TYPE_NFT.equals(type) || t.contains("nft");}
        if ("token".equals(filter)) {return t.contains("jetton") || t.contains("token");}
        if ("swap".equals(filter)) {return t.contains("swap") || t.contains("dex");}
        if ("stake".equals(filter)) {return t.contains("stake");}
        if ("domain".equals(filter)) {
            if (t.contains("dns") || t.contains("domain")) {return true;}
            if (!isNft()) {return false;}
            String name = (nftName + " " + collection).toLowerCase(Locale.ROOT);
            return name.contains(".ton") || name.contains("domain") || name.contains("dns");
        }
        if ("contract".equals(filter)) {return t.contains("contract") || t.contains("gasless") || t.contains("deploy");}
        return true;
    }

    private static boolean looksLikePromo(String note) {
        String n = note.toLowerCase(Locale.ROOT);
        if (note.indexOf('@') >= 0) {return true;}
        if (n.contains("http://") || n.contains("https://") || n.contains("t.me") || n.contains(".com") || n.contains(".vip") || n.contains(".cc")) {return true;}
        return note.length() >= 80;
    }
}
