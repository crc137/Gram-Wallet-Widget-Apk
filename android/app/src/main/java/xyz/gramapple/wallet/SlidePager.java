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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;
import android.view.animation.PathInterpolator;

public class SlidePager extends ViewGroup {
    public interface Listener {
        void onPageSelected(int index);
        void onScrollProgress(float progress);
    }

    private final OverScroller scroller;
    private final int touchSlop;
    private final int minFling;
    private final int maxFling;
    private VelocityTracker tracker;
    private float lastX;
    private float lastY;
    private boolean dragging;
    private boolean horizontal;
    private int page;
    private Listener listener;
    private boolean locked;

    public SlidePager(Context context) {this(context, null);}
    public SlidePager(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new OverScroller(context, new PathInterpolator(0.2f, 0f, 0f, 1f));
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFling = vc.getScaledMinimumFlingVelocity();
        maxFling = vc.getScaledMaximumFlingVelocity();
        setMotionEventSplittingEnabled(false);
    }

    public void setListener(Listener listener) {this.listener = listener;}
    public int getCurrentPage() {return page;}
    public void setSwipeLocked(boolean locked) {this.locked = locked;}

    public void setCurrentPage(int index, boolean animate) {
        int count = Math.max(1, getChildCount());
        index = Math.max(0, Math.min(count - 1, index));
        page = index;
        int target = index * getWidth();
        if (!animate || getWidth() == 0) {
            scroller.abortAnimation();
            scrollTo(target, 0);
            if (listener != null) {listener.onScrollProgress(index); listener.onPageSelected(index);}
            return;
        }
        int dx = target - getScrollX();
        scroller.startScroll(getScrollX(), 0, dx, 0, 360);
        postInvalidateOnAnimation();
    }

    public void removePageAt(int index) {
        if (index < 0 || index >= getChildCount()) {return;}
        scroller.abortAnimation();
        removeViewAt(index);
        if (page > index) {page--;}
        page = Math.max(0, Math.min(Math.max(0, getChildCount() - 1), page));
        requestLayout();
        int w = getWidth();
        if (w > 0) {scrollTo(page * w, 0);}
        if (listener != null) {
            listener.onScrollProgress(page);
            listener.onPageSelected(page);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int childW = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int childH = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {getChildAt(i).measure(childW, childH);}
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(i * w, 0, i * w + w, h);
        }
        scrollTo(page * w, 0);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), 0);
            emitProgress();
            postInvalidateOnAnimation();
            if (scroller.isFinished()) {snapSettled();}
        }
    }

    private void emitProgress() {
        int w = getWidth();
        if (w <= 0 || listener == null) {return;}
        listener.onScrollProgress(getScrollX() / (float) w);
    }

    private void snapSettled() {
        int w = getWidth();
        if (w <= 0) {return;}
        int next = Math.round(getScrollX() / (float) w);
        next = Math.max(0, Math.min(getChildCount() - 1, next));
        page = next;
        int target = page * w;
        if (getScrollX() != target) {scrollTo(target, 0);}
        if (listener != null) {
            listener.onScrollProgress(page);
            listener.onPageSelected(page);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (locked) {return false;}
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            scroller.abortAnimation();
            lastX = ev.getX();
            lastY = ev.getY();
            dragging = false;
            horizontal = false;
            if (tracker != null) {tracker.clear();}
            else {tracker = VelocityTracker.obtain();}
            tracker.addMovement(ev);
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = ev.getX() - lastX;
            float dy = ev.getY() - lastY;
            if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                dragging = true;
                horizontal = true;
                lastX = ev.getX();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (locked) {return false;}
        if (tracker == null) {tracker = VelocityTracker.obtain();}
        tracker.addMovement(ev);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            scroller.abortAnimation();
            lastX = ev.getX();
            dragging = true;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float x = ev.getX();
            float dx = lastX - x;
            lastX = x;
            int max = Math.max(0, (getChildCount() - 1) * getWidth());
            int next = getScrollX() + Math.round(dx);
            if (next < 0) {next = next / 3;}
            else if (next > max) {next = max + (next - max) / 3;}
            scrollTo(next, 0);
            emitProgress();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            tracker.computeCurrentVelocity(1000, maxFling);
            float vx = tracker.getXVelocity();
            tracker.recycle();
            tracker = null;
            int w = getWidth();
            if (w <= 0) {return true;}
            int targetPage = page;
            if (Math.abs(vx) > minFling) {targetPage = vx < 0 ? page + 1 : page - 1;}
            else {targetPage = Math.round(getScrollX() / (float) w);}
            targetPage = Math.max(0, Math.min(getChildCount() - 1, targetPage));
            page = targetPage;
            int target = page * w;
            scroller.startScroll(getScrollX(), 0, target - getScrollX(), 0, 360);
            postInvalidateOnAnimation();
            getParent().requestDisallowInterceptTouchEvent(false);
            dragging = false;
            return true;
        }
        return true;
    }
}
