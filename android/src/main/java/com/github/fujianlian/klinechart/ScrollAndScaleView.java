package com.github.fujianlian.klinechart;

import android.content.Context;
import androidx.core.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;
import android.widget.RelativeLayout;

/**
 * View that can scroll and zoom
 * Created by tian on 2016/5/3.
 */
public abstract class ScrollAndScaleView extends RelativeLayout implements
        GestureDetector.OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener {
    protected int mScrollX = 0;
    
    /**
     * Get minimum visible candles
     * @return minimum number of candles that should be visible
     */
    protected abstract float getMinVisibleCandles();
    protected GestureDetectorCompat mDetector;
    protected ScaleGestureDetector mScaleDetector;

    protected boolean isLongPress = false;

    protected OverScroller mScroller;

    protected boolean touch = false;

    protected float mScaleX = 1;

    protected float mScaleXMax = 2f;

    protected float mScaleXMin = 0.5f;

    private boolean mMultipleTouch = false;

    private boolean mScrollEnable = true;

    private boolean mScaleEnable = true;

    protected boolean mHasTriggeredLeftSide = false;
    protected boolean mHasTriggeredRightSide = false;

    public ScrollAndScaleView(Context context) {
        super(context);
        init();
    }

    public ScrollAndScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScrollAndScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private float mDownX = 0;

    private float mDownY = 0;


    private void init() {
        setWillNotDraw(false);
        mDetector = new GestureDetectorCompat(getContext(), this);
        mScaleDetector = new ScaleGestureDetector(getContext(), this);
        mScroller = new OverScroller(getContext());
        this.setClickable(true);
    }



    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!isLongPress && !isMultipleTouch()) {
            scrollBy(-Math.round(distanceX), 0);
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        isLongPress = true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!isTouch() && isScrollEnable()) {
            mScroller.fling(-mScrollX, 0
                    , Math.round(velocityX / mScaleX), 0,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    0, 0);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (!isTouch()) {
                scrollTo(-mScroller.getCurrX(), mScroller.getCurrY());
            } else {
                mScroller.forceFinished(true);
            }
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(mScrollX - Math.round(x / mScaleX), 0);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (!isScrollEnable()) {
            mScroller.forceFinished(true);
            return;
        }
        int oldX = mScrollX;
        mScrollX = x;
        checkAndFixScrollX();
        onScrollChanged(mScrollX, 0, oldX, 0);
        invalidate();
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (!isScaleEnable()) {
            return false;
        }
        if (isLongPress) {
            return false;
        }
        float oldScale = mScaleX;
        mScaleX *= detector.getScaleFactor();
        if (mScaleX < mScaleXMin) {
            mScaleX = mScaleXMin;
        } else if (mScaleX > mScaleXMax) {
            mScaleX = mScaleXMax;
        } else {
            onScaleChanged(mScaleX, oldScale);
        }
        return true;
    }

    protected void onScaleChanged(float scale, float oldScale) {
        invalidate();
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    float x;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean shouldContinue = true;
        // More than 1 finger pressed
        if (event.getPointerCount() > 1) {
            isLongPress = false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (isLongPress) {
                    isLongPress = false;
                }
                touch = true;
                x = event.getX();
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                // Move after long press
                if (isLongPress) {
                    onLongPress(event);
                }
                if(Math.abs(event.getX() - mDownX) >= Math.abs(event.getY()-mDownY) || isLongPress) {
                    shouldContinue = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    // Found not handled by self, return to parent class
                    shouldContinue = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (x == event.getX()) {

                }
                touch = false;
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                isLongPress = false;
                touch = false;
                invalidate();
                break;
            default:
                break;
        }
        mMultipleTouch = event.getPointerCount() > 1;
        this.mDetector.onTouchEvent(event);
        this.mScaleDetector.onTouchEvent(event);
        return shouldContinue;
    }


    /**
     * Scrolled to the leftmost side
     */
    abstract public void onLeftSide();

    /**
     * Scrolled to the rightmost side
     */
    abstract public void onRightSide();

    /**
     * Whether currently touching
     *
     * @return
     */
    public boolean isTouch() {
        return touch;
    }

    /**
     * Get the minimum offset value
     *
     * @return
     */
    public abstract int getMinScrollX();

    /**
     * Get the maximum offset value
     *
     * @return
     */
    public abstract int getMaxScrollX();

    /**
     * Get the point width
     *
     * @return
     */
    public abstract float getPointWidth();

    /**
     * Get the total data length (itemCount * pointWidth)
     *
     * @return
     */
    public abstract float getDataLength();

    /**
     * Set ScrollX
     *
     * @param scrollX
     */
    public void setScrollX(int scrollX) {
        int maxAllowedScroll = (int)(getDataLength() - getMinVisibleCandles() * getPointWidth());
        int normalizedMaxAllowedScroll = Math.max(0, maxAllowedScroll);
        this.mScrollX = Math.max(0, Math.min(scrollX, normalizedMaxAllowedScroll));
        scrollTo(this.mScrollX, 0);
    }

    /**
     * Whether it's multi-touch
     *
     * @return
     */
    public boolean isMultipleTouch() {
        return mMultipleTouch;
    }

    protected void checkAndFixScrollX() {
        float dataLength = getDataLength();
        float minVisibleCandles = getMinVisibleCandles();
        float pointWidth = getPointWidth();
        int maxAllowedScroll = (int)(dataLength - minVisibleCandles * pointWidth);
        int normalizedMaxAllowedScroll = Math.max(0, maxAllowedScroll);

        if (mScrollX < getMinScrollX()) {
            mScrollX = getMinScrollX();
            mScroller.forceFinished(true);
        } else if (mScrollX > normalizedMaxAllowedScroll) {
            mScrollX = normalizedMaxAllowedScroll;
            mScroller.forceFinished(true);
            if (!mHasTriggeredRightSide) {
                mHasTriggeredRightSide = true;
                mHasTriggeredLeftSide = false;
                onRightSide();
            }
        } else {
            // Reset right side flag when we're not at right boundary
            mHasTriggeredRightSide = false;
        }

        // Check if there are less than 100 candlesticks to the left
        checkLeftSideThreshold();
    }

    protected void checkLeftSideThreshold() {
        // This method should be overridden by BaseKLineChartView to access itemWidth
    }

    public float getScaleXMax() {
        return mScaleXMax;
    }

    public float getScaleXMin() {
        return mScaleXMin;
    }

    public boolean isScrollEnable() {
        return mScrollEnable;
    }

    public boolean isScaleEnable() {
        return mScaleEnable;
    }

    /**
     * Set maximum scale value
     */
    public void setScaleXMax(float scaleXMax) {
        mScaleXMax = scaleXMax;
    }

    /**
     * Set minimum scale value
     */
    public void setScaleXMin(float scaleXMin) {
        mScaleXMin = scaleXMin;
    }

    /**
     * Set whether scrolling is enabled
     */
    public void setScrollEnable(boolean scrollEnable) {
        mScrollEnable = scrollEnable;
    }

    /**
     * Set whether scaling is enabled
     */
    public void setScaleEnable(boolean scaleEnable) {
        mScaleEnable = scaleEnable;
    }

    @Override
    public float getScaleX() {
        return mScaleX;
    }
}
