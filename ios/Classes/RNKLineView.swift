//
//  HTKLineViewManager.swift
//  Base64
//
//  Created by hublot on 2020/4/3.
//

import UIKit

@objc(RNKLineView)
@objcMembers
class RNKLineView: RCTViewManager {

    static let queue = DispatchQueue.init(label: "com.hublot.klinedata")

    override func view() -> UIView! {
        return HTKLineContainerView()
    }

    override class func requiresMainQueueSetup() -> Bool {
        return true
    }

    @objc func updateLastCandlestick(_ node: NSNumber, candlestick: NSDictionary) {
        print("RNKLineView: updateLastCandlestick called for node \(node) with data: \(candlestick)")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.updateLastCandlestick")
            view.updateLastCandlestick(candlestick)
        }
    }

    @objc func addCandlesticksAtTheEnd(_ node: NSNumber, candlesticks: NSArray) {
        print("RNKLineView: addCandlesticksAtTheEnd called for node \(node) with \(candlesticks.count) candlesticks")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.addCandlesticksAtTheEnd")
            view.addCandlesticksAtTheEnd(candlesticks)
        }
    }

    @objc func addCandlesticksAtTheStart(_ node: NSNumber, candlesticks: NSArray) {
        print("RNKLineView: addCandlesticksAtTheStart called for node \(node) with \(candlesticks.count) candlesticks")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.addCandlesticksAtTheStart")
            view.addCandlesticksAtTheStart(candlesticks)
        }
    }

    @objc func addOrderLine(_ node: NSNumber, orderLine: NSDictionary) {
        print("RNKLineView: addOrderLine called for node \(node) with data: \(orderLine)")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.addOrderLine")
            view.addOrderLine(orderLine)
        }
    }

    @objc func removeOrderLine(_ node: NSNumber, orderLineId: NSString) {
        print("RNKLineView: removeOrderLine called for node \(node) with id: \(orderLineId)")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.removeOrderLine")
            view.removeOrderLine(orderLineId as String)
        }
    }

    @objc func updateOrderLine(_ node: NSNumber, orderLine: NSDictionary) {
        print("RNKLineView: updateOrderLine called for node \(node) with data: \(orderLine)")
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            print("RNKLineView: Calling view.updateOrderLine")
            view.updateOrderLine(orderLine)
        }
    }

    @objc func getOrderLines(_ node: NSNumber) -> NSArray {
        print("RNKLineView: getOrderLines called for node \(node)")
        guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
            print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
            return NSArray()
        }
        print("RNKLineView: Calling view.getOrderLines")
        return view.getOrderLines()
    }

    @objc func addBuySellMark(_ node: NSNumber, buySellMark: NSDictionary) {
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            view.addBuySellMark(buySellMark)
        }
    }

    @objc func removeBuySellMark(_ node: NSNumber, buySellMarkId: NSString) {
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            view.removeBuySellMark(buySellMarkId as String)
        }
    }

    @objc func updateBuySellMark(_ node: NSNumber, buySellMark: NSDictionary) {
        DispatchQueue.main.async {
            guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
                print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
                return
            }
            view.updateBuySellMark(buySellMark)
        }
    }

    @objc func getBuySellMarks(_ node: NSNumber) -> NSArray {
        guard let view = self.bridge?.uiManager.view(forReactTag: node) as? HTKLineContainerView else {
            print("RNKLineView: Could not find HTKLineContainerView for node \(node)")
            return NSArray()
        }
        return view.getBuySellMarks()
    }

}
