/**
 * Business Logic Functions
 * Core data processing and business logic for K-line chart application
 */

import { Platform, PixelRatio, processColor } from 'react-native'
import {
	calculateBOLL,
	calculateMACD,
	calculateKDJ,
	calculateMAWithConfig,
	calculateVolumeMAWithConfig,
	calculateRSIWithConfig,
	calculateWRWithConfig
} from './indicators'
import { ThemeManager, COLOR } from './themes'
import { TimeTypes, DrawStateConstants, FORMAT } from './constants'
import { fixRound, formatTime, isHorizontalScreen } from './helpers'

/**
 * Get target configuration list for indicators
 */
export const getTargetList = (selectedIndicators) => {
	const {
		selectedMainIndicator,
		selectedSubIndicator,
		showVolumeChart
	} = selectedIndicators

	// Indicator judgment helper functions
	const isMASelected = () => selectedMainIndicator === 1
	const isBOLLSelected = () => selectedMainIndicator === 2
	const isMACDSelected = () => selectedSubIndicator === 3
	const isKDJSelected = () => selectedSubIndicator === 4
	const isRSISelected = () => selectedSubIndicator === 5
	const isWRSelected = () => selectedSubIndicator === 6

	return {
		maList: [
			{ title: '5', selected: isMASelected(), index: 0 },
			{ title: '10', selected: isMASelected(), index: 1 },
			{ title: '20', selected: isMASelected(), index: 2 },
		],
		maVolumeList: [
			{ title: '5', selected: showVolumeChart, index: 0 },
			{ title: '10', selected: showVolumeChart, index: 1 },
		],
		bollN: '20',
		bollP: '2',
		macdS: '12',
		macdL: '26',
		macdM: '9',
		kdjN: '9',
		kdjM1: '3',
		kdjM2: '3',
		rsiList: [
			{ title: '6', selected: isRSISelected(), index: 0 },
			{ title: '12', selected: isRSISelected(), index: 1 },
			{ title: '24', selected: isRSISelected(), index: 2 },
		],
		wrList: [
			{ title: '14', selected: isWRSelected(), index: 0 },
		],
		// Helper functions for external use
		isMASelected,
		isBOLLSelected,
		isMACDSelected,
		isKDJSelected,
		isRSISelected,
		isWRSelected
	}
}

/**
 * Calculate technical indicators based on target configuration
 */
export const calculateIndicatorsFromTargetList = (data, targetList, showVolumeChart) => {
	let processedData = [...data]

	// Calculate MA indicator
	const selectedMAPeriods = targetList.maList
		.filter(item => item.selected)
		.map(item => ({ period: parseInt(item.title, 10), index: item.index }))

	if (selectedMAPeriods.length > 0) {
		processedData = calculateMAWithConfig(processedData, selectedMAPeriods)
	}

	// Calculate volume MA indicator
	const selectedVolumeMAPeriods = targetList.maVolumeList
		.filter(item => item.selected)
		.map(item => ({ period: parseInt(item.title, 10), index: item.index }))

	if (selectedVolumeMAPeriods.length > 0 && showVolumeChart) {
		processedData = calculateVolumeMAWithConfig(processedData, selectedVolumeMAPeriods)
	}

	// Calculate BOLL indicator
	if (targetList.isBOLLSelected()) {
		processedData = calculateBOLL(processedData, parseInt(targetList.bollN, 10), parseInt(targetList.bollP, 10))
	}

	// Calculate MACD indicator
	if (targetList.isMACDSelected()) {
		processedData = calculateMACD(processedData,
			parseInt(targetList.macdS, 10),
			parseInt(targetList.macdL, 10),
			parseInt(targetList.macdM, 10))
	}

	// Calculate KDJ indicator
	if (targetList.isKDJSelected()) {
		processedData = calculateKDJ(processedData,
			parseInt(targetList.kdjN, 10),
			parseInt(targetList.kdjM1, 10),
			parseInt(targetList.kdjM2, 10))
	}

	// Calculate RSI indicator
	const selectedRSIPeriods = targetList.rsiList
		.filter(item => item.selected)
		.map(item => ({ period: parseInt(item.title, 10), index: item.index }))

	if (selectedRSIPeriods.length > 0) {
		processedData = calculateRSIWithConfig(processedData, selectedRSIPeriods)
	}

	// Calculate WR indicator
	const selectedWRPeriods = targetList.wrList
		.filter(item => item.selected)
		.map(item => ({ period: parseInt(item.title, 10), index: item.index }))

	if (selectedWRPeriods.length > 0) {
		processedData = calculateWRWithConfig(processedData, selectedWRPeriods)
	}

	return processedData
}

/**
 * Add technical indicators to selected item list
 */
export const addIndicatorToSelectedList = (item, targetList, priceCount) => {
	// Add MA indicator display
	if (targetList.isMASelected() && item.maList) {
		item.maList.forEach((ma, index) => {
			if (ma && ma.value != null) {
				item.selectedItemList.push({
					title: `MA${ma.title}`,
					detail: fixRound(ma.value, priceCount, true, false)
				})
			}
		})
	}

	// Add BOLL indicator display
	if (targetList.isBOLLSelected() && item.bollMb != null) {
		item.selectedItemList.push(
			{ title: 'BOLL-MB', detail: fixRound(item.bollMb, priceCount, true, false) },
			{ title: 'BOLL-UP', detail: fixRound(item.bollUp, priceCount, true, false) },
			{ title: 'BOLL-DN', detail: fixRound(item.bollDn, priceCount, true, false) }
		)
	}

	// Add MACD indicator display
	if (targetList.isMACDSelected() && item.macdValue != null) {
		item.selectedItemList.push(
			{ title: 'MACD', detail: fixRound(item.macdValue, 4, true, false) },
			{ title: 'DEA', detail: fixRound(item.macdDea, 4, true, false) },
			{ title: 'DIF', detail: fixRound(item.macdDif, 4, true, false) }
		)
	}

	// Add KDJ indicator display
	if (targetList.isKDJSelected() && item.kdjK != null) {
		item.selectedItemList.push(
			{ title: 'K', detail: fixRound(item.kdjK, 2, true, false) },
			{ title: 'D', detail: fixRound(item.kdjD, 2, true, false) },
			{ title: 'J', detail: fixRound(item.kdjJ, 2, true, false) }
		)
	}

	// Add RSI indicator display
	if (targetList.isRSISelected() && item.rsiList) {
		item.rsiList.forEach((rsi, index) => {
			if (rsi && rsi.value != null) {
				item.selectedItemList.push({
					title: `RSI${rsi.title}`,
					detail: fixRound(rsi.value, 2, true, false)
				})
			}
		})
	}

	// Add WR indicator display
	if (targetList.isWRSelected() && item.wrList) {
		item.wrList.forEach((wr, index) => {
			if (wr && wr.value != null) {
				item.selectedItemList.push({
					title: `WR${wr.title}`,
					detail: fixRound(wr.value, 2, true, false)
				})
			}
		})
	}
}

/**
 * Process K-line data, add technical indicator calculations
 */
export const processKLineData = (rawData, selectedIndicators, isDarkTheme) => {
	// Simulate symbol configuration
	const symbol = {
		price: 2, // Price precision
		volume: 0 // Volume precision
	}
	const priceCount = symbol.price
	const volumeCount = symbol.volume

	// Get target configuration
	const targetList = getTargetList(selectedIndicators)

	// Calculate all technical indicators
	let processedData = rawData.map(item => ({
		...item,
		id: item.time,
		open: item.open,
		high: item.high,
		low: item.low,
		close: item.close,
		vol: item.vol || item.volume || 0,  // Use vol field, fallback to volume for compatibility
	}))

	// Calculate technical indicators based on targetList configuration
	processedData = calculateIndicatorsFromTargetList(processedData, targetList, selectedIndicators.showVolumeChart)

	return processedData.map((item, index) => {
		// Time formatting
		let time = formatTime(item.id, 'MM-DD HH:mm')

		// Calculate price change amount and percentage
		let appendValue = item.close - item.open
		let appendPercent = appendValue / item.open * 100
		let isAppend = appendValue >= 0
		let prefixString = isAppend ? '+' : '-'
		let appendValueString = prefixString + fixRound(Math.abs(appendValue), priceCount, true, false)
		let appendPercentString = prefixString + fixRound(Math.abs(appendPercent), 2, true, false) + '%'

		// Color configuration
		const theme = ThemeManager.getCurrentTheme(isDarkTheme)
		let color = isAppend ? processColor(theme.increaseColor) : processColor(theme.decreaseColor)

		// Add formatted fields
		item.dateString = `${time}`
		item.selectedItemList = [
			{ title: FORMAT('Time'), detail: `${time}` },
			{ title: FORMAT('Open'), detail: fixRound(item.open, priceCount, true, false) },
			{ title: FORMAT('High'), detail: fixRound(item.high, priceCount, true, false) },
			{ title: FORMAT('Low'), detail: fixRound(item.low, priceCount, true, false) },
			{ title: FORMAT('Close'), detail: fixRound(item.close, priceCount, true, false) },
			{ title: FORMAT('Change'), detail: appendValueString, color },
			{ title: FORMAT('Change %'), detail: appendPercentString, color },
			{ title: FORMAT('Volume'), detail: fixRound(item.vol, volumeCount, true, false) }
		]

		// Add technical indicator display info to selectedItemList
		addIndicatorToSelectedList(item, targetList, priceCount)

		return item
	})
}

/**
 * Pack option list for chart configuration
 */
export const packOptionList = (modelArray, appState, shouldScrollToEnd = true, useImperativeApi = false) => {
	const {
		isDarkTheme,
		selectedTimeType,
		selectedMainIndicator,
		selectedSubIndicator,
		selectedDrawTool,
		showVolumeChart,
		candleCornerRadius,
		drawShouldContinue,
		minVisibleCandles
	} = appState

	const theme = ThemeManager.getCurrentTheme(isDarkTheme)

	// Basic configuration
	const pixelRatio = Platform.select({
		android: PixelRatio.get(),
		ios: 1,
	})

	const configList = {
		colorList: {
			increaseColor: processColor(theme.increaseColor),
			decreaseColor: processColor(theme.decreaseColor),
		},
		targetColorList: [
			processColor(COLOR(0.96, 0.86, 0.58)),
			processColor(COLOR(0.38, 0.82, 0.75)),
			processColor(COLOR(0.8, 0.57, 1)),
			processColor(COLOR(1, 0.23, 0.24)),
			processColor(COLOR(0.44, 0.82, 0.03)),
			processColor(COLOR(0.44, 0.13, 1)),
		],
		minuteLineColor: processColor(theme.minuteLineColor),
		minuteGradientColorList: [
			processColor(COLOR(0.094117647, 0.341176471, 0.831372549, 0.149019608)), // 15% transparent blue
			processColor(COLOR(0.266666667, 0.501960784, 0.972549020, 0.149019608)), // 26% transparent blue
			processColor(COLOR(0.074509804, 0.121568627, 0.188235294, 0)), // Fully transparent
			processColor(COLOR(0.074509804, 0.121568627, 0.188235294, 0)), // Fully transparent
		],
		minuteGradientLocationList: [0, 0.3, 0.6, 1],
		backgroundColor: processColor(theme.backgroundColor),
		textColor: processColor(theme.detailColor),
		gridColor: processColor(theme.gridColor),
		candleTextColor: processColor(theme.titleColor),
		panelBackgroundColor: processColor(isDarkTheme ? COLOR(0.03, 0.09, 0.14, 0.9) : COLOR(1, 1, 1, 0.95)), // 95% transparency
		panelBorderColor: processColor(theme.detailColor),
		panelTextColor: processColor(theme.titleColor),
		selectedPointContainerColor: processColor('transparent'),
		selectedPointContentColor: processColor(isDarkTheme ? theme.titleColor : 'white'),
		closePriceCenterBackgroundColor: processColor(theme.backgroundColor9703),
		closePriceCenterBorderColor: processColor(theme.textColor7724),
		closePriceCenterTriangleColor: processColor(theme.textColor7724),
		closePriceCenterSeparatorColor: processColor(theme.detailColor),
		closePriceRightBackgroundColor: processColor(theme.backgroundColor),
		closePriceRightSeparatorColor: processColor(theme.backgroundColorBlue),
		closePriceRightLightLottieFloder: 'images',
		closePriceRightLightLottieScale: 0.4,
		panelGradientColorList: isDarkTheme ? [
			processColor(COLOR(0.0588235, 0.101961, 0.160784, 0.2)),
			processColor(COLOR(0.811765, 0.827451, 0.913725, 0.101961)),
			processColor(COLOR(0.811765, 0.827451, 0.913725, 0.2)),
			processColor(COLOR(0.811765, 0.827451, 0.913725, 0.101961)),
			processColor(COLOR(0.0784314, 0.141176, 0.223529, 0.2)),
		] : [
			processColor(COLOR(1, 1, 1, 0)),
			processColor(COLOR(0.54902, 0.623529, 0.678431, 0.101961)),
			processColor(COLOR(0.54902, 0.623529, 0.678431, 0.25098)),
			processColor(COLOR(0.54902, 0.623529, 0.678431, 0.101961)),
			processColor(COLOR(1, 1, 1, 0)),
		],
		panelGradientLocationList: [0, 0.25, 0.5, 0.75, 1],
		mainFlex: showVolumeChart
			? (selectedSubIndicator === 0 ? (isHorizontalScreen ? 0.75 : 0.85) : 0.6)
			: (selectedSubIndicator === 0 ? 1.0 : 0.75),
		volumeFlex: showVolumeChart ? (isHorizontalScreen ? 0.25 : 0.15) : 0,
		paddingTop: 20 * pixelRatio,
		paddingBottom: 20 * pixelRatio,
		paddingRight: 50 * pixelRatio,
		itemWidth: 8 * pixelRatio,
		candleWidth: 6 * pixelRatio,
		candleCornerRadius: candleCornerRadius * pixelRatio,
		minVisibleCandles: minVisibleCandles || 5,
		minuteVolumeCandleColor: processColor(showVolumeChart ? COLOR(0.0941176, 0.509804, 0.831373, 0.501961) : 'transparent'),
		minuteVolumeCandleWidth: showVolumeChart ? 2 * pixelRatio : 0,
		macdCandleWidth: 1 * pixelRatio,
		headerTextFontSize: 10 * pixelRatio,
		rightTextFontSize: 10 * pixelRatio,
		candleTextFontSize: 10 * pixelRatio,
		panelTextFontSize: 10 * pixelRatio,
		panelMinWidth: 130 * pixelRatio,
		fontFamily: Platform.select({
			ios: 'DINPro-Medium',
			android: ''
		}),
		closePriceRightLightLottieSource: '',
	}

	// Use unified target configuration
	const targetList = getTargetList({
		selectedMainIndicator,
		selectedSubIndicator,
		showVolumeChart
	})

	let drawList = {
		'shotBackgroundColor': processColor(theme.backgroundColor),
		// Basic drawing configuration
		'drawType': selectedDrawTool,
		'shouldReloadDrawItemIndex': DrawStateConstants.none,
		'drawShouldContinue': drawShouldContinue,
		'drawColor': processColor(COLOR(1, 0.46, 0.05)),
		'drawLineHeight': 2,
		'drawDashWidth': 4,
		'drawDashSpace': 4,
		'drawIsLock': false,
		'shouldFixDraw': false,
		'shouldClearDraw': false
	}

	return {
		modelArray: modelArray,
		shouldScrollToEnd: shouldScrollToEnd,
		targetList: targetList,
		price: 2, // Price precision
		volume: 0, // Volume precision
		primary: selectedMainIndicator,
		second: selectedSubIndicator,
		time: TimeTypes[selectedTimeType].value,
		configList: configList,
		drawList: drawList,
		useImperativeApi: useImperativeApi
	}
}