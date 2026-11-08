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

/** One entry of the gram.org account activity feed. */
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
}
