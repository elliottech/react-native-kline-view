package com.github.fujianlian.klinechart.container;


import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import com.facebook.react.bridge.*;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import com.github.fujianlian.klinechart.HTKLineConfigManager;
import com.github.fujianlian.klinechart.KLineChartView;
import com.github.fujianlian.klinechart.KLineEntity;
import com.github.fujianlian.klinechart.RNKLineView;
import com.github.fujianlian.klinechart.HTKLineTargetItem;
import com.github.fujianlian.klinechart.formatter.DateFormatter;


public class HTKLineContainerView extends RelativeLayout {

    private ThemedReactContext reactContext;

    public HTKLineConfigManager configManager = new HTKLineConfigManager();

    public KLineChartView klineView;

    public HTShotView shotView;

    // Order line management
    private Map<String, Map<String, Object>> orderLines = new HashMap<>();

    // Buy/sell mark management - indexed by timestamp for O(1) lookup
    private Map<Long, Map<String, Object>> buyMarks = new HashMap<>();
    private Map<Long, Map<String, Object>> sellMarks = new HashMap<>();
    private Map<String, Map<String, Object>> buySellMarks = new HashMap<>(); // Keep for compatibility

    public HTKLineContainerView(ThemedReactContext context) {
        super(context);
        this.reactContext = context;
        klineView = new KLineChartView(getContext(), configManager);
        klineView.setGridColumns(5);
        klineView.setGridRows(3);
        klineView.setChildDraw(0);
        klineView.setDateTimeFormatter(new DateFormatter());
        klineView.configManager = configManager;
        addView(klineView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewGroup willShotView = (ViewGroup)getParent();
        if (shotView == null) {
            shotView = new HTShotView(getContext(), willShotView);
            shotView.setEnabled(false);
            shotView.dimension = 300;
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(shotView.dimension, shotView.dimension);
            layoutParams.setMargins(50, 50, 0, 0);
            ((ViewGroup)willShotView.getParent().getParent()).addView(shotView, layoutParams);
        }
    }

    public void reloadConfigManager() {
        klineView.changeMainDrawType(klineView.configManager.primaryStatus);
        klineView.changeSecondDrawType(klineView.configManager.secondStatus);
        klineView.setMainDrawLine(klineView.configManager.isMinute);
        klineView.setPointWidth(klineView.configManager.itemWidth);
        klineView.setCandleWidth(klineView.configManager.candleWidth);

        if (klineView.configManager.fontFamily.length() > 0) {
            klineView.setTextFontFamily(klineView.configManager.fontFamily);
        }
        klineView.setTextColor(klineView.configManager.textColor);
        klineView.setTextSize(klineView.configManager.rightTextFontSize);
        klineView.setMTextSize(klineView.configManager.candleTextFontSize);
        klineView.setMTextColor(klineView.configManager.candleTextColor);
        klineView.reloadColor();
        Boolean isEnd = klineView.getScrollOffset() >= klineView.getMaxScrollX();
        int previousScrollX = klineView.getScrollOffset();
        klineView.notifyChanged();

        if (klineView.configManager.shouldAdjustScrollPosition) {
            // 调整滚动位置以补偿新增的数据
            int newScrollX = previousScrollX + klineView.configManager.scrollPositionAdjustment;
            klineView.setScrollX(newScrollX);
        } else if (isEnd || klineView.configManager.shouldScrollToEnd) {
            klineView.setScrollX(klineView.getMaxScrollX());
        }


        final int id = this.getId();
        configManager.onDrawItemDidTouch = new Callback() {
            @Override
            public void invoke(Object... args) {
                HTDrawItem drawItem = (HTDrawItem) args[0];
                int drawItemIndex = (int) args[1];
                configManager.shouldReloadDrawItemIndex = drawItemIndex;

                WritableMap map = Arguments.createMap();
                if (drawItem != null) {
                    int drawColor = drawItem.drawColor;
                    int alpha = (drawColor >> 24) & 0xFF;
                    int red = (drawColor >> 16) & 0xFF;
                    int green = (drawColor >> 8) & 0xFF;
                    int blue = (drawColor) & 0xFF;
                    WritableArray colorList = Arguments.createArray();

                    colorList.pushDouble(red / 255.0);
                    colorList.pushDouble(green / 255.0);
                    colorList.pushDouble(blue / 255.0);
                    colorList.pushDouble(alpha / 255.0);

                    map.putArray("drawColor", colorList);
                    map.putDouble("drawLineHeight", drawItem.drawLineHeight);
                    map.putDouble("drawDashWidth", drawItem.drawDashWidth);
                    map.putDouble("drawDashSpace", drawItem.drawDashSpace);
                    map.putBoolean("drawIsLock", drawItem.drawIsLock);
                }
                map.putInt("shouldReloadDrawItemIndex", drawItemIndex);
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        id,
                        RNKLineView.onDrawItemDidTouchKey,
                        map
                );
            }
        };
        configManager.onScrollLeft = new Callback() {
            @Override
            public void invoke(Object... args) {
                long timestamp = (long) args[0];

                WritableMap map = Arguments.createMap();
                map.putDouble("timestamp", timestamp);
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        id,
                        RNKLineView.onScrollLeftKey,
                        map
                );
            }
        };
        configManager.onChartTouch = new Callback() {
            @Override
            public void invoke(Object... args) {
                float x = (float) args[0];
                float y = (float) args[1];
                boolean isOnClosePriceLabel = (boolean) args[2];

                // If touched on close price label, trigger smooth scroll to end
                if (isOnClosePriceLabel) {
                    klineView.smoothScrollToEnd();
                }

                WritableMap map = Arguments.createMap();
                map.putDouble("x", x);
                map.putDouble("y", y);
                map.putBoolean("isOnClosePriceLabel", isOnClosePriceLabel);

                // Add close price frame for debugging
                WritableMap closePriceFrame = Arguments.createMap();
                closePriceFrame.putDouble("x", klineView.getClosePriceLabelFrameLeft());
                closePriceFrame.putDouble("y", klineView.getClosePriceLabelFrameTop());
                closePriceFrame.putDouble("width", klineView.getClosePriceLabelFrameWidth());
                closePriceFrame.putDouble("height", klineView.getClosePriceLabelFrameHeight());
                map.putMap("closePriceFrame", closePriceFrame);

                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        id,
                        RNKLineView.onChartTouchKey,
                        map
                );
            }
        };
        configManager.onDrawItemComplete = new Callback() {
            @Override
            public void invoke(Object... args) {
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        id,
                        RNKLineView.onDrawItemCompleteKey,
                        Arguments.createMap()
                );
            }
        };
        configManager.onDrawPointComplete = new Callback() {
            @Override
            public void invoke(Object... args) {
                HTDrawItem drawItem = (HTDrawItem) args[0];
                WritableMap map = Arguments.createMap();
                map.putInt("pointCount", drawItem.pointList.size());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        id,
                        RNKLineView.onDrawPointCompleteKey,
                        map
                );
            }
        };

        int reloadIndex = configManager.shouldReloadDrawItemIndex;
        if (reloadIndex >= 0 && reloadIndex < klineView.drawContext.drawItemList.size()) {
            HTDrawItem drawItem = klineView.drawContext.drawItemList.get(reloadIndex);
            drawItem.drawColor = configManager.drawColor;
            drawItem.drawLineHeight = configManager.drawLineHeight;
            drawItem.drawDashWidth = configManager.drawDashWidth;
            drawItem.drawDashSpace = configManager.drawDashSpace;
            drawItem.drawIsLock = configManager.drawIsLock;
            if (configManager.drawShouldTrash) {
                configManager.shouldReloadDrawItemIndex = HTDrawState.showPencil;
                klineView.drawContext.drawItemList.remove(reloadIndex);
                configManager.drawShouldTrash = false;
            }
            klineView.drawContext.invalidate();
        }


        if (configManager.shouldFixDraw) {
            configManager.shouldFixDraw = false;
            klineView.drawContext.fixDrawItemList();
        }
        if (configManager.shouldClearDraw) {
            configManager.shouldReloadDrawItemIndex = HTDrawState.none;
            configManager.shouldClearDraw = false;
            klineView.drawContext.clearDrawItemList();
        }

    }

    private HTPoint convertLocation(HTPoint location) {
        HTPoint reloadLocation = new HTPoint(location.x, location.y);
        reloadLocation.x = Math.max(0, Math.min(reloadLocation.x, getWidth()));
        reloadLocation.y = Math.max(0, Math.min(reloadLocation.y, getHeight()));
//        reloadLocation.x += klineView.getScrollOffset();
        reloadLocation = klineView.valuePointFromViewPoint(reloadLocation);
        return reloadLocation;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int reloadIndex = configManager.shouldReloadDrawItemIndex;
        switch (reloadIndex) {
            case HTDrawState.none: {
                return false;
            }
            case HTDrawState.showPencil: {
                if (configManager.drawType == HTDrawType.none) {
                    HTPoint location = new HTPoint(event.getX(), event.getY());
                    location = convertLocation(location);
                    if ((HTDrawItem.canResponseLocation(klineView.drawContext.drawItemList, location, klineView)) == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private HTPoint lastLocation;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handlerDraw(event);
        handlerShot(event);
        return true;
    }

    private void handlerDraw(MotionEvent event) {
        HTPoint location = new HTPoint(event.getX(), event.getY());
        location = convertLocation(location);
        HTPoint previousLocation = lastLocation != null ? lastLocation : location;
        lastLocation = location;
        int state = event.getAction();
        Boolean isCancel = state == MotionEvent.ACTION_CANCEL;
        if (isCancel) {
            state = MotionEvent.ACTION_UP;
        }
        HTPoint translation = new HTPoint(
                location.x - previousLocation.x,
                location.y - previousLocation.y
        );
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            lastLocation = null;
        }
        klineView.drawContext.touchesGesture(location, translation, state);
    }

    private void handlerShot(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            shotView.setPoint(null);
            lastLocation = null;
        } else {
            shotView.setPoint(new HTPoint(event.getX(), event.getY()));
        }
    }

    public void updateLastCandlestick(Map<String, Object> candlestickData) {
        android.util.Log.d("HTKLineContainerView", "updateLastCandlestick called with data: " + candlestickData);

        if (klineView == null || configManager.modelArray == null ||
            configManager.modelArray.isEmpty()) {
            android.util.Log.w("HTKLineContainerView", "updateLastCandlestick: Null check failed");
            return;
        }

        // Test 2: Don't touch the view at all, just log
        android.util.Log.d("HTKLineContainerView", "Test 2: No view operations, just updating data");

        try {
            // Get the existing last candlestick to preserve indicator data
            int lastIndex = configManager.modelArray.size() - 1;
            if (lastIndex < 0) {
                android.util.Log.w("HTKLineContainerView", "No items in modelArray");
                return;
            }

            KLineEntity existingEntity = configManager.modelArray.get(lastIndex);
            if (existingEntity == null) {
                android.util.Log.w("HTKLineContainerView", "Existing entity is null");
                return;
            }

            // Create a new entity but preserve indicator lists from the existing entity
            KLineEntity newEntity = configManager.packModel(candlestickData);
            android.util.Log.d("HTKLineContainerView", "Created new entity: Close=" + newEntity.Close + ", Volume=" + newEntity.Volume);
            android.util.Log.d("HTKLineContainerView", "Input candlestick data vol field: " + candlestickData.get("vol"));

            // Validate the new entity
            if (Float.isNaN(newEntity.Close) || Float.isInfinite(newEntity.Close)) {
                android.util.Log.w("HTKLineContainerView", "Invalid close price, skipping update");
                return;
            }

            // Only preserve indicator lists if the new data doesn't contain them
            android.util.Log.d("HTKLineContainerView", "Using new indicator data from React Native");
            if (newEntity.maList.isEmpty()) {
                newEntity.maList = existingEntity.maList;
            }
            if (newEntity.maVolumeList.isEmpty()) {
                newEntity.maVolumeList = existingEntity.maVolumeList;
            }
            if (newEntity.rsiList.isEmpty()) {
                newEntity.rsiList = existingEntity.rsiList;
            }
            if (newEntity.wrList.isEmpty()) {
                newEntity.wrList = existingEntity.wrList;
            }
            if (newEntity.selectedItemList.isEmpty()) {
                newEntity.selectedItemList = existingEntity.selectedItemList;
            }

            android.util.Log.d("HTKLineContainerView", "New maVolumeList size: " + newEntity.maVolumeList.size());
            if (!newEntity.maVolumeList.isEmpty()) {
                android.util.Log.d("HTKLineContainerView", "Volume MA5: " + ((HTKLineTargetItem)newEntity.maVolumeList.get(0)).value +
                                   ", MA10: " + ((HTKLineTargetItem)newEntity.maVolumeList.get(1)).value);
            }

            // Update the last item in the data list with synchronization
            synchronized (configManager.modelArray) {
                if (lastIndex >= configManager.modelArray.size()) {
                    android.util.Log.w("HTKLineContainerView", "Index out of bounds: " + lastIndex);
                    return;
                }

                configManager.modelArray.set(lastIndex, newEntity);
                android.util.Log.d("HTKLineContainerView", "Updated last candlestick at index: " + lastIndex);
            }

            android.util.Log.d("HTKLineContainerView", "Data update completed, now triggering safe redraw");

            // Use postDelayed to ensure the data update is fully complete before triggering redraw
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Call notifyChanged to properly recalculate the chart state
                        android.util.Log.d("HTKLineContainerView", "Calling notifyChanged to recalculate chart state");
                        klineView.notifyChanged();
                        android.util.Log.d("HTKLineContainerView", "notifyChanged completed successfully");

                        // Force immediate invalidation to ensure visual update
                        android.util.Log.d("HTKLineContainerView", "Forcing view invalidation for immediate redraw");
                        klineView.invalidate();
                        android.util.Log.d("HTKLineContainerView", "View invalidation completed");
                    } catch (Exception e) {
                        android.util.Log.e("HTKLineContainerView", "Error in redraw operations", e);
                    }
                }
            }, 50); // 50ms delay to ensure data is stable

        } catch (Exception e) {
            android.util.Log.e("HTKLineContainerView", "Error updating data", e);
        }

    }

    public void addCandlesticksAtTheEnd(ReadableArray candlesticksArray) {
        android.util.Log.d("HTKLineContainerView", "addCandlesticksAtTheEnd called with " + candlesticksArray.size() + " candlesticks");

        if (klineView == null || configManager.modelArray == null) {
            android.util.Log.w("HTKLineContainerView", "addCandlesticksAtTheEnd: Null check failed");
            return;
        }

        if (candlesticksArray.size() == 0) {
            android.util.Log.w("HTKLineContainerView", "addCandlesticksAtTheEnd: Empty candlesticks array");
            return;
        }

        try {
            // Check if user is currently at the end of the chart
            boolean wasAtEnd = klineView.getScrollOffset() >= klineView.getMaxScrollX() - 10;

            // Get existing model for preserving indicator lists structure
            KLineEntity templateEntity = null;
            if (!configManager.modelArray.isEmpty()) {
                templateEntity = configManager.modelArray.get(configManager.modelArray.size() - 1);
            }

            // Convert ReadableArray to List of KLineEntity
            List<KLineEntity> newEntities = new ArrayList<>();
            for (int i = 0; i < candlesticksArray.size(); i++) {
                ReadableMap candlestickMap = candlesticksArray.getMap(i);
                if (candlestickMap != null) {
                    Map<String, Object> candlestickData = candlestickMap.toHashMap();
                    KLineEntity entity = configManager.packModel(candlestickData);

                    // Validate the entity
                    if (!Float.isNaN(entity.Close) && !Float.isInfinite(entity.Close)) {
                        // The indicator lists are now properly populated by packModel() from React Native data
                        // No need for manual calculation since the data already includes calculated indicators
                        android.util.Log.d("HTKLineContainerView", "Using indicator data from React Native - maList.size=" + entity.maList.size() + ", maVolumeList.size=" + entity.maVolumeList.size());

                        newEntities.add(entity);
                    } else {
                        android.util.Log.w("HTKLineContainerView", "Skipping invalid candlestick at index " + i);
                    }
                }
            }

            if (newEntities.isEmpty()) {
                android.util.Log.w("HTKLineContainerView", "No valid candlesticks to add");
                return;
            }

            // Add new entities to the end of the array with synchronization
            synchronized (configManager.modelArray) {
                configManager.modelArray.addAll(newEntities);
                android.util.Log.d("HTKLineContainerView", "Added " + newEntities.size() + " new candlesticks to the end");
                android.util.Log.d("HTKLineContainerView", "Total candlesticks now: " + configManager.modelArray.size());
                android.util.Log.d("HTKLineContainerView", "Was at end before adding: " + wasAtEnd);
            }

            // Trigger redraw and optionally scroll to end
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        android.util.Log.d("HTKLineContainerView", "Calling notifyChanged after adding candlesticks");
                        klineView.notifyChanged();

                        android.util.Log.d("HTKLineContainerView", "Forcing view invalidation after adding candlesticks");
                        klineView.invalidate();

                        // If user was at the end, keep them at the end
                        if (wasAtEnd) {
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    android.util.Log.d("HTKLineContainerView", "Scrolling to end after adding new data");
                                    klineView.setScrollX(klineView.getMaxScrollX());
                                }
                            }, 100); // Additional delay for scroll
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HTKLineContainerView", "Error in redraw operations after adding candlesticks", e);
                    }
                }
            }, 50); // 50ms delay to ensure data is stable

        } catch (Exception e) {
            android.util.Log.e("HTKLineContainerView", "Error adding candlesticks", e);
        }
    }

    public void addCandlesticksAtTheStart(ReadableArray candlesticksArray) {
        android.util.Log.d("HTKLineContainerView", "addCandlesticksAtTheStart called with " + candlesticksArray.size() + " candlesticks");

        if (klineView == null || configManager.modelArray == null) {
            android.util.Log.w("HTKLineContainerView", "addCandlesticksAtTheStart: Null check failed");
            return;
        }

        if (candlesticksArray.size() == 0) {
            android.util.Log.w("HTKLineContainerView", "addCandlesticksAtTheStart: Empty candlesticks array");
            return;
        }

        try {
            // Reset the scroll left trigger flag to allow new triggers
            klineView.resetScrollLeftTrigger();

            // Convert ReadableArray to List of KLineEntity
            List<KLineEntity> newEntities = new ArrayList<>();
            for (int i = 0; i < candlesticksArray.size(); i++) {
                ReadableMap candlestickMap = candlesticksArray.getMap(i);
                if (candlestickMap != null) {
                    Map<String, Object> candlestickData = candlestickMap.toHashMap();
                    KLineEntity entity = configManager.packModel(candlestickData);

                    // Validate the entity
                    if (!Float.isNaN(entity.Close) && !Float.isInfinite(entity.Close)) {
                        // The indicator lists are now properly populated by packModel() from React Native data
                        android.util.Log.d("HTKLineContainerView", "Using indicator data from React Native - maList.size=" + entity.maList.size() + ", maVolumeList.size=" + entity.maVolumeList.size());

                        newEntities.add(entity);
                    } else {
                        android.util.Log.w("HTKLineContainerView", "Skipping invalid candlestick at index " + i);
                    }
                }
            }

            if (newEntities.isEmpty()) {
                android.util.Log.w("HTKLineContainerView", "No valid candlesticks to add");
                return;
            }

            // Get current scroll position before modifying data
            int currentScrollX = klineView.getScrollOffset();

            // Add new entities to the beginning of the array (prepend)
            synchronized (configManager.modelArray) {
                configManager.modelArray.addAll(0, newEntities);
                android.util.Log.d("HTKLineContainerView", "Added " + newEntities.size() + " new candlesticks to the start");
                android.util.Log.d("HTKLineContainerView", "Total candlesticks now: " + configManager.modelArray.size());
            }

            // Set up scroll position adjustment using the config manager mechanism
            int addedWidth = newEntities.size() * (int) klineView.configManager.itemWidth;
            configManager.scrollPositionAdjustment = addedWidth;
            configManager.shouldAdjustScrollPosition = true;

            android.util.Log.d("HTKLineContainerView", "Set scroll position adjustment: " + addedWidth + " pixels");

            // Use the same reload mechanism as optionList
            int previousScrollX = klineView.getScrollOffset();
            klineView.notifyChanged();

            if (klineView.configManager.shouldAdjustScrollPosition) {
                // Adjust scroll position to compensate for the new data added at start
                int newScrollX = previousScrollX + klineView.configManager.scrollPositionAdjustment;
                android.util.Log.d("HTKLineContainerView", "Adjusting scroll position from " + previousScrollX + " to " + newScrollX);
                klineView.setScrollX(newScrollX);

                // Reset the flags
                configManager.shouldAdjustScrollPosition = false;
                configManager.scrollPositionAdjustment = 0;
            }

        } catch (Exception e) {
            android.util.Log.e("HTKLineContainerView", "Error adding candlesticks at start", e);
        }
    }

    public void addOrderLine(Map<String, Object> orderLineData) {
        android.util.Log.d("HTKLineContainerView", "addOrderLine called with data: " + orderLineData);

        if (orderLineData == null || !orderLineData.containsKey("id")) {
            android.util.Log.w("HTKLineContainerView", "addOrderLine - Invalid order line data");
            return;
        }

        String id = (String) orderLineData.get("id");

        // Store the order line
        synchronized (orderLines) {
            orderLines.put(id, orderLineData);
            android.util.Log.d("HTKLineContainerView", "Added order line with id: " + id);
        }

        // Trigger redraw to show the order line
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public void removeOrderLine(String orderLineId) {
        android.util.Log.d("HTKLineContainerView", "removeOrderLine called with id: " + orderLineId);

        if (orderLineId == null) {
            android.util.Log.w("HTKLineContainerView", "removeOrderLine - Invalid order line id");
            return;
        }

        // Remove the order line
        synchronized (orderLines) {
            orderLines.remove(orderLineId);
            android.util.Log.d("HTKLineContainerView", "Removed order line with id: " + orderLineId);
        }

        // Trigger redraw to remove the order line
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public void updateOrderLine(Map<String, Object> orderLineData) {
        android.util.Log.d("HTKLineContainerView", "updateOrderLine called with data: " + orderLineData);

        if (orderLineData == null || !orderLineData.containsKey("id")) {
            android.util.Log.w("HTKLineContainerView", "updateOrderLine - Invalid order line data");
            return;
        }

        String id = (String) orderLineData.get("id");

        // Update the order line
        synchronized (orderLines) {
            orderLines.put(id, orderLineData);
            android.util.Log.d("HTKLineContainerView", "Updated order line with id: " + id);
        }

        // Trigger redraw to update the order line
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public List<Map<String, Object>> getOrderLines() {
        android.util.Log.d("HTKLineContainerView", "getOrderLines called");

        // Return all order lines as a list
        synchronized (orderLines) {
            List<Map<String, Object>> orderLinesList = new ArrayList<>(orderLines.values());
            android.util.Log.d("HTKLineContainerView", "Returning " + orderLinesList.size() + " order lines");
            return orderLinesList;
        }
    }

    // Method to allow KLineChartView to access order lines for drawing
    public Map<String, Map<String, Object>> getAllOrderLines() {
        synchronized (orderLines) {
            return new HashMap<>(orderLines);
        }
    }

    public void addBuySellMark(Map<String, Object> buySellMarkData) {

        if (buySellMarkData == null || !buySellMarkData.containsKey("id") ||
            !buySellMarkData.containsKey("time") || !buySellMarkData.containsKey("type")) {
            return;
        }

        String id = (String) buySellMarkData.get("id");
        long time = ((Number) buySellMarkData.get("time")).longValue();
        String type = (String) buySellMarkData.get("type");

        // Store in compatibility map
        synchronized (buySellMarks) {
            buySellMarks.put(id, buySellMarkData);
        }

        // Store in efficient lookup maps by timestamp
        if ("buy".equals(type)) {
            synchronized (buyMarks) {
                buyMarks.put(time, buySellMarkData);
            }
        } else if ("sell".equals(type)) {
            synchronized (sellMarks) {
                sellMarks.put(time, buySellMarkData);
            }
        }

        android.util.Log.d("HTKLineContainerView", "Added " + type + " mark with id: " + id + " at time: " + time);

        // Trigger redraw to show the buy/sell mark
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public void removeBuySellMark(String buySellMarkId) {

        if (buySellMarkId == null || buySellMarkId.trim().isEmpty()) {
            return;
        }

        // Find and remove from efficient lookup maps
        Map<String, Object> markData;
        synchronized (buySellMarks) {
            markData = buySellMarks.get(buySellMarkId);
            if (markData != null) {
                buySellMarks.remove(buySellMarkId);
            }
        }

        if (markData != null && markData.containsKey("time") && markData.containsKey("type")) {
            long time = ((Number) markData.get("time")).longValue();
            String type = (String) markData.get("type");

            if ("buy".equals(type)) {
                synchronized (buyMarks) {
                    buyMarks.remove(time);
                }
            } else if ("sell".equals(type)) {
                synchronized (sellMarks) {
                    sellMarks.remove(time);
                }
            }
        }

        android.util.Log.d("HTKLineContainerView", "Removed buy/sell mark with id: " + buySellMarkId);

        // Trigger redraw to remove the buy/sell mark
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public void updateBuySellMark(Map<String, Object> buySellMarkData) {

        if (buySellMarkData == null || !buySellMarkData.containsKey("id") ||
            !buySellMarkData.containsKey("time") || !buySellMarkData.containsKey("type")) {
            return;
        }

        String id = (String) buySellMarkData.get("id");
        long time = ((Number) buySellMarkData.get("time")).longValue();
        String type = (String) buySellMarkData.get("type");

        // Remove old entry from efficient lookup maps if it exists
        Map<String, Object> oldMarkData;
        synchronized (buySellMarks) {
            oldMarkData = buySellMarks.get(id);
            buySellMarks.put(id, buySellMarkData);
        }

        if (oldMarkData != null && oldMarkData.containsKey("time") && oldMarkData.containsKey("type")) {
            long oldTime = ((Number) oldMarkData.get("time")).longValue();
            String oldType = (String) oldMarkData.get("type");

            if ("buy".equals(oldType)) {
                synchronized (buyMarks) {
                    buyMarks.remove(oldTime);
                }
            } else if ("sell".equals(oldType)) {
                synchronized (sellMarks) {
                    sellMarks.remove(oldTime);
                }
            }
        }

        // Add to efficient lookup maps
        if ("buy".equals(type)) {
            synchronized (buyMarks) {
                buyMarks.put(time, buySellMarkData);
            }
        } else if ("sell".equals(type)) {
            synchronized (sellMarks) {
                sellMarks.put(time, buySellMarkData);
            }
        }

        android.util.Log.d("HTKLineContainerView", "Updated " + type + " mark with id: " + id + " at time: " + time);

        // Trigger redraw to update the buy/sell mark
        post(new Runnable() {
            @Override
            public void run() {
                klineView.invalidate();
            }
        });
    }

    public List<Map<String, Object>> getBuySellMarks() {

        // Return all buy/sell marks as a list
        synchronized (buySellMarks) {
            List<Map<String, Object>> buySellMarksList = new ArrayList<>(buySellMarks.values());
            android.util.Log.d("HTKLineContainerView", "Returning " + buySellMarksList.size() + " buy/sell marks");
            return buySellMarksList;
        }
    }

    // Method to allow KLineChartView to access buy/sell marks for drawing
    public Map<String, Map<String, Object>> getAllBuySellMarks() {
        synchronized (buySellMarks) {
            return new HashMap<>(buySellMarks);
        }
    }

    // Efficient O(1) lookup methods for buy/sell marks by timestamp
    public Map<String, Object> getBuyMarkForTime(long time) {
        synchronized (buyMarks) {
            return buyMarks.get(time);
        }
    }

    public Map<String, Object> getSellMarkForTime(long time) {
        synchronized (sellMarks) {
            return sellMarks.get(time);
        }
    }

}
