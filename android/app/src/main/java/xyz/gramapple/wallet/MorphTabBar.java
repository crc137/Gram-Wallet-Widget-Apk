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
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

public class MorphTabBar extends View {
    public interface Listener {void onTabSelected(int index);}
    private static final int TAB_COUNT = 4;
    private static final int COLOR_TRACK = 0xFFFFFFFF;
    private static final int COLOR_PILL = 0xFF0088FF;
    private static final int COLOR_ICON_OFF = 0xFF0088FF;
    private static final int COLOR_ICON_ON = 0xFFFFFFFF;
    private static final float STRETCH = 0.45f;
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF pillRect = new RectF();
    private final Path clipPath = new Path();
    private final Drawable[] icons = new Drawable[TAB_COUNT];
    private final float[] optOffX = new float[TAB_COUNT];
    private final float[] optOffY = new float[TAB_COUNT];
    private final int[] iconIds = {R.drawable.ic_tab_wallet, R.drawable.ic_tab_history, R.drawable.ic_tab_tokens, R.drawable.ic_tab_nfts};
    private float indexPos;
    private float leadPos;
    private float trailPos;
    private int selectedIndex;
    private ValueAnimator animator;
    private Listener listener;
    private float iconSize;
    private float tabW;
    private float pad;
    private float density;
    private float innerH;
    public MorphTabBar(Context context) {super(context); init();}
    public MorphTabBar(Context context, AttributeSet attrs) {super(context, attrs); init();}
    public MorphTabBar(Context context, AttributeSet attrs, int defStyleAttr) {super(context, attrs, defStyleAttr); init();}

    private void init() {
        setWillNotDraw(false);
        setClickable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        iconSize = 22f * density;
        pad = 4f * density;
        innerH = 44f * density;
        tabW = 64f * density;
        trackPaint.setColor(COLOR_TRACK);
        pillPaint.setColor(COLOR_PILL);
        shadowPaint.setColor(0x22000000);
        for (int i = 0; i < TAB_COUNT; i++) {
            Drawable raw = getContext().getDrawable(iconIds[i]);
            if (raw == null) {continue;}
            icons[i] = raw.mutate();
            measureOpticalOffset(icons[i], i);
        }
    }

    private void measureOpticalOffset(Drawable icon, int index) {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        icon.setTint(0xFFFFFFFF);
        icon.setBounds(0, 0, size, size);
        icon.draw(c);
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
        optOffX[index] = (size * 0.5f - (minX + maxX) * 0.5f) / size;
        optOffY[index] = (size * 0.5f - (minY + maxY) * 0.5f) / size;
    }

    public void setListener(Listener listener) {this.listener = listener;}
    public int getSelectedIndex() {return selectedIndex;}

    public void setSelectedIndex(int index, boolean animate) {
        index = clampIndex(index);
        selectedIndex = index;
        if (!animate || getWidth() == 0) {
            indexPos = leadPos = trailPos = index;
            invalidate();
            return;
        }
        animateTo(index);
    }

    public void setScrollProgress(float progress) {
        if (animator != null && animator.isRunning()) {return;}
        progress = Math.max(0f, Math.min(TAB_COUNT - 1f, progress));
        indexPos = progress;
        float from = (float) Math.floor(progress);
        float t = progress - from;
        if (t < 0.001f) {leadPos = trailPos = progress;}
        else {
            float ease = t * t * (3f - 2f * t);
            leadPos = from + Math.min(1f, ease / Math.max(0.001f, 1f - STRETCH * 0.5f));
            trailPos = from + Math.max(0f, (ease - STRETCH) / Math.max(0.001f, 1f - STRETCH));
            leadPos = Math.min(from + 1f, leadPos);
            trailPos = Math.max(from, trailPos);
        }
        selectedIndex = Math.round(progress);
        invalidate();
    }

    private void animateTo(int index) {
        if (animator != null) {animator.cancel();}
        final float start = indexPos;
        final float end = index;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420L);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            indexPos = start + (end - start) * t;
            float dir = end >= start ? 1f : -1f;
            float leadT = Math.min(1f, t / Math.max(0.001f, 1f - STRETCH * 0.35f));
            float trailT = Math.max(0f, (t - STRETCH) / Math.max(0.001f, 1f - STRETCH));
            float lead = start + (end - start) * leadT;
            float trail = start + (end - start) * trailT;
            if (dir > 0) {leadPos = lead; trailPos = trail;}
            else {leadPos = trail; trailPos = lead;}
            invalidate();
        });
        animator.start();
    }

    private static int clampIndex(int index) {return Math.max(0, Math.min(TAB_COUNT - 1, index));}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = Math.round(pad * 2f + innerH + 4f * density);
        int want = Math.round(pad * 2f + tabW * TAB_COUNT);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {w = want;}
        setMeasuredDimension(w, h);
        tabW = (w - pad * 2f) / TAB_COUNT;
    }

    private float slotLeft(float index) {return pad + tabW * index;}

    private void drawIcons(Canvas canvas, int color) {
        float cy = trackRect.centerY();
        for (int i = 0; i < TAB_COUNT; i++) {
            Drawable icon = icons[i];
            if (icon == null) {continue;}
            float icx = slotLeft(i) + tabW * 0.5f + optOffX[i] * iconSize;
            float icy = cy + optOffY[i] * iconSize;
            int left = Math.round(icx - iconSize * 0.5f);
            int top = Math.round(icy - iconSize * 0.5f);
            icon.setBounds(left, top, left + Math.round(iconSize), top + Math.round(iconSize));
            icon.setTint(color);
            icon.draw(canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {return;}
        float top = (h - innerH) * 0.5f - pad;
        trackRect.set(0, top, w, top + innerH + pad * 2f);
        float radius = trackRect.height() * 0.5f;
        canvas.drawRoundRect(trackRect.left, trackRect.top + 2f * density, trackRect.right, trackRect.bottom + 2f * density, radius, radius, shadowPaint);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
        drawIcons(canvas, COLOR_ICON_OFF);
        float leftIndex = Math.min(leadPos, trailPos);
        float rightIndex = Math.max(leadPos, trailPos);
        float left = slotLeft(leftIndex);
        float right = slotLeft(rightIndex) + tabW;
        float pillPad = 2f * density;
        pillRect.set(left + pillPad, trackRect.top + pad, right - pillPad, trackRect.bottom - pad);
        float pr = pillRect.height() * 0.5f;
        canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(pillRect, pr, pr, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawRoundRect(pillRect, pr, pr, pillPaint);
        drawIcons(canvas, COLOR_ICON_ON);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && isEnabled()) {
            int index = (int) ((event.getX() - pad) / tabW);
            index = clampIndex(index);
            if (index != selectedIndex) {
                setSelectedIndex(index, true);
                if (listener != null) {listener.onTabSelected(index);}
            } else if (listener != null) {listener.onTabSelected(index);}
            return true;
        }
        return true;
    }
}
