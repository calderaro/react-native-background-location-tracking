//
//  myapp-Bridging-Header.h
//  myapp
//
//  Created by angel calderaro on 23/02/25.
//
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(LocationServiceModule, NSObject)
  RCT_EXTERN_METHOD(simpleMethod)
  RCT_EXTERN_METHOD(simpleMethodReturns:
    (RCTResponseSenderBlock) callback
  )
  RCT_EXTERN_METHOD(simpleMethodWithParams:
    (NSString *) param
    callback: (RCTResponseSenderBlock)callback
  )
  RCT_EXTERN_METHOD(
    resolvePromise: (RCTPromiseResolveBlock) resolve
    rejecter: (RCTPromiseRejectBlock) reject
  )
  RCT_EXTERN_METHOD(rejectPromise:
    (RCTPromiseResolveBlock) resolve
    rejecter: (RCTPromiseRejectBlock) reject
  )
@end
