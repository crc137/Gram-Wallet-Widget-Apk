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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

final class CardArt {
    static final float BASE_WIDTH = 974f;
    static final float CARD_HEIGHT_RATIO = 205f / 336f;
    private static final String BALANCE_GB = "GRAM";
    private static final float ADVANCE_RATIO = 1229f / 2048f;
    private static final float RADIUS = 2.875f;
    private static final float BALANCE_LEFT = 7.58929f;
    private static final float BALANCE_TOP = 21.131f;
    private static final float BALANCE_LINE = 8.33333f;
    private static final float BALANCE_ROW_CENTER = 4.37168f;
    private static final float GRAM_BASELINE_SHIFT = 0.2053f;
    private static final float BALANCE_SIZE = 6.54762f;
    private static final float BALANCE_SMALL = 5.35714f;
    private static final float BALANCE_RIGHT = 29.7619f;
    private static final float BALANCE_GAP = 1.33929f;
    private static final float DIAMOND_W = 8.03571f;
    private static final float DIAMOND_H = 6.69643f;
    private static final float DIAMOND_ART = 12.0536f;
    private static final float USD_LEFT = 7.14286f;
    private static final float USD_TOP = 30.3571f;
    private static final float USD_SIZE = 3.86905f;
    private static final float LABEL_LEFT = 7.14286f;
    private static final float LABEL_BOTTOM = 5.05952f;
    private static final float LABEL_SIZE = 3.86905f;
    private static final float ADDR_RIGHT = 3.57143f;
    private static final float ADDR_INSET = 5.35714f;
    private static final float ADDR_STRIP = 6.54762f;
    private static final float ADDR_BLOCK = 50.2976f;
    private static final float ADDR_SIZE = 2.97619f;
    private static final float ADDR_SHADOW = 0.297619f;
    private static final float QR_TOP = 22.0238f;
    private static final float QR_RIGHT = 13.6905f;
    private static final float QR_W = 14.2857f;
    private static final float QR_H = 10.7143f;
    private static final float QR_RADIUS = 2.38095f;
    private static final float QR_RING = 0.297619f;
    private static final float QR_ICON = 7.14286f;
    private static final float PATTERN_REF_W = 1008f;
    private static final float PATTERN_STAMP_W = 62f;
    private static final float PATTERN_STAMP_H = 61f;
    private static final int[][] PATTERN_XY = {{63, 30}, {315, 30}, {567, 30}, {819, 30},{189, 105}, {441, 105}, {693, 105}, {945, 105},{63, 180}, {315, 180}, {567, 180}, {819, 180},{189, 255}, {441, 255}, {693, 255}, {945, 255},{63, 330}, {315, 330}, {567, 330}, {819, 330},{189, 405}, {441, 405}, {693, 405}, {945, 405},{63, 480}, {315, 480}, {567, 480}, {819, 480},{189, 555}, {441, 555}, {693, 555}, {945, 555},{63, 630}, {315, 630}, {567, 630}, {819, 630}};
    private static final int[] STAR_X = {89, 78, 95, 84, 72, 11, 7, 21, 14, 92,86, 95, 10, 20, 7, 46, 60, 6, 78, 66};
    private static final int[] STAR_Y = {13, 8, 24, 22, 14, 76, 88, 86, 66, 62,74, 48, 14, 9, 28, 7, 10, 48, 91, 94};
    private static final float[] STAR_SIZE = {4.7619f, 2.38095f, 2.08333f, 1.4881f,1.19048f, 4.46429f, 2.38095f, 1.78571f, 1.4881f, 3.57143f,1.78571f, 1.4881f, 2.67857f, 1.4881f, 1.19048f, 1.4881f,1.19048f, 1.4881f, 2.08333f, 1.19048f};
    private static final float[] STAR_ALPHA = {0.48f, 0.24f, 0.2f, 0.14f, 0.1f,0.44f, 0.22f, 0.16f, 0.12f, 0.36f, 0.16f, 0.12f, 0.26f, 0.12f,0.1f, 0.12f, 0.1f, 0.12f, 0.18f, 0.1f};
    private static final float[] T1_POS = {0f, 0.01375f, 0.04375f, 0.090625f,0.09875f, 0.15625f, 0.215625f, 0.38125f};
    private static final int[] T1_COL = {0x6BFFFFFF, 0x33000000, 0x00000000,0x2EFFFFFF, 0x00000000, 0x29000000, 0x00000000};
    private static final float[] T2_POS = {0f, 0.028125f, 0.071875f, 0.121875f,0.134375f, 0.2625f, 0.27375f, 0.51875f};
    private static final int[] T2_COL = {0x2E000000, 0x00000000, 0x47FFFFFF,0x1F000000, 0x00000000, 0x1FFFFFFF, 0x00000000};
    private static final float[] T3_POS = {0f, 0.00875f, 0.059375f, 0.115625f,0.16875f, 0.325f};
    private static final int[] T3_COL = {0x38FFFFFF, 0x00000000, 0x24000000,0x29FFFFFF, 0x00000000};
    private static final float[] T4_POS = {0f, 0.04375f, 0.078125f, 0.73125f};
    private static final int[] T4_COL = {0x52FFFFFF, 0x2E000000, 0x00000000};
    private static Typeface sTypeface;
    private static Bitmap sDiamond;
    private static Bitmap sQrIcon;
    private static Path sStar;
    private CardArt() {}
    static float heightForWidth(float width) {return width * CARD_HEIGHT_RATIO;}

    static void massHitRect(float w, RectF out) {
        float q = w / 100f;
        out.set(BALANCE_LEFT * q, (BALANCE_TOP - 1.5f) * q, (100f - BALANCE_RIGHT) * q, (BALANCE_TOP + BALANCE_LINE + 1.5f) * q);
    }

    static Bitmap render(Context context, int width, String balance, String usd, String address, CardDecor decor) {
        int h = Math.max(1, Math.round(heightForWidth(width)));
        Bitmap bitmap = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas, width, h, balance, usd, address, context, decor == null ? CardDecor.EMPTY : decor);
        return bitmap;
    }

    static Bitmap render(Resources res, int width, String balance, String usd, String address) {return render(null, width, balance, usd, address, CardDecor.EMPTY);}

    static void draw(Canvas canvas,float w,float h,String balance,String usd,String address,Context context,CardDecor decor) {
        Resources res = context == null ? null : context.getResources();
        float q = w / 100f;
        float radius = RADIUS * q;
        Path cardPath = new Path();
        cardPath.addRoundRect(new RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(cardPath);
        boolean customBg = decor.background != null;
        Palette pal = palette(decor.background);
        drawBackground(canvas, w, h, decor.background);
        if (customBg) {drawPattern(canvas, w, h, context, decor);}
        drawTexture(canvas, w, h);
        drawStars(canvas, w, h, pal.star);
        drawGifts(canvas, w, h, context, decor);
        drawGlare(canvas, w, h);
        boolean hasAddress = address != null && !address.isEmpty();
        drawBalanceRow(canvas, w, balance, res, pal);
        drawUsd(canvas, w, h, usd, pal);
        drawLabel(canvas, w, h, res, decor);
        if (hasAddress) {
            drawAddress(canvas, w, h, address, pal.address);
            drawQr(canvas, w, h, res, pal);
        }
        drawInnerShade(canvas, w, h, pal);
        canvas.restore();
    }

    private static final class Palette {
        final int unit;
        final int usd;
        final int address;
        final int star;
        final int rim;
        final int ring;
        final int chipTop;
        final int chipBottom;
        final int chipShadow;
        Palette(int unit, int usd, int address, int star, int rim, int ring, int chipTop, int chipBottom, int chipShadow) {
            this.unit = unit;
            this.usd = usd;
            this.address = address;
            this.star = star;
            this.rim = rim;
            this.ring = ring;
            this.chipTop = chipTop;
            this.chipBottom = chipBottom;
            this.chipShadow = chipShadow;
        }
    }

    private static Palette palette(CardDecor.Background bg) {
        if (bg == null) {return new Palette(0xFF6DDCFF, 0xFF87EFFF, 0xCC055AC4, Color.WHITE, 0x5978D6FF, 0xFF0C73D5, 0xFFF4F9FF, 0xFFA8C7ED, 0x4000328C);}
        int accent = mix(bg.centerColor, 0xFFFFFFFF, 0.72f);
        int rim = withAlpha(mix(bg.centerColor, 0xFFFFFFFF, 0.55f), 0.55f);
        int ring = mix(bg.edgeColor, bg.centerColor, 0.35f);
        int address = withAlpha(mix(bg.edgeColor, 0xFF000000, 0.35f), 0.8f);
        int star = withAlpha(mix(bg.centerColor, 0xFFFFFFFF, 0.88f), 0.95f);
        return new Palette(accent, accent, address, star, rim, ring | 0xFF000000, 0xFFF7F8FF, 0xFFB9C1EC, withAlpha(bg.edgeColor, 0.28f));
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int mix(int a, int b, float t) {
        float u = 1f - t;
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar * u + br * t);
        int g = Math.round(ag * u + bg * t);
        int bl = Math.round(ab * u + bb * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void drawBackground(Canvas canvas, float w, float h, CardDecor.Background bg) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (bg != null) {paint.setShader(new RadialGradient(w * 0.42f, h * 0.38f, Math.max(w, h) * 0.92f,bg.centerColor, bg.edgeColor, Shader.TileMode.CLAMP));} else {paint.setShader(linear(145f,new RectF(0f, 0f, w, h),new int[]{0xFF5CC8FF, 0xFF0088FF, 0xFF0057C2},new float[]{0f, 0.46f, 1f}));}
        canvas.drawRect(0f, 0f, w, h, paint);
    }

    private static void drawPattern(Canvas canvas, float w, float h, Context context, CardDecor decor) {
        CardDecor.Background bg = decor == null ? null : decor.background;
        if (context == null || bg == null || bg.giftId == null) {return;}
        String nft = null;
        for (CardDecor.Gift gift : decor.gifts) {
            if (bg.giftId.equals(gift.id)) {nft = gift.nft; break;}
            if (nft == null && gift.background) {nft = gift.nft;}
        }
        Bitmap stamp = GiftImages.getPattern(context, bg.giftId, nft);
        if (stamp == null) {return;}
        float sx = w / PATTERN_REF_W;
        float sy = h / (PATTERN_REF_W * CARD_HEIGHT_RATIO);
        float sw = PATTERN_STAMP_W * sx;
        float sh = PATTERN_STAMP_H * sy;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new PorterDuffColorFilter(bg.patternColor, PorterDuff.Mode.SRC_IN));
        paint.setAlpha(110);
        for (int[] p : PATTERN_XY) {
            float cx = p[0] * sx;
            float cy = p[1] * sy;
            canvas.drawBitmap(stamp, null, new RectF(cx - sw * 0.5f, cy - sh * 0.5f, cx + sw * 0.5f, cy + sh * 0.5f), paint);
        }
        paint.setColorFilter(null);
    }

    private static void drawGifts(Canvas canvas, float w, float h, Context context, CardDecor decor) {
        if (context == null || decor == null || decor.gifts.isEmpty()) {return;}
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        outline.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        shadow.setColorFilter(new PorterDuffColorFilter(0x66000000, PorterDuff.Mode.SRC_IN));
        shadow.setMaskFilter(new BlurMaskFilter(Math.max(2f, Math.min(w, h) * 0.014f), BlurMaskFilter.Blur.NORMAL));
        List<CardDecor.Gift> ordered = new ArrayList<>(decor.gifts);
        ordered.sort((a, b) -> Float.compare(b.w * b.h, a.w * a.h));
        for (CardDecor.Gift gift : ordered) {
            float bw = gift.w * w / 100f;
            float bh = gift.h * h / 100f;
            float cx = gift.x * w / 100f;
            float cy = gift.y * h / 100f;
            Bitmap bitmap = GiftImages.get(context, gift);
            if (bitmap == null) {continue;}
            float stroke = Math.max(1.6f, Math.min(bw, bh) * 0.038f);
            float drop = Math.max(1.5f, Math.min(bw, bh) * 0.04f);
            RectF dst = new RectF(-bw * 0.5f, -bh * 0.5f, bw * 0.5f, bh * 0.5f);
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(gift.rotation);
            canvas.drawBitmap(bitmap,null,new RectF(dst.left + drop * 0.2f, dst.top + drop, dst.right + drop * 0.2f, dst.bottom + drop),shadow);
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI / 8.0;
                float ox = (float) (Math.cos(ang) * stroke);
                float oy = (float) (Math.sin(ang) * stroke);
                canvas.drawBitmap(bitmap,null,new RectF(dst.left + ox, dst.top + oy, dst.right + ox, dst.bottom + oy),outline);
            }
            canvas.drawBitmap(bitmap, null, dst, paint);
            canvas.restore();
        }
        outline.setColorFilter(null);
        shadow.setMaskFilter(null);
        shadow.setColorFilter(null);
    }

    private static void drawTexture(Canvas canvas, float w, float h) {
        Bitmap texture = buildTexture(Math.round(w), Math.round(h));
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAlpha(38);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        canvas.drawBitmap(texture, 0f, 0f, paint);
        texture.recycle();
    }

    private static void drawStars(Canvas canvas, float w, float h, int color) {
        Path star = starPath();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color | 0xFF000000);
        for (int i = 0; i < STAR_X.length; i++) {
            float size = STAR_SIZE[i] * w / 100f;
            float scale = size / 24f;
            canvas.save();
            canvas.translate(STAR_X[i] * w / 100f, STAR_Y[i] * h / 100f);
            canvas.scale(scale, scale);
            canvas.translate(-12f, -12f);
            int baseA = Math.round(STAR_ALPHA[i] * 255f);
            int tintA = (color >>> 24) & 0xFF;
            paint.setAlpha(Math.round(baseA * (tintA / 255f)));
            canvas.drawPath(star, paint);
            canvas.restore();
        }
    }

    private static void drawGlare(Canvas canvas, float w, float h) {
        float q = w / 100f;
        float scale = 16f;
        int gw = Math.max(48, Math.round(w / scale));
        int gh = Math.max(32, Math.round(h / scale));
        Bitmap glare = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888);
        Canvas gc = new Canvas(glare);
        int[] colors = {0x00FFFFFF, 0x00FFFFFF, 0x1AFFFFFF, 0x2BFFFFFF, 0x60FFFFFF,0x2BFFFFFF, 0x1AFFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x1AFFFFFF,0x2BFFFFFF, 0x60FFFFFF, 0x2BFFFFFF, 0x1AFFFFFF, 0x00FFFFFF,0x00FFFFFF};
        float[] stops = {0f, 0.0111f, 0.0778f, 0.1167f, 0.15f, 0.1833f, 0.2389f,0.3444f, 0.5111f, 0.5778f, 0.6167f, 0.65f, 0.6833f, 0.7389f,0.8444f, 1f};
        SweepGradient sweep = new SweepGradient(gw / 2f, gh / 2f, colors, stops);
        Matrix matrix = new Matrix();
        matrix.setRotate(-90f, gw / 2f, gh / 2f);
        sweep.setLocalMatrix(matrix);
        Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
        gp.setShader(sweep);
        gc.drawRect(0f, 0f, gw, gh, gp);
        int[] pixels = new int[gw * gh];
        glare.getPixels(pixels, 0, gw, 0, 0, gw, gh);
        byte[] alpha = new byte[gw * gh];
        for (int i = 0; i < alpha.length; i++) {alpha[i] = (byte) (pixels[i] >>> 24);}
        float sigmaLow = 0.0535714f * gw;
        int radius = Math.max(1, Math.round(sigmaLow - 0.5f));
        byte[] tmp = new byte[alpha.length];
        for (int pass = 0; pass < 3; pass++) {
            blurAlphaH(alpha, tmp, gw, gh, radius);
            blurAlphaV(tmp, alpha, gw, gh, radius);
        }
        for (int i = 0; i < pixels.length; i++) {
            int a = alpha[i] & 0xFF;
            pixels[i] = (a << 24) | 0x00FFFFFF;
        }
        glare.setPixels(pixels, 0, gw, 0, 0, gw, gh);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setAlpha(163);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        canvas.drawBitmap(glare, null, new RectF(0f, 0f, w, h), paint);
        glare.recycle();
    }

    private static void blurAlphaH(byte[] src, byte[] dst, int w, int h, int r) {
        int div = 2 * r + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sum = 0;
            for (int i = -r; i <= r; i++) {sum += src[row + clamp(i, w)] & 0xFF;}
            for (int x = 0; x < w; x++) {
                dst[row + x] = (byte) ((sum + div / 2) / div);
                sum -= src[row + clamp(x - r, w)] & 0xFF;
                sum += src[row + clamp(x + r + 1, w)] & 0xFF;
            }
        }
    }

    private static void blurAlphaV(byte[] src, byte[] dst, int w, int h, int r) {
        int div = 2 * r + 1;
        for (int x = 0; x < w; x++) {
            int sum = 0;
            for (int i = -r; i <= r; i++) {sum += src[clamp(i, h) * w + x] & 0xFF;}
            for (int y = 0; y < h; y++) {
                dst[y * w + x] = (byte) ((sum + div / 2) / div);
                sum -= src[clamp(y - r, h) * w + x] & 0xFF;
                sum += src[clamp(y + r + 1, h) * w + x] & 0xFF;
            }
        }
    }

    private static int clamp(int value, int limit) {
        if (value < 0) {return 0;}
        if (value >= limit) {return limit - 1;}
        return value;
    }

    private static void drawInnerShade(Canvas canvas, float w, float h, Palette pal) {
        float q = w / 100f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0f, 0f, 0f, 0.9f * q,new int[]{0x24000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, w, 0.9f * q, paint);
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setColor(pal.rim);
        canvas.drawRect(0f, 0f, w, Math.max(1f, 0.06f * q), edge);
        edge.setColor(withAlpha(pal.rim, 0.18f));
        canvas.drawRect(0f, 0f, Math.max(1f, 0.06f * q), h, edge);
    }

    private static void drawBalanceRow(Canvas canvas, float w, String balance, Resources res, Palette pal) {
        float q = w / 100f;
        float left = BALANCE_LEFT * q;
        float top = BALANCE_TOP * q;
        float line = BALANCE_LINE * q;
        float bigSize = BALANCE_SIZE * q;
        float smallSize = BALANCE_SMALL * q;
        float boxW = DIAMOND_W * q;
        float boxH = DIAMOND_H * q;
        float gap = BALANCE_GAP * q;
        String value = balance == null || balance.isEmpty() ? "—" : balance;
        int sep = value.lastIndexOf(',');
        if (sep < 0) {sep = value.lastIndexOf('.');}
        String whole = sep >= 0 ? value.substring(0, sep) : value;
        String fraction = sep >= 0 ? value.substring(sep) : "";
        Paint big = text(bigSize, 0xFFFFFFFF, res);
        Paint small = text(smallSize, 0xFFFFFFFF, res);
        Paint unit = text(smallSize, withAlpha(pal.unit, 0.78f), res);
        float wholeW = big.measureText(whole);
        float fractionW = small.measureText(fraction);
        float unitW = unit.measureText(BALANCE_GB);
        float total = boxW + gap + wholeW + fractionW + gap + unitW;
        float available = (100f - BALANCE_LEFT - BALANCE_RIGHT) * q;
        float scale = total > available ? available / total : 1f;
        canvas.save();
        canvas.translate(left, top);
        if (scale < 1f) {canvas.scale(scale, scale);}

        Bitmap diamond = diamond(res);
        if (diamond != null) {
            float side = DIAMOND_ART * q;
            float cx = boxW / 2f;
            float cy = BALANCE_ROW_CENTER * q;
            RectF dst = new RectF(cx - side / 2f, cy - side / 2f,cx + side / 2f, cy + side / 2f);
            Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(diamond, null, dst, bitmapPaint);
        }
        float numberX = boxW + gap;
        float baseline = baseline(big, 0f, line);
        float after = drawText(canvas, whole, numberX, baseline, big);
        if (!fraction.isEmpty()) {after = drawText(canvas, fraction, after, baseline, small);}
        drawText(canvas, BALANCE_GB, after + gap,baseline - GRAM_BASELINE_SHIFT * q, unit);
        canvas.restore();
    }

    private static void drawUsd(Canvas canvas, float w, float h, String usd, Palette pal) {
        float q = w / 100f;
        float size = USD_SIZE * q;
        Paint paint = text(size, pal.usd, null);
        String value = usd == null || usd.isEmpty() ? "—" : usd;
        drawText(canvas, value, USD_LEFT * q, baseline(paint, USD_TOP * q, size * 1.2f), paint);
    }

    private static void drawLabel(Canvas canvas, float w, float h, Resources res, CardDecor decor) {
        float q = w / 100f;
        float size = LABEL_SIZE * q;
        float line = size * 1.2f;
        float top = h - LABEL_BOTTOM * q - line;
        Paint paint = text(size, 0xFFFFFFFF, null);
        String label;
        if (decor != null && decor.domain != null) {label = decor.domain;}
        else if (res != null) {label = res.getString(R.string.card_label);}
        else {label = "Telegram Wallet";}
        label = label.toUpperCase(java.util.Locale.getDefault());
        canvas.drawText(label, LABEL_LEFT * q, baseline(paint, top, line), paint);
    }

    private static void drawAddress(Canvas canvas, float w, float h, String address, int color) {
        float q = w / 100f;
        String pretty = TonAddress.cardAddress(address);
        String[] lines = pretty.split("\n");
        float size = ADDR_SIZE * q;
        float line = size * 1.15f;
        float strip = ADDR_STRIP * q;
        float cx = w - ADDR_RIGHT * q - strip / 2f;
        float cy = h / 2f;
        Paint paint = text(size, color, null);
        Paint shadow = new Paint(paint);
        shadow.setColor(0x1AFFFFFF);
        float total = lines.length * line;
        float top = cy - total / 2f;
        canvas.save();
        canvas.rotate(90f, cx, cy);
        for (int i = 0; i < lines.length; i++) {
            String lineText = lines[i];
            float lineWidth = measure(paint, lineText);
            float x0 = cx - lineWidth / 2f;
            float base = baseline(paint, top + i * line, line);
            drawText(canvas, lineText, x0 + ADDR_SHADOW * q, base, shadow);
            drawText(canvas, lineText, x0, base, paint);
        }
        canvas.restore();
    }

    private static void drawQr(Canvas canvas, float w, float h, Resources res, Palette pal) {
        float q = w / 100f;
        float right = w - QR_RIGHT * q;
        RectF qr = new RectF(right - QR_W * q, QR_TOP * q, right, (QR_TOP + QR_H) * q);
        float radius = QR_RADIUS * q;
        Path shape = new Path();
        shape.addRoundRect(qr, radius, radius, Path.Direction.CW);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x1A000000);
        RectF ring = new RectF(qr);
        ring.inset(-QR_RING * q, -QR_RING * q);
        canvas.drawRoundRect(ring, radius + QR_RING * q, radius + QR_RING * q, paint);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(pal.chipShadow);
        paint.setMaskFilter(new BlurMaskFilter(0.9f * q, BlurMaskFilter.Blur.NORMAL));
        RectF shadow = new RectF(qr);
        shadow.offset(0f, 0.595238f * q);
        canvas.drawRoundRect(shadow, radius, radius, paint);
        canvas.save();
        canvas.clipPath(shape);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(linear(172f, qr, new int[]{pal.chipTop, pal.chipBottom}, new float[]{0.27f, 0.97f}));
        canvas.drawRect(qr, paint);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0f, qr.top, 0f, qr.top + 1.65f * q,new int[]{0xD9FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(qr.left, qr.top, qr.right, qr.top + 1.65f * q, paint);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * QR_RING * q);
        paint.setColor(pal.ring);
        canvas.drawPath(shape, paint);
        RectF sheenRect = new RectF(qr);
        sheenRect.inset(-0.6f * qr.width(), -0.4f * qr.height());
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(linear(115f, sheenRect,new int[]{0x00FFFFFF, 0x00FFFFFF, 0x80FFFFFF, 0xD9FFFFFF,0x66FFFFFF, 0x00FFFFFF},new float[]{0f, 0.38f, 0.47f, 0.5f, 0.53f, 0.62f}));
        canvas.drawRect(sheenRect, paint);

        Bitmap icon = qrIcon(res);
        if (icon != null) {
            float size = QR_ICON * q;
            RectF dst = new RectF(qr.centerX() - size / 2f, qr.centerY() - size / 2f,qr.centerX() + size / 2f, qr.centerY() + size / 2f);
            Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            RectF shadowDst = new RectF(dst);
            shadowDst.offset(0f, QR_RING * q);
            iconPaint.setColorFilter(new PorterDuffColorFilter(0xFFE6EDF5,PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(icon, null, shadowDst, iconPaint);
            iconPaint.setColorFilter(null);
            canvas.drawBitmap(icon, null, dst, iconPaint);
        }
        canvas.restore();
    }

    private static Paint text(float size, int color, Resources res) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(typeface(res));
        paint.setTextSize(size);
        paint.setHinting(Paint.HINTING_OFF);
        paint.setColor(color);
        return paint;
    }

    private static float drawText(Canvas canvas, String value, float x, float baseline, Paint paint) {
        float cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String glyph = value.substring(i, i + 1);
            canvas.drawText(glyph, cursor, baseline, paint);
            cursor += glyphAdvance(paint, glyph);
        }
        return cursor;
    }

    private static float measure(Paint paint, String value) {
        float width = 0f;
        for (int i = 0; i < value.length(); i++) {width += glyphAdvance(paint, value.substring(i, i + 1));}
        return width;
    }

    private static float glyphAdvance(Paint paint, String glyph) {
        float measured = paint.measureText(glyph);
        float exact = paint.getTextSize() * ADVANCE_RATIO;
        return Math.abs(measured - exact) > 0.05f ? exact : measured;
    }

    private static Typeface typeface(Resources res) {
        if (sTypeface == null) {
            Typeface typeface = null;
            if (res != null) {try {typeface = res.getFont(R.font.roboto_mono_medium);} catch (Exception ignored) {}}
            sTypeface = typeface != null ? typeface : Typeface.MONOSPACE;
        }
        return sTypeface;
    }

    private static float baseline(Paint paint, float top, float lineHeight) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float content = metrics.descent - metrics.ascent;
        return top + (lineHeight - content) / 2f - metrics.ascent;
    }

    private static Shader linear(float degrees, RectF rect, int[] colors, float[] positions) {
        double radians = Math.toRadians(degrees);
        float dx = (float) Math.sin(radians);
        float dy = (float) -Math.cos(radians);
        float length = Math.abs(rect.width() * dx) + Math.abs(rect.height() * dy);
        float x0 = rect.centerX() - dx * length / 2f;
        float y0 = rect.centerY() - dy * length / 2f;
        float x1 = rect.centerX() + dx * length / 2f;
        float y1 = rect.centerY() + dy * length / 2f;
        return new LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP);
    }

    private static Bitmap diamond(Resources res) {
        if (sDiamond == null && res != null) {try {sDiamond = BitmapFactory.decodeResource(res, R.drawable.white_diamond);} catch (Exception ignored) {sDiamond = null;}}
        return sDiamond;
    }

    private static Bitmap qrIcon(Resources res) {
        if (sQrIcon == null && res != null) {try {sQrIcon = BitmapFactory.decodeResource(res, R.drawable.card_qr);} catch (Exception ignored) {sQrIcon = null;}}
        return sQrIcon;
    }

    private static Bitmap buildTexture(int w, int h) {
        float rem = 16f * w / BASE_WIDTH;
        float maxRadius = (float) Math.sqrt(w * w + h * h) / 2f + 2f;
        float step = 0.5f;
        int count = (int) Math.ceil(maxRadius / step) + 2;
        int[] lut = new int[count];
        for (int i = 0; i < count; i++) {
            float r = i * step;
            lut[i] = combineTexture(r, rem);
        }

        int[] pixels = new int[w * h];
        float cx = w / 2f;
        float cy = h / 2f;
        for (int y = 0; y < h; y++) {
            float dy = y + 0.5f - cy;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                float dx = x + 0.5f - cx;
                float radius = (float) Math.sqrt(dx * dx + dy * dy);
                float pos = radius / step;
                int index = (int) pos;
                if (index >= count - 1) {
                    pixels[row + x] = lut[count - 1];
                    continue;
                }
                float t = pos - index;
                int c0 = lut[index];
                int c1 = lut[index + 1];
                int a = (int) (((c0 >>> 24) & 0xFF) * (1f - t) + ((c1 >>> 24) & 0xFF) * t);
                int rr = (int) (((c0 >> 16) & 0xFF) * (1f - t) + ((c1 >> 16) & 0xFF) * t);
                int gg = (int) (((c0 >> 8) & 0xFF) * (1f - t) + ((c1 >> 8) & 0xFF) * t);
                int bb = (int) ((c0 & 0xFF) * (1f - t) + (c1 & 0xFF) * t);
                pixels[row + x] = (a << 24) | (rr << 16) | (gg << 8) | bb;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private static int combineTexture(float radius, float rem) {
        float[] offsets = {-0.375f, -0.125f, 0.125f, 0.375f};
        long alpha = 0;
        long red = 0;
        long green = 0;
        long blue = 0;
        for (float offset : offsets) {
            float r = Math.max(0f, radius + offset);
            int color = 0;
            color = over(stopColor(r, rem, T4_POS, T4_COL), color);
            color = over(stopColor(r, rem, T3_POS, T3_COL), color);
            color = over(stopColor(r, rem, T2_POS, T2_COL), color);
            color = over(stopColor(r, rem, T1_POS, T1_COL), color);
            int a = (color >>> 24) & 0xFF;
            alpha += a;
            red += ((color >> 16) & 0xFF) * a;
            green += ((color >> 8) & 0xFF) * a;
            blue += (color & 0xFF) * a;
        }
        int a = (int) (alpha / offsets.length);
        if (a == 0) {return 0;}
        int r = (int) (red / alpha);
        int g = (int) (green / alpha);
        int b = (int) (blue / alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int stopColor(float radius, float rem, float[] positions, int[] colors) {
        float period = positions[positions.length - 1] * rem;
        if (period <= 0f) {return 0;}
        float u = radius % period;
        for (int i = 0; i < colors.length; i++) {if (u < positions[i + 1] * rem) {return colors[i];}}
        return 0;
    }

    private static int over(int src, int dst) {
        int sa = (src >>> 24) & 0xFF;
        if (sa == 0) {return dst;}
        int da = (dst >>> 24) & 0xFF;
        float as = sa / 255f;
        float ad = da / 255f;
        float ao = as + ad * (1f - as);
        if (ao <= 0f) {return 0;}
        int r = (int) ((((src >> 16) & 0xFF) * as + ((dst >> 16) & 0xFF) * ad * (1f - as)) / ao);
        int g = (int) ((((src >> 8) & 0xFF) * as + ((dst >> 8) & 0xFF) * ad * (1f - as)) / ao);
        int b = (int) (((src & 0xFF) * as + (dst & 0xFF) * ad * (1f - as)) / ao);
        return ((int) (ao * 255f) << 24) | (r << 16) | (g << 8) | b;
    }

    private static Path starPath() {
        if (sStar != null) {return sStar;}
        String data = "M11.65 2.09Q12 1.91 12.35 2.09C12.6 2.22 12.76 2.73 13.1 3.73"+ "L14.71 8.56Q14.85 9 15.44 9.29L20.27 10.9Q21.7 11.37 21.91 11.65"+ "Q22.09 12 21.91 12.34Q21.7 12.63 20.27 13.09L15.44 14.71"+ "Q14.85 14.85 14.71 15.44L13.1 20.27Q12.63 21.7 12.35 21.91"+ "Q12 22.09 11.65 21.91Q11.37 21.7 10.9 20.27L9.29 15.44"+ "Q9.15 14.85 8.56 14.71L3.73 13.1Q2.3 12.63 2.09 12.35"+ "Q1.91 12 2.09 11.65Q2.3 11.37 3.73 10.9L8.56 9.29"+ "Q9.15 9.15 9.29 8.56L10.9 3.73Q11.37 2.3 11.65 2.09Z";
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < data.length()) {
            char c = data.charAt(i);
            if (Character.isLetter(c)) {
                tokens.add(String.valueOf(c));
                i++;
            } else if (c == '-' || c == '.' || Character.isDigit(c)) {
                int start = i;
                i++;
                while (i < data.length()&& (Character.isDigit(data.charAt(i)) || data.charAt(i) == '.')) {i++;}
                tokens.add(data.substring(start, i));
            } else {i++;}
        }

        Path path = new Path();
        float x = 0f;
        float y = 0f;
        int index = 0;
        String command = "";
        while (index < tokens.size()) {
            String token = tokens.get(index);
            if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
                command = token;
                index++;
                if (command.equals("Z")) {
                    path.close();
                    continue;
                }
            }
            try {
                switch (command) {
                    case "M":
                        x = Float.parseFloat(tokens.get(index++));
                        y = Float.parseFloat(tokens.get(index++));
                        path.moveTo(x, y);
                        command = "L";
                        break;
                    case "L":
                        x = Float.parseFloat(tokens.get(index++));
                        y = Float.parseFloat(tokens.get(index++));
                        path.lineTo(x, y);
                        break;
                    case "Q": {
                        float cx = Float.parseFloat(tokens.get(index++));
                        float cy = Float.parseFloat(tokens.get(index++));
                        x = Float.parseFloat(tokens.get(index++));
                        y = Float.parseFloat(tokens.get(index++));
                        path.quadTo(cx, cy, x, y);
                        break;
                    }
                    case "C": {
                        float c1x = Float.parseFloat(tokens.get(index++));
                        float c1y = Float.parseFloat(tokens.get(index++));
                        float c2x = Float.parseFloat(tokens.get(index++));
                        float c2y = Float.parseFloat(tokens.get(index++));
                        x = Float.parseFloat(tokens.get(index++));
                        y = Float.parseFloat(tokens.get(index++));
                        path.cubicTo(c1x, c1y, c2x, c2y, x, y);
                        break;
                    }
                    default:index = tokens.size();break;
                }
            } catch (IndexOutOfBoundsException error) {break;}
        }
        sStar = path;
        return path;
    }
}
