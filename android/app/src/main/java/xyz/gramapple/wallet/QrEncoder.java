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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;

final class QrEncoder {
    private static final int TELEGRAM_BLUE = 0xFF0088CC;
    private static final float LOGO_RATIO = 0.14f;
    private QrEncoder() {}

    static Bitmap bitmap(Resources resources, String text, int sizePx) {
        boolean[][] modules = encode(transferPayload(text));
        int n = modules.length;
        int quiet = 4;
        int dim = n + quiet * 2;
        int scale = Math.max(3, sizePx / dim);
        int out = dim * scale;
        int logoBox = Math.max(scale * 3, Math.round(out * LOGO_RATIO));
        float mid = out * 0.5f;
        float plateR = logoBox * 0.58f;
        float clearR = plateR + scale * 0.25f;
        float clearR2 = clearR * clearR;
        Bitmap bitmap = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(TELEGRAM_BLUE);
        paint.setStyle(Paint.Style.FILL);
        Path path = new Path();
        RectF oval = new RectF();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!modules[y][x] || isFinderModule(x, y, n)) {continue;}
                float x0 = (x + quiet) * scale;
                float y0 = (y + quiet) * scale;
                float mcx = x0 + scale * 0.5f;
                float mcy = y0 + scale * 0.5f;
                float dx = mcx - mid;
                float dy = mcy - mid;
                if (dx * dx + dy * dy <= clearR2) {continue;}
                boolean left = dark(modules, x - 1, y, n);
                boolean right = dark(modules, x + 1, y, n);
                boolean top = dark(modules, x, y - 1, n);
                boolean bottom = dark(modules, x, y + 1, n);
                drawRoundedDot(canvas, paint, path, oval, x0, y0, scale, left, right, top, bottom);
            }
        }
        float m = scale;
        float q = quiet * m;
        drawExtraRoundedFinder(canvas, paint, path, oval, q, q, m);
        drawExtraRoundedFinder(canvas, paint, path, oval, q + (n - 7) * m, q, m);
        drawExtraRoundedFinder(canvas, paint, path, oval, q, q + (n - 7) * m, m);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(mid, mid, plateR, paint);
        Bitmap logo = BitmapFactory.decodeResource(resources, R.drawable.arzunas_logo);
        if (logo != null) {
            float left = mid - logoBox * 0.5f;
            float top = mid - logoBox * 0.5f;
            canvas.drawBitmap(logo, null, new RectF(left, top, left + logoBox, top + logoBox),new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            logo.recycle();
        }
        return bitmap;
    }

    static String transferPayload(String addressOrUri) {
        if (addressOrUri == null || addressOrUri.isEmpty()) {return "";}
        String value = addressOrUri.trim();
        if (value.startsWith("ton://") || value.startsWith("https://") || value.startsWith("http://")) {return value;}
        return "ton://transfer/" + value;
    }

    private static boolean dark(boolean[][] modules, int x, int y, int n) {return x >= 0 && y >= 0 && x < n && y < n && modules[y][x] && !isFinderModule(x, y, n);}

    private static void drawRoundedDot(Canvas canvas, Paint paint, Path path, RectF oval,float x0, float y0, float size, boolean left, boolean right, boolean top, boolean bottom) {
        int count = (left ? 1 : 0) + (right ? 1 : 0) + (top ? 1 : 0) + (bottom ? 1 : 0);
        float cx = x0 + size * 0.5f;
        float cy = y0 + size * 0.5f;
        path.reset();
        if (count == 0) {
            path.addCircle(cx, cy, size * 0.5f, Path.Direction.CW);
            canvas.drawPath(path, paint);
            return;
        }
        if (count > 2 || (left && right) || (top && bottom)) {
            canvas.drawRect(x0, y0, x0 + size, y0 + size, paint);
            return;
        }
        float rotation = 0f;
        if (count == 2) {
            if (left && top) {rotation = 90f;}
            else if (top && right) {rotation = 180f;}
            else if (right && bottom) {rotation = -90f;}
            drawCornerRounded(canvas, paint, path, oval, cx, cy, size, rotation);
            return;
        }
        if (top) {rotation = 90f;}
        else if (right) {rotation = 180f;}
        else if (bottom) {rotation = -90f;}
        drawSideRounded(canvas, paint, path, oval, cx, cy, size, rotation);
    }

    private static void drawSideRounded(Canvas canvas, Paint paint, Path path, RectF oval,float cx, float cy, float size, float rotationDeg) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(rotationDeg);
        path.reset();
        oval.set(-size * 0.5f, -size * 0.5f, size * 0.5f, size * 0.5f);
        path.moveTo(0f, -size * 0.5f);
        path.arcTo(oval, -90f, 180f, false);
        path.lineTo(-size * 0.5f, size * 0.5f);
        path.lineTo(-size * 0.5f, -size * 0.5f);
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private static void drawCornerRounded(Canvas canvas, Paint paint, Path path, RectF oval,float cx, float cy, float size, float rotationDeg) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(rotationDeg);
        path.reset();
        oval.set(-size * 0.5f, -size * 0.5f, size * 0.5f, size * 0.5f);
        path.moveTo(0f, -size * 0.5f);
        path.arcTo(oval, -90f, 90f, false);
        path.lineTo(size * 0.5f, size * 0.5f);
        path.lineTo(-size * 0.5f, size * 0.5f);
        path.lineTo(-size * 0.5f, -size * 0.5f);
        path.lineTo(0f, -size * 0.5f);
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private static void drawExtraRoundedFinder(Canvas canvas, Paint paint, Path path, RectF oval,float left, float top, float module) {
        float size = module * 7f;
        float n = module;
        float cx = left + size * 0.5f;
        float cy = top + size * 0.5f;
        canvas.save();
        canvas.translate(cx, cy);
        path.reset();
        path.setFillType(Path.FillType.EVEN_ODD);
        appendRingArc(path, oval, -n, -n, 2.5f * n, 180f, 90f, true);
        path.lineTo(n, -3.5f * n);
        appendRingArc(path, oval, n, -n, 2.5f * n, -90f, 90f, false);
        path.lineTo(3.5f * n, n);
        appendRingArc(path, oval, n, n, 2.5f * n, 0f, 90f, false);
        path.lineTo(-n, 3.5f * n);
        appendRingArc(path, oval, -n, n, 2.5f * n, 90f, 90f, false);
        path.lineTo(-3.5f * n, -n);
        path.close();
        appendRingArc(path, oval, -n, -n, 1.5f * n, 180f, 90f, true);
        path.lineTo(n, -2.5f * n);
        appendRingArc(path, oval, n, -n, 1.5f * n, -90f, 90f, false);
        path.lineTo(2.5f * n, n);
        appendRingArc(path, oval, n, n, 1.5f * n, 0f, 90f, false);
        path.lineTo(-n, 2.5f * n);
        appendRingArc(path, oval, -n, n, 1.5f * n, 90f, 90f, false);
        path.lineTo(-2.5f * n, -n);
        path.close();
        paint.setColor(TELEGRAM_BLUE);
        canvas.drawPath(path, paint);
        path.reset();
        path.setFillType(Path.FillType.WINDING);
        canvas.drawCircle(0f, 0f, 1.5f * n, paint);
        canvas.restore();
    }

    private static void appendRingArc(Path path, RectF oval, float cx, float cy, float radius, float startDeg, float sweepDeg, boolean forceMove) {
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        path.arcTo(oval, startDeg, sweepDeg, forceMove);
    }

    private static boolean isFinderModule(int x, int y, int n) {return inFinder(x, y, 0, 0) || inFinder(x, y, n - 7, 0) || inFinder(x, y, 0, n - 7);}
    private static boolean inFinder(int x, int y, int ox, int oy) {return x >= ox && x < ox + 7 && y >= oy && y < oy + 7;}

    static boolean[][] encode(String text) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 1, 1, hints);
            int width = matrix.getWidth();
            boolean[][] modules = new boolean[width][width];
            for (int y = 0; y < width; y++) {for (int x = 0; x < width; x++) {modules[y][x] = matrix.get(x, y);}}
            return modules;
        } catch (Exception error) {throw new IllegalArgumentException("QR encode failed: " + error.getMessage(), error);}
    }
}
