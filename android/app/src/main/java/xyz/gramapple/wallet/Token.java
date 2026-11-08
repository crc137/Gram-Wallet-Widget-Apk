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

final class Token {
    final String name;
    final String symbol;
    final String balanceRaw;
    final int decimals;
    final Double price;
    final String image;
    final boolean scam;
    final boolean verified;

    Token(String name, String symbol, String balanceRaw, int decimals, Double price, String image, boolean scam, boolean verified) {
        this.name = name == null ? "" : name;
        this.symbol = symbol == null ? "" : symbol;
        this.balanceRaw = balanceRaw == null ? "0" : balanceRaw;
        this.decimals = Math.max(0, decimals);
        this.price = price;
        this.image = image == null ? "" : image;
        this.scam = scam;
        this.verified = verified;
    }
}
