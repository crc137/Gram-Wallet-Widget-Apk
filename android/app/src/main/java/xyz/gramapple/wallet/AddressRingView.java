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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class AddressRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final RectF corner = new RectF();
    private final float[] pos = new float[2];
    private final float[] tan = new float[2];
    private String ringText = "";
    private float unitWidth;
    private float pathLen;
    private float offset;
    private ValueAnimator anim;

    public AddressRingView(Context context) {this(context, null);}
    public AddressRingView(Context context, AttributeSet attrs) {this(context, attrs, 0);}
    public AddressRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
        paint.setColor(0xFF0072CE);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);
    }

    void setAddress(String address) {
        String raw = address == null ? "" : address.replaceAll("\\s+", "");
        if (raw.isEmpty()) {
            ringText = "";
            unitWidth = 0f;
            stopAnim();
            invalidate();
            return;
        }
        StringBuilder spaced = new StringBuilder();
        for (int i = 0; i < raw.length(); i += 4) {
            if (spaced.length() > 0) {spaced.append(' ');}
            spaced.append(raw, i, Math.min(i + 4, raw.length()));
        }
        String unit = spaced + "   ·   ";
        paint.setTextSize(sp(12f));
        unitWidth = Math.max(1f, paint.measureText(unit));
        StringBuilder built = new StringBuilder();
        while (built.length() < 260) {built.append(unit);}
        ringText = built.toString();
        rebuildPath(getWidth(), getHeight());
        invalidate();
        startAnim();
    }

    private void rebuildPath(int w, int h) {
        path.reset();
        pathLen = 0f;
        if (w <= 0 || h <= 0) {return;}
        float d = getResources().getDisplayMetrics().density;
        float inset = 10f * d;
        float radius = 38f * d;
        float left = inset;
        float top = inset;
        float right = w - inset;
        float bottom = h - inset;
        float r = Math.min(radius, Math.min(right - left, bottom - top) * 0.5f);
        path.moveTo(left + r, top);
        path.lineTo(right - r, top);
        corner.set(right - 2f * r, top, right, top + 2f * r);
        path.arcTo(corner, -90f, 90f, false);
        path.lineTo(right, bottom - r);
        corner.set(right - 2f * r, bottom - 2f * r, right, bottom);
        path.arcTo(corner, 0f, 90f, false);
        path.lineTo(left + r, bottom);
        corner.set(left, bottom - 2f * r, left + 2f * r, bottom);
        path.arcTo(corner, 90f, 90f, false);
        path.lineTo(left, top + r);
        corner.set(left, top, left + 2f * r, top + 2f * r);
        path.arcTo(corner, 180f, 90f, false);
        path.close();
        measure.setPath(path, false);
        pathLen = measure.getLength();
    }

    private void startAnim() {
        stopAnim();
        if (unitWidth <= 0f) {return;}
        long dur = Math.max(30_000L, Math.round(unitWidth / 24f * 1000f));
        anim = ValueAnimator.ofFloat(0f, unitWidth);
        anim.setDuration(dur);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private void stopAnim() {
        if (anim != null) {anim.cancel(); anim = null;}
        offset = 0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildPath(w, h);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!ringText.isEmpty() && unitWidth > 0f && (anim == null || !anim.isRunning())) {startAnim();}
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnim();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ringText.isEmpty() || pathLen <= 1f) {return;}
        paint.setTextSize(sp(12f));
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = -(fm.ascent + fm.descent) * 0.5f;
        float cursor = ((-offset % pathLen) + pathLen) % pathLen;
        float covered = 0f;
        int i = 0;
        int n = ringText.length();
        while (covered < pathLen) {
            int idx = i % n;
            float cw = paint.measureText(ringText, idx, idx + 1);
            float mid = cursor + cw * 0.5f;
            if (mid >= pathLen) {mid -= pathLen;}
            measure.getPosTan(mid, pos, tan);
            float angle = (float) (Math.atan2(tan[1], tan[0]) * 180.0 / Math.PI);
            canvas.save();
            canvas.translate(pos[0], pos[1]);
            canvas.rotate(angle);
            canvas.drawText(ringText, idx, idx + 1, 0f, baseline, paint);
            canvas.restore();
            cursor += cw;
            if (cursor >= pathLen) {cursor -= pathLen;}
            covered += cw;
            i++;
        }
    }

    private float sp(float value) {return value * getResources().getDisplayMetrics().scaledDensity;}
}
