declare module 'react-native-kline-view' {
  import { Component } from 'react';
  import { ViewProps, ViewStyle } from 'react-native';

  export interface OrderLine {
    id: string;
    type: string; // 'limit' | 'liquidation' | 'stop_loss' | 'take_profit' | etc.
    price: number;
    amount: number;
    color: string; // Hex color string with optional transparency (e.g., '#FF0000' or '#FF000080' for 50% transparent red)
    label?: string; // Optional label text to display on the line
    labelFontSize?: number; // Optional font size for the label (default: 12)
    labelBackgroundColor?: string; // Optional background color for the label pill with optional transparency (e.g., '#FF0000' or '#00000080' for 50% transparent black)
    labelColor?: string; // Optional color for the label text (defaults to line color if not specified)
    labelDescription?: string; // Optional description text displayed next to the label
    labelDescriptionColor?: string; // Optional color for the description text (defaults to labelColor if not specified)
  }

  export interface BuySellMark {
    id: string;
    time: number;
    type: 'buy' | 'sell';
    amount: string;
    price: string;
    orderCount?: number;
    tooltipText?: string;
  }

  export interface RNKLineViewRef {
    updateLastCandlestick: (candlestick: any) => void;
    addCandlesticksAtTheEnd: (candlesticks: any[]) => void;
    addCandlesticksAtTheStart: (candlesticks: any[]) => void;
    addOrderLine: (orderLine: OrderLine) => void;
    removeOrderLine: (orderLineId: string) => void;
    updateOrderLine: (orderLine: OrderLine) => void;
    getOrderLines: () => OrderLine[];
    addBuySellMark: (buySellMark: BuySellMark) => void;
    removeBuySellMark: (buySellMarkId: string) => void;
    updateBuySellMark: (buySellMark: BuySellMark) => void;
    getBuySellMarks: () => BuySellMark[];
  }

  export interface RNKLineViewProps extends ViewProps {
    style?: ViewStyle;
    optionList?: string;
    onDrawItemDidTouch?: (event: any) => void;
    onScrollLeft?: (event: any) => void;
    onChartTouch?: (event: any) => void;
    onDrawItemComplete?: (event: any) => void;
    onDrawPointComplete?: (event: any) => void;
  }

  export default class RNKLineView extends Component<RNKLineViewProps> {
    // Component ref methods will be available through imperative handle
  }
}