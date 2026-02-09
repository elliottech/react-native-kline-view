package com.github.fujianlian.klinechart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.core.view.GestureDetectorCompat;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.airbnb.lottie.*;
import com.github.fujianlian.klinechart.base.IChartDraw;
import com.github.fujianlian.klinechart.base.IDateTimeFormatter;
import com.github.fujianlian.klinechart.base.IValueFormatter;
import com.github.fujianlian.klinechart.container.HTDrawContext;
import com.github.fujianlian.klinechart.container.HTKLineContainerView;
import com.github.fujianlian.klinechart.container.HTPoint;
import com.github.fujianlian.klinechart.draw.MainDraw;
import com.github.fujianlian.klinechart.draw.PrimaryStatus;
import com.github.fujianlian.klinechart.entity.IKLine;
import com.github.fujianlian.klinechart.formatter.TimeFormatter;
import com.github.fujianlian.klinechart.formatter.ValueFormatter;
import com.github.fujianlian.klinechart.utils.ViewUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * k线图
 * Created by tian on 2016/5/3.
 */
public abstract class BaseKLineChartView extends ScrollAndScaleView implements Drawable.Callback {

    public HTKLineConfigManager configManager;

    public HTDrawContext drawContext;



    private int mChildDrawPosition = -1;

    private int mWidth = 0;

    private int mTopPadding;

    private int mChildPadding;

    private int mBottomPadding;

    private float mMainScaleY = 1;

    private float mVolScaleY = 1;

    private float mChildScaleY = 1;

    private float mDataLen = 0;

    private float mMainMaxValue = Float.MAX_VALUE;

    private float mMainMinValue = Float.MIN_VALUE;

    private float mMainHighMaxValue = 0;

    private float mMainLowMinValue = 0;

    private int mMainMaxIndex = 0;

    private int mMainMinIndex = 0;

    private Float mVolMaxValue = Float.MAX_VALUE;

    private Float mVolMinValue = Float.MIN_VALUE;

    private Float mChildMaxValue = Float.MAX_VALUE;

    private Float mChildMinValue = Float.MIN_VALUE;

    private int mStartIndex = 0;

    private int mStopIndex = 0;

    private float mPointWidth = 6;

    private int mGridRows = 4;

    private int mGridColumns = 4;

    private Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mMaxMinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectedXLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectedYLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectCenterBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectorFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mClosePriceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mClosePricePointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mClosePriceTrianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mClosePriceRightTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    protected RectF mClosePriceLabelFrame = new RectF();

    private LottieDrawable lottieDrawable = new LottieDrawable();

    private String lastLoadLottieSource = "";

    private int mSelectedIndex;

    private IChartDraw mMainDraw;
    private MainDraw mainDraw;
    private IChartDraw mVolDraw;

    public Boolean isMinute = false;

    private Boolean isWR = false;
    private Boolean isShowChild = false;

    //当前点的个数
    private int mItemCount;
    private IChartDraw mChildDraw;
    private List<IChartDraw> mChildDraws = new ArrayList<>();

    private IValueFormatter mValueFormatter;
    private IDateTimeFormatter mDateTimeFormatter;

    private ValueAnimator mAnimator;

    private long mAnimationDuration = 500;

    private float mOverScrollRange = 0;

    private OnSelectedChangedListener mOnSelectedChangedListener = null;

    private Rect mMainRect;

    private Rect mVolRect;

    private Rect mChildRect;

    private float mLineWidth;

    public BaseKLineChartView(Context context, HTKLineConfigManager configManager) {
        super(context);
        this.configManager = configManager;
        init();
    }

    private void init() {
        setWillNotDraw(false);
        drawContext = new HTDrawContext(this, configManager);




        mDetector = new GestureDetectorCompat(getContext(), this);
        mScaleDetector = new ScaleGestureDetector(getContext(), this);
//        mTopPadding = (int) getResources().getDimension(R.dimen.chart_top_padding);
//        mChildPadding = (int) getResources().getDimension(R.dimen.child_top_padding);
//        mBottomPadding = (int) getResources().getDimension(R.dimen.chart_bottom_padding);


        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mAnimationDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // invalidate();
            }
        });

        mSelectorFramePaint.setStrokeWidth(ViewUtil.Dp2Px(getContext(), 0.6f));
        mSelectorFramePaint.setStyle(Paint.Style.STROKE);
        mSelectorFramePaint.setColor(Color.WHITE);

        mClosePriceLinePaint.setStyle(Paint.Style.STROKE);
        mClosePriceLinePaint.setAntiAlias(true);
        mClosePriceLinePaint.setStrokeWidth(ViewUtil.Dp2Px(getContext(), 0.7f));
        mClosePriceLinePaint.setPathEffect(new DashPathEffect(new float[]{8, 8}, 0));

        mClosePricePointPaint.setStrokeWidth(1);

        mClosePriceTrianglePaint.setStyle(Paint.Style.FILL);

    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        if (isMinute) {
            this.invalidate();
        }
    }

    private void initLottieView() {
    	String jsonString = configManager.closePriceRightLightLottieSource;
    	if (lastLoadLottieSource == jsonString) {
    		return;
    	}
    	lastLoadLottieSource = jsonString;
        lottieDrawable.setCallback(this);
        
        final float scale = configManager.closePriceRightLightLottieScale;
        if (jsonString.length() > 0) {
            lottieDrawable.setImagesAssetsFolder(configManager.closePriceRightLightLottieFloder);
            LottieCompositionFactory.fromJsonString(jsonString, null).addListener(new LottieListener<LottieComposition>() {
                @Override
                public void onResult(LottieComposition composition) {
                    lottieDrawable.setComposition(composition);
                    lottieDrawable.setRepeatCount(Integer.MAX_VALUE);
                    lottieDrawable.setScale(scale);
                    lottieDrawable.playAnimation();
                }
            });
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        notifyChanged();
    }


    private void initRect() {
        android.util.Log.d("BaseKLineChartView", "initRect() START");
        try {
            mTopPadding = (int) configManager.paddingTop;
            mChildPadding = 50;
            mBottomPadding = (int) configManager.paddingBottom;
            android.util.Log.d("BaseKLineChartView", "Padding values - Top: " + mTopPadding + ", Child: " + mChildPadding + ", Bottom: " + mBottomPadding);

            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            int textHeight = (int)((fm.descent - fm.ascent) / 2.0);
            android.util.Log.d("BaseKLineChartView", "TextHeight: " + textHeight);

            int allHeight = this.getHeight() - mBottomPadding;
            int mMainHeight = (int) (allHeight * configManager.mainFlex);
            int mVolHeight = (int) (allHeight * configManager.volumeFlex);
            int mChildHeight = (int) (allHeight * (1 - configManager.mainFlex - configManager.volumeFlex));
            android.util.Log.d("BaseKLineChartView", "Heights - All: " + allHeight + ", Main: " + mMainHeight + ", Vol: " + mVolHeight + ", Child: " + mChildHeight);
            android.util.Log.d("BaseKLineChartView", "Flex values - Main: " + configManager.mainFlex + ", Volume: " + configManager.volumeFlex);

            mMainRect = new Rect(0, mTopPadding - textHeight, mWidth, mMainHeight - textHeight);
            android.util.Log.d("BaseKLineChartView", "mMainRect: " + mMainRect);

            if (configManager.volumeFlex > 0) {
                mVolRect = new Rect(0, mMainRect.bottom + textHeight + mChildPadding, mWidth, mMainRect.bottom + textHeight + mVolHeight);
                mChildRect = new Rect(0, mVolRect.bottom + mChildPadding, mWidth, mVolRect.bottom + mChildHeight);
                android.util.Log.d("BaseKLineChartView", "With volume - mVolRect: " + mVolRect + ", mChildRect: " + mChildRect);
            } else {
                mVolRect = new Rect(0, 0, 0, 0); // Empty rect when volumeFlex is 0
                // When volumeFlex is 0, child section should extend to the bottom of the view
                mChildRect = new Rect(0, mMainRect.bottom + mChildPadding, mWidth, allHeight);
                android.util.Log.d("BaseKLineChartView", "Without volume - mVolRect: " + mVolRect + ", mChildRect: " + mChildRect);
            }

            if (!isShowChild) {
                android.util.Log.d("BaseKLineChartView", "Child not shown, adjusting rects");
                if (configManager.volumeFlex > 0) {
                    mChildRect.top = mVolRect.bottom;
                    mChildRect.bottom = mVolRect.bottom;
                } else {
                    // When volumeFlex is 0 and child is not shown, position at bottom for time bar
                    mChildRect.top = allHeight;
                    mChildRect.bottom = allHeight;
                }
                android.util.Log.d("BaseKLineChartView", "Final mChildRect after adjustment: " + mChildRect);
            }

            calculateValue();
            android.util.Log.d("BaseKLineChartView", "initRect() COMPLETED");
        } catch (Exception e) {
            android.util.Log.e("BaseKLineChartView", "ERROR in initRect()", e);
            throw e;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        android.util.Log.d("BaseKLineChartView", "onDraw() START");
        try {
            super.onDraw(canvas);
//        canvas.drawColor(mBackgroundPaint.getColor());

            android.util.Log.d("BaseKLineChartView", "Checking dimensions: mWidth=" + mWidth + ", mMainRect.height()=" + mMainRect.height());
            if (mWidth == 0 || mMainRect.height() == 0) {
                android.util.Log.w("BaseKLineChartView", "Invalid dimensions, skipping draw");
                return;
            }

            android.util.Log.d("BaseKLineChartView", "Calling calculateValue() from onDraw");
            calculateValue();

            android.util.Log.d("BaseKLineChartView", "Starting canvas operations");
            canvas.save();
            canvas.scale(1, 1);

            android.util.Log.d("BaseKLineChartView", "Drawing grid");
            drawGird(canvas);

            if (mItemCount > 0) {
                android.util.Log.d("BaseKLineChartView", "Drawing chart elements for " + mItemCount + " items");
                drawK(canvas);
                drawText(canvas);
                drawMaxAndMin(canvas);
                drawOrderLines(canvas);
                drawValue(canvas, isLongPress ? mSelectedIndex : mStopIndex);
                drawClosePriceLine(canvas);
                drawSelector(canvas);
                android.util.Log.d("BaseKLineChartView", "Chart elements drawn successfully");
            } else {
                android.util.Log.w("BaseKLineChartView", "No items to draw (mItemCount = 0)");
            }

            canvas.restore();
            android.util.Log.d("BaseKLineChartView", "Canvas operations completed");

            android.util.Log.d("BaseKLineChartView", "Drawing context overlay");
            drawContext.onDraw(canvas);
            android.util.Log.d("BaseKLineChartView", "onDraw() COMPLETED SUCCESSFULLY");
        } catch (Exception e) {
            android.util.Log.e("BaseKLineChartView", "ERROR in onDraw()", e);
            throw e;
        }
    }

    public float yFromValue(float value) {
        if (mItemCount <= 0) {
            return value;
        }
    	float distance = (mMainMaxValue - value) * mMainScaleY;
    	if (mMainMaxValue == mMainMinValue && value == mMainMinValue) {
    		distance = mMainRect.height() * 0.5f;
    	}
        return distance + mMainRect.top;
    }

    public float valueFromY(float y) {
        if (mItemCount <= 0) {
            return y;
        }
        float value = mMainMaxValue - ((y - mMainRect.top) / mMainScaleY);
        if (mMainMaxValue == mMainMinValue && value == mMainMinValue) {
            value = mMainMinValue;
        }
        return value;
    }

    public float xFromValue(float value) {
        if (mItemCount < 2) {
            return value;
        }
        KLineEntity firstItem = getItem(0);
        KLineEntity lastItem = getItem(mItemCount - 1);
        float scale = (lastItem.id - firstItem.id) / (configManager.itemWidth * (mItemCount - 1));
        float x = (value - firstItem.id) / scale + configManager.itemWidth / 2.0f - mScrollX;
        return x;
    }

    public float valueFromX(float x) {
        if (mItemCount < 2) {
            return x;
        }
        KLineEntity firstItem = getItem(0);
        KLineEntity lastItem = getItem(mItemCount - 1);
        float scale = (lastItem.id - firstItem.id) / (configManager.itemWidth * (mItemCount - 1));
        float value = scale * (x + mScrollX - configManager.itemWidth / 2.0f) + firstItem.id;
        return value;
    }

    public HTPoint valuePointFromViewPoint(HTPoint point) {
        return new HTPoint(valueFromX(point.x), valueFromY(point.y));
    }

    public HTPoint viewPointFromValuePoint(HTPoint point) {
        return new HTPoint(xFromValue(point.x), yFromValue(point.y));
    }

    public float getMainBottom() {
        return mMainRect.bottom;
    }

    public float getVolY(float value) {
        return (mVolMaxValue - value) * mVolScaleY + mVolRect.top;
    }

    public float getChildY(float value) {
        return (mChildMaxValue - value) * mChildScaleY + mChildRect.top;
    }

    /**
     * 解决text居中的问题
     */
    public float fixTextY(float y) {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return y + fontMetrics.descent - fontMetrics.ascent;
    }

    /**
     * 解决text居中的问题
     */
    public float fixTextY1(float y) {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return (y + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent);
    }

    /**
     * 画表格
     *
     * @param canvas
     */
    private void drawGird(Canvas canvas) {
//        //-----------------------上方k线图------------------------
//        //横向的grid
//        float rowSpace = mMainRect.height() / mGridRows;
//        for (int i = 0; i <= mGridRows; i++) {
//            canvas.drawLine(0, rowSpace * i + mMainRect.top, mWidth, rowSpace * i + mMainRect.top, mGridPaint);
//        }
//        //-----------------------下方子图------------------------
//        if (mChildDraw != null) {
//            canvas.drawLine(0, mVolRect.bottom, mWidth, mVolRect.bottom, mGridPaint);
//            canvas.drawLine(0, mChildRect.bottom, mWidth, mChildRect.bottom, mGridPaint);
//        } else {
//            canvas.drawLine(0, mVolRect.bottom, mWidth, mVolRect.bottom, mGridPaint);
//        }
//        //纵向的grid
//        float columnSpace = mWidth / mGridColumns;
//        for (int i = 1; i < mGridColumns; i++) {
//            canvas.drawLine(columnSpace * i, 0, columnSpace * i, mMainRect.bottom, mGridPaint);
//            canvas.drawLine(columnSpace * i, mMainRect.bottom, columnSpace * i, mVolRect.bottom, mGridPaint);
//            if (mChildDraw != null) {
//                canvas.drawLine(columnSpace * i, mVolRect.bottom, columnSpace * i, mChildRect.bottom, mGridPaint);
//            }
//        }
    }

    private void drawClosePriceLine(Canvas canvas) {
        if (mItemCount <= 0) {
            return;
        }
        float paddingRight = this.configManager.paddingRight;
        IKLine point = (IKLine) getItem(mItemCount - 1);
        float price = point.getClosePrice();
        String text = mainDraw.getValueFormatter().format(price);
        float width = calculateWidth(text);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float height = fm.descent - fm.ascent;
        float y = yFromValue(price);
        float x = scrollXtoViewX(getItemMiddleScrollX(mItemCount - 1) + mPointWidth * 0.5f);



        if (x > mWidth - width) {
            mClosePriceLinePaint.setColor(configManager.closePriceCenterSeparatorColor);
            mClosePriceTrianglePaint.setColor(configManager.closePriceCenterTriangleColor);
            float paddingX = 20;
            float paddingY = 14;
            y = Math.max(mMainRect.top + height / 2 + paddingY, y);
            y = Math.min(getMainBottom() - height / 2 - paddingY, y);

            float triangleWidth = 14;
            float triangleHeight = 20;
            float triangleMarginLeft = 10;
            float containerWidth = paddingX * 2 + width + triangleWidth + triangleMarginLeft;

            float marginRight = paddingRight - containerWidth / 2;
            float textX = mWidth - paddingRight - containerWidth / 2 + paddingX;

            RectF rect = new RectF(textX - paddingX, y - height / 2 - paddingY, mWidth - marginRight, y + height / 2 + paddingY);
            mClosePriceLabelFrame.set(rect);
            android.util.Log.d("BaseKLineChartView", "Set closePriceLabelFrame (center): " + rect);

            float radius = (paddingY * 2 + height) / 2;
            mClosePricePointPaint.setColor(configManager.closePriceCenterBackgroundColor);
            mClosePricePointPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect,radius,radius, mClosePricePointPaint);
            mClosePricePointPaint.setColor(configManager.closePriceCenterBorderColor);
            mClosePricePointPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(rect,radius,radius, mClosePricePointPaint);
            canvas.drawText(text, textX, fixTextY1(y), mTextPaint);
            Path path = new Path();
            float triangleMarginTop = (rect.bottom - rect.top - triangleHeight) / 2;
            path.moveTo(rect.right - paddingX - triangleWidth, triangleMarginTop + rect.top);
            path.lineTo(rect.right - paddingX - triangleWidth, rect.bottom - triangleMarginTop);
            path.lineTo(rect.right - paddingX, (rect.bottom - rect.top) / 2 + rect.top);
            path.close();
            canvas.drawPath(path, mClosePriceTrianglePaint);
        } else {
            mClosePriceLinePaint.setColor(configManager.closePriceRightSeparatorColor);
            mClosePricePointPaint.setColor(configManager.closePriceRightBackgroundColor);
            mClosePricePointPaint.setStyle(Paint.Style.FILL);
            mClosePriceRightTextPaint.setColor(configManager.closePriceRightSeparatorColor);

            RectF rightRect = new RectF(mWidth - width, y - height / 2, mWidth, y + height / 2);
            mClosePriceLabelFrame.set(rightRect);
            android.util.Log.d("BaseKLineChartView", "Set closePriceLabelFrame (right): " + rightRect);

            canvas.drawRect(rightRect, mClosePricePointPaint);
            canvas.drawText(text, mWidth - width, fixTextY1(y), mClosePriceRightTextPaint);

            if (isMinute) {
                int lottieWidth = lottieDrawable.getIntrinsicWidth();
                int lottieHeight = lottieDrawable.getIntrinsicHeight();
                canvas.save();
                canvas.translate((int)x - lottieWidth / 2, (int)y - lottieHeight / 2);
                lottieDrawable.draw(canvas);
                canvas.restore();
            }

        }

    }

    /**
     * 画k线图
     *
     * @param canvas
     */
    private void drawK(Canvas canvas) {
        //保存之前的平移，缩放
        canvas.save();
        canvas.translate(-mScrollX * mScaleX, 0);
        canvas.scale(mScaleX, 1);
        mainDraw.drawMinuteMinute(mTopPadding, mStartIndex, getMainBottom(), mStopIndex, canvas, this);
        for (int i = mStartIndex; i <= mStopIndex; i++) {
            if (i < 0 || i >= configManager.modelArray.size()) {
                continue;
            }
            Object currentPoint = getItem(i);
            float currentPointX = getItemMiddleScrollX(i);
            Object lastPoint = i == 0 ? currentPoint : getItem(i - 1);
            float lastX = i == 0 ? currentPointX : getItemMiddleScrollX(i - 1);
            if (mMainDraw != null) {
                mMainDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }
            if (mVolDraw != null && configManager.volumeFlex > 0) {
                mVolDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }
            if (mChildDraw != null) {
                mChildDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }

            // Draw buy/sell marks for this candlestick if they exist (O(1) lookup)
            drawBuySellMarksForCandlestick((KLineEntity) currentPoint, i, currentPointX, canvas);
        }

        //还原 平移缩放
        canvas.restore();
    }

    /**
     * 计算文本长度
     *
     * @return
     */
    private int calculateWidth(String text) {
        Rect rect = new Rect();
        mTextPaint.getTextBounds(text, 0, text.length(), rect);
        return rect.width() + 5;
    }

    /**
     * 计算文本长度
     *
     * @return
     */
    private Rect calculateMaxMin(String text) {
        Rect rect = new Rect();
        mMaxMinPaint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * 画文字
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseLine = (textHeight - fm.bottom - fm.top) / 2;
        //--------------画上方k线图的值-------------
        if (mMainDraw != null) {
//            canvas.drawText(formatValue(mMainMaxValue), mWidth - calculateWidth(formatValue(mMainMaxValue)), baseLine + mMainRect.top, mTextPaint);
//            canvas.drawText(formatValue(mMainMinValue), mWidth - calculateWidth(formatValue(mMainMinValue)), mMainRect.bottom - textHeight + baseLine, mTextPaint);
            float rowValue = (mMainMaxValue - mMainMinValue) / mGridRows;
            float rowSpace = mMainRect.height() / mGridRows;
            for (int i = 0; i < mGridRows + 1; i++) {
                String text = formatValue(rowValue * (mGridRows - i) + mMainMinValue);
                float y = rowSpace * i + mMainRect.top;
                y = fixTextY1(y);
                canvas.drawText(text, mWidth - calculateWidth(text), y, mTextPaint);
            }
        }
        //--------------画中间子图的值-------------
        if (mVolDraw != null && configManager.volumeFlex > 0) {
            IValueFormatter formatter = mVolDraw.getValueFormatter();
            if (formatter instanceof ValueFormatter) {
                ValueFormatter valueFormatter = (ValueFormatter)formatter;
                String formatValue = valueFormatter.formatVolume(mVolMaxValue);
                canvas.drawText(formatValue,
                        mWidth - calculateWidth(formatValue), mVolRect.top + baseLine, mTextPaint);
            }
            /*canvas.drawText(mVolDraw.getValueFormatter().format(mVolMinValue),
                    mWidth - calculateWidth(formatValue(mVolMinValue)), mVolRect.bottom, mTextPaint);*/
        }
        //--------------画下方子图的值-------------
        if (mChildDraw != null) {
            IValueFormatter formatter = mChildDraw.getValueFormatter();
            if (formatter instanceof ValueFormatter) {
                ValueFormatter valueFormatter = (ValueFormatter)formatter;
                String formatValue = valueFormatter.format(mChildMaxValue);
                canvas.drawText(formatValue,
                        mWidth - calculateWidth(formatValue), mVolRect.bottom + baseLine, mTextPaint);
            }
            /*canvas.drawText(mChildDraw.getValueFormatter().format(mChildMinValue),
                    mWidth - calculateWidth(formatValue(mChildMinValue)), mChildRect.bottom, mTextPaint);*/
        }
        //--------------画时间---------------------
        float columnSpace = mWidth / mGridColumns;
        float y = fixTextY1((float) (mChildRect.bottom + mBottomPadding / 2.0));

        float startX = getItemMiddleScrollX(mStartIndex) - mPointWidth / 2;
        float stopX = getItemMiddleScrollX(mStopIndex) + mPointWidth / 2;

        for (int i = 1; i < mGridColumns; i++) {
            float scrollX = viewXToScrollX(columnSpace * i);
            if (scrollX >= startX && scrollX <= stopX) {
                int index = indexFromScrollX(scrollX);
                String text = getItem(index).Date;
                canvas.drawText(text, columnSpace * i - mTextPaint.measureText(text) / 2, y, mTextPaint);
            }
        }

        if (mStartIndex < 0 || mStopIndex <= 0) {
            return;
        }

        float scrollX = viewXToScrollX(0);
        if (scrollX >= startX && scrollX <= stopX) {
            String text = getItem(mStartIndex).Date;
            canvas.drawText(text, -mTextPaint.measureText(text) / 2, y, mTextPaint);
        }
        scrollX = viewXToScrollX(mWidth);
        if (scrollX >= startX && scrollX <= stopX) {
            String text = getItem(mStopIndex).Date;
            canvas.drawText(text, mWidth - mTextPaint.measureText(text) / 2, y, mTextPaint);
        }

    }

    private void drawSelector(Canvas canvas) {
        if (!isLongPress) {
            return;
        }
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseLine = (textHeight - fm.bottom - fm.top) / 2;

        mSelectPointPaint.setColor(configManager.panelBackgroundColor);
        // 画Y值
        IKLine point = (IKLine) getItem(mSelectedIndex);
        float w1 = ViewUtil.Dp2Px(getContext(), 5);
        float w2 = ViewUtil.Dp2Px(getContext(), 3);
        float r = textHeight / 2 + w2;
        float triangleWidth = 10;
        float y = yFromValue(point.getClosePrice());
        float x;
        float startX;
        float endX;
        String text = formatValue(point.getClosePrice());
        float textWidth = mTextPaint.measureText(text);
        if (scrollXtoViewX(getItemMiddleScrollX(mSelectedIndex)) < getChartWidth() / 2) {
            x = 1;
            startX = textWidth + 2 * w1 + w2 + triangleWidth;
            endX = mWidth;
            Path path = new Path();
            path.moveTo(x, y - r);
            path.lineTo(x, y + r);
            path.lineTo(textWidth + 2 * w1, y + r);
            path.lineTo(startX, y);
            path.lineTo(textWidth + 2 * w1, y - r);
            path.close();
            canvas.drawPath(path, mSelectPointPaint);
            canvas.drawPath(path, mSelectorFramePaint);
            canvas.drawText(text, x + w1, fixTextY1(y), mMaxMinPaint);
        } else {
            x = mWidth - textWidth - 1 - 2 * w1 - w2 - triangleWidth;
            startX = 0;
            endX = x;

            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x + w2 + triangleWidth, y + r);
            path.lineTo(mWidth - 2, y + r);
            path.lineTo(mWidth - 2, y - r);
            path.lineTo(x + w2 + triangleWidth, y - r);
            path.close();

            canvas.drawPath(path, mSelectPointPaint);
            canvas.drawPath(path, mSelectorFramePaint);
            canvas.drawText(text, x + w1 + w2, fixTextY1(y), mMaxMinPaint);
        }

        // k线图横线
        canvas.drawLine(startX, y,  endX, y, mSelectedXLinePaint);


        // k线图竖线
        mSelectedXLinePaint.setStrokeWidth(1.5f);
        mSelectedXLinePaint.setColor(configManager.candleTextColor);

        // 柱状图竖线
        LinearGradient linearGradient = new LinearGradient(
            0,
            0,
            0,
            mChildRect.bottom - mMainRect.top,
            configManager.panelGradientColorList,
            configManager.panelGradientLocationList,
            Shader.TileMode.CLAMP
        );
        mSelectedYLinePaint.setShader(linearGradient);

        float pointX = scrollXtoViewX(getItemMiddleScrollX(mSelectedIndex));
        mSelectedYLinePaint.setStrokeWidth(mScaleX * configManager.candleWidth);
        canvas.drawLine(pointX, 0, pointX, mChildRect.bottom, mSelectedYLinePaint);



        mSelectCenterPaint.setColor(configManager.selectedPointContentColor);
        mSelectCenterBackgroundPaint.setColor(configManager.selectedPointContainerColor);
        float radiusX = 6 * this.mScaleX;
        float radiusY = 6 * this.mScaleX;
        RectF rect = new RectF(pointX - radiusX, y - radiusY, pointX + radiusX, y + radiusY);
        radiusX *= 3;
        radiusY *= 3;
        RectF backgroundRect = new RectF(pointX - radiusX, y - radiusY, pointX + radiusX, y + radiusY);
        if (pointX > startX && pointX < endX) {
            canvas.drawOval(backgroundRect, mSelectCenterBackgroundPaint);
            canvas.drawOval(rect, mSelectCenterPaint);
        }



        // 画X值
        String date = getItem(mSelectedIndex).Date;
        textWidth = mMaxMinPaint.measureText(date);
        r = textHeight / 2;
        x = scrollXtoViewX(getItemMiddleScrollX(mSelectedIndex));
        y = mChildRect.bottom;

        if (x < textWidth + 2 * w1) {
            x = 1 + textWidth / 2 + w1;
        } else if (mWidth - x < textWidth + 2 * w1) {
            x = mWidth - 1 - textWidth / 2 - w1;
        }

        canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + mBottomPadding, mSelectPointPaint);
        canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + mBottomPadding, mSelectorFramePaint);
        canvas.drawText(date, x - textWidth / 2, fixTextY1(y + (mBottomPadding / 2)), mMaxMinPaint);
        mainDraw.drawSelector(this, canvas);
    }

    private void drawMaxMinValue(Canvas canvas, float value, float x, float y) {
        IValueFormatter formatter = this.getValueFormatter();
        String valueString = formatter.format(value);
        int height = calculateMaxMin(valueString).height();
        y += height / 2;
        String lineString = "---";
        if (x < getWidth() / 2) {
            valueString = lineString + valueString;
        } else {
            valueString = valueString + lineString;
            float width = mMaxMinPaint.measureText(valueString);
            x -= width;
        }
        canvas.drawText(valueString, x, y, mMaxMinPaint);
    }

    /**
     * 画文字
     *
     * @param canvas
     */
    private void drawMaxAndMin(Canvas canvas) {
        if (!isMinute) {
            //绘制最大值和最小值
            float x = scrollXtoViewX(getItemMiddleScrollX(mMainMinIndex));
            float y = yFromValue(mMainLowMinValue);
            drawMaxMinValue(canvas, mMainLowMinValue, x, y);

            x = scrollXtoViewX(getItemMiddleScrollX(mMainMaxIndex));
            y = yFromValue(mMainHighMaxValue);
            drawMaxMinValue(canvas, mMainHighMaxValue, x, y);

        }
    }

    /**
     * 画值
     *
     * @param canvas
     * @param position 显示某个点的值
     */
    private void drawValue(Canvas canvas, int position) {
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseLine = (textHeight - fm.bottom - fm.top) / 2;
        float x = 10;
        if (position >= 0 && position < mItemCount) {
            if (mMainDraw != null) {
                float y = textHeight + 15;
                mMainDraw.drawText(canvas, this, position, x, y);
            }
            if (mVolDraw != null && configManager.volumeFlex > 0) {
                float y = mVolRect.top - mChildPadding + textHeight;
                mVolDraw.drawText(canvas, this, position, x, y);
            }
            if (mChildDraw != null) {
                float y = mVolRect.bottom + textHeight;
                mChildDraw.drawText(canvas, this, position, x, y);
            }
        }
    }

    private void drawOrderLines(Canvas canvas) {
        // Get parent container view to access order lines
        if (getParent() instanceof HTKLineContainerView) {
            HTKLineContainerView containerView = (HTKLineContainerView) getParent();
            java.util.Map<String, java.util.Map<String, Object>> orderLines = containerView.getAllOrderLines();

            for (java.util.Map.Entry<String, java.util.Map<String, Object>> entry : orderLines.entrySet()) {
                java.util.Map<String, Object> orderLineData = entry.getValue();

                if (orderLineData.containsKey("price") && orderLineData.containsKey("type")) {
                    double price = 0;
                    Object priceObj = orderLineData.get("price");
                    if (priceObj instanceof Number) {
                        price = ((Number) priceObj).doubleValue();
                    }

                    String type = (String) orderLineData.get("type");

                    // Convert price to Y coordinate
                    float y = yFromValue((float) price);

                    // Only draw if the price is within the visible main chart area
                    if (y >= mMainRect.top && y <= mMainRect.bottom) {
                        // Create a paint for drawing order lines
                        Paint orderLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        orderLinePaint.setStyle(Paint.Style.STROKE);
                        orderLinePaint.setStrokeWidth(2.0f);

                        // Use color property if available, otherwise default to orange
                        int lineColor = Color.parseColor("#FF9500"); // Default orange color
                        if (orderLineData.containsKey("color")) {
                            String colorString = (String) orderLineData.get("color");
                            if (colorString != null) {
                                try {
                                    // Handle RGBA format (convert to ARGB for Android)
                                    if (colorString.length() == 9 && colorString.startsWith("#")) {
                                        // Convert #RRGGBBAA to #AARRGGBB
                                        String rgba = colorString.substring(1);
                                        String argb = "#" + rgba.substring(6) + rgba.substring(0, 6);
                                        lineColor = Color.parseColor(argb);
                                    } else {
                                        lineColor = Color.parseColor(colorString);
                                    }
                                } catch (IllegalArgumentException e) {
                                    // If parsing fails, use default orange color
                                    lineColor = Color.parseColor("#FF9500");
                                }
                            }
                        }
                        orderLinePaint.setColor(lineColor);

                        // Create dashed line effect
                        float[] dashIntervals = {15.0f, 10.0f}; // dash length, gap length
                        orderLinePaint.setPathEffect(new DashPathEffect(dashIntervals, 0));

                        // Calculate line start position (after label if it exists)
                        float lineStartX = 0;

                        // Draw label if available and calculate where line should start
                        if (orderLineData.containsKey("label")) {
                            String label = (String) orderLineData.get("label");
                            if (label != null && !label.isEmpty()) {
                                // Get font size or use default
                                float fontSize = 12.0f;
                                if (orderLineData.containsKey("labelFontSize")) {
                                    Object fontSizeObj = orderLineData.get("labelFontSize");
                                    if (fontSizeObj instanceof Number) {
                                        fontSize = ((Number) fontSizeObj).floatValue();
                                    }
                                }

                                // Get label color (defaults to line color)
                                int labelColor = lineColor;
                                if (orderLineData.containsKey("labelColor")) {
                                    String labelColorString = (String) orderLineData.get("labelColor");
                                    if (labelColorString != null) {
                                        try {
                                            if (labelColorString.length() == 9 && labelColorString.startsWith("#")) {
                                                String rgba = labelColorString.substring(1);
                                                String argb = "#" + rgba.substring(6) + rgba.substring(0, 6);
                                                labelColor = Color.parseColor(argb);
                                            } else {
                                                labelColor = Color.parseColor(labelColorString);
                                            }
                                        } catch (IllegalArgumentException e) {
                                            labelColor = lineColor;
                                        }
                                    }
                                }

                                // Create paint for label text
                                Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                labelPaint.setColor(labelColor);
                                labelPaint.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
                                labelPaint.setTypeface(configManager.font);

                                // Calculate label text bounds
                                Rect labelBounds = new Rect();
                                labelPaint.getTextBounds(label, 0, label.length(), labelBounds);

                                // Calculate description text bounds if available
                                String description = null;
                                Rect descriptionBounds = new Rect();
                                Paint descriptionPaint = null;
                                int descriptionColor = labelColor;
                                float spacing = 0;

                                if (orderLineData.containsKey("labelDescription")) {
                                    description = (String) orderLineData.get("labelDescription");
                                    if (description != null && !description.isEmpty()) {
                                        spacing = 4 * getResources().getDisplayMetrics().density;

                                        // Get description color (defaults to label color)
                                        if (orderLineData.containsKey("labelDescriptionColor")) {
                                            String descriptionColorString = (String) orderLineData.get("labelDescriptionColor");
                                            if (descriptionColorString != null) {
                                                try {
                                                    if (descriptionColorString.length() == 9 && descriptionColorString.startsWith("#")) {
                                                        String rgba = descriptionColorString.substring(1);
                                                        String argb = "#" + rgba.substring(6) + rgba.substring(0, 6);
                                                        descriptionColor = Color.parseColor(argb);
                                                    } else {
                                                        descriptionColor = Color.parseColor(descriptionColorString);
                                                    }
                                                } catch (IllegalArgumentException e) {
                                                    descriptionColor = labelColor;
                                                }
                                            }
                                        }

                                        descriptionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        descriptionPaint.setColor(descriptionColor);
                                        descriptionPaint.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
                                        descriptionPaint.setTypeface(configManager.font);
                                        descriptionPaint.getTextBounds(description, 0, description.length(), descriptionBounds);
                                    }
                                }

                                // Calculate total width (label + space + description)
                                float totalTextWidth = labelBounds.width() + spacing + (description != null ? descriptionBounds.width() : 0);
                                float textHeight = Math.max(labelBounds.height(), description != null ? descriptionBounds.height() : 0);

                                // Pill container dimensions (match close price pill)
                                float density = getResources().getDisplayMetrics().density;
                                float horizontalPadding = 20;
                                float verticalPadding = 14;
                                float outerPadding = 8 * density;

                                float pillWidth = totalTextWidth + horizontalPadding * 2;
                                float pillHeight = textHeight + verticalPadding * 2;

                                float labelX = outerPadding;
                                float labelY = y - pillHeight / 2;

                                // Only draw if there's enough space (left third of screen)
                                if (labelX + pillWidth + outerPadding < getWidth() / 3) {
                                    // Create pill background paint
                                    Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

                                    // Get background color or use default (transparent)
                                    int backgroundColor = Color.TRANSPARENT;
                                    if (orderLineData.containsKey("labelBackgroundColor")) {
                                        String backgroundColorString = (String) orderLineData.get("labelBackgroundColor");
                                        if (backgroundColorString != null) {
                                            try {
                                                // Handle RGBA format (convert to ARGB for Android)
                                                if (backgroundColorString.length() == 9 && backgroundColorString.startsWith("#")) {
                                                    // Convert #RRGGBBAA to #AARRGGBB
                                                    String rgba = backgroundColorString.substring(1);
                                                    String argb = "#" + rgba.substring(6) + rgba.substring(0, 6);
                                                    backgroundColor = Color.parseColor(argb);
                                                } else {
                                                    backgroundColor = Color.parseColor(backgroundColorString);
                                                }
                                            } catch (IllegalArgumentException e) {
                                                backgroundColor = Color.TRANSPARENT;
                                            }
                                        }
                                    }

                                    // Draw pill background
                                    RectF pillRect = new RectF(labelX, labelY, labelX + pillWidth, labelY + pillHeight);
                                    // Use same radius calculation as close price pill
                                    float radius = (verticalPadding * 2 + textHeight) / 2;
                                    pillPaint.setColor(backgroundColor);
                                    pillPaint.setStyle(Paint.Style.FILL);
                                    canvas.drawRoundRect(pillRect, radius, radius, pillPaint);

                                    // Draw pill border with same color as text
                                    pillPaint.setColor(lineColor);
                                    pillPaint.setStyle(Paint.Style.STROKE);
                                    pillPaint.setStrokeWidth(1 * density);
                                    canvas.drawRoundRect(pillRect, radius, radius, pillPaint);

                                    // Draw label and description separately with their respective colors
                                    float textX = labelX + horizontalPadding;
                                    float textY = y + textHeight / 2;

                                    // Draw label text
                                    canvas.drawText(label, textX, textY, labelPaint);

                                    // Draw description text if available
                                    if (description != null && descriptionPaint != null) {
                                        float descriptionX = textX + labelBounds.width() + spacing;
                                        canvas.drawText(description, descriptionX, textY, descriptionPaint);
                                    }

                                    // Set line to start after pill with additional padding
                                    lineStartX = labelX + pillWidth + outerPadding;
                                }
                            }
                        }

                        // Draw horizontal line starting after the label (or from 0 if no label)
                        // Stop before the Y-axis scale area (paddingRight)
                        float lineEndX = getWidth() - configManager.paddingRight;
                        canvas.drawLine(lineStartX, y, lineEndX, y, orderLinePaint);
                    }
                }
            }
        }
    }

    private void drawBuySellMarksForCandlestick(KLineEntity candlestick, int index, float currentPointX, Canvas canvas) {
        // Get parent container view to access buy/sell marks
        if (getParent() instanceof HTKLineContainerView) {
            HTKLineContainerView containerView = (HTKLineContainerView) getParent();

            // Use the preserved timestamp to avoid precision loss
            long candleTime = candlestick.timestamp;

            // Check for both marks to handle collision detection
            java.util.Map<String, Object> buyMark = containerView.getBuyMarkForTime(candleTime);
            java.util.Map<String, Object> sellMark = containerView.getSellMarkForTime(candleTime);

            // Check if both marks exist to handle collision avoidance
            boolean hasBothMarks = buyMark != null && sellMark != null;

            // Draw buy mark
            if (buyMark != null) {
                drawBuySellMark(buyMark, candlestick, index, currentPointX, "buy", canvas, hasBothMarks);
            }

            // Draw sell mark
            if (sellMark != null) {
                drawBuySellMark(sellMark, candlestick, index, currentPointX, "sell", canvas, hasBothMarks);
            }
        }
    }

    private void drawBuySellMark(java.util.Map<String, Object> markData, KLineEntity candlestick, int index, float currentPointX, String type, Canvas canvas, boolean hasBothMarks) {
        // Use the same X coordinate as candlesticks (same as mMainDraw.drawTranslated)
        float candleX = currentPointX;

        // Use the same Y coordinate calculation as MainDraw.drawCandle
        float candleHigh = candlestick.getHighPrice();
        float highY = yFromValue(candleHigh); // Same method used by MainDraw.drawCandle

        // Circle properties - diameter should match candlestick width
        float circleRadius = mPointWidth * 0.4f; // Use 80% of candlestick width for diameter

        // Position both marks above the candlestick, with collision avoidance
        float markCenterY;
        if ("buy".equals(type)) {
            // Buy mark directly above the candlestick
            markCenterY = highY - circleRadius - dp2px(2);
        } else { // sell
            if (hasBothMarks) {
                // If both marks exist, position sell mark one diameter higher
                markCenterY = highY - circleRadius - dp2px(2) - (circleRadius * 2) - dp2px(2);
            } else {
                // If only sell mark exists, position it directly above
                markCenterY = highY - circleRadius - dp2px(2);
            }
        }

        android.util.Log.d("BuySellDebug", "Drawing " + type + " mark - visible at (" + candleX + ", " + markCenterY + ") radius: " + circleRadius);

        // Determine colors based on type, using same colors as candlesticks
        int circleColor;
        if ("buy".equals(type)) {
            circleColor = configManager.increaseColor; // Use same color as increasing candlesticks
        } else { // sell
            circleColor = configManager.decreaseColor; // Use same color as decreasing candlesticks
        }

        // Create paint for circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(circleColor);
        circlePaint.setStyle(Paint.Style.FILL);

        // Draw circle
        canvas.drawCircle(candleX, markCenterY, circleRadius, circlePaint);

        // Draw border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(circleColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2.0f);
        canvas.drawCircle(candleX, markCenterY, circleRadius, borderPaint);

        // Draw text inside circle
        String markText = "buy".equals(type) ? "B" : "S";
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(android.graphics.Color.WHITE);
        textPaint.setTextSize(circleRadius * 1.2f); // Text size proportional to circle
        textPaint.setTypeface(configManager.font);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate text position (center of circle)
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textY = markCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2;

        canvas.drawText(markText, candleX, textY, textPaint);
    }

    public int dp2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 格式化值
     */
    public String formatValue(float value) {
        if (getValueFormatter() == null) {
            setValueFormatter(new ValueFormatter());
        }
        return getValueFormatter().format(value);
    }

    /**
     * 重新计算并刷新线条
     */
    public void notifyChanged() {
        android.util.Log.d("BaseKLineChartView", "notifyChanged() START");
        try {
            android.util.Log.d("BaseKLineChartView", "Setting mItemCount from configManager.modelArray.size(): " + configManager.modelArray.size());
            mItemCount = configManager.modelArray.size();
            mDataLen = mItemCount * mPointWidth;
            android.util.Log.d("BaseKLineChartView", "mItemCount: " + mItemCount + ", mDataLen: " + mDataLen + ", mPointWidth: " + mPointWidth);

            if (isShowChild && mChildDrawPosition == -1) {
                android.util.Log.d("BaseKLineChartView", "Setting up child draw");
                mChildDraw = mChildDraws.get(0);
                mChildDrawPosition = 0;
            }

            if (mItemCount != 0) {
                android.util.Log.d("BaseKLineChartView", "Recalculating mDataLen and checking scroll");
                mDataLen = mItemCount * mPointWidth;
                android.util.Log.d("BaseKLineChartView", "Calling checkAndFixScrollX()");
                checkAndFixScrollX();
                android.util.Log.d("BaseKLineChartView", "checkAndFixScrollX() completed");
            }

            if (mSelectedIndex >= mItemCount) {
                android.util.Log.d("BaseKLineChartView", "Resetting long press: mSelectedIndex(" + mSelectedIndex + ") >= mItemCount(" + mItemCount + ")");
                isLongPress = false;
            }

            android.util.Log.d("BaseKLineChartView", "Calling initRect()");
            initRect();
            android.util.Log.d("BaseKLineChartView", "initRect() completed");

            android.util.Log.d("BaseKLineChartView", "Calling initLottieView()");
            initLottieView();
            android.util.Log.d("BaseKLineChartView", "initLottieView() completed");

            android.util.Log.d("BaseKLineChartView", "Calling invalidate()");
            invalidate();
            android.util.Log.d("BaseKLineChartView", "invalidate() completed");

            android.util.Log.d("BaseKLineChartView", "notifyChanged() COMPLETED SUCCESSFULLY");
        } catch (Exception e) {
            android.util.Log.e("BaseKLineChartView", "ERROR in notifyChanged()", e);
            throw e;
        }
    }

    /**
     * MA/BOLL切换及隐藏
     *
     * @param primaryStatus MA/BOLL/NONE
     */
    public void changeMainDrawType(PrimaryStatus primaryStatus) {
        if (mainDraw != null && mainDraw.getPrimaryStatus() != primaryStatus) {
            mainDraw.setPrimaryStatus(primaryStatus);
            // invalidate();
        }
    }

    private void calculateSelectedX(float x) {
        mSelectedIndex = indexFromScrollX(viewXToScrollX(x));
        if (mSelectedIndex < mStartIndex) {
            mSelectedIndex = mStartIndex;
        }
        if (mSelectedIndex > mStopIndex) {
            mSelectedIndex = mStopIndex;
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        super.onLongPress(e);
        int lastIndex = mSelectedIndex;
        calculateSelectedX(e.getX());
        if (lastIndex != mSelectedIndex) {
            onSelectedChanged(this, getItem(mSelectedIndex), mSelectedIndex);
        }
        invalidate();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        android.util.Log.d("BaseKLineChartView", "onScrollChanged() - l=" + l + ", t=" + t + ", oldl=" + oldl + ", oldt=" + oldt);
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    protected void onScaleChanged(float scale, float oldScale) {
        android.util.Log.d("BaseKLineChartView", "onScaleChanged() - scale=" + scale + ", oldScale=" + oldScale);
        checkAndFixScrollX();
        super.onScaleChanged(scale, oldScale);
    }

    @Override
    protected void checkLeftSideThreshold() {
        // Calculate how many candlesticks are to the left of the current view
        float candlesticksToLeft = mScrollX / configManager.itemWidth;

        if (candlesticksToLeft <= 100) {
            if (!mHasTriggeredLeftSide) {
                mHasTriggeredLeftSide = true;
                onLeftSide();
            }
        } else {
            mHasTriggeredLeftSide = false;
        }
    }

    /**
     * 计算当前的显示区域
     */
    private void calculateValue() {
        android.util.Log.d("BaseKLineChartView", "calculateValue() START");
        try {
            android.util.Log.d("BaseKLineChartView", "isLongPress: " + isLongPress() + ", mSelectedIndex: " + mSelectedIndex);
            if (!isLongPress()) {
                mSelectedIndex = -1;
            }

            android.util.Log.d("BaseKLineChartView", "Resetting min/max values");
            mMainMaxValue = Float.MIN_VALUE;
            mMainMinValue = Float.MAX_VALUE;
            mVolMaxValue = Float.MIN_VALUE;
            mVolMinValue = Float.MAX_VALUE;
            mChildMaxValue = Float.MIN_VALUE;
            mChildMinValue = Float.MAX_VALUE;

            android.util.Log.d("BaseKLineChartView", "Calculating visible range: mWidth=" + mWidth + ", mItemCount=" + mItemCount);
            mStartIndex = Math.min(Math.max(0, indexFromScrollX(viewXToScrollX(0))), mItemCount - 1);
            mStopIndex = Math.max(0, Math.min(indexFromScrollX(viewXToScrollX(mWidth)), mItemCount - 1));
            android.util.Log.d("BaseKLineChartView", "Visible range: mStartIndex=" + mStartIndex + ", mStopIndex=" + mStopIndex);
            mMainMaxIndex = mStartIndex;
            mMainMinIndex = mStartIndex;
            mMainHighMaxValue = Float.MIN_VALUE;
            mMainLowMinValue = Float.MAX_VALUE;

            android.util.Log.d("BaseKLineChartView", "Starting data loop for range " + mStartIndex + " to " + mStopIndex);
            for (int i = mStartIndex; i <= mStopIndex; i++) {
                if (i < 0 || i >= configManager.modelArray.size()) {
                    android.util.Log.w("BaseKLineChartView", "Skipping invalid index: " + i + " (size: " + configManager.modelArray.size() + ")");
                    continue;
                }
                IKLine point = (IKLine) getItem(i);
                if (point == null) {
                    android.util.Log.w("BaseKLineChartView", "getItem(" + i + ") returned null");
                    continue;
                }
            if (mMainDraw != null) {
                mMainMaxValue = Math.max(mMainMaxValue, mMainDraw.getMaxValue(point));
                mMainMinValue = Math.min(mMainMinValue, mMainDraw.getMinValue(point));
                if (mMainHighMaxValue != Math.max(mMainHighMaxValue, point.getHighPrice())) {
                    mMainHighMaxValue = point.getHighPrice();
                    mMainMaxIndex = i;
                }
                if (mMainLowMinValue != Math.min(mMainLowMinValue, point.getLowPrice())) {
                    mMainLowMinValue = point.getLowPrice();
                    mMainMinIndex = i;
                }
            }
            if (mVolDraw != null && configManager.volumeFlex > 0) {
                mVolMaxValue = Math.max(mVolMaxValue, mVolDraw.getMaxValue(point));
                mVolMinValue = Math.min(mVolMinValue, mVolDraw.getMinValue(point));
                // 成交量最小应该是 0 或者比最小成交量大一点点
                mVolMinValue = mVolMinValue - (mVolMaxValue - mVolMinValue) / 10.0f;
                mVolMinValue = Math.max(0, mVolMinValue);
            }
            if (mChildDraw != null) {
                mChildMaxValue = Math.max(mChildMaxValue, mChildDraw.getMaxValue(point));
                mChildMinValue = Math.min(mChildMinValue, mChildDraw.getMinValue(point));
            }
        }
        android.util.Log.d("BaseKLineChartView", "Data loop completed. Final values - MainMax: " + mMainMaxValue + ", MainMin: " + mMainMinValue + ", VolMax: " + mVolMaxValue + ", VolMin: " + mVolMinValue);
        android.util.Log.d("BaseKLineChartView", "calculateValue() COMPLETED");
        } catch (Exception e) {
            android.util.Log.e("BaseKLineChartView", "ERROR in calculateValue()", e);
            throw e;
        }
//        if (mItemCount > 0) {
//            int i = mItemCount - 1;
//            IKLine point = (IKLine)getItem(i);
//            mMainMaxValue = Math.max(mMainMaxValue, mMainDraw.getMaxValue(point));
//            mMainMinValue = Math.min(mMainMinValue, mMainDraw.getMinValue(point));
//            if (mMainHighMaxValue != Math.max(mMainHighMaxValue, point.getHighPrice())) {
//                mMainHighMaxValue = point.getHighPrice();
//                mMainMaxIndex = i;
//            }
//            if (mMainLowMinValue != Math.min(mMainLowMinValue, point.getLowPrice())) {
//                mMainLowMinValue = point.getLowPrice();
//                mMainMinIndex = i;
//            }
//        }

        if (mMainMaxValue != mMainMinValue) {
//            float padding = (mMainMaxValue - mMainMinValue) * 0.05f;
//            padding = 0;
//            mMainMaxValue += padding;
//            mMainMinValue -= padding;
        } else {
            //当最大值和最小值都相等的时候 分别增大最大值和 减小最小值
//            mMainMaxValue += Math.abs(mMainMaxValue * 0.05f);
//            mMainMinValue -= Math.abs(mMainMinValue * 0.05f);
//            if (mMainMaxValue == 0) {
//                mMainMaxValue = 1;
//            }
        }

        if (Math.abs(mVolMaxValue) < 0.01) {
            mVolMaxValue = 15.00f;
        }

        // big number replace
//        if (Math.abs(mChildMaxValue) < 0.01 && Math.abs(mChildMinValue) < 0.01) {
//            mChildMaxValue = 1f;
//        } else
            if (mChildMaxValue.equals(mChildMinValue)) {
            //当最大值和最小值都相等的时候 分别增大最大值和 减小最小值
            mChildMaxValue += Math.abs(mChildMaxValue * 0.05f);
            mChildMinValue -= Math.abs(mChildMinValue * 0.05f);
            if (mChildMaxValue == 0) {
                mChildMaxValue = 1f;
            }
        }

        if (isWR) {
            mChildMaxValue = 0f;
            if (Math.abs(mChildMinValue) < 0.01)
                mChildMinValue = -10.00f;
        }
        mMainScaleY = mMainRect.height() * 1f / (mMainMaxValue - mMainMinValue);
        if (configManager.volumeFlex > 0) {
            mVolScaleY = mVolRect.height() * 1f / (mVolMaxValue - mVolMinValue);
        } else {
            mVolScaleY = 0;
        }
        if (mChildRect != null)
            mChildScaleY = mChildRect.height() * 1f / (mChildMaxValue - mChildMinValue);
        if (mAnimator.isRunning()) {
            float value = (float) mAnimator.getAnimatedValue();
            mStopIndex = mStartIndex + Math.round(value * (mStopIndex - mStartIndex));
        }
    }

    @Override
    public int getMinScrollX() {
//        return (int) -(mOverScrollRange / mScaleX);
        return 0;
    }

    public int getMaxScrollX() {
        int contentWidth = (int) Math.max((mDataLen - (mWidth - configManager.paddingRight) / mScaleX), 0);
        return contentWidth;
    }

    /**
     * 在主区域画线
     *
     * @param startX    开始点的横坐标
     * @param stopX     开始点的值
     * @param stopX     结束点的横坐标
     * @param stopValue 结束点的值
     */
    public void drawMainLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, yFromValue(startValue), stopX, yFromValue(stopValue), paint);
    }

    /**
     * 在子区域画线
     *
     * @param startX     开始点的横坐标
     * @param startValue 开始点的值
     * @param stopX      结束点的横坐标
     * @param stopValue  结束点的值
     */
    public void drawChildLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, getChildY(startValue), stopX, getChildY(stopValue), paint);
    }

    /**
     * 在子区域画线
     *
     * @param startX     开始点的横坐标
     * @param startValue 开始点的值
     * @param stopX      结束点的横坐标
     * @param stopValue  结束点的值
     */
    public void drawVolLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, getVolY(startValue), stopX, getVolY(stopValue), paint);
    }

    /**
     * 根据索引获取实体
     *
     * @param position 索引值
     * @return
     */
    public KLineEntity getItem(int position) {
        try {
            if (position < 0 || position >= configManager.modelArray.size()) {
                android.util.Log.w("BaseKLineChartView", "getItem() - Invalid position: " + position + " (size: " + configManager.modelArray.size() + ")");
                return null;
            }
            KLineEntity item = configManager.modelArray.get(position);
            if (item == null) {
                android.util.Log.w("BaseKLineChartView", "getItem() - Item at position " + position + " is null");
            }
            return item;
        } catch (Exception e) {
            android.util.Log.e("BaseKLineChartView", "ERROR in getItem(" + position + ")", e);
            return null;
        }
    }

    /**
     * 根据索引索取x坐标
     *
     * @param position 索引值
     * @return
     */
    public float getItemMiddleScrollX(int position) {
        return position * mPointWidth + mPointWidth * 0.5f;
    }


    /**
     * 设置当前子图
     *
     * @param position
     */
    public void setChildDraw(int position) {
        if (mChildDrawPosition != position) {
            if (!isShowChild) {
                isShowChild = true;
                // initRect();
            }
            mChildDraw = mChildDraws.get(position);
            mChildDrawPosition = position;
            isWR = position == 5;
            // invalidate();
        }
    }

    /**
     * 隐藏子图
     */
    public void hideChildDraw() {
        mChildDrawPosition = -1;
        isShowChild = false;
        mChildDraw = null;
        // initRect();
        // invalidate();
    }

    /**
     * 给子区域添加画图方法
     *
     * @param childDraw IChartDraw
     */
    public void addChildDraw(IChartDraw childDraw) {
        mChildDraws.add(childDraw);
    }

    /**
     * 获取ValueFormatter
     *
     * @return
     */
    public IValueFormatter getValueFormatter() {
        return mValueFormatter;
    }

    /**
     * 设置ValueFormatter
     *
     * @param valueFormatter value格式化器
     */
    public void setValueFormatter(IValueFormatter valueFormatter) {
        this.mValueFormatter = valueFormatter;
    }

    /**
     * 获取DatetimeFormatter
     *
     * @return 时间格式化器
     */
    public IDateTimeFormatter getDateTimeFormatter() {
        return mDateTimeFormatter;
    }

    /**
     * 设置dateTimeFormatter
     *
     * @param dateTimeFormatter 时间格式化器
     */
    public void setDateTimeFormatter(IDateTimeFormatter dateTimeFormatter) {
        mDateTimeFormatter = dateTimeFormatter;
    }

    /**
     * 格式化时间
     *
     * @param date
     */
    public String formatDateTime(Date date) {
        if (getDateTimeFormatter() == null) {
            setDateTimeFormatter(new TimeFormatter());
        }
        return getDateTimeFormatter().format(date);
    }

    /**
     * 获取主区域的 IChartDraw
     *
     * @return IChartDraw
     */
    public IChartDraw getMainDraw() {
        return mMainDraw;
    }

    /**
     * 设置主区域的 IChartDraw
     *
     * @param mainDraw IChartDraw
     */
    public void setMainDraw(IChartDraw mainDraw) {
        mMainDraw = mainDraw;
        this.mainDraw = (MainDraw) mMainDraw;
    }

    public IChartDraw getVolDraw() {
        return mVolDraw;
    }

    public void setVolDraw(IChartDraw mVolDraw) {
        this.mVolDraw = mVolDraw;
    }

    /**
     * 二分查找当前值的index
     *
     * @return
     */
    public int indexFromScrollX(float scrollX) {
        return Math.max(0, Math.min((int)Math.floor(scrollX / mPointWidth), mItemCount - 1));
    }

    /**
     * 开始动画
     */
    public void startAnimation() {
        if (mAnimator != null) {
            mAnimator.start();
        }
    }

    /**
     * 设置动画时间
     */
    public void setAnimationDuration(long duration) {
        if (mAnimator != null) {
            mAnimator.setDuration(duration);
        }
    }

    /**
     * 设置表格行数
     */
    public void setGridRows(int gridRows) {
        if (gridRows < 1) {
            gridRows = 1;
        }
        mGridRows = gridRows;
    }

    /**
     * 设置表格列数
     */
    public void setGridColumns(int gridColumns) {
        if (gridColumns < 1) {
            gridColumns = 1;
        }
        mGridColumns = gridColumns;
    }

    /**
     * view中的x转化为scrollX
     *
     * @param x
     * @return
     */
    public float viewXToScrollX(float x) {
        return mScrollX +  x / mScaleX;
    }

    /**
     * scrollX转化为view中的x
     *
     * @param viewx
     * @return
     */
    public float scrollXtoViewX(float viewx) {
        return (viewx - mScrollX) * mScaleX;
    }

    /**
     * 获取上方padding
     */
    public float getTopPadding() {
        return mTopPadding;
    }

    /**
     * 获取上方padding
     */
    public float getChildPadding() {
        return mChildPadding;
    }

    /**
     * 获取子试图上方padding
     */
    public float getmChildScaleYPadding() {
        return mChildPadding;
    }

    /**
     * 获取图的宽度
     *
     * @return
     */
    public int getChartWidth() {
        return mWidth;
    }

    public int getScrollOffset() {
        return mScrollX;
    }

    /**
     * 是否长按
     */
    public boolean isLongPress() {
        return isLongPress;
    }

    /**
     * 获取选择索引
     */
    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public Rect getChildRect() {
        return mChildRect;
    }

    public Rect getVolRect() {
        return mVolRect;
    }

    /**
     * 设置选择监听
     */
    public void setOnSelectedChangedListener(OnSelectedChangedListener l) {
        this.mOnSelectedChangedListener = l;
    }

    public void onSelectedChanged(BaseKLineChartView view, Object point, int index) {
        if (this.mOnSelectedChangedListener != null) {
            mOnSelectedChangedListener.onSelectedChanged(view, point, index);
        }
    }

    /**
     * 数据是否充满屏幕
     *
     * @return
     */
    public boolean isFullScreen() {
        return mDataLen >= mWidth / mScaleX;
    }

    /**
     * 设置超出右方后可滑动的范围
     */
    public void setOverScrollRange(float overScrollRange) {
        if (overScrollRange < 0) {
            overScrollRange = 0;
        }
        mOverScrollRange = overScrollRange;
    }

    /**
     * 设置上方padding
     *
     * @param topPadding
     */
    public void setTopPadding(int topPadding) {
        mTopPadding = topPadding;
    }

    /**
     * 设置下方padding
     *
     * @param bottomPadding
     */
    public void setBottomPadding(int bottomPadding) {
        mBottomPadding = bottomPadding;
    }

    /**
     * 设置表格线宽度
     */
    public void setGridLineWidth(float width) {
        mGridPaint.setStrokeWidth(width);
    }

    /**
     * 设置表格线颜色
     */
    public void setGridLineColor(int color) {
        mGridPaint.setColor(color);
    }

    /**
     * 设置选择器横线宽度
     */
    public void setSelectedXLineWidth(float width) {
        mSelectedXLinePaint.setStrokeWidth(width);
    }

    /**
     * 设置选择器横线颜色
     */
    public void setSelectedXLineColor(int color) {
        mSelectedXLinePaint.setColor(color);
    }

    /**
     * 设置选择器竖线宽度
     */
    public void setSelectedYLineWidth(float width) {
        mSelectedYLinePaint.setStrokeWidth(width);
    }

    /**
     * 设置选择器竖线颜色
     */
    public void setSelectedYLineColor(int color) {
        mSelectedYLinePaint.setColor(color);
    }

    /**
     * 设置文字颜色
     */
    public void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    public void setTextFontFamily(String fontFamily) {
        Typeface typeface = HTKLineConfigManager.findFont(getContext(), fontFamily);
        mGridPaint.setTypeface(typeface);

        mTextPaint.setTypeface(typeface);

        mMaxMinPaint.setTypeface(typeface);

        mBackgroundPaint.setTypeface(typeface);

        mSelectedXLinePaint.setTypeface(typeface);

        mSelectedYLinePaint.setTypeface(typeface);

        mSelectPointPaint.setTypeface(typeface);

        mSelectCenterPaint.setTypeface(typeface);

        mSelectCenterBackgroundPaint.setTypeface(typeface);

        mSelectorFramePaint.setTypeface(typeface);

        mClosePriceLinePaint.setTypeface(typeface);

        mClosePricePointPaint.setTypeface(typeface);

        mClosePriceTrianglePaint.setTypeface(typeface);

        mClosePriceRightTextPaint.setTypeface(typeface);
    }

    /**
     * 设置文字大小
     */
    public void setTextSize(float textSize) {
        mTextPaint.setTextSize(textSize);
        mClosePriceRightTextPaint.setTextSize(textSize);
    }

    /**
     * 设置最大值/最小值文字颜色
     */
    public void setMTextColor(int color) {
        mMaxMinPaint.setColor(color);
        mSelectedXLinePaint.setColor(color);
        mSelectorFramePaint.setColor(color);
    }

    /**
     * 设置最大值/最小值文字大小
     */
    public void setMTextSize(float textSize) {
        mMaxMinPaint.setTextSize(textSize);
    }

    /**
     * 设置背景颜色
     */
    public void setBackgroundColor(int color) {
        mBackgroundPaint.setColor(color);
    }

    /**
     * 设置选中point 值显示背景
     */
    public void setSelectPointColor(int color) {
        mSelectPointPaint.setColor(color);
    }

    /**
     * 选中点变化时的监听
     */
    public interface OnSelectedChangedListener {
        /**
         * 当选点中变化时
         *
         * @param view  当前view
         * @param point 选中的点
         * @param index 选中点的索引
         */
        void onSelectedChanged(BaseKLineChartView view, Object point, int index);
    }

    /**
     * 获取文字大小
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * 获取曲线宽度
     */
    public float getLineWidth() {
        return mLineWidth;
    }

    /**
     * 设置曲线的宽度
     */
    public void setLineWidth(float lineWidth) {
        mLineWidth = lineWidth;
    }

    /**
     * 设置每个点的宽度
     */
    public void setPointWidth(float pointWidth) {
        mPointWidth = pointWidth;
    }

    /**
     * 获取每个点的宽度
     */
    public float getPointWidth() {
        return mPointWidth;
    }

    public Paint getGridPaint() {
        return mGridPaint;
    }

    public Paint getTextPaint() {
        return mTextPaint;
    }

    public Paint getBackgroundPaint() {
        return mBackgroundPaint;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        // Check if touch is on close price label
        boolean isOnClosePriceLabel = mClosePriceLabelFrame.contains(x, y);

        android.util.Log.d("BaseKLineChartView", "tapLocation: (" + x + ", " + y + ")");
        android.util.Log.d("BaseKLineChartView", "closePriceLabelFrame: " + mClosePriceLabelFrame);
        android.util.Log.d("BaseKLineChartView", "isOnClosePriceLabel: " + isOnClosePriceLabel);

        // Trigger the chart touch callback
        if (configManager.onChartTouch != null) {
            configManager.onChartTouch.invoke(x, y, isOnClosePriceLabel);
        }

        return true;
    }

    public void smoothScrollToEnd() {
        int endScrollX = getMaxScrollX();
        int currentScrollX = getScrollOffset();
        int distance = endScrollX - currentScrollX;

        // android.util.Log.d("BaseKLineChartView", "smoothScrollToEnd DEBUG:");
        // android.util.Log.d("BaseKLineChartView", "  mDataLen=" + mDataLen + ", mItemCount=" + mItemCount + ", mPointWidth=" + mPointWidth);
        // android.util.Log.d("BaseKLineChartView", "  mWidth=" + mWidth + ", mScaleX=" + mScaleX + ", paddingRight=" + configManager.paddingRight);
        // android.util.Log.d("BaseKLineChartView", "  current=" + currentScrollX + ", end=" + endScrollX + ", distance=" + distance);

        // Always scroll to end position, regardless of current position
        // This ensures we go to the rightmost position to show the latest data
        setScrollX(endScrollX);
        // android.util.Log.d("BaseKLineChartView", "Set scroll position to end: " + endScrollX);
    }

    // Public getter methods for accessing protected fields
    public float getClosePriceLabelFrameLeft() {
        return mClosePriceLabelFrame.left;
    }

    public float getClosePriceLabelFrameTop() {
        return mClosePriceLabelFrame.top;
    }

    public float getClosePriceLabelFrameWidth() {
        return mClosePriceLabelFrame.width();
    }

    public float getClosePriceLabelFrameHeight() {
        return mClosePriceLabelFrame.height();
    }

    /**
     * Reset the scroll left trigger flag to allow new triggers
     */
    public void resetScrollLeftTrigger() {
        mHasTriggeredLeftSide = false;
        android.util.Log.d("BaseKLineChartView", "Reset scroll left trigger flag");
    }

}
