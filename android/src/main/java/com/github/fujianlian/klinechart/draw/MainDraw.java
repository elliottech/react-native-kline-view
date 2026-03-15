package com.github.fujianlian.klinechart.draw;

import android.content.Context;
import android.graphics.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.fujianlian.klinechart.*;
import com.github.fujianlian.klinechart.base.IChartDraw;
import com.github.fujianlian.klinechart.base.IValueFormatter;
import com.github.fujianlian.klinechart.entity.ICandle;
import com.github.fujianlian.klinechart.formatter.ValueFormatter;
import com.github.fujianlian.klinechart.utils.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main chart implementation class
 * Created by tifezh on 2016/6/14.
 */
public class MainDraw implements IChartDraw<ICandle> {

    private float mCandleWidth = 0;
    private float mCandleLineWidth = 0;

    private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mRedWickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mGreenWickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ma5Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ma10Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ma30Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint minuteGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mSelectorBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Context mContext;

    private boolean mCandleSolid = true;

    private PrimaryStatus primaryStatus = PrimaryStatus.MA;
    private KLineChartView kChartView;

    public MainDraw(BaseKLineChartView view) {
        Context context = view.getContext();
        kChartView = (KLineChartView) view;
        mContext = context;


        mLinePaint.setColor(ContextCompat.getColor(context, R.color.chart_line));
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStyle(Paint.Style.STROKE);


        minuteGradientPaint.setStrokeJoin(Paint.Join.ROUND);
        minuteGradientPaint.setStrokeCap(Paint.Cap.ROUND);
        minuteGradientPaint.setStyle(Paint.Style.FILL);
    }

    public void reloadColor(BaseKLineChartView view) {
        mRedPaint.setColor(view.configManager.increaseColor);
        mGreenPaint.setColor(view.configManager.decreaseColor);
        mRedWickPaint.setColor(view.configManager.increaseWickColor);
        mGreenWickPaint.setColor(view.configManager.decreaseWickColor);
        mLinePaint.setColor(view.configManager.minuteLineColor);
    }

    public void setPrimaryStatus(PrimaryStatus primaryStatus) {
        this.primaryStatus = primaryStatus;
    }

    public PrimaryStatus getPrimaryStatus() {
        return primaryStatus;
    }


    public void drawMinuteMinute(float top, int startIndex, float bottom, int stopIndex, @NonNull Canvas canvas, @NonNull BaseKLineChartView view) {
        if (!view.isMinute) {
            return;
        }
        float r = mCandleWidth / 2;
        LinearGradient linearGradient = new LinearGradient(
                0,
                0,
                0,
                bottom - top,
                view.configManager.minuteGradientColorList,
                view.configManager.minuteGradientLocationList,
                Shader.TileMode.CLAMP
        );
//        minuteGradientPaint.setColor(Color.BLUE);
        minuteGradientPaint.setShader(linearGradient);

        Path path = new Path();
        for (int i = startIndex; i <= stopIndex; i++) {
            ICandle currentPoint = (ICandle) view.getItem(i);
            float currentX = view.getItemMiddleScrollX(i);
            float currentY = view.yFromValue(currentPoint.getClosePrice());
            ICandle lastPoint = i == 0 ? currentPoint : (ICandle) view.getItem(i - 1);

            float lastX = i == 0 ? currentX : view.getItemMiddleScrollX(i - 1);
            float lastY = view.yFromValue(lastPoint.getClosePrice());
            float centerX = (currentX - lastX) / 2 + lastX;
            float centerY = (currentY - lastY) / 2 + lastY;
            if (i == startIndex) {
                path.moveTo(lastX, lastY);
            }
            path.cubicTo(centerX, lastY, centerX, currentY, currentX, currentY);
        }
        Path gradientPath = new Path(path);
        gradientPath.lineTo(view.getItemMiddleScrollX(stopIndex), view.getMainBottom());
        gradientPath.lineTo(view.getItemMiddleScrollX(startIndex), view.getMainBottom());
//        gradientPath.lineTo(view.getX(startIndex), top);
        gradientPath.close();
        canvas.drawPath(gradientPath, minuteGradientPaint);
        canvas.drawPath(path, mLinePaint);

    }

    @Override
    public void drawTranslated(@Nullable ICandle lastPoint, @NonNull ICandle curPoint, float lastX, float curX, @NonNull Canvas canvas, @NonNull BaseKLineChartView view, int position) {
        if (view.isMinute) {
            return;
        }
        drawCandle(view, canvas, curX, curPoint.getHighPrice(), curPoint.getLowPrice(), curPoint.getOpenPrice(), curPoint.getClosePrice());
        if (primaryStatus == PrimaryStatus.MA) {
            KLineEntity lastItem = (KLineEntity) lastPoint;
            KLineEntity currentItem = (KLineEntity) curPoint;
            // Use the embedded indicator data from candlestick rather than config manager's empty list
            int maCount = Math.min(currentItem.maList.size(), lastItem.maList.size());
            for (int i = 0; i < maCount; i ++) {
                HTKLineTargetItem currentTargetItem = (HTKLineTargetItem) currentItem.maList.get(i);
                HTKLineTargetItem lastTargetItem = (HTKLineTargetItem) lastItem.maList.get(i);
                // Use the target item's index for color selection
                int colorIndex = Math.min(currentTargetItem.index, view.configManager.targetColorList.length - 1);
                primaryPaint.setColor(view.configManager.targetColorList[colorIndex]);
                view.drawMainLine(canvas, this.primaryPaint, lastX, lastTargetItem.value, curX, currentTargetItem.value);
            }
        } else if (primaryStatus == PrimaryStatus.BOLL) {
            // Draw BOLL
            if (lastPoint.getMb() != 0) {
                primaryPaint.setColor(view.configManager.targetColorList[0]);
                view.drawMainLine(canvas, primaryPaint, lastX, lastPoint.getMb(), curX, curPoint.getMb());
            }
            if (lastPoint.getUp() != 0) {
                primaryPaint.setColor(view.configManager.targetColorList[1]);
                view.drawMainLine(canvas, primaryPaint, lastX, lastPoint.getUp(), curX, curPoint.getUp());
            }
            if (lastPoint.getDn() != 0) {
                primaryPaint.setColor(view.configManager.targetColorList[2]);
                view.drawMainLine(canvas, primaryPaint, lastX, lastPoint.getDn(), curX, curPoint.getDn());
            }
        }

    }

    @Override
    public void drawText(@NonNull Canvas canvas, @NonNull BaseKLineChartView view, int position, float x, float y) {
        KLineEntity point = (KLineEntity) view.getItem(position);
        String text = "";
        String space = "  ";
        if (view.isMinute) {

        } else {
            if (primaryStatus == PrimaryStatus.MA) {
                // Use the embedded indicator data from candlestick rather than config manager's empty list
                for (int i = 0; i < point.maList.size(); i ++) {
                    HTKLineTargetItem targetItem = (HTKLineTargetItem) point.maList.get(i);
                    // Use the target item's index for color selection
                    int colorIndex = Math.min(targetItem.index, view.configManager.targetColorList.length - 1);
                    this.primaryPaint.setColor(view.configManager.targetColorList[colorIndex]);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MA");
                    stringBuilder.append(targetItem.title);
                    stringBuilder.append(":");
                    stringBuilder.append(view.formatValue(targetItem.value));
                    stringBuilder.append(space);
                    text = stringBuilder.toString();
                    canvas.drawText(text, x, y, this.primaryPaint);
                    x += this.primaryPaint.measureText(text);
                }
            } else if (primaryStatus == PrimaryStatus.BOLL) {
                if (point.getMb() != 0) {
                    text = "BOLL:" + view.formatValue(point.getMb()) + space;
                    this.primaryPaint.setColor(view.configManager.targetColorList[0]);
                    canvas.drawText(text, x, y, primaryPaint);
                    x += ma5Paint.measureText(text);
                    text = "UB:" + view.formatValue(point.getUp()) + space;
                    this.primaryPaint.setColor(view.configManager.targetColorList[1]);
                    canvas.drawText(text, x, y, primaryPaint);
                    x += ma10Paint.measureText(text);
                    text = "LB:" + view.formatValue(point.getDn());
                    this.primaryPaint.setColor(view.configManager.targetColorList[2]);
                    canvas.drawText(text, x, y, primaryPaint);
                }
            }
        }
    }

    public float findIsMaxValue(ICandle point, final boolean isMax) {
        final KLineEntity item = (KLineEntity) point;
        ArrayList<Float> valueList = new ArrayList<Float>(){{
            add(item.getHighPrice());
            add(item.getLowPrice());
        }};
        if (primaryStatus == PrimaryStatus.MA) {
            valueList.add(item.targetListISMax(item.maList, isMax));
        } else if (primaryStatus == PrimaryStatus.BOLL) {
            valueList.add(item.getMb());
            valueList.add(item.getUp());
            valueList.add(item.getDn());
        }
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (float value: valueList) {
            if (isMax) {
                max = Math.max(max, value);
            } else {
                min = Math.min(min, value);
            }
        }
        if (isMax) {
            return max;
        }
        return min;

    }

    @Override
    public float getMaxValue(final ICandle point) {
        return findIsMaxValue(point, true);
    }

    @Override
    public float getMinValue(ICandle point) {
        return findIsMaxValue(point, false);
    }

    @Override
    public IValueFormatter getValueFormatter() {
        return new ValueFormatter();
    }

    /**
     * Draw Candle
     *
     * @param canvas
     * @param x      x-axis coordinate
     * @param high   highest price
     * @param low    lowest price
     * @param open   opening price
     * @param close  closing price
     */
    private void drawCandle(BaseKLineChartView view, Canvas canvas, float x, float high, float low, float open, float close) {
        high = view.yFromValue(high);
        low = view.yFromValue(low);
        open = view.yFromValue(open);
        close = view.yFromValue(close);
        float r = mCandleWidth / 2;
        float lineR = mCandleLineWidth / 2;
        float cornerRadius = view.configManager.candleCornerRadius;

        if (open > close) {
            // Solid
            if (mCandleSolid) {
                // Draw wick first (behind body)
                canvas.drawRect(x - lineR, high, x + lineR, low, mRedWickPaint);
                if (cornerRadius > 0) {
                    // Draw rounded rectangle for candle body
                    RectF rectF = new RectF(x - r, close, x + r, open);
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, mRedPaint);
                } else {
                    // Draw regular rectangle (original behavior)
                    canvas.drawRect(x - r, close, x + r, open, mRedPaint);
                }
            } else {
                mRedWickPaint.setStrokeWidth(mCandleLineWidth);
                canvas.drawLine(x, high, x, close, mRedWickPaint);
                canvas.drawLine(x, open, x, low, mRedWickPaint);
                canvas.drawLine(x - r + lineR, open, x - r + lineR, close, mRedPaint);
                canvas.drawLine(x + r - lineR, open, x + r - lineR, close, mRedPaint);
                mRedPaint.setStrokeWidth(mCandleLineWidth * view.getScaleX());
                canvas.drawLine(x - r, open, x + r, open, mRedPaint);
                canvas.drawLine(x - r, close, x + r, close, mRedPaint);
            }

        } else if (open < close) {
            // Draw wick first (behind body)
            canvas.drawRect(x - lineR, high, x + lineR, low, mGreenWickPaint);
            if (cornerRadius > 0) {
                // Draw rounded rectangle for candle body
                RectF rectF = new RectF(x - r, open, x + r, close);
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, mGreenPaint);
            } else {
                // Draw regular rectangle (original behavior)
                canvas.drawRect(x - r, open, x + r, close, mGreenPaint);
            }
        } else {
            // Draw wick first (behind body)
            canvas.drawRect(x - lineR, high, x + lineR, low, mRedWickPaint);
            if (cornerRadius > 0) {
                // Draw rounded rectangle for candle body
                RectF rectF = new RectF(x - r, open, x + r, close + 1);
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, mRedPaint);
            } else {
                // Draw regular rectangle (original behavior)
                canvas.drawRect(x - r, open, x + r, close + 1, mRedPaint);
            }
        }
    }

    /**
     * Draw selector
     *
     * @param view
     * @param canvas
     */
    public void drawSelector(final BaseKLineChartView view, Canvas canvas) {
        if (view.isMinute) {
            return;
        }
        Paint.FontMetrics metrics = mSelectorTextPaint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;

        final int index = view.getSelectedIndex();
        float padding = ViewUtil.Dp2Px(mContext, 7);
        float lineHeight = ViewUtil.Dp2Px(mContext, 8);
        float margin = ViewUtil.Dp2Px(mContext, 5);
        float width = 0;
        float left;
        float top = margin + view.getTopPadding();
        final KLineEntity point = (KLineEntity) view.getItem(index);

        // Get the basic selectedItemList
        List<Map<String, Object>> itemList = new ArrayList<>(point.selectedItemList);

        // Check if there are buy/sell marks for this candlestick and add them to tooltip
        if (view.getParent() instanceof com.github.fujianlian.klinechart.container.HTKLineContainerView) {
            com.github.fujianlian.klinechart.container.HTKLineContainerView containerView =
                (com.github.fujianlian.klinechart.container.HTKLineContainerView) view.getParent();

            long candleTime = point.timestamp;

            // Add buy mark tooltip if exists
            java.util.Map<String, Object> buyMark = containerView.getBuyMarkForTime(candleTime);
            if (buyMark != null && buyMark.containsKey("tooltipText")) {
                Map<String, Object> buyTooltipItem = new HashMap<>();
                buyTooltipItem.put("title", "");
                buyTooltipItem.put("detail", buyMark.get("tooltipText"));
                itemList.add(buyTooltipItem);
            }

            // Add sell mark tooltip if exists
            java.util.Map<String, Object> sellMark = containerView.getSellMarkForTime(candleTime);
            if (sellMark != null && sellMark.containsKey("tooltipText")) {
                Map<String, Object> sellTooltipItem = new HashMap<>();
                sellTooltipItem.put("title", "");
                sellTooltipItem.put("detail", sellMark.get("tooltipText"));
                itemList.add(sellTooltipItem);
            }
        }

        float height = padding * 2 + (textHeight + lineHeight) * itemList.size() - lineHeight;

        for (int i = 0; i < itemList.size(); i ++) {
            Map<String, Object> map = itemList.get(i);
            String leftString = (String) map.get("title");
            String rightString = (String) map.get("detail");
            width = Math.max(width, mSelectorTextPaint.measureText(leftString + rightString));
        }

        width += padding * 2;
        width = Math.max(width, view.configManager.panelMinWidth);

        float x = view.scrollXtoViewX(view.getItemMiddleScrollX(index));
        if (x > view.getChartWidth() / 2) {
            left = margin;
        } else {
            left = view.getChartWidth() - width - margin;
        }

        RectF r = new RectF(left, top, left + width, top + height);

        mSelectorBackgroundPaint.setStyle(Paint.Style.FILL);
        mSelectorBackgroundPaint.setColor(view.configManager.panelBackgroundColor);
        canvas.drawRoundRect(r, 5, 5, mSelectorBackgroundPaint);

        mSelectorBackgroundPaint.setStyle(Paint.Style.STROKE);
        mSelectorBackgroundPaint.setStrokeWidth(1);
        mSelectorBackgroundPaint.setColor(view.configManager.panelBorderColor);
        canvas.drawRoundRect(r, 5, 5, mSelectorBackgroundPaint);

        float y = top + padding + (textHeight - metrics.bottom - metrics.top) / 2;

        for (int i = 0; i < itemList.size(); i ++) {
            Map<String, Object> map = itemList.get(i);
            String leftString = (String) map.get("title");
            String rightString = (String) map.get("detail");
            mSelectorTextPaint.setTextSize(view.configManager.panelTextFontSize);

            int paintColor = view.configManager.candleTextColor;
            mSelectorTextPaint.setColor(paintColor);
            canvas.drawText(leftString, left + padding, y, mSelectorTextPaint);
            if (map.get("color") != null) {
                paintColor = new Integer(((Number) map.get("color")).intValue());
                mSelectorTextPaint.setColor(paintColor);
            }
            canvas.drawText(rightString, r.right - mSelectorTextPaint.measureText(rightString) - padding, y, mSelectorTextPaint);
            y += textHeight + lineHeight;
        }

    }

    /**
     * Set candle width
     *
     * @param candleWidth
     */
    public void setCandleWidth(float candleWidth) {
        mCandleWidth = candleWidth;
    }

    /**
     * Set candle line width
     *
     * @param candleLineWidth
     */
    public void setCandleLineWidth(float candleLineWidth) {
        mCandleLineWidth = candleLineWidth;
    }

    /**
     * Set MA5 color
     *
     * @param color
     */
    public void setMa5Color(int color) {
        this.ma5Paint.setColor(color);
    }

    /**
     * Set MA10 color
     *
     * @param color
     */
    public void setMa10Color(int color) {
        this.ma10Paint.setColor(color);
    }

    /**
     * Set MA30 color
     *
     * @param color
     */
    public void setMa30Color(int color) {
        this.ma30Paint.setColor(color);
    }

    /**
     * Set selector text color
     *
     * @param color
     */
    public void setSelectorTextColor(int color) {
        mSelectorTextPaint.setColor(color);
    }

    /**
     * Set selector text size
     *
     * @param textSize
     */
    public void setSelectorTextSize(float textSize) {
        mSelectorTextPaint.setTextSize(textSize);
    }

    /**
     * Set selector background
     *
     * @param color
     */
    public void setSelectorBackgroundColor(int color) {
        mSelectorBackgroundPaint.setColor(color);
    }

    /**
     * Set curve width
     */
    public void setLineWidth(float width) {
        ma30Paint.setStrokeWidth(width);
        ma10Paint.setStrokeWidth(width);
        ma5Paint.setStrokeWidth(width);
        primaryPaint.setStrokeWidth(width);
        mLinePaint.setStrokeWidth(width);
    }

    /**
     * Set text size
     */
    public void setTextSize(float textSize) {
        ma30Paint.setTextSize(textSize);
        ma10Paint.setTextSize(textSize);
        ma5Paint.setTextSize(textSize);
        primaryPaint.setTextSize(textSize);
    }

    public void setTextFontFamily(String fontFamily) {
        Typeface typeface = HTKLineConfigManager.findFont(mContext, fontFamily);
        mLinePaint.setTypeface(typeface);
        mRedPaint.setTypeface(typeface);
        mGreenPaint.setTypeface(typeface);
        mRedWickPaint.setTypeface(typeface);
        mGreenWickPaint.setTypeface(typeface);
        ma5Paint.setTypeface(typeface);
        ma10Paint.setTypeface(typeface);
        ma30Paint.setTypeface(typeface);
        primaryPaint.setTypeface(typeface);

        minuteGradientPaint.setTypeface(typeface);

        mSelectorTextPaint.setTypeface(typeface);
        mSelectorBackgroundPaint.setTypeface(typeface);
    }

    /**
     * Whether candles are solid
     */
    public void setCandleSolid(boolean candleSolid) {
        mCandleSolid = candleSolid;
    }

}
