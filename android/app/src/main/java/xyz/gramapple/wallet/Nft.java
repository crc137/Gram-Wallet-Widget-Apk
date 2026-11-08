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

final class Nft {
    final String name;
    final String collection;
    final String image;
    final boolean scam;

    Nft(String name, String collection, String image, boolean scam) {
        this.name = name == null ? "" : name;
        this.collection = collection == null ? "" : collection;
        this.image = image == null ? "" : image;
        this.scam = scam;
    }
}
