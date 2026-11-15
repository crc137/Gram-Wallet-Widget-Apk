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
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

public class WalletCardView extends View {
    public interface OnQrClickListener {void onQrClick();}
    public interface OnCardTapListener {void onCardTap();}
    public interface OnMassClickListener {void onMassClick();}
    private Bitmap bitmap;
    private String balance = "—";
    private String usd = "—";
    private String address = "";
    private CardDecor decor = CardDecor.EMPTY;
    private OnQrClickListener qrListener;
    private OnMassClickListener massListener;
    private OnCardTapListener cardTapListener;
    private final RectF qrRect = new RectF();
    private final RectF massRect = new RectF();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint loadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint loadBlurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF glowRect = new RectF();
    private final RectF loadRect = new RectF();
    private boolean loading;
    private float loadPulse;
    private float contentAlpha = 1f;
    private float loadFade = 0f;
    private ValueAnimator loadAnim;
    private ValueAnimator fadeAnim;
    private final PathInterpolator fadeEase = new PathInterpolator(.22f,1f,.36f,1f);
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
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                if (w <= 0 || h <= 0) {outline.setEmpty(); return;}
                outline.setRoundRect(0, 0, w, h, w * .02875f);
            }
        });
        glowPaint.setColor(0x990060E6);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(9f), BlurMaskFilter.Blur.NORMAL));
        loadBlurPaint.setMaskFilter(new BlurMaskFilter(dp(28f), BlurMaskFilter.Blur.NORMAL));
    }

    public void setValues(String balance, String usd) {apply(balance, usd, this.address, this.decor, false);}
    public void setAddress(String address) {apply(this.balance, this.usd, address, this.decor, false);}
    public void setDecor(CardDecor decor) {apply(this.balance, this.usd, this.address, decor, true);}
    public void apply(String balance, String usd, String address, CardDecor decor) {apply(balance, usd, address, decor, true);}

    public void bindWallet(String address, String balance, String usd, CardDecor decor) {
        this.balance = balance == null ? "—" : balance;
        this.usd = usd == null ? "—" : usd;
        this.address = address == null ? "" : address;
        this.decor = decor == null ? CardDecor.EMPTY : decor;
        if (this.decor.background != null) {
            glowPaint.setColor((this.decor.background.edgeColor & 0x00FFFFFF) | 0x99000000);
            loadColor = this.decor.background.centerColor | 0xFF000000;
        } else {
            glowPaint.setColor(0x990060E6);
            loadColor = 0xFF0088FF;
        }
        if (!loading) {rebuild();} else {invalidate();}
    }
    public void refreshArt() {if (!loading) {rebuild();}}

    private void apply(String balance, String usd, String address, CardDecor decor, boolean replaceDecor) {
        String nextBalance = balance == null ? this.balance : balance;
        String nextUsd = usd == null ? this.usd : usd;
        String nextAddress = address == null ? this.address : address;
        boolean addressChanged = !nextAddress.equals(this.address);
        CardDecor nextDecor = (replaceDecor || addressChanged) ? (decor == null ? CardDecor.EMPTY : decor) : this.decor;
        if (nextDecor == null) {nextDecor = CardDecor.EMPTY;}
        boolean same = nextBalance.equals(this.balance) && nextUsd.equals(this.usd) && nextAddress.equals(this.address) && sameDecor(this.decor, nextDecor);
        this.balance = nextBalance;
        this.usd = nextUsd;
        this.address = nextAddress;
        this.decor = nextDecor;
        if (this.decor.background != null) {
            glowPaint.setColor((this.decor.background.edgeColor & 0x00FFFFFF) | 0x99000000);
            loadColor = this.decor.background.centerColor | 0xFF000000;
        } else {
            glowPaint.setColor(0x990060E6);
            loadColor = 0xFF0088FF;
        }
        if (!same && !loading) {rebuild();}
    }

    private static boolean sameDecor(CardDecor a, CardDecor b) {
        if (a == b) {return true;}
        if (a == null || b == null) {return false;}
        if (a.isEmpty() && b.isEmpty()) {return true;}
        if (a.telegramWallet != b.telegramWallet || a.gifts.size() != b.gifts.size()) {return false;}
        if (a.background == null || b.background == null) {return a.background == b.background;}
        return a.background.centerColor == b.background.centerColor && a.background.edgeColor == b.background.edgeColor;
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            if (loading) {invalidate();}
            return;
        }
        this.loading = loading;
        if (loading) {
            if (decor.background != null) {loadColor = decor.background.centerColor | 0xFF000000;}
            cancelFade();
            contentAlpha = 0f;
            loadFade = 1f;
            startLoadAnim();
            invalidate();
        } else {
            stopLoadAnim();
            rebuild();
            fadeInContent(true);
        }
    }

    public void setOnQrClickListener(OnQrClickListener listener) {qrListener = listener;}
    public void setOnMassClickListener(OnMassClickListener listener) {massListener = listener;}
    public void setOnCardTapListener(OnCardTapListener listener) {cardTapListener = listener;}

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
        if (width <= 0) {return;}
        Bitmap next = CardArt.render(getContext(), width, balance, usd, address, decor);
        Bitmap old = bitmap;
        boolean soft = old != null && next != null && !loading;
        bitmap = next;
        updateQrRect(width, Math.round(width * CardArt.CARD_HEIGHT_RATIO));
        if (soft) {fadeInContent(false);} else {invalidate();}
        if (old != null && old != next) {post(() -> {if (!old.isRecycled()) {old.recycle();}});}
    }

    private void fadeInContent(boolean fromLoading) {
        cancelFade();
        float start = fromLoading ? 0f : Math.max(.35f, contentAlpha);
        contentAlpha = start;
        loadFade = fromLoading ? 1f : 0f;
        fadeAnim = ValueAnimator.ofFloat(0f, 1f);
        fadeAnim.setDuration(fromLoading ? 420L : 280L);
        fadeAnim.setInterpolator(fadeEase);
        fadeAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            contentAlpha = start + (1f - start) * t;
            if (fromLoading) {loadFade = 1f - t;}
            invalidate();
        });
        fadeAnim.start();
    }

    private void cancelFade() {if (fadeAnim != null) {fadeAnim.cancel(); fadeAnim = null;}}

    private void updateQrRect(int width, int height) {
        float q = width / 100f;
        float right = width - 13.6905f * q;
        qrRect.set(right - 14.2857f * q, 22.0238f * q, right, 32.7381f * q);
        CardArt.massHitRect(width, massRect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateOutline();
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
        float radius = getWidth() * .02875f;
        glowRect.set(0f, 0f, getWidth(), getHeight());
        glowRect.inset(getWidth() * .015f, getHeight() * .02f);
        glowRect.offset(0f, getHeight() * .028f);
        canvas.drawRoundRect(glowRect, radius * .95f, radius * .95f, glowPaint);
        if (loading || loadFade > .01f) {drawLoading(canvas, radius);}
        if (loading && loadFade >= .99f) {return;}
        if (bitmap == null) {
            rebuild();
            if (bitmap == null) {return;}
        }
        int alpha = Math.round(255f * Math.max(0f, Math.min(1f, contentAlpha)));
        loadPaint.setAlpha(alpha);
        canvas.drawBitmap(bitmap, 0f, 0f, loadPaint);
        loadPaint.setAlpha(255);
    }

    private void drawLoading(Canvas canvas, float radius) {
        float fade = loading ? 1f : Math.max(0f, Math.min(1f, loadFade));
        loadRect.set(0f, 0f, getWidth(), getHeight());
        loadPaint.setColor(loadColor);
        loadPaint.setAlpha(Math.round(255f * fade));
        canvas.drawRoundRect(loadRect, radius, radius, loadPaint);
        loadPaint.setAlpha(255);
        float w = getWidth();
        float h = getHeight();
        float t = loadPulse;
        float wave = (float) Math.sin(t * Math.PI * 2.0);
        int a66 = Math.round(0x66 * fade);
        int a44 = Math.round(0x44 * fade);
        int light = mixWhite(loadColor, .42f + .18f * wave);
        loadBlurPaint.setColor((light & 0x00FFFFFF) | (a66 << 24));
        float cx = w * (.28f + .44f * t);
        float cy = h * (.35f + .12f * wave);
        canvas.drawCircle(cx, cy, Math.min(w, h) * .38f, loadBlurPaint);
        loadBlurPaint.setColor((mixWhite(loadColor, .55f) & 0x00FFFFFF) | (a44 << 24));
        canvas.drawCircle(w * (.72f - .3f * t), h * .68f, Math.min(w, h) * .28f, loadBlurPaint);
        float barW = w * .34f;
        float barH = Math.max(dp(4f), h * .028f);
        float barX = (w - barW) * .5f;
        float barY = h * .78f;
        loadPaint.setColor((0x33FFFFFF & 0x00FFFFFF) | (Math.round(0x33 * fade) << 24));
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, barH, barH, loadPaint);
        float fill = barW * (.22f + .56f * t);
        loadPaint.setColor((0xCCFFFFFF & 0x00FFFFFF) | (Math.round(0xCC * fade) << 24));
        canvas.drawRoundRect(barX, barY, barX + fill, barY + barH, barH, barH, loadPaint);
        loadPaint.setAlpha(255);
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
        cancelFade();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (loading) {return super.onTouchEvent(event);}
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {return true;}
        if (action == MotionEvent.ACTION_UP) {
            if (qrRect.contains(x, y)) {performClick(); return true;}
            if (massRect.contains(x, y)) {
                if (massListener != null) {massListener.onMassClick();}
                return true;
            }
            if (cardTapListener != null) {cardTapListener.onCardTap(); return true;}
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
