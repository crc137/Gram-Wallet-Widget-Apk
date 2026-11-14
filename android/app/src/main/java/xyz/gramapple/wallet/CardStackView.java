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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

public class CardStackView extends FrameLayout {
    public interface Listener {void onWalletSelected(String address);}
    public interface Binder {void bind(WalletCardView view, String address);}
    private enum Mode {STACK, FAN}
    private static final int[] PEEK_COLORS = {0xFF3A3A3D, 0xFF34588F, 0xFF7A9A84, 0xFFCDBB9F};
    private static final float STACK_STEP_DP = 12f;
    private static final float FAN_STEP_DP = 64f;
    private static final float BACK_SCALE_STACK = .95f;
    private static final float TUCK_DP = 5f;
    private static final float PEEK_INSET_DP = 5f;
    private static final long MOVE_MS = 260L;
    private static final int MAX_PEEKS = 4;
    private final Paint peekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF peekRect = new RectF();
    private final List<Store.WalletEntry> wallets = new ArrayList<>();
    private final PathInterpolator ease = new PathInterpolator(.2f, .9f, .2f, 1f);
    private Listener listener;
    private Binder binder;
    private ValueAnimator modeAnim;
    private float density;
    private WalletCardView front;
    private WalletCardView back;
    private boolean animating;
    private Mode mode = Mode.STACK;
    private float stepPx;
    private float stackStepPx;
    private float fanStepPx;
    private int peekPad;
    private int touchSlop;
    private float downX;
    private float downY;
    private boolean dragging;
    private boolean verticalDrag;
    public CardStackView(Context context) {super(context); init();}
    public CardStackView(Context context, AttributeSet attrs) {super(context, attrs); init();}
    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr) {super(context, attrs, defStyleAttr); init();}

    private void init() {
        setWillNotDraw(false);
        setClipToPadding(true);
        setClipChildren(true);
        density = getResources().getDisplayMetrics().density;
        stackStepPx = STACK_STEP_DP * density;
        fanStepPx = FAN_STEP_DP * density;
        stepPx = stackStepPx;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        labelPaint.setColor(0xF2FFFFFF);
        labelPaint.setTextSize(13f * density);
        labelPaint.setFakeBoldText(true);
    }

    public void setListener(Listener listener) {this.listener = listener;}
    public void setBinder(Binder binder) {this.binder = binder;}
    public WalletCardView getFrontCard() {ensureCards(); return front;}

    public void onFrontTap() {
        if (wallets.size() < 2) {return;}
        if (mode == Mode.STACK) {setMode(Mode.FAN);}
        else {setMode(Mode.STACK);}
    }

    public void cycleNext() {
        if (wallets.size() < 2) {return;}
        if (mode == Mode.STACK) {setMode(Mode.FAN); return;}
        selectIndex(1);
    }

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
        if (wallets.size() < 2) {mode = Mode.STACK; stepPx = stackStepPx;}
        peekPad = Math.round(visiblePeekCount() * stepPx);
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

    private int visiblePeekCount() {return Math.max(0, Math.min(MAX_PEEKS, wallets.size() - 1));}

    private int hitPeekIndex(float y) {
        int peeks = visiblePeekCount();
        if (peeks <= 0 || y >= peekPad) {return -1;}
        int band = (int) (y / Math.max(1f, stepPx));
        if (band < 0) {band = 0;}
        if (band >= peeks) {band = peeks - 1;}
        return peeks - band;
    }

    private void resetTransforms() {
        if (front != null) {
            front.animate().setListener(null);
            front.animate().cancel();
            front.setTranslationX(0f);
            front.setTranslationY(0f);
            front.setScaleX(1f);
            front.setScaleY(1f);
            front.setAlpha(1f);
            front.setVisibility(VISIBLE);
        }
        if (back != null && wallets.size() >= 2) {
            back.animate().cancel();
            back.setVisibility(VISIBLE);
            back.setTranslationX(0f);
            back.setTranslationY(0f);
            float scale = mode == Mode.STACK ? BACK_SCALE_STACK : 1f;
            back.setScaleX(scale);
            back.setScaleY(scale);
            back.setAlpha(1f);
        }
    }

    private void cancelModeAnim() {
        if (modeAnim != null) {
            modeAnim.cancel();
            modeAnim = null;
        }
        animating = false;
    }

    private void setMode(Mode next) {
        if (next == mode || wallets.size() < 2) {return;}
        cancelModeAnim();
        final Mode from = mode;
        mode = next;
        animating = true;
        float start = stepPx;
        float end = next == Mode.FAN ? fanStepPx : stackStepPx;
        float startScale = from == Mode.STACK ? BACK_SCALE_STACK : 1f;
        float endScale = next == Mode.FAN ? 1f : BACK_SCALE_STACK;
        modeAnim = ValueAnimator.ofFloat(0f, 1f);
        modeAnim.setDuration(MOVE_MS);
        modeAnim.setInterpolator(ease);
        modeAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            stepPx = start + (end - start) * t;
            peekPad = Math.round(visiblePeekCount() * stepPx);
            float backScale = startScale + (endScale - startScale) * t;
            if (back != null && back.getVisibility() == VISIBLE) {
                back.setScaleX(backScale);
                back.setScaleY(backScale);
            }
            requestLayout();
            invalidate();
        });
        modeAnim.addListener(new AnimatorListenerAdapter() {
            private boolean done;
            private void finish() {
                if (done) {return;}
                done = true;
                stepPx = end;
                peekPad = Math.round(visiblePeekCount() * stepPx);
                resetTransforms();
                animating = false;
                modeAnim = null;
                requestLayout();
                invalidate();
            }
            @Override public void onAnimationEnd(Animator animation) {finish();}
            @Override public void onAnimationCancel(Animator animation) {finish();}
        });
        modeAnim.start();
    }

    private void selectIndex(int index) {
        if (listener == null || back == null) {return;}
        if (index < 1 || index >= wallets.size()) {return;}
        ensureCards();
        cancelModeAnim();
        final String next = wallets.get(index).address;
        promote(index);
        mode = Mode.STACK;
        stepPx = stackStepPx;
        peekPad = Math.round(visiblePeekCount() * stepPx);
        resetTransforms();
        requestLayout();
        invalidate();
        if (listener != null) {listener.onWalletSelected(next);}
    }

    private void promote(int index) {
        Store.WalletEntry picked = wallets.remove(index);
        wallets.add(0, picked);
        WalletCardView oldFront = front;
        front = back;
        back = oldFront;
        if (front != null) {bringChildToFront(front);}
        if (binder != null && front != null && !wallets.isEmpty()) {binder.bind(front, wallets.get(0).address);}
        if (back != null) {
            if (wallets.size() >= 2) {
                back.setVisibility(VISIBLE);
                final String backAddr = wallets.get(1).address;
                post(() -> {if (binder != null && back != null) {binder.bind(back, backAddr);}});
            } else {back.setVisibility(GONE);}
        }
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
        int ch = Math.max(front.getMeasuredHeight(), Math.round(w * .63f));
        setMeasuredDimension(w, peekPad + ch);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ensureCards();
        int w = right - left;
        int ch = front != null ? front.getMeasuredHeight() : 0;
        if (ch <= 0) {ch = Math.round(w * .63f);}
        if (back != null && back.getVisibility() != GONE) {
            int backTop = Math.max(0, Math.round(peekPad - stepPx));
            back.layout(0, backTop, w, backTop + ch);
            back.setPivotX(w * .5f);
            back.setPivotY(0f);
        }
        if (front != null) {
            front.layout(0, peekPad, w, peekPad + ch);
            front.setPivotX(w * .5f);
            front.setPivotY(0f);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float w = getWidth();
        float cardTop = peekPad;
        float radius = 18f * density;
        int deep = Math.max(0, visiblePeekCount() - 1);
        float tuck = TUCK_DP * density;
        for (int i = deep; i >= 1; i--) {
            int wi = i + 1;
            if (wi >= wallets.size()) {continue;}
            float bandTop = cardTop - (i + 1) * stepPx;
            float bandBot = cardTop - i * stepPx + tuck;
            if (bandBot <= bandTop) {continue;}
            drawPeekHeader(canvas, wallets.get(wi), wi, bandTop, bandBot, w, radius, i);
        }
        super.dispatchDraw(canvas);
    }

    private void drawPeekHeader(Canvas canvas, Store.WalletEntry entry, int wi, float bandTop, float bandBot, float w, float radius, int depth) {
        float scale = mode == Mode.STACK ? Math.max(.85f, 1f - depth * .05f) : 1f;
        float inset = (1f - scale) * w * .5f + PEEK_INSET_DP * density;
        peekRect.set(inset, bandTop, w - inset, bandBot + radius);
        int save = canvas.save();
        canvas.clipRect(inset, bandTop, w - inset, bandBot);
        int color = peekColorFor(entry, wi);
        int edge = darken(color);
        peekPaint.setShader(new LinearGradient(peekRect.left, bandTop, peekRect.right, bandTop + Math.max(bandBot - bandTop, 1f), lighten(color), edge, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(peekRect, radius, radius, peekPaint);
        peekPaint.setShader(null);
        if (mode == Mode.FAN) {
            String label = entry.label != null && !entry.label.isEmpty() ? entry.label : shortAddr(entry.address);
            if (label != null && !label.isEmpty()) {canvas.drawText(label, peekRect.left + 14f * density, bandTop + 20f * density, labelPaint);}}
        canvas.restoreToCount(save);
    }

    private static String shortAddr(String address) {
        if (address == null || address.length() < 8) {return address;}
        return address.substring(0, 4) + "…" + address.substring(address.length() - 4);
    }

    private int peekColorFor(Store.WalletEntry entry, int wi) {
        if (entry != null && entry.address != null) {
            CardDecor decor = Store.loadCardCache(getContext(), entry.address);
            if (decor != null && decor.background != null) {return decor.background.centerColor | 0xFF000000;}
        }
        return PEEK_COLORS[Math.max(0, wi) % PEEK_COLORS.length];
    }

    private static int lighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 36);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 36);
        int b = Math.min(255, (color & 0xFF) + 36);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
        int b = Math.max(0, (color & 0xFF) - 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void disallowParent(boolean disallow) {
        ViewParent p = getParent();
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow);
            p = p.getParent();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (wallets.size() >= 2) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {disallowParent(true);}
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {disallowParent(false);}
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (wallets.size() < 2) {return false;}
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = ev.getX();
            downY = ev.getY();
            dragging = false;
            verticalDrag = false;
            return downY < peekPad;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dy = ev.getY() - downY;
            float dx = ev.getX() - downX;
            if (!dragging && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                dragging = true;
                verticalDrag = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (wallets.size() < 2) {return super.onTouchEvent(event);}
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            dragging = false;
            verticalDrag = false;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dy = event.getY() - downY;
            float dx = event.getX() - downX;
            if (!dragging && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                dragging = true;
                verticalDrag = true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            float dy = event.getY() - downY;
            float dx = event.getX() - downX;
            if (action == MotionEvent.ACTION_UP) {
                if (verticalDrag && Math.abs(dy) > touchSlop * 2) {
                    if (dy > 0) {setMode(Mode.FAN);}
                    else {setMode(Mode.STACK);}
                } else if (!dragging && Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                    int idx = hitPeekIndex(event.getY());
                    if (idx >= 1) {
                        if (mode == Mode.STACK) {setMode(Mode.FAN);}
                        else {selectIndex(idx);}
                    } else if (mode == Mode.FAN) {setMode(Mode.STACK);}
                }
            }
            dragging = false;
            verticalDrag = false;
            return true;
        }
        return super.onTouchEvent(event);
    }
}
