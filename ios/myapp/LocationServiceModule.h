//
//  LocationServiceModule.h
//  myapp
//
//  Created by angel calderaro on 23/02/25.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <CoreLocation/CoreLocation.h>

@interface LocationServiceModule : RCTEventEmitter <RCTBridgeModule, CLLocationManagerDelegate>

@end
