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
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

public class CardStackView extends FrameLayout {
    public interface Listener {void onWalletSelected(String address);}
    public interface Binder {void bind(WalletCardView view, String address);}
    private static final int[] PEEK_COLORS = {0xFF3A3A3D, 0xFF34588F, 0xFF7A9A84, 0xFFCDBB9F};
    private static final float PEEK_STEP_DP = 18f;
    private static final float BACK_SCALE = 1f;
    private static final long ANIM_MS = 150L;
    private static final int MAX_PEEKS = 4;
    private final Paint peekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF peekRect = new RectF();
    private final List<Store.WalletEntry> wallets = new ArrayList<>();
    private final PathInterpolator ease = new PathInterpolator(0.2f, 0.9f, 0.2f, 1f);
    private GestureDetector detector;
    private Listener listener;
    private Binder binder;
    private ValueAnimator peekAnim;
    private float density;
    private WalletCardView front;
    private WalletCardView back;
    private boolean animating;
    private int peekPad;
    private int stepPx;

    public CardStackView(Context context) {super(context); init();}
    public CardStackView(Context context, AttributeSet attrs) {super(context, attrs); init();}
    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr) {super(context, attrs, defStyleAttr); init();}

    private void init() {
        setWillNotDraw(false);
        setClipToPadding(false);
        setClipChildren(false);
        density = getResources().getDisplayMetrics().density;
        stepPx = Math.round(PEEK_STEP_DP * density);
        labelPaint.setColor(0xF2FFFFFF);
        labelPaint.setTextSize(12f * density);
        labelPaint.setFakeBoldText(true);
        detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {return true;}
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (animating || wallets.size() < 2) {return false;}
                if (Math.abs(velocityY) > 280 || Math.abs(velocityX) > 280) {selectIndex(1); return true;}
                return false;
            }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                if (animating || wallets.size() < 2) {return false;}
                int idx = hitPeekIndex(e.getY());
                if (idx >= 1) {selectIndex(idx); return true;}
                return false;
            }
        });
    }

    public void setListener(Listener listener) {this.listener = listener;}
    public void setBinder(Binder binder) {this.binder = binder;}
    public WalletCardView getFrontCard() {ensureCards(); return front;}

    private void ensureCards() {
        if (front != null && back != null) {return;}
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (!(c instanceof WalletCardView)) {continue;}
            if (c.getId() == R.id.card) {front = (WalletCardView) c;}
            else if (c.getId() == R.id.card_back) {back = (WalletCardView) c;}
        }
        if (front == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                if (c instanceof WalletCardView) {front = (WalletCardView) c; break;}
            }
        }
    }

    public void setWallets(List<Store.WalletEntry> list, String active) {
        if (animating) {return;}
        ensureCards();
        String prevFront = wallets.isEmpty() ? null : wallets.get(0).address;
        String prevBack = wallets.size() >= 2 ? wallets.get(1).address : null;
        wallets.clear();
        if (list != null) {
            Store.WalletEntry activeEntry = null;
            for (Store.WalletEntry e : list) {
                if (active != null && active.equals(e.address)) {activeEntry = e;}
                else {wallets.add(e);}
            }
            if (activeEntry != null) {wallets.add(0, activeEntry);}
            else {wallets.addAll(list);}
        }
        peekPad = visiblePeekCount() * stepPx;
        String nextFront = wallets.isEmpty() ? null : wallets.get(0).address;
        String nextBack = wallets.size() >= 2 ? wallets.get(1).address : null;
        boolean sameOrder = eq(prevFront, nextFront) && eq(prevBack, nextBack);
        if (sameOrder) {refreshCardData();}
        else {
            bindCards();
            resetTransforms();
            requestLayout();
            invalidate();
        }
    }

    public void refreshCardData() {
        if (animating || binder == null) {return;}
        bindCards();
        invalidate();
    }

    public void updateAddress(String address) {
        if (animating || binder == null || address == null) {return;}
        if (!wallets.isEmpty() && address.equals(wallets.get(0).address) && front != null) {binder.bind(front, address);}
        if (wallets.size() >= 2 && address.equals(wallets.get(1).address) && back != null) {binder.bind(back, address);}
        invalidate();
    }

    private static boolean eq(String a, String b) {return a == null ? b == null : a.equals(b);}

    private void bindCards() {
        if (binder == null) {return;}
        if (front != null && !wallets.isEmpty()) {binder.bind(front, wallets.get(0).address);}
        if (back != null) {
            if (wallets.size() >= 2) {
                back.setVisibility(VISIBLE);
                binder.bind(back, wallets.get(1).address);
            } else {back.setVisibility(GONE);}
        }
    }

    public void cycleNext() {selectIndex(1);}
    private int visiblePeekCount() {return Math.max(0, Math.min(MAX_PEEKS, wallets.size() - 1));}
    private int fakePeekCount() {return Math.max(0, visiblePeekCount() - 1);}

    private int hitPeekIndex(float y) {
        int peeks = visiblePeekCount();
        if (peeks <= 0 || y >= peekPad) {return -1;}
        int band = (int) (y / stepPx);
        if (band < 0) {band = 0;}
        if (band >= peeks) {band = peeks - 1;}
        return peeks - band;
    }

    private void resetTransforms() {
        if (front != null) {
            front.animate().setListener(null);
            front.animate().cancel();
            front.setTranslationY(0f);
            front.setScaleX(1f);
            front.setScaleY(1f);
            front.setAlpha(1f);
            front.setVisibility(VISIBLE);
        }
        if (back != null && wallets.size() >= 2) {
            back.animate().cancel();
            back.setVisibility(VISIBLE);
            back.setTranslationY(0f);
            back.setScaleX(BACK_SCALE);
            back.setScaleY(BACK_SCALE);
            back.setAlpha(1f);
        }
    }

    private void selectIndex(int index) {
        if (animating || listener == null || back == null) {return;}
        if (index < 1 || index >= wallets.size()) {return;}
        ensureCards();
        final int pick = index;
        final String next = wallets.get(pick).address;
        if (pick != 1 && binder != null) {binder.bind(back, next);}
        animating = true;
        if (peekAnim != null) {peekAnim.cancel();}
        float down = 28f * density;
        float rise = pick * stepPx;
        peekAnim = ValueAnimator.ofFloat(0f, 1f);
        peekAnim.setDuration(ANIM_MS);
        peekAnim.setInterpolator(ease);
        peekAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            back.setTranslationY(t * rise);
            front.setTranslationY(t * down);
            front.setAlpha(1f - t);
        });
        peekAnim.addListener(new AnimatorListenerAdapter() {
            private boolean done;
            private void finish() {
                if (done) {return;}
                done = true;
                promote(pick);
                animating = false;
                if (listener != null) {listener.onWalletSelected(next);}
            }
            @Override public void onAnimationEnd(Animator animation) {finish();}
            @Override public void onAnimationCancel(Animator animation) {finish();}
        });
        peekAnim.start();
    }

    private void promote(int index) {
        Store.WalletEntry picked = wallets.remove(index);
        wallets.add(0, picked);
        WalletCardView oldFront = front;
        front = back;
        back = oldFront;
        if (front != null) {bringChildToFront(front);}
        peekPad = visiblePeekCount() * stepPx;
        resetTransforms();
        if (back != null) {
            if (wallets.size() >= 2) {
                back.setVisibility(VISIBLE);
                final String backAddr = wallets.get(1).address;
                if (binder != null) {binder.bind(back, backAddr);}
            } else {back.setVisibility(GONE);}
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ensureCards();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ensureCards();
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (front == null) {
            setMeasuredDimension(w, Math.max(peekPad, 1));
            return;
        }
        int childW = MeasureSpec.makeMeasureSpec(Math.max(w, 1), MeasureSpec.EXACTLY);
        front.measure(childW, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        if (back != null && back.getVisibility() != GONE) {back.measure(childW, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));}
        int ch = Math.max(front.getMeasuredHeight(), Math.round(w * 0.63f));
        setMeasuredDimension(w, peekPad + ch);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ensureCards();
        int w = right - left;
        int ch = front != null ? front.getMeasuredHeight() : 0;
        if (ch <= 0) {ch = Math.round(w * 0.63f);}
        if (back != null && back.getVisibility() != GONE) {
            int backTop = Math.max(0, peekPad - stepPx);
            back.layout(0, backTop, w, backTop + ch);
            back.setPivotX(w * 0.5f);
            back.setPivotY(0f);
        }
        if (front != null) {
            front.layout(0, peekPad, w, peekPad + ch);
            front.setPivotX(w * 0.5f);
            front.setPivotY(0f);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int peeks = fakePeekCount();
        float w = getWidth();
        float cardTop = peekPad;
        float cardH = front != null ? Math.max(1f, front.getHeight()) : Math.max(1f, getHeight() - cardTop);
        float radius = 18f * density;
        if (peeks > 0) {
            for (int i = peeks; i >= 1; i--) {
                float ty = cardTop - (i + 1) * stepPx;
                peekRect.set(0f, ty, w, ty + cardH);
                int color = PEEK_COLORS[i % PEEK_COLORS.length];
                peekPaint.setShader(new LinearGradient(peekRect.left, peekRect.top, peekRect.right, peekRect.bottom, lighten(color), color, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(peekRect, radius, radius, peekPaint);
                peekPaint.setShader(null);
                int wi = Math.min(i + 1, wallets.size() - 1);
                if (wi >= 0 && wi < wallets.size()) {
                    String label = wallets.get(wi).label;
                    if (label != null && !label.isEmpty()) {canvas.drawText(label, 14f * density, peekRect.top + 13f * density, labelPaint);}
                }
            }
        }
        super.dispatchDraw(canvas);
    }

    private static int lighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 36);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 36);
        int b = Math.min(255, (color & 0xFF) + 36);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        detector.onTouchEvent(ev);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {return detector.onTouchEvent(event) || super.onTouchEvent(event);}
}
