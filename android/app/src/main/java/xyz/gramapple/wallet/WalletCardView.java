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
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WalletCardView extends View {
    public interface OnQrClickListener {void onQrClick();}
    public interface OnMassClickListener {void onMassClick();}
    private Bitmap bitmap;
    private String balance = "—";
    private String usd = "—";
    private String address = "";
    private CardDecor decor = CardDecor.EMPTY;
    private OnQrClickListener qrListener;
    private OnMassClickListener massListener;
    private final RectF qrRect = new RectF();
    private final RectF massRect = new RectF();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint loadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint loadBlurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF glowRect = new RectF();
    private final RectF loadRect = new RectF();
    private boolean loading;
    private float loadPulse;
    private ValueAnimator loadAnim;
    private int loadColor = 0xFF0088FF;

    public WalletCardView(Context context) {
        super(context);
        init();
    }

    public WalletCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WalletCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);
        glowPaint.setColor(0x990060E6);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(9f), BlurMaskFilter.Blur.NORMAL));
        loadBlurPaint.setMaskFilter(new BlurMaskFilter(dp(28f), BlurMaskFilter.Blur.NORMAL));
    }

    public void setValues(String balance, String usd) {
        this.balance = balance;
        this.usd = usd;
        if (!loading) {rebuild();}
    }

    public void setAddress(String address) {
        this.address = address == null ? "" : address;
        if (!loading) {rebuild();}
    }

    public void setDecor(CardDecor decor) {
        this.decor = decor == null ? CardDecor.EMPTY : decor;
        if (this.decor.background != null) {
            glowPaint.setColor((this.decor.background.edgeColor & 0x00FFFFFF) | 0x99000000);
            loadColor = this.decor.background.centerColor | 0xFF000000;
        } else {
            glowPaint.setColor(0x990060E6);
            loadColor = 0xFF0088FF;
        }
        if (!loading) {rebuild();}
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            if (loading) {invalidate();}
            return;
        }
        this.loading = loading;
        if (loading) {
            if (decor.background != null) {loadColor = decor.background.centerColor | 0xFF000000;}
            startLoadAnim();
            invalidate();
        } else {
            stopLoadAnim();
            rebuild();
        }
    }

    public void setOnQrClickListener(OnQrClickListener listener) {qrListener = listener;}
    public void setOnMassClickListener(OnMassClickListener listener) {massListener = listener;}

    private void startLoadAnim() {
        if (loadAnim != null && loadAnim.isRunning()) {return;}
        loadAnim = ValueAnimator.ofFloat(0f, 1f);
        loadAnim.setDuration(1600L);
        loadAnim.setRepeatCount(ValueAnimator.INFINITE);
        loadAnim.setRepeatMode(ValueAnimator.RESTART);
        loadAnim.setInterpolator(new LinearInterpolator());
        loadAnim.addUpdateListener(a -> {
            loadPulse = (float) a.getAnimatedValue();
            invalidate();
        });
        loadAnim.start();
    }

    private void stopLoadAnim() {
        if (loadAnim != null) {
            loadAnim.cancel();
            loadAnim = null;
        }
        loadPulse = 0f;
    }

    private void rebuild() {
        int width = getWidth();
        if (width <= 0) {
            bitmap = null;
            return;
        }
        Bitmap old = bitmap;
        bitmap = CardArt.render(getContext(), width, balance, usd, address, decor);
        if (old != null) {old.recycle();}
        updateQrRect(width, Math.round(width * CardArt.CARD_HEIGHT_RATIO));
        invalidate();
    }

    private void updateQrRect(int width, int height) {
        float q = width / 100f;
        float right = width - 13.6905f * q;
        qrRect.set(right - 14.2857f * q, 22.0238f * q, right, 32.7381f * q);
        CardArt.massHitRect(width, massRect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && !loading) {rebuild();}
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int height = Math.round(width * CardArt.CARD_HEIGHT_RATIO);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = getWidth() * 0.02875f;
        glowRect.set(0f, 0f, getWidth(), getHeight());
        glowRect.inset(getWidth() * 0.015f, getHeight() * 0.02f);
        glowRect.offset(0f, getHeight() * 0.028f);
        canvas.drawRoundRect(glowRect, radius * 0.95f, radius * 0.95f, glowPaint);
        if (loading) {
            drawLoading(canvas, radius);
            return;
        }
        if (bitmap == null) {
            rebuild();
            if (bitmap == null) {return;}
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null);
    }

    private void drawLoading(Canvas canvas, float radius) {
        loadRect.set(0f, 0f, getWidth(), getHeight());
        loadPaint.setColor(loadColor);
        canvas.drawRoundRect(loadRect, radius, radius, loadPaint);
        float w = getWidth();
        float h = getHeight();
        float t = loadPulse;
        float wave = (float) Math.sin(t * Math.PI * 2.0);
        int light = mixWhite(loadColor, 0.42f + 0.18f * wave);
        loadBlurPaint.setColor((light & 0x00FFFFFF) | 0x66000000);
        float cx = w * (0.28f + 0.44f * t);
        float cy = h * (0.35f + 0.12f * wave);
        canvas.drawCircle(cx, cy, Math.min(w, h) * 0.38f, loadBlurPaint);
        loadBlurPaint.setColor((mixWhite(loadColor, 0.55f) & 0x00FFFFFF) | 0x44000000);
        canvas.drawCircle(w * (0.72f - 0.3f * t), h * 0.68f, Math.min(w, h) * 0.28f, loadBlurPaint);
        loadPaint.setColor(0x66FFFFFF);
        float barW = w * 0.34f;
        float barH = Math.max(dp(4f), h * 0.028f);
        float barX = (w - barW) * 0.5f;
        float barY = h * 0.78f;
        loadPaint.setColor(0x33FFFFFF);
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, barH, barH, loadPaint);
        float fill = barW * (0.22f + 0.56f * t);
        loadPaint.setColor(0xCCFFFFFF);
        canvas.drawRoundRect(barX, barY, barX + fill, barY + barH, barH, barH, loadPaint);
    }

    private static int mixWhite(int color, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.round(r + (255 - r) * t);
        g = Math.round(g + (255 - g) * t);
        b = Math.round(b + (255 - b) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoadAnim();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (loading) {return super.onTouchEvent(event);}
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN && (qrRect.contains(x, y) || massRect.contains(x, y))) {return true;}
        if (action == MotionEvent.ACTION_UP) {
            if (qrRect.contains(x, y)) {performClick(); return true;}
            if (massRect.contains(x, y)) {
                if (massListener != null) {massListener.onMassClick();}
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (!loading && qrListener != null) {qrListener.onQrClick();}
        return true;
    }

    private float dp(float value) {return value * getResources().getDisplayMetrics().density;}
}
