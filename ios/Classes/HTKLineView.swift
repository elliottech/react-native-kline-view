//
//  HTKLineView.swift
//  HTKLineView
//
//  Created by hublot on 2020/3/17.
//  Copyright © 2020 hublot. All rights reserved.
//

import Lottie
import UIKit

// Extension to create UIColor from hex string with transparency support
extension UIColor {
    convenience init?(hexString: String) {
        let hex = hexString.trimmingCharacters(in: .whitespacesAndNewlines)
        let scanner = Scanner(string: hex.hasPrefix("#") ? String(hex.dropFirst()) : hex)
        var rgbValue: UInt64 = 0

        guard scanner.scanHexInt64(&rgbValue) else {
            return nil
        }

        let cleanHex = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
        let length = cleanHex.count

        if length == 8 {
            // 8-digit hex with alpha: #RRGGBBAA (React Native style)
            let red = CGFloat((rgbValue & 0xFF000000) >> 24) / 255.0
            let green = CGFloat((rgbValue & 0x00FF0000) >> 16) / 255.0
            let blue = CGFloat((rgbValue & 0x0000FF00) >> 8) / 255.0
            let alpha = CGFloat(rgbValue & 0x000000FF) / 255.0
            self.init(red: red, green: green, blue: blue, alpha: alpha)
        } else if length == 6 {
            // 6-digit hex without alpha: #RRGGBB
            let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
            let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
            let blue = CGFloat(rgbValue & 0x0000FF) / 255.0
            self.init(red: red, green: green, blue: blue, alpha: 1.0)
        } else {
            return nil
        }
    }
}

class HTKLineView: UIScrollView {

    var configManager: HTKLineConfigManager

    lazy var drawContext: HTDrawContext = {
        let drawContext = HTDrawContext.init(self, configManager)
        return drawContext
    }()

    var visibleRange = 0...0

    var selectedIndex = -1

    var scale: CGFloat = 1

    let mainDraw = HTMainDraw.init()

    let volumeDraw = HTVolumeDraw.init()

    let macdDraw = HTMacdDraw.init()

    let kdjDraw = HTKdjDraw.init()

    let rsiDraw = HTRsiDraw.init()

    let wrDraw = HTWrDraw.init()

    var childDraw: HTKLineDrawProtocol?

    var animationView = LottieAnimationView()

    var lastLoadAnimationSource = ""

    private var hasTriggeredScrollLeft = false

    var closePriceLabelFrame: CGRect = .zero

    // Computed properties
    var visibleModelArray = [HTKLineModel]()
    var volumeRange: ClosedRange<CGFloat> = 0...0
    var allWidth: CGFloat = 0
    var allHeight: CGFloat = 0
    var mainMinMaxRange = Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
    var textHeight: CGFloat = 0
    var mainBaseY: CGFloat = 0
    var mainHeight: CGFloat = 0
    var volumeMinMaxRange = Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
    var volumeBaseY: CGFloat = 0
    var volumeHeight: CGFloat = 0
    var childMinMaxRange = Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
    var childBaseY: CGFloat = 0
    var childHeight: CGFloat = 0

    init(_ frame: CGRect, _ configManager: HTKLineConfigManager) {
        self.configManager = configManager
        super.init(frame: frame)
        delegate = self
        bounces = false
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false
        backgroundColor = UIColor.clear

        addGestureRecognizer(
            UILongPressGestureRecognizer.init(target: self, action: #selector(longPressSelector)))
        addGestureRecognizer(
            UITapGestureRecognizer.init(target: self, action: #selector(tapSelector)))
        addGestureRecognizer(
            UIPinchGestureRecognizer.init(target: self, action: #selector(pinchSelector)))
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func reloadConfigManager(_ configManager: HTKLineConfigManager) {

        switch configManager.childType {
        case .none:
            childDraw = nil
        case .macd:
            childDraw = macdDraw
        case .kdj:
            childDraw = kdjDraw
        case .rsi:
            childDraw = rsiDraw
        case .wr:
            childDraw = wrDraw
        }

        let isEnd = contentOffset.x + 1 + bounds.size.width >= contentSize.width
        let previousContentOffset = contentOffset.x
        reloadContentSize()

        if configManager.shouldAdjustScrollPosition {
            // Adjust scroll position to compensate for newly added data
            let newContentOffset = previousContentOffset + configManager.scrollPositionAdjustment
            reloadContentOffset(newContentOffset, false)
        } else if configManager.shouldScrollToEnd || isEnd {
            let toEndContentOffset = contentSize.width - bounds.size.width
            let distance = abs(contentOffset.x - toEndContentOffset)
            let animated = distance <= configManager.itemWidth
            reloadContentOffset(toEndContentOffset, animated)
        }

        scrollViewDidScroll(self)

        guard lastLoadAnimationSource != configManager.closePriceRightLightLottieSource else {
            return
        }

        lastLoadAnimationSource = configManager.closePriceRightLightLottieSource

        DispatchQueue.global().async { [weak self] in
            guard let this = self,
                let data = this.configManager.closePriceRightLightLottieSource.data(
                    using: String.Encoding.utf8),
                let animation = try? JSONDecoder().decode(LottieAnimation.self, from: data)
            else {
                return
            }
            DispatchQueue.main.async {
                this.animationView.animation = animation
                this.animationView.loopMode = .loop
                this.animationView.play()
                var size = animation.size
                let scale = this.configManager.closePriceRightLightLottieScale
                size.width *= scale
                size.height *= scale
                this.animationView.frame.size = size
                this.animationView.isHidden = true
                this.addSubview(this.animationView)
                this.setNeedsDisplay()
            }
        }
    }

    func reloadContentSize() {
        configManager.reloadScrollViewScale(scale)
        let contentWidth =
            configManager.itemWidth * CGFloat(configManager.modelArray.count)
            + configManager.paddingRight
        contentSize = CGSize.init(width: contentWidth, height: frame.size.height)
    }

    func reloadContentOffset(_ contentOffsetX: CGFloat, _ animated: Bool = false) {
        let offsetX = max(0, min(contentOffsetX, contentSize.width - bounds.size.width))
        setContentOffset(CGPoint.init(x: offsetX, y: 0), animated: animated)
    }

    func smoothScrollToEnd() {
        let endOffsetX = contentSize.width - bounds.size.width
        reloadContentOffset(endOffsetX, true)
    }

    func contextTranslate(_ context: CGContext, _ x: CGFloat, _ block: (CGContext) -> Void) {
        context.saveGState()
        context.translateBy(x: x, y: 0)
        block(context)
        context.restoreGState()
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext(), configManager.modelArray.count > 0 else {
            return
        }

        calculateBaseHeight()
        contextTranslate(
            context, CGFloat(visibleRange.lowerBound) * configManager.itemWidth,
            { context in
                drawCandle(context)
            })

        contextTranslate(
            context, contentOffset.x,
            { context in
                //            context.setFillColor(UIColor.red.withAlphaComponent(0.1).cgColor)
                //            context.fill(CGRect.init(x: 0, y: mainBaseY, width: allWidth, height: mainHeight))

                drawText(context)
                drawValue(context)

                drawHighLow(context)
                drawOrderLines(context)
                drawTime(context)
                drawClosePrice(context)
                drawSelectedLine(context)
                drawSelectedBoard(context)
                drawSelectedTime(context)

                drawContext.draw(contentOffset.x)
            })

    }

    func calculateBaseHeight() {
        // Safely handle visible model array calculation with bounds checking
        if configManager.modelArray.count > 0 {
            let startIndex = max(0, min(visibleRange.lowerBound, configManager.modelArray.count - 1))
            let endIndex = max(0, min(visibleRange.upperBound, configManager.modelArray.count - 1))

            if startIndex >= 0 && endIndex >= startIndex && endIndex < configManager.modelArray.count {
                self.visibleModelArray = Array(configManager.modelArray[startIndex...endIndex])
            } else {
                self.visibleModelArray = configManager.modelArray
            }
        } else {
            self.visibleModelArray = configManager.modelArray
        }
        self.volumeRange =
            configManager.mainFlex...configManager.mainFlex + configManager.volumeFlex

        self.allHeight = self.bounds.size.height - configManager.paddingBottom
        self.allWidth = self.bounds.size.width

        self.mainMinMaxRange = mainDraw.minMaxRange(visibleModelArray, configManager)
        self.textHeight = mainDraw.textHeight(font: UIFont.systemFont(ofSize: 11)) / 2
        self.mainBaseY = configManager.paddingTop - textHeight
        self.mainHeight = allHeight * volumeRange.lowerBound - mainBaseY - textHeight

        if configManager.volumeFlex > 0 {
            self.volumeMinMaxRange = volumeDraw.minMaxRange(visibleModelArray, configManager)
            self.volumeBaseY = allHeight * volumeRange.lowerBound + configManager.headerHeight + textHeight
            self.volumeHeight = allHeight * (volumeRange.upperBound - volumeRange.lowerBound) - configManager.headerHeight - textHeight

            self.childMinMaxRange = childDraw?.minMaxRange(visibleModelArray, configManager) ?? Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
            self.childBaseY = allHeight * volumeRange.upperBound + configManager.headerHeight + textHeight
            self.childHeight = allHeight * (1 - volumeRange.upperBound) - configManager.headerHeight - textHeight
        } else {
            self.volumeMinMaxRange = Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
            self.volumeBaseY = 0
            self.volumeHeight = 0

            self.childMinMaxRange = childDraw?.minMaxRange(visibleModelArray, configManager) ?? Range<CGFloat>.init(uncheckedBounds: (lower: 0, upper: 0))
            self.childBaseY = allHeight * volumeRange.lowerBound + configManager.headerHeight + textHeight
            self.childHeight = allHeight * (1 - volumeRange.lowerBound) - configManager.headerHeight - textHeight
        }

    }

    func yFromValue(_ value: CGFloat) -> CGFloat {
        let scale = (mainMinMaxRange.upperBound - mainMinMaxRange.lowerBound) / mainHeight
        var y = mainBaseY + mainHeight * 0.5
        if scale != 0 {
            y = mainBaseY + (mainMinMaxRange.upperBound - value) / scale
        }
        return y
    }

    func valueFromY(_ y: CGFloat) -> CGFloat {
        let scale = (mainMinMaxRange.upperBound - mainMinMaxRange.lowerBound) / mainHeight
        var value = scale * mainHeight * 0.5
        if scale != 0 {
            value = mainMinMaxRange.upperBound - (y - mainBaseY) * scale
        }
        return value
    }

    func xFromValue(_ value: CGFloat) -> CGFloat {
        guard let firstItem = configManager.modelArray.first,
            let lastItem = configManager.modelArray.last
        else {
            return 0
        }
        let scale =
            (lastItem.id - firstItem.id)
            / (configManager.itemWidth * CGFloat(configManager.modelArray.count - 1))
        let x = (value - firstItem.id) / scale + configManager.itemWidth / 2.0 - contentOffset.x
        return x
    }

    func valueFromX(_ x: CGFloat) -> CGFloat {
        guard let firstItem = configManager.modelArray.first,
            let lastItem = configManager.modelArray.last
        else {
            return 0
        }
        let scale =
            (lastItem.id - firstItem.id)
            / (configManager.itemWidth * CGFloat(configManager.modelArray.count - 1))
        let value = scale * (x + contentOffset.x - configManager.itemWidth / 2.0) + firstItem.id
        return value
    }

    func drawCandle(_ context: CGContext) {
        if configManager.isMinute {
            mainDraw.drawGradient(
                visibleModelArray, mainMinMaxRange.upperBound, mainMinMaxRange.lowerBound, allWidth,
                mainBaseY, mainHeight, context, configManager)
        }

        for (i, model) in visibleModelArray.enumerated() {
            mainDraw.drawCandle(model, i, mainMinMaxRange.upperBound, mainMinMaxRange.lowerBound, mainBaseY, mainHeight, context, configManager)
            if configManager.volumeFlex > 0 {
                volumeDraw.drawCandle(model, i, volumeMinMaxRange.upperBound, volumeMinMaxRange.lowerBound, volumeBaseY, volumeHeight, context, configManager)
            }
            childDraw?.drawCandle(model, i, childMinMaxRange.upperBound, childMinMaxRange.lowerBound, childBaseY, childHeight, context, configManager)

            // Draw buy/sell marks for this candlestick if they exist (O(1) lookup)
            drawBuySellMarksForCandlestick(model, i, context)

            let lastIndex = i == 0 ? i : i - 1
            let lastModel = visibleModelArray[lastIndex]
            mainDraw.drawLine(model, lastModel, mainMinMaxRange.upperBound, mainMinMaxRange.lowerBound, mainBaseY, mainHeight, i, lastIndex, context, configManager)
            if configManager.volumeFlex > 0 {
                volumeDraw.drawLine(model, lastModel, volumeMinMaxRange.upperBound, volumeMinMaxRange.lowerBound, volumeBaseY, volumeHeight, i, lastIndex, context, configManager)
            }
            childDraw?.drawLine(model, lastModel, childMinMaxRange.upperBound, childMinMaxRange.lowerBound, childBaseY, childHeight, i, lastIndex, context, configManager)
        }
    }

    func drawText(_ context: CGContext) {
        var model = visibleModelArray.last
        if visibleRange.contains(selectedIndex) {
            model = visibleModelArray[selectedIndex - visibleRange.lowerBound]
        }
        if let model = model {
            let baseX: CGFloat = 5
            mainDraw.drawText(model, baseX, 10, context, configManager)
            if configManager.volumeFlex > 0 {
                volumeDraw.drawText(model, baseX, volumeBaseY - configManager.headerHeight, context, configManager)
            }
            childDraw?.drawText(model, baseX, childBaseY - configManager.headerHeight, context, configManager)
        }
    }

    func drawValue(_ context: CGContext) {
        let baseX = self.allWidth
        mainDraw.drawValue(mainMinMaxRange.upperBound, mainMinMaxRange.lowerBound, baseX, mainBaseY, mainHeight, context, configManager)
        if configManager.volumeFlex > 0 {
            volumeDraw.drawValue(volumeMinMaxRange.upperBound, volumeMinMaxRange.lowerBound, baseX, volumeBaseY, volumeHeight, context, configManager)
        }
        childDraw?.drawValue(childMinMaxRange.upperBound, childMinMaxRange.lowerBound, baseX, childBaseY, childHeight, context, configManager)

    }

    func drawTime(_ context: CGContext) {
        let count = 6
        let valueDistance = self.allWidth / CGFloat(count - 1)
        for i in 0..<count {
            let font = configManager.createFont(configManager.candleTextFontSize)
            let x = valueDistance * CGFloat(i)
            let itemNumber = (x - 1 + contentOffset.x) / configManager.itemWidth
            var itemIndex = Int(ceil(itemNumber))
            itemIndex -= 1
            itemIndex -= visibleRange.lowerBound
            itemIndex = max(0, itemIndex)
            if itemIndex >= visibleModelArray.count {
                continue
            }
            let item = visibleModelArray[itemIndex]
            let title = item.dateString
            let width = mainDraw.textWidth(title: title, font: font)
            let height = mainDraw.textHeight(font: font)
            let y = childBaseY + childHeight + (configManager.paddingBottom - height) / 2
            mainDraw.drawText(
                title: title, point: CGPoint.init(x: x - width / 2.0, y: y),
                color: configManager.textColor, font: font, context: context,
                configManager: configManager)
        }
    }

    func drawHighLow(_ context: CGContext) {
        guard !configManager.isMinute else {
            return
        }
        var highIndex = 0
        var lowIndex = 0
        for (i, model) in visibleModelArray.enumerated() {
            if model.high > visibleModelArray[highIndex].high {
                highIndex = i
            }
            if model.low < visibleModelArray[lowIndex].low {
                lowIndex = i
            }
        }

        let drawValue: (Int, CGFloat) -> Void = { [weak self] (index, value) in
            guard let this = self else {
                return
            }

            var title = this.configManager.precision(value, this.configManager.price)
            let font = this.configManager.createFont(this.configManager.candleTextFontSize)
            let lineString = "--"
            let offset =
                CGFloat(index + this.visibleRange.lowerBound) * this.configManager.itemWidth
                - this.contentOffset.x
            let halfWidth = this.allWidth / 2
            var x = offset + this.configManager.itemWidth / 2

            var y = this.yFromValue(value)
            if offset < halfWidth {
                title = lineString + title
            } else {
                title = title + lineString
                x -= this.mainDraw.textWidth(title: title, font: font)
            }
            y -= this.mainDraw.textHeight(font: font) / 2
            y -= 1
            this.mainDraw.drawText(
                title: title, point: CGPoint.init(x: x, y: y),
                color: this.configManager.candleTextColor, font: font, context: context,
                configManager: this.configManager)
        }
        drawValue(highIndex, visibleModelArray[highIndex].high)
        drawValue(lowIndex, visibleModelArray[lowIndex].low)

    }

    func drawClosePrice(_ context: CGContext) {
        guard let lastModel = configManager.modelArray.last else {
            return
        }
        let offset = CGFloat(visibleRange.upperBound) * configManager.itemWidth - contentOffset.x
        let valueWidth = mainDraw.textWidth(
            title: configManager.precision(lastModel.close, configManager.price),
            font: configManager.createFont(configManager.rightTextFontSize))
        let showCenter = offset > allWidth - valueWidth - configManager.itemWidth
        animationView.isHidden = true
        if showCenter {
            drawClosePriceCenter(context, lastModel)
        } else {
            drawClosePriceRight(context, lastModel, offset)
        }
    }

    func drawClosePriceCenter(_ context: CGContext, _ lastModel: HTKLineModel) {
        let title = configManager.precision(lastModel.close, configManager.price)
        let font = configManager.createFont(configManager.candleTextFontSize)
        let width = mainDraw.textWidth(title: title, font: font)
        let height = mainDraw.textHeight(font: font)
        let paddingHorizontal: CGFloat = 7
        let paddingVertical: CGFloat = 5
        let triangleWidth: CGFloat = 5
        let triangleHeight: CGFloat = 7
        let triangleMarginLeft: CGFloat = 3
        let x = allWidth - configManager.paddingRight
        let rectHeight = height + paddingVertical * 2
        let y = max(
            mainBaseY - textHeight + rectHeight / 2,
            min(mainBaseY + mainHeight + textHeight - rectHeight / 2, yFromValue(lastModel.close)))
        let rectWidth =
            paddingHorizontal + width + triangleMarginLeft + triangleWidth + paddingHorizontal
        let rect = CGRect.init(
            x: x - rectWidth / 2, y: y - height / 2 - paddingVertical, width: rectWidth,
            height: rectHeight)

        // Adjust frame to account for content offset since drawing happens in translated context
        let adjustedRect = CGRect.init(
            x: rect.origin.x + contentOffset.x,
            y: rect.origin.y,
            width: rect.size.width,
            height: rect.size.height)

        closePriceLabelFrame = adjustedRect
        print("HTKLineView: Set closePriceLabelFrame (center): \(rect) -> adjusted: \(adjustedRect)")

        let rectPath = UIBezierPath.init(roundedRect: rect, cornerRadius: rect.size.height / 2)
        context.setFillColor(configManager.closePriceCenterBackgroundColor.cgColor)
        context.addPath(rectPath.cgPath)
        context.fillPath()
        context.setStrokeColor(configManager.closePriceCenterBorderColor.cgColor)
        context.addPath(rectPath.cgPath)
        context.strokePath()
        mainDraw.drawText(
            title: title,
            point: CGPoint.init(x: rect.minX + paddingHorizontal, y: rect.minY + paddingVertical),
            color: configManager.textColor, font: font, context: context,
            configManager: configManager)

        let trianglePath = UIBezierPath.init()
        trianglePath.move(to: CGPoint.init(x: rect.maxX - paddingHorizontal, y: y))
        trianglePath.addLine(
            to: CGPoint.init(
                x: rect.maxX - paddingHorizontal - triangleWidth, y: y + triangleHeight / 2))
        trianglePath.addLine(
            to: CGPoint.init(
                x: rect.maxX - paddingHorizontal - triangleWidth, y: y - triangleHeight / 2))
        trianglePath.close()
        context.setFillColor(configManager.closePriceCenterTriangleColor.cgColor)
        context.addPath(trianglePath.cgPath)
        context.fillPath()
    }

    func drawClosePriceRight(_ context: CGContext, _ lastModel: HTKLineModel, _ offset: CGFloat) {
        let y = yFromValue(lastModel.close)
        let title = configManager.precision(lastModel.close, configManager.price)
        let font = configManager.createFont(configManager.rightTextFontSize)
        let color = configManager.closePriceRightSeparatorColor
        let width = mainDraw.textWidth(title: title, font: font)
        let height = mainDraw.textHeight(font: font)

        let rect = CGRect.init(x: allWidth - width, y: y - height / 2, width: width, height: height)

        // Adjust frame to account for content offset since drawing happens in translated context
        let adjustedRect = CGRect.init(
            x: rect.origin.x + contentOffset.x,
            y: rect.origin.y,
            width: rect.size.width,
            height: rect.size.height)

        closePriceLabelFrame = adjustedRect
        print("HTKLineView: Set closePriceLabelFrame (right): \(rect) -> adjusted: \(adjustedRect)")

        context.setFillColor(configManager.closePriceRightBackgroundColor.cgColor)
        context.fill(rect)
        mainDraw.drawText(
            title: title, point: rect.origin, color: color, font: font, context: context,
            configManager: configManager)

        if configManager.isMinute {
            animationView.isHidden = false
            UIView.animate(withDuration: 0.15) {
                self.animationView.center = CGPoint.init(
                    x: rect.origin.x + self.configManager.itemWidth / 2 + self.contentOffset.x, y: y)
            }
        }
    }

    func drawSelectedLine(_ context: CGContext) {
        guard visibleRange.contains(selectedIndex) else {
            return
        }
        let value = visibleModelArray[selectedIndex - visibleRange.lowerBound].close
        let x = (CGFloat(selectedIndex) + 0.5) * configManager.itemWidth - contentOffset.x
        let y = yFromValue(value)

        context.setStrokeColor(configManager.candleTextColor.cgColor)
        context.setLineWidth(configManager.lineWidth / 2)
        context.addLines(between: [CGPoint.init(x: 0, y: y), CGPoint.init(x: allWidth, y: y)])
        context.strokePath()

        context.addArc(
            center: CGPoint.init(x: x, y: y), radius: configManager.candleWidth * 2 / 2,
            startAngle: 0, endAngle: CGFloat(Double.pi * 2), clockwise: true)
        context.setFillColor(configManager.selectedPointContainerColor.cgColor)
        context.fillPath()
        context.addArc(
            center: CGPoint.init(x: x, y: y), radius: configManager.candleWidth / 1.5 / 2,
            startAngle: 0, endAngle: CGFloat(Double.pi * 2), clockwise: true)
        context.setFillColor(configManager.selectedPointContentColor.cgColor)
        context.fillPath()

        let colorList = configManager.packGradientColorList(configManager.panelGradientColorList)
        let locationList = configManager.panelGradientLocationList
        if let gradient = CGGradient.init(
            colorSpace: CGColorSpaceCreateDeviceRGB(), colorComponents: colorList,
            locations: locationList, count: locationList.count)
        {
            let start = mainBaseY
            let end = childBaseY + childHeight
            context.addRect(
                CGRect.init(
                    x: x - configManager.candleWidth / 2, y: start,
                    width: configManager.candleWidth, height: end - start))
            context.clip()
            context.drawLinearGradient(
                gradient, start: CGPoint.init(x: 0, y: start), end: CGPoint.init(x: 0, y: end),
                options: .drawsBeforeStartLocation)
            context.resetClip()
        }

        let offset = CGFloat(selectedIndex) * configManager.itemWidth - contentOffset.x
        let halfWidth = allWidth / 2
        let leftAlign = offset < halfWidth

        let title = configManager.precision(value, configManager.price)
        let paddingVertical: CGFloat = 3
        let paddingHorizontal: CGFloat = 3
        let triangleWidth: CGFloat = 5
        let font = configManager.createFont(configManager.candleTextFontSize)
        let width = mainDraw.textWidth(title: title, font: font)
        let height = mainDraw.textHeight(font: font)
        let startX = leftAlign ? 0 : allWidth - width - paddingHorizontal * 2
        let rect = CGRect.init(
            x: startX, y: y - height / 2 - paddingVertical, width: width + paddingHorizontal * 2,
            height: height + paddingVertical * 2)
        let bezierPath = UIBezierPath.init()
        if leftAlign {
            bezierPath.move(to: CGPoint.init(x: rect.maxX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.minX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.minX, y: rect.maxY))
            bezierPath.move(to: CGPoint.init(x: rect.maxX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.maxX + triangleWidth, y: y))
            bezierPath.addLine(to: CGPoint.init(x: rect.maxX, y: rect.maxY))
            bezierPath.addLine(to: CGPoint.init(x: rect.minX, y: rect.maxY))
        } else {
            bezierPath.move(to: CGPoint.init(x: rect.minX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.maxX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.maxX, y: rect.maxY))
            bezierPath.move(to: CGPoint.init(x: rect.minX, y: rect.minY))
            bezierPath.addLine(to: CGPoint.init(x: rect.minX - triangleWidth, y: y))
            bezierPath.addLine(to: CGPoint.init(x: rect.minX, y: rect.maxY))
            bezierPath.addLine(to: CGPoint.init(x: rect.maxX, y: rect.maxY))
        }
        context.setFillColor(configManager.panelBackgroundColor.cgColor)
        context.setLineWidth(configManager.lineWidth / 2)
        context.setStrokeColor(configManager.candleTextColor.cgColor)
        context.addPath(bezierPath.cgPath)
        context.fillPath()
        context.addPath(bezierPath.cgPath)
        context.strokePath()
        mainDraw.drawText(
            title: title, point: CGPoint.init(x: startX + paddingHorizontal, y: y - height / 2),
            color: configManager.candleTextColor, font: font, context: context,
            configManager: configManager)

    }

    func drawSelectedBoard(_ context: CGContext) {
        guard visibleRange.contains(selectedIndex) else {
            return
        }
        guard !configManager.isMinute else {
            return
        }

        // Get the selected model and its basic selectedItemList
        let selectedModel = visibleModelArray[selectedIndex - visibleRange.lowerBound]
        var itemList = selectedModel.selectedItemList

        // Check if there are buy/sell marks for this candlestick and add them to tooltip
        if let containerView = superview as? HTKLineContainerView {
            let candleTime = Int64(selectedModel.id)

            // Add buy mark tooltip if exists
            if let buyMark = containerView.getBuyMarkForTime(candleTime),
               let tooltipText = buyMark["tooltipText"] as? String {
                let buyTooltipItem = ["title": "", "detail": tooltipText]
                itemList.append(buyTooltipItem)
            }

            // Add sell mark tooltip if exists
            if let sellMark = containerView.getSellMarkForTime(candleTime),
               let tooltipText = sellMark["tooltipText"] as? String {
                let sellTooltipItem = ["title": "", "detail": tooltipText]
                itemList.append(sellTooltipItem)
            }
        }

        let font = configManager.createFont(configManager.panelTextFontSize)
        let color = configManager.candleTextColor
        let offset = CGFloat(selectedIndex) * configManager.itemWidth - contentOffset.x
        let halfWidth = allWidth / 2
        let leftAlign = offset > halfWidth
        let margin: CGFloat = 5
        let padding: CGFloat = 7
        let lineSpace: CGFloat = 8
        let y = mainBaseY - textHeight + configManager.lineWidth
        var textY = y + padding
        var width = configManager.panelMinWidth
        for item in itemList {
            let title = item["title"] as? String ?? ""
            let detail = item["detail"] as? String ?? ""
            let text = String(format: "%@%@", title, detail)
            let textWidth = mainDraw.textWidth(title: text, font: font)
            let detailHeight = mainDraw.textHeight(font: font)
            width = max(width, textWidth + 20)
            textY += detailHeight
            textY += lineSpace
        }
        let x = leftAlign ? margin : allWidth - width - margin
        context.setFillColor(configManager.panelBackgroundColor.cgColor)
        context.setLineWidth(configManager.lineWidth / 2.0)
        context.setStrokeColor(configManager.panelBorderColor.cgColor)
        let rect = CGRect.init(x: x, y: y, width: width, height: textY - lineSpace + padding - y)
        let bezierPath = UIBezierPath.init(roundedRect: rect, cornerRadius: 5)
        context.addPath(bezierPath.cgPath)
        context.fillPath()
        context.addPath(bezierPath.cgPath)
        context.strokePath()
        textY = y + padding
        for item in itemList {
            let title = item["title"] as? String ?? ""
            let detail = item["detail"] as? String ?? ""
            let detailColor = item["color"] as? UIColor ?? color
            mainDraw.drawText(
                title: title, point: CGPoint.init(x: x + padding, y: textY), color: color,
                font: font, context: context, configManager: configManager)
            let detailWidth = mainDraw.textWidth(title: detail, font: font)
            let detailHeight = mainDraw.textHeight(font: font)
            mainDraw.drawText(
                title: detail, point: CGPoint.init(x: x + width - padding - detailWidth, y: textY),
                color: detailColor, font: font, context: context, configManager: configManager)
            textY += detailHeight
            textY += lineSpace
        }
    }

    func drawSelectedTime(_ context: CGContext) {
        guard visibleRange.contains(selectedIndex) else {
            return
        }
        let value = visibleModelArray[selectedIndex - visibleRange.lowerBound].dateString
        let color = configManager.candleTextColor
        let x = (CGFloat(selectedIndex) + 0.5) * configManager.itemWidth - contentOffset.x
        let font = configManager.createFont(configManager.candleTextFontSize)
        let title = value
        let width = mainDraw.textWidth(title: title, font: font)
        let padding: CGFloat = 5
        let height = mainDraw.textHeight(font: font)
        let y = childBaseY + childHeight
        context.setFillColor(configManager.panelBackgroundColor.cgColor)
        context.setLineWidth(configManager.lineWidth / 2.0)
        context.setStrokeColor(color.cgColor)
        let rect = CGRect.init(
            x: x - width / 2 - padding, y: y, width: width + padding * 2,
            height: configManager.paddingBottom - configManager.lineWidth)
        context.fill(rect)
        context.stroke(rect)
        mainDraw.drawText(
            title: title,
            point: CGPoint.init(
                x: x - width / 2.0, y: y + (configManager.paddingBottom - height) / 2),
            color: color, font: font, context: context, configManager: configManager)
    }

    func drawOrderLines(_ context: CGContext) {
        // Access order lines from parent container view
        guard let containerView = superview as? HTKLineContainerView else {
            return
        }

        let orderLines = containerView.getAllOrderLines()

        for (_, orderLineData) in orderLines {
            guard let price = orderLineData["price"] as? Double,
                  let type = orderLineData["type"] as? String else {
                continue
            }

            // Convert price to Y coordinate
            let priceRange = mainMinMaxRange.upperBound - mainMinMaxRange.lowerBound
            let normalizedPrice = (price - mainMinMaxRange.lowerBound) / priceRange
            let y = mainBaseY + mainHeight - CGFloat(normalizedPrice) * mainHeight

            // Only draw if the price is within the visible range
            if y >= mainBaseY && y <= mainBaseY + mainHeight {
                // Set line style based on order type
                context.setLineWidth(1.0)

                // Use color property if available, otherwise default to orange
                var lineColor = UIColor.orange
                if let colorString = orderLineData["color"] as? String {
                    lineColor = UIColor(hexString: colorString) ?? UIColor.orange
                }
                context.setStrokeColor(lineColor.cgColor)

                // Create dashed line pattern
                let dashPattern: [CGFloat] = [5.0, 3.0]
                context.setLineDash(phase: 0, lengths: dashPattern)

                // Calculate line start position (after label if it exists)
                var lineStartX: CGFloat = 0

                // Draw label if available and calculate where line should start
                if let label = orderLineData["label"] as? String, !label.isEmpty {
                    let fontSize = (orderLineData["labelFontSize"] as? CGFloat) ?? 12.0
                    let font = configManager.createFont(fontSize)

                    // Get label color (defaults to line color)
                    var labelColor = lineColor
                    if let labelColorString = orderLineData["labelColor"] as? String {
                        labelColor = UIColor(hexString: labelColorString) ?? lineColor
                    }

                    // Calculate label size
                    let labelAttributes: [NSAttributedString.Key: Any] = [
                        .font: font,
                        .foregroundColor: labelColor
                    ]
                    let labelSize = label.size(withAttributes: labelAttributes)

                    // Calculate description size if available
                    var descriptionSize = CGSize.zero
                    var descriptionColor = labelColor
                    var description: String? = nil

                    if let desc = orderLineData["labelDescription"] as? String, !desc.isEmpty {
                        description = desc

                        // Get description color (defaults to label color)
                        if let descriptionColorString = orderLineData["labelDescriptionColor"] as? String {
                            descriptionColor = UIColor(hexString: descriptionColorString) ?? labelColor
                        }

                        let descriptionAttributes: [NSAttributedString.Key: Any] = [
                            .font: font,
                            .foregroundColor: descriptionColor
                        ]
                        descriptionSize = desc.size(withAttributes: descriptionAttributes)
                    }

                    // Calculate total width (label + space + description)
                    let spacing: CGFloat = description != nil ? 4 : 0
                    let totalTextWidth = labelSize.width + spacing + descriptionSize.width
                    let textHeight = max(labelSize.height, descriptionSize.height)

                    // Pill container dimensions (match close price pill)
                    let horizontalPadding: CGFloat = 7
                    let verticalPadding: CGFloat = 5
                    let outerPadding: CGFloat = 8

                    let pillWidth = totalTextWidth + horizontalPadding * 2
                    let pillHeight = textHeight + verticalPadding * 2

                    let labelX = outerPadding
                    let labelY = y - pillHeight / 2

                    // Only draw label if there's enough space
                    if labelX + pillWidth + outerPadding < allWidth / 3 {
                        // Draw pill background
                        let pillRect = CGRect(x: labelX, y: labelY, width: pillWidth, height: pillHeight)
                        // Use same radius calculation as close price pill
                        let pillPath = UIBezierPath(roundedRect: pillRect, cornerRadius: pillRect.size.height / 2)

                        // Get background color or use default
                        var backgroundColor = UIColor.clear
                        if let backgroundColorString = orderLineData["labelBackgroundColor"] as? String {
                            backgroundColor = UIColor(hexString: backgroundColorString) ?? UIColor.clear
                        }

                        // Fill background (ensure alpha blending is enabled)
                        context.setBlendMode(.normal)
                        context.setFillColor(backgroundColor.cgColor)
                        context.addPath(pillPath.cgPath)
                        context.fillPath()

                        // Draw border with same color as text (reset dash pattern first)
                        context.setLineDash(phase: 0, lengths: [])
                        context.setStrokeColor(lineColor.cgColor)
                        context.setLineWidth(1.0)
                        context.addPath(pillPath.cgPath)
                        context.strokePath()

                        // Restore dashed line pattern for the order line
                        let dashPattern: [CGFloat] = [5.0, 3.0]
                        context.setLineDash(phase: 0, lengths: dashPattern)

                        // Draw label text
                        let labelTextX = labelX + horizontalPadding
                        let labelTextY = labelY + verticalPadding
                        let labelRect = CGRect(x: labelTextX, y: labelTextY, width: labelSize.width, height: labelSize.height)
                        label.draw(in: labelRect, withAttributes: labelAttributes)

                        // Draw description text if available
                        if let description = description {
                            let descriptionTextX = labelTextX + labelSize.width + spacing
                            let descriptionTextY = labelY + verticalPadding
                            let descriptionRect = CGRect(x: descriptionTextX, y: descriptionTextY, width: descriptionSize.width, height: descriptionSize.height)
                            let descriptionAttributes: [NSAttributedString.Key: Any] = [
                                .font: font,
                                .foregroundColor: descriptionColor
                            ]
                            description.draw(in: descriptionRect, withAttributes: descriptionAttributes)
                        }

                        // Set line to start after pill with additional padding
                        lineStartX = labelX + pillWidth + outerPadding
                    }
                }

                // Draw horizontal line starting after the label (or from 0 if no label)
                // Stop before the Y-axis scale area (paddingRight)
                // Ensure correct line color is set just before drawing the line
                context.setStrokeColor(lineColor.cgColor)
                let lineEndX = allWidth - configManager.paddingRight
                context.move(to: CGPoint(x: lineStartX, y: y))
                context.addLine(to: CGPoint(x: lineEndX, y: y))
                context.strokePath()

                // Reset dash pattern for other drawing operations
                context.setLineDash(phase: 0, lengths: [])
            }
        }
    }

    func drawBuySellMarksForCandlestick(_ model: HTKLineModel, _ index: Int, _ context: CGContext) {
        // Access buy/sell marks from parent container view
        guard let containerView = superview as? HTKLineContainerView else {
            return
        }

        let candleTime = Int64(model.id) // Use model.id which contains the timestamp

        // Check for both marks to handle collision detection
        let buyMark = containerView.getBuyMarkForTime(candleTime)
        let sellMark = containerView.getSellMarkForTime(candleTime)

        // Check if both marks exist to handle collision avoidance
        let hasBothMarks = buyMark != nil && sellMark != nil

        // Draw buy mark
        if let buyMark = buyMark {
            drawBuySellMark(buyMark, model, index, "buy", context, hasBothMarks: hasBothMarks)
        }

        // Draw sell mark
        if let sellMark = sellMark {
            drawBuySellMark(sellMark, model, index, "sell", context, hasBothMarks: hasBothMarks)
        }
    }

    private func drawBuySellMark(_ markData: [String: Any], _ model: HTKLineModel, _ index: Int, _ type: String, _ context: CGContext, hasBothMarks: Bool = false) {
        // Calculate X position for the candlestick
        let candleX = CGFloat(index) * configManager.itemWidth + configManager.itemWidth / 2

        // Get the candlestick high price for positioning above it
        let markPrice = model.high

        // Convert price to Y coordinate
        let priceRange = mainMinMaxRange.upperBound - mainMinMaxRange.lowerBound
        let normalizedPrice = (markPrice - mainMinMaxRange.lowerBound) / priceRange
        let markY = mainBaseY + mainHeight - CGFloat(normalizedPrice) * mainHeight

        // Make circle radius match candlestick width (same as Android logic)
        let circleRadius: CGFloat = configManager.itemWidth * 0.4 // 80% of candlestick width for diameter

        // Position both marks above the candlestick, with collision avoidance
        let markCenterY: CGFloat
        if type == "buy" {
            // Buy mark directly above the candlestick
            markCenterY = markY - circleRadius - 2
        } else { // sell
            if hasBothMarks {
                // If both marks exist, position sell mark one diameter higher
                markCenterY = markY - circleRadius - 2 - (circleRadius * 2) - 2
            } else {
                // If only sell mark exists, position it directly above
                markCenterY = markY - circleRadius - 2
            }
        }

        // Determine colors and text based on type
        let circleColor: UIColor
        let markText: String

        if type == "buy" {
            circleColor = configManager.increaseColor
            markText = "B"
        } else { // sell
            circleColor = configManager.decreaseColor
            markText = "S"
        }

        // Draw circle
        let circleRect = CGRect(x: candleX - circleRadius, y: markCenterY - circleRadius,
                              width: circleRadius * 2, height: circleRadius * 2)
        let circlePath = UIBezierPath(ovalIn: circleRect)

        context.setFillColor(circleColor.cgColor)
        context.addPath(circlePath.cgPath)
        context.fillPath()

        // Draw border
        context.setStrokeColor(circleColor.cgColor)
        context.setLineWidth(1.0)
        context.setLineDash(phase: 0, lengths: []) // Solid line
        context.addPath(circlePath.cgPath)
        context.strokePath()

        // Draw text inside circle
        let fontSize: CGFloat = circleRadius * 1.2 // Text size proportional to circle (same as Android)
        let font = configManager.createFont(fontSize)
        let textAttributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: UIColor.white
        ]

        let textSize = markText.size(withAttributes: textAttributes)
        let textRect = CGRect(x: candleX - textSize.width / 2,
                            y: markCenterY - textSize.height / 2,
                            width: textSize.width,
                            height: textSize.height)

        markText.draw(in: textRect, withAttributes: textAttributes)
    }

    func valuePointFromViewPoint(_ point: CGPoint) -> CGPoint {
        return CGPoint.init(x: valueFromX(point.x), y: valueFromY(point.y))
    }

    func viewPointFromValuePoint(_ point: CGPoint) -> CGPoint {
        return CGPoint.init(x: xFromValue(point.x), y: yFromValue(point.y))
    }

}

extension HTKLineView: UIScrollViewDelegate {

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let contentOffsetX = scrollView.contentOffset.x
        var visibleStartIndex = Int(floor(contentOffsetX / configManager.itemWidth))
        var visibleEndIndex = Int(
            ceil((contentOffsetX + scrollView.bounds.size.width) / configManager.itemWidth))
        visibleStartIndex = min(max(0, visibleStartIndex), configManager.modelArray.count - 1)
        visibleEndIndex = min(max(0, visibleEndIndex), configManager.modelArray.count - 1)
        visibleRange = visibleStartIndex...visibleEndIndex

        // Trigger onScrollLeft when there are less than 100 candlesticks to the left
        let candlesticksToLeft = contentOffsetX / configManager.itemWidth
        if candlesticksToLeft <= 100 {
            if !hasTriggeredScrollLeft {
                hasTriggeredScrollLeft = true
                let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
                configManager.onScrollLeft?(timestamp)
            }
        } else {
            hasTriggeredScrollLeft = false
        }

        self.setNeedsDisplay()
    }

    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        selectedIndex = -1
        self.setNeedsDisplay()
    }

    @objc
    func longPressSelector(_ gesture: UILongPressGestureRecognizer) {
        let index = Int(floor(gesture.location(in: self).x / configManager.itemWidth))
        selectedIndex = index
        if selectedIndex >= configManager.modelArray.count {
            selectedIndex = configManager.modelArray.count - 1
        }
        self.setNeedsDisplay()
    }

    @objc
    func tapSelector(_ gesture: UITapGestureRecognizer) {
        let tapLocation = gesture.location(in: self)

        // Check if touch is on close price label
        let isOnClosePriceLabel = closePriceLabelFrame.contains(tapLocation)

        print("HTKLineView: tapLocation: \(tapLocation)")
        print("HTKLineView: closePriceLabelFrame: \(closePriceLabelFrame)")
        print("HTKLineView: isOnClosePriceLabel: \(isOnClosePriceLabel)")

        // Trigger the chart touch callback with close price detection
        configManager.onChartTouch?(tapLocation, isOnClosePriceLabel)

        selectedIndex = -1
        self.setNeedsDisplay()
    }

    @objc
    func pinchSelector(_ gesture: UIPinchGestureRecognizer) {
        switch gesture.state {
        case .changed:
            scale += (gesture.scale - 1) / 10
        default:
            break
        }
        scale = max(0.3, min(scale, 3))

        let width = bounds.size.width
        let halfWidth = width / 2
        let offsetScale =
            (contentOffset.x + halfWidth) / (contentSize.width - configManager.paddingRight)

        reloadContentSize()
        let contentOffsetX = max(
            0,
            min(
                (contentSize.width - configManager.paddingRight) * offsetScale - halfWidth,
                contentSize.width - width))
        reloadContentOffset(contentOffsetX)
        scrollViewDidScroll(self)
    }

    // Reset the scroll left trigger flag to allow new triggers
    func resetScrollLeftTrigger() {
        hasTriggeredScrollLeft = false
        print("HTKLineView: Reset scroll left trigger flag")
    }

}
