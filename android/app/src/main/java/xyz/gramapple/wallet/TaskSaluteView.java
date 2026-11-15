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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import java.util.Random;

final class TaskSaluteView extends View {
    interface Done {void onFinished();}
    private static final int[] BLUES = {0xFF0088FF, 0xFF5CC8FF, 0xFF33A7FF, 0xFF0057C2, 0xFFFFFFFF};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path thumb = new Path();
    private final Random rnd = new Random();
    private final float[] px = new float[48];
    private final float[] py = new float[48];
    private final float[] vx = new float[48];
    private final float[] vy = new float[48];
    private final float[] pr = new float[48];
    private final int[] pc = new int[48];
    private float progress;
    private float fade = 1f;
    private float iconOffX;
    private float iconOffY;
    private Drawable icon;
    private Done done;
    private ValueAnimator anim;

    TaskSaluteView(Context context) {
        super(context);
        setClickable(true);
        circlePaint.setColor(0xFFFFFFFF);
        icon = context.getDrawable(R.drawable.ic_thumbs_up);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(0xFF0088FF);
            measureOpticalOffset(icon);
        }
    }

    private void measureOpticalOffset(Drawable d) {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        int minX = size, minY = size, maxX = -1, maxY = -1;
        int[] row = new int[size];
        for (int y = 0; y < size; y++) {
            bmp.getPixels(row, 0, size, 0, y, size, 1);
            for (int x = 0; x < size; x++) {
                if (((row[x] >>> 24) & 0xFF) < 24) {continue;}
                if (x < minX) {minX = x;}
                if (y < minY) {minY = y;}
                if (x > maxX) {maxX = x;}
                if (y > maxY) {maxY = y;}
            }
        }
        bmp.recycle();
        if (maxX < minX) {return;}
        iconOffX = (size * .5f - (minX + maxX) * .5f) / size;
        iconOffY = (size * .5f - (minY + maxY) * .5f) / size;
    }

    void play(Done done) {
        this.done = done;
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < px.length; i++) {
            float a = (float) (rnd.nextFloat() * Math.PI * 2.0);
            float sp = (6f + rnd.nextFloat() * 14f) * d;
            px[i] = 0; py[i] = 0;
            vx[i] = (float) Math.cos(a) * sp;
            vy[i] = (float) Math.sin(a) * sp - 2f * d;
            pr[i] = (2.5f + rnd.nextFloat() * 4f) * d;
            pc[i] = BLUES[rnd.nextInt(BLUES.length)];
        }
        progress = 0f; fade = 1f;
        if (anim != null) {anim.cancel();}
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1600L);
        anim.setInterpolator(new DecelerateInterpolator(1.2f));
        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            if (progress > .72f) {fade = Math.max(0f, 1f - (progress - .72f) / .28f);}
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {@Override public void onAnimationEnd(Animator animation) {if (TaskSaluteView.this.done != null) {TaskSaluteView.this.done.onFinished();}}});
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {return;}
        float cx = w * .5f, cy = h * .42f;
        int veil = Math.round(0xB0 * fade);
        canvas.drawColor((veil << 24) | 0x000088FF);
        float t = progress;
        for (int i = 0; i < px.length; i++) {
            float x = cx + vx[i] * t * 18f;
            float y = cy + vy[i] * t * 18f + 40f * t * t;
            float a = fade * (1f - t * .85f);
            if (a <= .02f) {continue;}
            paint.setColor(pc[i]);
            paint.setAlpha(Math.round(255f * a));
            canvas.drawCircle(x, y, pr[i] * (1f - t * .35f), paint);
        }
        float scale = new AccelerateDecelerateInterpolator().getInterpolation(Math.min(1f, t * 1.6f));
        float r = Math.min(w, h) * .11f * (.7f + .3f * scale);
        circlePaint.setAlpha(Math.round(255f * fade));
        canvas.drawCircle(cx, cy, r, circlePaint);
        if (icon != null) {
            int s = Math.round(r * 1.1f);
            float icx = cx + iconOffX * s;
            float icy = cy + iconOffY * s;
            icon.setAlpha(Math.round(255f * fade));
            icon.setBounds(Math.round(icx - s * .5f), Math.round(icy - s * .5f), Math.round(icx + s * .5f), Math.round(icy + s * .5f));
            icon.draw(canvas);
        } else {
            paint.setColor(0xFF0088FF);
            paint.setAlpha(Math.round(255f * fade));
            paint.setStyle(Paint.Style.FILL);
            thumb.reset();
            float u = r * .35f;
            thumb.moveTo(cx - u, cy + u * .2f);
            thumb.lineTo(cx - u * .2f, cy + u);
            thumb.lineTo(cx + u, cy - u * .4f);
            thumb.lineTo(cx + u * .55f, cy - u * .7f);
            thumb.lineTo(cx - u * .1f, cy + u * .15f);
            thumb.close();
            canvas.drawPath(thumb, paint);
        }
    }
}
