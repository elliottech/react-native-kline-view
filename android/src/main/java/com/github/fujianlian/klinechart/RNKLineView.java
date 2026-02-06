package com.github.fujianlian.klinechart;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import androidx.annotation.Nullable;
import com.github.fujianlian.klinechart.container.HTKLineContainerView;
import com.github.fujianlian.klinechart.draw.PrimaryStatus;
import com.github.fujianlian.klinechart.draw.SecondStatus;
import com.github.fujianlian.klinechart.formatter.DateFormatter;
import com.github.fujianlian.klinechart.formatter.ValueFormatter;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

public class RNKLineView extends SimpleViewManager<HTKLineContainerView> {

	public static String onDrawItemDidTouchKey = "onDrawItemDidTouch";

	public static String onScrollLeftKey = "onScrollLeft";

	public static String onDrawItemCompleteKey = "onDrawItemComplete";

	public static String onDrawPointCompleteKey = "onDrawPointComplete";

	public static String onChartTouchKey = "onChartTouch";

    @Nonnull
    @Override
    public String getName() {
        return "RNKLineView";
    }

    @Nonnull
    @Override
    protected HTKLineContainerView createViewInstance(@Nonnull ThemedReactContext reactContext) {
    	HTKLineContainerView containerView = new HTKLineContainerView(reactContext);
    	return containerView;
    }

	@Override
	public Map getExportedCustomDirectEventTypeConstants() {
		return MapBuilder.of(
				onDrawItemDidTouchKey, MapBuilder.of("registrationName", onDrawItemDidTouchKey),
				onScrollLeftKey, MapBuilder.of("registrationName", onScrollLeftKey),
				onDrawItemCompleteKey, MapBuilder.of("registrationName", onDrawItemCompleteKey),
				onDrawPointCompleteKey, MapBuilder.of("registrationName", onDrawPointCompleteKey),
				onChartTouchKey, MapBuilder.of("registrationName", onChartTouchKey)
		);
	}





    @ReactProp(name = "optionList")
    public void setOptionList(final HTKLineContainerView containerView, String optionList) {
        if (optionList == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int disableDecimalFeature = JSON.DEFAULT_PARSER_FEATURE & ~Feature.UseBigDecimal.getMask();
                Map optionMap = (Map)JSON.parse(optionList, disableDecimalFeature);
                containerView.configManager.reloadOptionList(optionMap);
                containerView.post(new Runnable() {
                    @Override
                    public void run() {
                        containerView.reloadConfigManager();
                    }
                });
            }
        }).start();
    }

    @Override
    public Map<String, Integer> getCommandsMap() {
        Map<String, Integer> commands = MapBuilder.of(
            "updateLastCandlestick", 1,
            "addCandlesticksAtTheEnd", 2,
            "addCandlesticksAtTheStart", 3,
            "addOrderLine", 4,
            "removeOrderLine", 5,
            "updateOrderLine", 6,
            "getOrderLines", 7
        );

        // Add the new buy/sell mark commands
        commands.put("addBuySellMark", 8);
        commands.put("removeBuySellMark", 9);
        commands.put("updateBuySellMark", 10);
        commands.put("getBuySellMarks", 11);

        return commands;
    }

    @Override
    public void receiveCommand(@Nonnull HTKLineContainerView containerView, String commandId, @Nullable ReadableArray args) {
        android.util.Log.d("RNKLineView", "receiveCommand called with commandId: " + commandId);
        switch (commandId) {
            case "updateLastCandlestick":
                android.util.Log.d("RNKLineView", "Processing updateLastCandlestick command");
                if (args != null && args.size() > 0) {
                    try {
                        ReadableMap candlestickData = args.getMap(0);
                        Map<String, Object> dataMap = candlestickData.toHashMap();
                        android.util.Log.d("RNKLineView", "Calling containerView.updateLastCandlestick with data: " + dataMap);
                        containerView.updateLastCandlestick(dataMap);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in receiveCommand", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "updateLastCandlestick: args is null or empty");
                }
                break;
            case "addCandlesticksAtTheEnd":
                android.util.Log.d("RNKLineView", "Processing addCandlesticksAtTheEnd command");
                if (args != null && args.size() > 0) {
                    try {
                        ReadableArray candlesticksArray = args.getArray(0);
                        android.util.Log.d("RNKLineView", "Calling containerView.addCandlesticksAtTheEnd with " + candlesticksArray.size() + " candlesticks");
                        containerView.addCandlesticksAtTheEnd(candlesticksArray);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in addCandlesticksAtTheEnd command", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "addCandlesticksAtTheEnd: args is null or empty");
                }
                break;
            case "addCandlesticksAtTheStart":
                android.util.Log.d("RNKLineView", "Processing addCandlesticksAtTheStart command");
                if (args != null && args.size() > 0) {
                    try {
                        ReadableArray candlesticksArray = args.getArray(0);
                        android.util.Log.d("RNKLineView", "Calling containerView.addCandlesticksAtTheStart with " + candlesticksArray.size() + " candlesticks");
                        containerView.addCandlesticksAtTheStart(candlesticksArray);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in addCandlesticksAtTheStart command", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "addCandlesticksAtTheStart: args is null or empty");
                }
                break;
            case "addOrderLine":
                android.util.Log.d("RNKLineView", "Processing addOrderLine command");
                if (args != null && args.size() > 0) {
                    try {
                        ReadableMap orderLineData = args.getMap(0);
                        Map<String, Object> dataMap = orderLineData.toHashMap();
                        android.util.Log.d("RNKLineView", "Calling containerView.addOrderLine with data: " + dataMap);
                        containerView.addOrderLine(dataMap);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in addOrderLine command", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "addOrderLine: args is null or empty");
                }
                break;
            case "removeOrderLine":
                android.util.Log.d("RNKLineView", "Processing removeOrderLine command");
                if (args != null && args.size() > 0) {
                    try {
                        String orderLineId = args.getString(0);
                        android.util.Log.d("RNKLineView", "Calling containerView.removeOrderLine with id: " + orderLineId);
                        containerView.removeOrderLine(orderLineId);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in removeOrderLine command", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "removeOrderLine: args is null or empty");
                }
                break;
            case "updateOrderLine":
                android.util.Log.d("RNKLineView", "Processing updateOrderLine command");
                if (args != null && args.size() > 0) {
                    try {
                        ReadableMap orderLineData = args.getMap(0);
                        Map<String, Object> dataMap = orderLineData.toHashMap();
                        android.util.Log.d("RNKLineView", "Calling containerView.updateOrderLine with data: " + dataMap);
                        containerView.updateOrderLine(dataMap);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in updateOrderLine command", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.w("RNKLineView", "updateOrderLine: args is null or empty");
                }
                break;
            case "getOrderLines":
                android.util.Log.d("RNKLineView", "Processing getOrderLines command");
                try {
                    android.util.Log.d("RNKLineView", "Calling containerView.getOrderLines");
                    containerView.getOrderLines();
                } catch (Exception e) {
                    android.util.Log.e("RNKLineView", "Error in getOrderLines command", e);
                    e.printStackTrace();
                }
                break;
            case "addBuySellMark":
                if (args != null && args.size() > 0) {
                    try {
                        ReadableMap buySellMarkData = args.getMap(0);
                        Map<String, Object> dataMap = buySellMarkData.toHashMap();
                        containerView.addBuySellMark(dataMap);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in addBuySellMark command", e);
                        e.printStackTrace();
                    }
                } else {
                }
                break;
            case "removeBuySellMark":
                if (args != null && args.size() > 0) {
                    try {
                        String buySellMarkId = args.getString(0);
                        containerView.removeBuySellMark(buySellMarkId);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in removeBuySellMark command", e);
                        e.printStackTrace();
                    }
                } else {
                }
                break;
            case "updateBuySellMark":
                if (args != null && args.size() > 0) {
                    try {
                        ReadableMap buySellMarkData = args.getMap(0);
                        Map<String, Object> dataMap = buySellMarkData.toHashMap();
                        containerView.updateBuySellMark(dataMap);
                    } catch (Exception e) {
                        android.util.Log.e("RNKLineView", "Error in updateBuySellMark command", e);
                        e.printStackTrace();
                    }
                } else {
                }
                break;
            case "getBuySellMarks":
                try {
                    containerView.getBuySellMarks();
                } catch (Exception e) {
                    android.util.Log.e("RNKLineView", "Error in getBuySellMarks command", e);
                    e.printStackTrace();
                }
                break;
            default:
                android.util.Log.w("RNKLineView", "Unknown command: " + commandId);
                break;
        }
    }

}
