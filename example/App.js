/**
 * K-line Chart Example Application
 * Supports indicators, finger drawing, theme switching and other features
 */

import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
	View,
	StyleSheet,
	StatusBar,
	Platform,
	PixelRatio,
	ScrollView
} from 'react-native'
import RNKLineView from 'react-native-kline-view'
import { ThemeManager } from './utils/themes'
import {
	TimeTypes,
	DrawTypeConstants,
	DrawStateConstants,
	DrawToolHelper
} from './utils/constants'
import {
	isHorizontalScreen,
	formatTime
} from './utils/helpers'
import Toolbar from './components/Toolbar'
import ControlBar from './components/ControlBar'
import OrderInput from './components/OrderInput'
import BuySellMarkInput from './components/BuySellMarkInput'
import Selectors from './components/Selectors'
import {
	processKLineData,
	packOptionList
} from './utils/businessLogic'
import { generateMockData, generateMoreHistoricalData } from './utils/generateData'
import {
	testUpdateLastCandlestick,
	testAddCandlesticksAtTheEnd,
	testAddCandlesticksAtTheStart
} from './utils/testUtils'


const App = () => {
	const [isDarkTheme, setIsDarkTheme] = useState(false)
	const [selectedTimeType, setSelectedTimeType] = useState(2) // Corresponds to 1 minute
	const [selectedMainIndicator, setSelectedMainIndicator] = useState(1) // Corresponds to MA (1=MA, 2=BOLL)
	const [selectedSubIndicator, setSelectedSubIndicator] = useState(4) // Corresponds to KDJ (3=MACD, 4=KDJ, 5=RSI, 6=WR)
	const [selectedDrawTool, setSelectedDrawTool] = useState(DrawTypeConstants.none)
	const [showIndicatorSelector, setShowIndicatorSelector] = useState(false)
	const [showTimeSelector, setShowTimeSelector] = useState(false)
	const [showDrawToolSelector, setShowDrawToolSelector] = useState(false)
	const [klineData, setKlineData] = useState(generateMockData())
	const [drawShouldContinue, setDrawShouldContinue] = useState(true)
	const [optionList, setOptionList] = useState(null)
	const [lastDataLength, setLastDataLength] = useState(0)
	const [currentScrollPosition, setCurrentScrollPosition] = useState(0)
	const [showVolumeChart, setShowVolumeChart] = useState(true)
	const [candleCornerRadius, setCandleCornerRadius] = useState(0)
	const firstCandleTimeRef = useRef(klineData.length > 0 ? klineData[0].time : null)
	const [initialDataLoaded, setInitialDataLoaded] = useState(false)

	const kLineViewRef = useRef(null)

	const updateStatusBar = useCallback(() => {
		StatusBar.setBarStyle(
			isDarkTheme ? 'light-content' : 'dark-content',
			true
		)
	}, [isDarkTheme])

	useEffect(() => {
		updateStatusBar()
	}, [updateStatusBar])

	useEffect(() => {
		updateStatusBar()
		// Initialize loading K-line data
		setLastDataLength(klineData.length)
		setTimeout(() => reloadKLineData(), 0)
	}, [showVolumeChart, selectedMainIndicator, selectedSubIndicator])

	useEffect(() => {
		updateStatusBar()
	}, [isDarkTheme, updateStatusBar])

	// Toggle theme
	const toggleTheme = useCallback(() => {
		setIsDarkTheme(prev => {
			// Reload data after theme switch to apply new colors
			setTimeout(() => reloadKLineData(), 0)
			return !prev
		})
	}, [])

	// Select time period
	const selectTimeType = useCallback((timeType) => {
		setSelectedTimeType(timeType)
		setShowTimeSelector(false)
		// Reset initial data loaded flag and regenerate data
		setInitialDataLoaded(false)
		setKlineData(generateMockData())
		setTimeout(() => reloadKLineData(), 0)
		console.log('Switch time period:', TimeTypes[timeType].label)
	}, [])

	// Select indicator
	const selectIndicator = useCallback((type, indicator) => {
		if (type === 'main') {
			setSelectedMainIndicator(indicator)
		} else {
			setSelectedSubIndicator(indicator)
		}
		setShowIndicatorSelector(false)
		setTimeout(() => reloadKLineData(), 0)
	}, [])

	// Select drawing tool
	const selectDrawTool = useCallback((tool) => {
		setSelectedDrawTool(tool)
		setShowDrawToolSelector(false)
		setOptionListValue({
			drawList: {
				shouldReloadDrawItemIndex: tool === DrawTypeConstants.none ? DrawStateConstants.none : DrawStateConstants.showContext,
				drawShouldContinue: drawShouldContinue,
				drawType: tool,
				shouldFixDraw: false,
			}
		})
	}, [drawShouldContinue])

	// Clear drawings
	const clearDrawings = useCallback(() => {
		setSelectedDrawTool(DrawTypeConstants.none)
		setOptionListValue({
			drawList: {
				shouldReloadDrawItemIndex: DrawStateConstants.none,
				shouldClearDraw: true,
			}
		})
	}, [])

	// Reload K-line data
	const reloadKLineData = useCallback((shouldScrollToEnd = true) => {
		if (!kLineViewRef.current) {
			setTimeout(() => reloadKLineData(shouldScrollToEnd), 100)
			return
		}

		const processedData = processKLineData(klineData, {
			selectedMainIndicator,
			selectedSubIndicator,
			showVolumeChart
		}, isDarkTheme)
		const newOptionList = packOptionList(processedData, {
			isDarkTheme,
			selectedTimeType,
			selectedMainIndicator,
			selectedSubIndicator,
			selectedDrawTool,
			showIndicatorSelector,
			showTimeSelector,
			showDrawToolSelector,
			klineData,
			drawShouldContinue,
			optionList,
			lastDataLength,
			currentScrollPosition,
			showVolumeChart,
			candleCornerRadius
		}, shouldScrollToEnd, kLineViewRef.current ? true : false)
		setOptionListValue(newOptionList)
	}, [klineData, selectedMainIndicator, selectedSubIndicator, showVolumeChart, isDarkTheme, selectedTimeType, selectedDrawTool, showIndicatorSelector, showTimeSelector, showDrawToolSelector, drawShouldContinue, optionList, lastDataLength, currentScrollPosition, candleCornerRadius])

	// Load initial data when component mounts and ref is available
	useEffect(() => {
		if (kLineViewRef.current && klineData.length > 0 && !initialDataLoaded) {
			console.log('Loading initial candlesticks via imperative API:', klineData.length)
			const processedData = processKLineData(klineData, {
				selectedMainIndicator,
				selectedSubIndicator,
				showVolumeChart
			}, isDarkTheme)

			setTimeout(() => {
				kLineViewRef.current?.addCandlesticksAtTheEnd(processedData)
				setInitialDataLoaded(true)
			}, 200) // Give chart time to initialize
		}
	}, [kLineViewRef.current, klineData, selectedMainIndicator, selectedSubIndicator, showVolumeChart, isDarkTheme, initialDataLoaded])

	// Reload K-line data and adjust scroll position to maintain current view
	const reloadKLineDataWithScrollAdjustment = useCallback((addedDataCount) => {
		if (!kLineViewRef.current) {
			setTimeout(() => reloadKLineDataWithScrollAdjustment(addedDataCount), 100)
			return
		}

		const processedData = processKLineData(klineData, {
			selectedMainIndicator,
			selectedSubIndicator,
			showVolumeChart
		}, isDarkTheme)
		const newOptionList = packOptionList(processedData, {
			isDarkTheme,
			selectedTimeType,
			selectedMainIndicator,
			selectedSubIndicator,
			selectedDrawTool,
			showIndicatorSelector,
			showTimeSelector,
			showDrawToolSelector,
			klineData,
			drawShouldContinue,
			optionList,
			lastDataLength,
			currentScrollPosition,
			showVolumeChart,
			candleCornerRadius
		}, false)

		// Calculate scroll distance adjustment needed (based on item width)
		const pixelRatio = Platform.select({
			android: PixelRatio.get(),
			ios: 1,
		})
		const itemWidth = 8 * pixelRatio // This matches itemWidth in configList
		const scrollAdjustment = addedDataCount * itemWidth

		// Set scroll position adjustment parameters
		newOptionList.scrollPositionAdjustment = scrollAdjustment

		console.log(`Adjust scroll position: ${addedDataCount} data points, scroll distance: ${scrollAdjustment}px`)

		setOptionListValue(newOptionList)
	}, [klineData, selectedMainIndicator, selectedSubIndicator, showVolumeChart, isDarkTheme, selectedTimeType, selectedDrawTool, showIndicatorSelector, showTimeSelector, showDrawToolSelector, drawShouldContinue, optionList, lastDataLength, currentScrollPosition, candleCornerRadius])

	// Set optionList property
	const setOptionListValue = useCallback((optionList) => {
		setOptionList(JSON.stringify(optionList))
	}, [])

	// Drawing item touch event
	const onDrawItemDidTouch = useCallback((event) => {
		const { nativeEvent } = event
		console.log('Drawing item touched:', nativeEvent)
	}, [])

	// Chart touch event
	const onChartTouch = useCallback((event) => {
		const { nativeEvent } = event
		console.log('Chart touched:', nativeEvent)

		if (nativeEvent.isOnClosePriceLabel) {
			console.log('🎯 Touched close price label! Scroll to latest position')
			scrollToPresent()
		}
	}, [scrollToPresent])

	// Scroll to latest position
	const scrollToPresent = useCallback(() => {
		reloadKLineData(true)
	}, [reloadKLineData])

	// Drawing item complete event
	const onDrawItemComplete = useCallback((event) => {
		const { nativeEvent } = event
		console.log('Drawing item complete:', nativeEvent)

		// Processing after drawing completion
		if (!drawShouldContinue) {
			selectDrawTool(DrawTypeConstants.none)
		}
	}, [drawShouldContinue, selectDrawTool])

	// Drawing point complete event
	const onDrawPointComplete = useCallback((event) => {
		const { nativeEvent } = event
		console.log('Drawing point complete:', nativeEvent.pointCount)

		// Can display current drawing progress here
		const currentTool = selectedDrawTool
		const totalPoints = DrawToolHelper.count(currentTool)

		if (totalPoints > 0) {
			const progress = `${nativeEvent.pointCount}/${totalPoints}`
			console.log(`Drawing progress: ${progress}`)
		}
	}, [selectedDrawTool])


	const handleTestAddCandlesticksAtTheStart = useCallback(() => {
		console.log("handleTestAddCandlesticksAtTheStart called")
		testAddCandlesticksAtTheStart(klineData, showVolumeChart, firstCandleTimeRef.current, (candlesticks) => {
			kLineViewRef.current?.addCandlesticksAtTheStart(candlesticks)
			firstCandleTimeRef.current = candlesticks[0].time
		})
	}, [klineData, showVolumeChart,kLineViewRef.current,firstCandleTimeRef.current])

	// Handle new data loading triggered by left swipe
	const handleScrollLeft = useCallback((event) => {
			console.log('Loading 200 new historical candlesticks at start')
			handleTestAddCandlesticksAtTheStart()
	}, [handleTestAddCandlesticksAtTheStart])


	// Wrapper functions for test utilities
	const handleTestUpdateLastCandlestick = useCallback(() => {
		testUpdateLastCandlestick(klineData, showVolumeChart, (candlestick) => {
			kLineViewRef.current?.updateLastCandlestick(candlestick)
		})
	}, [klineData, showVolumeChart,kLineViewRef.current])

	const handleTestAddCandlesticksAtTheEnd = useCallback(() => {
		testAddCandlesticksAtTheEnd(klineData, showVolumeChart, (candlesticks) => {
			kLineViewRef.current?.addCandlesticksAtTheEnd(candlesticks)
		})
	}, [klineData, showVolumeChart,kLineViewRef.current])

	// Order line management
	const [orderIdCounter, setOrderIdCounter] = useState(1)
	const [orderLines, setOrderLines] = useState({})

	// Buy/sell mark management
	const [buySellMarkIdCounter, setBuySellMarkIdCounter] = useState(1)
	const [buySellMarks, setBuySellMarks] = useState({})

	const handleAddLimitOrder = useCallback((price, label) => {
		if (!kLineViewRef.current) return

		const orderLine = {
			id: `limit-order-${orderIdCounter}`,
			type: 'limit',
			price: price,
			amount: 1,
			color: '#00FF00', // Green color for the order line
			label: label || `Limit ${orderIdCounter}`,
			labelFontSize: 14,
			labelBackgroundColor: '#114411AA', // Black background for the label pill
			labelColor: '#FFFFFF', // Green color for the label text
			labelDescription: 'BUY', // Description text
			labelDescriptionColor: '#00FF00' // Gold color for the description text
		}

		console.log('Adding limit order:', orderLine)
		kLineViewRef.current.addOrderLine(orderLine)
		setOrderLines(prev => ({ ...prev, [orderLine.id]: orderLine }))
		setOrderIdCounter(prev => prev + 1)
	}, [kLineViewRef.current, orderIdCounter])

	const handleUpdateOrder = useCallback((orderId, newPrice) => {
		if (!kLineViewRef.current) return

		const existingOrder = orderLines[orderId]
		if (!existingOrder) {
			console.warn(`Order with ID ${orderId} not found`)
			return
		}

		const updatedOrderLine = {
			...existingOrder,
			price: newPrice,
			color: '#FF9500', // Orange color for updated orders
			label: `${existingOrder.label} (Updated)`,
			labelFontSize: 12,
			labelBackgroundColor: '#333333' // Dark gray background for updated orders
		}

		console.log('Updating order:', updatedOrderLine)
		kLineViewRef.current.updateOrderLine(updatedOrderLine)
		setOrderLines(prev => ({ ...prev, [orderId]: updatedOrderLine }))
	}, [kLineViewRef.current, orderLines])

	// Get current price for the input component
	const getCurrentPrice = useCallback(() => {
		if (klineData.length > 0) {
			return klineData[klineData.length - 1].close
		}
		return null
	}, [klineData])

	// Buy/sell mark handlers
	const handleAddBuySellMark = useCallback((type, time, price, amount, orderCount) => {
		if (!kLineViewRef.current) return

		const finalPrice = price || getCurrentPrice() || 0
		const finalAmount = amount || '1.0'

		const buySellMark = {
			id: `buysell-mark-${buySellMarkIdCounter}`,
			time: time,
			type: type, // 'buy' or 'sell'
			amount: finalAmount,
			price: finalPrice.toString(),
			orderCount: orderCount || 1,
			tooltipText: `${type.toUpperCase()} ${finalAmount} at ${finalPrice.toFixed(2)}`
		}

		console.log('Adding buy/sell mark:', buySellMark)
		kLineViewRef.current.addBuySellMark(buySellMark)
		setBuySellMarks(prev => ({ ...prev, [buySellMark.id]: buySellMark }))
		setBuySellMarkIdCounter(prev => prev + 1)
	}, [kLineViewRef.current, buySellMarkIdCounter, getCurrentPrice])

	const handleRemoveBuySellMark = useCallback((markId) => {
		if (!kLineViewRef.current) return

		console.log('Removing buy/sell mark:', markId)
		kLineViewRef.current.removeBuySellMark(markId)
		setBuySellMarks(prev => {
			const newMarks = { ...prev }
			delete newMarks[markId]
			return newMarks
		})
	}, [kLineViewRef.current])

	const handleUpdateBuySellMark = useCallback((markId, newType, newPrice, newAmount, newOrderCount) => {
		if (!kLineViewRef.current) return

		const existingMark = buySellMarks[markId]
		if (!existingMark) {
			console.warn(`Buy/sell mark with ID ${markId} not found`)
			return
		}

		const finalType = newType || existingMark.type
		const finalPrice = newPrice || parseFloat(existingMark.price)
		const finalAmount = newAmount || existingMark.amount

		const updatedMark = {
			...existingMark,
			type: finalType,
			price: finalPrice.toString(),
			amount: finalAmount,
			orderCount: newOrderCount || existingMark.orderCount,
			tooltipText: `${finalType.toUpperCase()} ${finalAmount} at ${finalPrice.toFixed(2)}`
		}

		console.log('Updating buy/sell mark:', updatedMark)
		kLineViewRef.current.updateBuySellMark(updatedMark)
		setBuySellMarks(prev => ({ ...prev, [markId]: updatedMark }))
	}, [kLineViewRef.current, buySellMarks])



	const renderKLineChart = useCallback((styles) => {
		const directRender = (
			<RNKLineView
				ref={kLineViewRef}
				style={styles.chart}
				optionList={optionList}
				onDrawItemDidTouch={onDrawItemDidTouch}
				onScrollLeft={handleScrollLeft}
				onChartTouch={onChartTouch}
				onDrawItemComplete={onDrawItemComplete}
				onDrawPointComplete={onDrawPointComplete}
			/>
		)
		if (global?.nativeFabricUIManager && Platform.OS == 'ios') {
			return directRender
		}
		return (
			<View style={{ flex: 1 }} collapsable={false}>
				<View style={{ flex: 1 }} collapsable={false}>
					<View style={styles.chartContainer} collapsable={false}>
						{directRender}
					</View>
				</View>
			</View>
		)
	}, [optionList, onDrawItemDidTouch, handleScrollLeft, onChartTouch, onDrawItemComplete, onDrawPointComplete])

	const getStyles = useCallback((theme) => {
		return StyleSheet.create({
			container: {
				flex: 1,
				backgroundColor: theme.backgroundColor,
				paddingTop: isHorizontalScreen ? 10 : 50,
				paddingBottom: isHorizontalScreen ? 20 : 100,
			},
			chartContainer: {
				flex: 1,
				margin: 8,
				borderRadius: 8,
				backgroundColor: theme.backgroundColor,
				borderWidth: 1,
				borderColor: theme.gridColor,
			},
			chart: {
				flex: 1,
				backgroundColor: 'transparent',
			},
		})
	}, [])

	const theme = ThemeManager.getCurrentTheme(isDarkTheme)
	const styles = getStyles(theme)
	console.log("App.js render", Platform.OS)

	return (
		<View style={styles.container}>
			{/* Top toolbar */}
			<Toolbar
				theme={theme}
				isDarkTheme={isDarkTheme}
				onToggleTheme={toggleTheme}
				onTestUpdate={handleTestUpdateLastCandlestick}
				onTestAddCandles={handleTestAddCandlesticksAtTheEnd}
				onTestAddCandlesAtStart={handleTestAddCandlesticksAtTheStart}
			/>

			{/* K-line chart */}
			<View style={{height: 400}}>
				{renderKLineChart(styles)}
			</View>


			<ScrollView style={{maxHeight: 400}}>
				{/* Order input */}
				<OrderInput
					theme={theme}
					onAddOrder={handleAddLimitOrder}
					onUpdateOrder={handleUpdateOrder}
					currentPrice={getCurrentPrice()}
					orderLines={orderLines}
				/>

				{/* Buy/Sell mark input */}
				<BuySellMarkInput
					theme={theme}
					onAddBuySellMark={handleAddBuySellMark}
					onRemoveBuySellMark={handleRemoveBuySellMark}
					onUpdateBuySellMark={handleUpdateBuySellMark}
					currentPrice={getCurrentPrice()}
					buySellMarks={buySellMarks}
					klineData={klineData}
				/>

				{/* Bottom control bar */}
				<ControlBar
					theme={theme}
					selectedTimeType={selectedTimeType}
					selectedMainIndicator={selectedMainIndicator}
					selectedSubIndicator={selectedSubIndicator}
					selectedDrawTool={selectedDrawTool}
					showVolumeChart={showVolumeChart}
					candleCornerRadius={candleCornerRadius}
					onShowTimeSelector={() => setShowTimeSelector(true)}
					onShowIndicatorSelector={() => setShowIndicatorSelector(true)}
					onToggleDrawToolSelector={() => {
						setShowDrawToolSelector(!showDrawToolSelector)
						setShowIndicatorSelector(false)
						setShowTimeSelector(false)
					}}
					onClearDrawings={clearDrawings}
					onToggleVolume={() => {
						setShowVolumeChart(!showVolumeChart)
						setTimeout(() => reloadKLineData(), 0)
					}}
					onToggleRounded={() => {
						setCandleCornerRadius(candleCornerRadius > 0 ? 0 : 1)
						setTimeout(() => reloadKLineData(), 0)
					}}
				/>

				{/* Selector popup */}
				<Selectors
					theme={theme}
					showTimeSelector={showTimeSelector}
					showIndicatorSelector={showIndicatorSelector}
					showDrawToolSelector={showDrawToolSelector}
					selectedTimeType={selectedTimeType}
					selectedMainIndicator={selectedMainIndicator}
					selectedSubIndicator={selectedSubIndicator}
					selectedDrawTool={selectedDrawTool}
					drawShouldContinue={drawShouldContinue}
					onSelectTimeType={selectTimeType}
					onSelectIndicator={selectIndicator}
					onSelectDrawTool={selectDrawTool}
					onCloseTimeSelector={() => setShowTimeSelector(false)}
					onCloseIndicatorSelector={() => setShowIndicatorSelector(false)}
					onToggleDrawShouldContinue={(value) => setDrawShouldContinue(value)}
				/>
			</ScrollView>
		</View>
	)
}

export default App
