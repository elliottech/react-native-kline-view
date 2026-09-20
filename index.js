import React, { forwardRef, useImperativeHandle, useRef } from 'react';
import { requireNativeComponent } from 'react-native';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

const NativeRNKLineView = requireNativeComponent('RNKLineView');
const Commands = codegenNativeCommands({
  supportedCommands: [
    'updateLastCandlestick',
    'addCandlesticksAtTheEnd',
    'addCandlesticksAtTheStart',
    'addOrderLine',
    'removeOrderLine',
    'updateOrderLine',
    'getOrderLines',
    'addBuySellMark',
    'removeBuySellMark',
    'updateBuySellMark',
    'getBuySellMarks',
  ],
});

const RNKLineView = forwardRef((props, ref) => {
  const nativeRef = useRef(null);

  const dispatch = (name, args, detachedResult) => {
    const view = nativeRef.current;
    if (!view) return detachedResult;

    // Dispatch through the host ref on every platform. Numeric tags force Fabric
    // through findShadowNodeByTag_DEPRECATED, whose tree walk can race concurrent
    // commits and native tree teardown (PRO-4970). iOS's legacy view manager
    // interop resolves these commands by method name.
    return Commands[name](view, ...args);
  };

  useImperativeHandle(ref, () => ({
    updateLastCandlestick: (candlestick) => dispatch('updateLastCandlestick', [candlestick]),
    addCandlesticksAtTheEnd: (candlesticks) => dispatch('addCandlesticksAtTheEnd', [candlesticks]),
    addCandlesticksAtTheStart: (candlesticks) => dispatch('addCandlesticksAtTheStart', [candlesticks]),
    addOrderLine: (orderLine) => dispatch('addOrderLine', [orderLine]),
    removeOrderLine: (orderLineId) => dispatch('removeOrderLine', [orderLineId]),
    updateOrderLine: (orderLine) => dispatch('updateOrderLine', [orderLine]),
    getOrderLines: () => dispatch('getOrderLines', [], []),
    addBuySellMark: (buySellMark) => dispatch('addBuySellMark', [buySellMark]),
    removeBuySellMark: (buySellMarkId) => dispatch('removeBuySellMark', [buySellMarkId]),
    updateBuySellMark: (buySellMark) => dispatch('updateBuySellMark', [buySellMark]),
    getBuySellMarks: () => dispatch('getBuySellMarks', [], []),
  }));

  return React.createElement(NativeRNKLineView, { ref: nativeRef, ...props });
});

export default RNKLineView;
