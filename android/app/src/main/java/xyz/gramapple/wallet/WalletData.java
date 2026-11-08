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

final class WalletData {
    final double gram;
    final double totalUsd;
    final String change;
    final boolean hasUsd;
    final long updatedAt;

    WalletData(double gram, double totalUsd, String change, boolean hasUsd, long updatedAt) {
        this.gram = gram;
        this.totalUsd = totalUsd;
        this.change = change == null ? "" : change;
        this.hasUsd = hasUsd;
        this.updatedAt = updatedAt;
    }
}
