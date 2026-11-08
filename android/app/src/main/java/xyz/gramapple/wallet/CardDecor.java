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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CardDecor {
    static final CardDecor EMPTY = new CardDecor(null, Collections.emptyList(), false, null);
    final Background background;
    final List<Gift> gifts;
    final boolean telegramWallet;
    final String domain;

    CardDecor(Background background, List<Gift> gifts, boolean telegramWallet, String domain) {
        this.background = background;
        this.gifts = gifts == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(gifts));
        this.telegramWallet = telegramWallet;
        this.domain = domain == null || domain.isEmpty() ? null : domain;
    }

    boolean isEmpty() {return background == null && gifts.isEmpty();}

    static final class Background {
        final int centerColor;
        final int edgeColor;
        final int patternColor;
        final String giftId;
        final String name;

        Background(int centerColor, int edgeColor, int patternColor, String giftId, String name) {
            this.centerColor = centerColor;
            this.edgeColor = edgeColor;
            this.patternColor = patternColor;
            this.giftId = giftId;
            this.name = name;
        }
    }

    static final class Gift {
        final String id;
        final String nft;
        final String tgs;
        final float x;
        final float y;
        final float w;
        final float h;
        final float rotation;
        final boolean background;

        Gift(String id,String nft,String tgs,float x,float y,float w,float h,float rotation,boolean background) {
            this.id = id;
            this.nft = nft;
            this.tgs = tgs;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.rotation = rotation;
            this.background = background;
        }
    }
}
