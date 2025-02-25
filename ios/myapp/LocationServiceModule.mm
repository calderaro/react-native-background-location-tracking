//
//  LocationServiceModule.m
//  myapp
//
//  Created by angel calderaro on 23/02/25.
//
#import "LocationServiceModule.h"
#import <React/RCTLog.h>
#import <CoreLocation/CoreLocation.h>

// Global variable to persist tracking state across reloads.
static BOOL isTracking = NO;
// Global background task identifier.
static UIBackgroundTaskIdentifier bgTask = UIBackgroundTaskInvalid;

@interface LocationServiceModule ()

@property (strong, nonatomic) CLLocationManager *locationManager;
@property (assign, nonatomic) BOOL hasListeners;

@end

@implementation LocationServiceModule

RCT_EXPORT_MODULE();

- (instancetype)init {
    self = [super init];
    if (self) {
        self.locationManager = [[CLLocationManager alloc] init];
        self.locationManager.delegate = self;
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation;
        self.locationManager.pausesLocationUpdatesAutomatically = NO;
        self.locationManager.allowsBackgroundLocationUpdates = YES;
      
        // Restore tracking state from the global variable.
        if (bgTask) {
            NSLog(@"BACKGROUND TASK ALREADY EXISTED");
            [self.locationManager startUpdatingLocation];
            [self keepAppAwake];
        }
    }
    return self;
}


// Register when JS listeners are added
- (void)startObserving {
    self.hasListeners = YES;
    // Immediately notify JS of the current tracking status
    [self sendEventWithName:@"onStatusChanged" body:@(isTracking)];
}

// Unregister when JS listeners are removed
- (void)stopObserving {
    self.hasListeners = NO;
}

// List of events that can be sent to React Native
- (NSArray<NSString *> *)supportedEvents {
    return @[@"onLocationUpdate", @"onStatusChanged"];
}

// Exported method to start tracking
RCT_EXPORT_METHOD(start) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([CLLocationManager locationServicesEnabled]) {
            [self.locationManager requestAlwaysAuthorization];
            [self.locationManager startUpdatingLocation];
            isTracking = YES;
            [self sendStatusUpdate];
            [self keepAppAwake];
            RCTLogInfo(@"[LocationService] Started tracking in background");
        } else {
            RCTLogInfo(@"[LocationService] Location services are disabled.");
        }
    });
}

// Exported method to stop tracking
RCT_EXPORT_METHOD(stop) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.locationManager stopUpdatingLocation];
        isTracking = NO;
        [self sendStatusUpdate];
        [self endBackgroundTask];
        RCTLogInfo(@"[LocationService] Stopped tracking");
    });
}

// Notify React Native about status changes
- (void)sendStatusUpdate {
    if (!self.hasListeners) {
        return; // Avoid sending events before JS initializes
    }
  
    [self sendEventWithName:@"onStatusChanged" body:@(isTracking)];
}


// Keep the app awake in background
- (void)keepAppAwake {
    if (bgTask != UIBackgroundTaskInvalid) {
        [self endBackgroundTask];
    }

    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        [self endBackgroundTask];
    }];
}

// End background task
- (void)endBackgroundTask {
    if (bgTask != UIBackgroundTaskInvalid) {
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }
}

// Location update delegate
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations {
    CLLocation *location = [locations lastObject];
    RCTLogInfo(@"[LocationService] Background Location: %f, %f", location.coordinate.latitude, location.coordinate.longitude);
    
  
  // Avoid sending events before JS initializes
    if (self.hasListeners) {
        // Send event to React Native
        [self sendEventWithName:@"onLocationUpdate" body:@{
            @"latitude": @(location.coordinate.latitude),
            @"longitude": @(location.coordinate.longitude)
        }];
    }

    // Send location data to server
    [self sendLocationToServer:location];
}

// Send location to server
- (void)sendLocationToServer:(CLLocation *)location {
    NSString *urlString = @"https://yourserver.com/api/location";
    NSURL *url = [NSURL URLWithString:urlString];

    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    request.HTTPMethod = @"POST";
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];

    NSDictionary *body = @{
        @"latitude": @(location.coordinate.latitude),
        @"longitude": @(location.coordinate.longitude)
    };

    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&error];

    if (!error) {
        request.HTTPBody = jsonData;
        NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request];
        [task resume];
    } else {
        RCTLogInfo(@"[LocationService] Error serializing JSON: %@", error.localizedDescription);
    }
}

// Handle app entering background
- (void)applicationDidEnterBackground:(UIApplication *)application {
    if (isTracking) {
        [self.locationManager startUpdatingLocation];
        [self keepAppAwake];
    }
}

// Handle app resuming to foreground
- (void)applicationWillEnterForeground:(UIApplication *)application {
    if (isTracking) {
        [self.locationManager startUpdatingLocation];
    }
}

@end
