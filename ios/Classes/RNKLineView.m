#import "RNKLineView-Swift.h"
#import "RCTViewManager.h"


@interface RCT_EXTERN_MODULE(RNKLineView, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onDrawItemDidTouch, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(onScrollLeft, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(onChartTouch, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(onDrawItemComplete, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(onDrawPointComplete, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(optionList, NSString)

RCT_EXTERN_METHOD(updateLastCandlestick:(nonnull NSNumber *)node
                  candlestick:(nonnull NSDictionary *)candlestick)

RCT_EXTERN_METHOD(addCandlesticksAtTheEnd:(nonnull NSNumber *)node
                  candlesticks:(nonnull NSArray *)candlesticks)

RCT_EXTERN_METHOD(addCandlesticksAtTheStart:(nonnull NSNumber *)node
                  candlesticks:(nonnull NSArray *)candlesticks)

RCT_EXTERN_METHOD(addOrderLine:(nonnull NSNumber *)node
                  orderLine:(nonnull NSDictionary *)orderLine)

RCT_EXTERN_METHOD(removeOrderLine:(nonnull NSNumber *)node
                  orderLineId:(nonnull NSString *)orderLineId)

RCT_EXTERN_METHOD(updateOrderLine:(nonnull NSNumber *)node
                  orderLine:(nonnull NSDictionary *)orderLine)

RCT_EXTERN_METHOD(getOrderLines:(nonnull NSNumber *)node)

RCT_EXTERN_METHOD(addBuySellMark:(nonnull NSNumber *)node
                  buySellMark:(nonnull NSDictionary *)buySellMark)

RCT_EXTERN_METHOD(removeBuySellMark:(nonnull NSNumber *)node
                  buySellMarkId:(nonnull NSString *)buySellMarkId)

RCT_EXTERN_METHOD(updateBuySellMark:(nonnull NSNumber *)node
                  buySellMark:(nonnull NSDictionary *)buySellMark)

RCT_EXTERN_METHOD(getBuySellMarks:(nonnull NSNumber *)node)

@end

