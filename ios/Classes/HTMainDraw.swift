//
//  HTMainDraw.swift
//  HTKLineView
//
//  Created by hublot on 2020/3/17.
//  Copyright © 2020 hublot. All rights reserved.
//

import UIKit

class HTMainDraw: NSObject, HTKLineDrawProtocol {

    func minMaxRange(_ visibleModelArray: [HTKLineModel], _ configManager: HTKLineConfigManager) -> Range<CGFloat> {
        var maxValue = CGFloat.leastNormalMagnitude
        var minValue = CGFloat.greatestFiniteMagnitude

        for model in visibleModelArray {
            var valueList = [model.high, model.low]
            switch configManager.mainType {
            case .ma:
                valueList.append(contentsOf: model.maList.map({ (item) -> CGFloat in
                    return item.value
                }))
                break
            case .boll:
                valueList.append(contentsOf: [model.bollMb, model.bollUp, model.bollDn])
                break
            default:
                break
            }
            maxValue = max(maxValue, valueList.max() ?? 0)
            minValue = min(minValue, valueList.min() ?? 0)
        }
        return Range<CGFloat>.init(uncheckedBounds: (lower: minValue, upper: maxValue))
    }

    func drawCandle(_ model: HTKLineModel, _ index: Int, _ maxValue: CGFloat, _ minValue: CGFloat, _ baseY: CGFloat, _ height: CGFloat, _ context: CGContext, _ configManager: HTKLineConfigManager) {
        let color = model.increment ? configManager.increaseColor : configManager.decreaseColor
        let wickColor = model.increment ? configManager.increaseWickColor : configManager.decreaseWickColor
        let findValue: (Bool) -> CGFloat = { (isHighValue: Bool) in
            var findCloseValue = model.increment
            if (!isHighValue) {
                findCloseValue = !findCloseValue
            }
            return findCloseValue ? model.close : model.open
        }
        if (configManager.isMinute) {

        } else {
            // Draw wick first (behind body)
            drawCandle(high: model.high, low: model.low, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, width: configManager.candleLineWidth, color: wickColor, verticalAlignBottom: false, context: context, configManager: configManager)

            // Draw body second (on top)
            let candleHigh = findValue(true)
            let candleLow = findValue(false)

            // Handle case when open == close (doji candlestick)
            if candleHigh == candleLow {
                // Draw a thin horizontal line to make the doji visible, similar to Android behavior
                drawCandleWithMinHeight(high: candleHigh, low: candleLow, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, width: configManager.candleWidth, color: color, context: context, configManager: configManager)
            } else {
                drawCandle(high: candleHigh, low: candleLow, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, width: configManager.candleWidth, color: color, verticalAlignBottom: false, context: context, configManager: configManager)
            }
        }
    }

    func drawCandleWithMinHeight(high: CGFloat, low: CGFloat, maxValue: CGFloat, minValue: CGFloat, baseY: CGFloat, height: CGFloat, index: Int, width: CGFloat, color: UIColor, context: CGContext, configManager: HTKLineConfigManager) {
        let itemWidth = configManager.itemWidth
        let scale = (maxValue - minValue) / height
        let paddingHorizontal = (itemWidth - width) / 2.0
        let x = CGFloat(index) * itemWidth + paddingHorizontal
        let y = baseY + (maxValue - high) / scale

        // Ensure minimum height of 1 pixel for doji candles (open == close)
        let minHeightInPixels: CGFloat = 1.0
        let candleHeight = max(minHeightInPixels, (high - low) / scale)

        context.setFillColor(color.cgColor)

        if configManager.candleCornerRadius > 0 {
            // Draw rounded rectangle
            let rect = CGRect(x: x, y: y, width: width, height: candleHeight)
            let path = UIBezierPath(roundedRect: rect, cornerRadius: configManager.candleCornerRadius)
            context.addPath(path.cgPath)
            context.fillPath()
        } else {
            // Draw regular rectangle with minimum height
            context.fill(CGRect.init(x: x, y: y, width: width, height: candleHeight))
        }
    }

    func drawGradient(_ visibleModelArray: [HTKLineModel], _ maxValue: CGFloat, _ minValue: CGFloat, _ baseX: CGFloat, _ baseY: CGFloat, _ height: CGFloat, _ context: CGContext, _ configManager: HTKLineConfigManager) {
        let colorList = configManager.packGradientColorList(configManager.minuteGradientColorList)
        let locationList = configManager.minuteGradientLocationList
        if let gradient = CGGradient.init(colorSpace: CGColorSpaceCreateDeviceRGB(), colorComponents: colorList, locations: locationList, count: locationList.count) {
            var bezierPath = UIBezierPath.init()
            for (i, model) in visibleModelArray.enumerated() {
                let lastIndex = i == 0 ? i : i - 1
                let lastModel = visibleModelArray[lastIndex]
                bezierPath = createLinePath(value: model.close, lastValue: lastModel.close, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: i, lastIndex: lastIndex, isBezier: true, existPath: bezierPath, context: context, configManager: configManager)
            }
            bezierPath.addLine(to: CGPoint.init(x: bezierPath.currentPoint.x, y: baseY + height))
            bezierPath.addLine(to: CGPoint.init(x: configManager.itemWidth / 2, y: baseY + height))
            bezierPath.close()
            context.addPath(bezierPath.cgPath)
            context.clip()
            context.drawLinearGradient(gradient, start: CGPoint.init(x: 0, y: baseY), end: CGPoint.init(x: 0, y: height + baseY), options: .drawsBeforeStartLocation)
            context.resetClip()
        }
    }

    func drawLine(_ model: HTKLineModel, _ lastModel: HTKLineModel, _ maxValue: CGFloat, _ minValue: CGFloat, _ baseY: CGFloat, _ height: CGFloat, _ index: Int, _ lastIndex: Int, _ context: CGContext, _ configManager: HTKLineConfigManager) {
        if (configManager.isMinute) {
            drawLine(value: model.close, lastValue: lastModel.close, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, lastIndex: lastIndex, color: configManager.minuteLineColor, isBezier: true, context: context, configManager: configManager)
        } else {
            switch configManager.mainType {
            case .none:
                break
            case .ma:
                // Use the embedded indicator data from candlestick rather than config manager's empty list
                for (i, itemModel) in model.maList.enumerated() {
                    guard i < lastModel.maList.count && itemModel.index < configManager.targetColorList.count else { continue }
                    let color = configManager.targetColorList[itemModel.index]
                    drawLine(value: itemModel.value, lastValue: lastModel.maList[i].value, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, lastIndex: lastIndex, color: color, isBezier: false, context: context, configManager: configManager)
                }
            case .boll:
                let itemList = [
                    ["value": model.bollMb, "lastValue": lastModel.bollMb, "color": configManager.targetColorList[0]],
                    ["value": model.bollUp, "lastValue": lastModel.bollUp, "color": configManager.targetColorList[1]],
                    ["value": model.bollDn, "lastValue": lastModel.bollDn, "color": configManager.targetColorList[2]],
                ]
                for item in itemList {
                    drawLine(value: item["value"] as? CGFloat ?? 0, lastValue: item["lastValue"] as? CGFloat ?? 0, maxValue: maxValue, minValue: minValue, baseY: baseY, height: height, index: index, lastIndex: lastIndex, color: item["color"] as? UIColor ?? UIColor.orange, isBezier: false, context: context, configManager: configManager)
                }
            }
        }
    }

    func drawText(_ model: HTKLineModel, _ baseX: CGFloat, _ baseY: CGFloat, _ context: CGContext, _ configManager: HTKLineConfigManager) {
        if (configManager.isMinute) {

        } else {
            switch configManager.mainType {
            case .none:
                break
            case .ma:
                var x = baseX
                // Use the embedded indicator data from candlestick rather than config manager's empty list
                for item in model.maList {
                    guard item.index < configManager.targetColorList.count else { continue }
                    let title = String(format: "MA%@:%@", item.title, configManager.precision(item.value, configManager.price))
                    let color = configManager.targetColorList[item.index]
                    let font = configManager.createFont(configManager.headerTextFontSize)
                    x += drawText(title: title, point: CGPoint.init(x: x, y: baseY), color: color, font: font, context: context, configManager: configManager)
                    x += 5
                }
            case .boll:
                var x = baseX
                let itemList = [
                    ["title": String(format: "BOLL:%@", configManager.precision(model.bollMb, configManager.price)), "color": configManager.targetColorList[0]],
                    ["title": String(format: "UB:%@", configManager.precision(model.bollUp, configManager.price)), "color": configManager.targetColorList[1]],
                    ["title": String(format: "LB:%@", configManager.precision(model.bollDn, configManager.price)), "color": configManager.targetColorList[2]],
                ]
                let font = configManager.createFont(configManager.headerTextFontSize)
                for item in itemList {
                    x += drawText(title: item["title"] as? String ?? "", point: CGPoint.init(x: x, y: baseY), color: item["color"] as? UIColor ?? UIColor.orange, font: font, context: context, configManager: configManager)
                    x += 5
                }
            }
        }
    }

    func drawValue(_ maxValue: CGFloat, _ minValue: CGFloat, _ baseX: CGFloat, _ baseY: CGFloat, _ height: CGFloat, _ context: CGContext, _ configManager: HTKLineConfigManager) {
        drawValue(maxValue, minValue, baseX, baseY, height, 4, configManager.price, context, configManager)
    }

}
