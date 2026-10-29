#include "location.h"

#import <CoreLocation/CoreLocation.h>
#import <Foundation/Foundation.h>

static const int kStatusNotDetermined = 0;
static const int kStatusDenied = 1;
static const int kStatusGranted = 2;
static const int kStatusUnavailable = 3;
static const int kCodeSuccess = 0;
static const int kCodeDenied = 1;
static const int kCodeUnavailable = 2;

static CLLocationManager *manager;
static id<CLLocationManagerDelegate> delegate;
static towards_location_callback permissionCallback;
static towards_location_callback currentCallback;
static int currentGeneration = 0;

static BOOL statusIsGranted(CLAuthorizationStatus status) {
    // macOS reports a when-in-use grant as AuthorizedAlways. The WhenInUse enum is iOS-only.
    return status == kCLAuthorizationStatusAuthorizedAlways;
}

static int mapStatus(CLAuthorizationStatus status) {
    switch (status) {
        case kCLAuthorizationStatusNotDetermined:
            return kStatusNotDetermined;
        case kCLAuthorizationStatusRestricted:
        case kCLAuthorizationStatusDenied:
            return kStatusDenied;
        case kCLAuthorizationStatusAuthorizedAlways:
            return kStatusGranted;
        default:
            return kStatusUnavailable;
    }
}

static void onMain(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_async(dispatch_get_main_queue(), block);
    }
}

static void onMainSync(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

static void postCallback(towards_location_callback callback, int code, double latitude, double longitude) {
    if (callback == NULL) {
        return;
    }
    // Leave the main queue before entering the JVM so a re-entrant status read cannot deadlock.
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        callback(code, latitude, longitude);
    });
}

static void deliver(towards_location_callback *slot, int code, double latitude, double longitude) {
    towards_location_callback callback = *slot;
    *slot = NULL;
    postCallback(callback, code, latitude, longitude);
}

@interface TowardsLocationDelegate : NSObject <CLLocationManagerDelegate>
@end

@implementation TowardsLocationDelegate

- (void)locationManagerDidChangeAuthorization:(CLLocationManager *)locationManager {
    if (permissionCallback == NULL) {
        return;
    }
    CLAuthorizationStatus status = locationManager.authorizationStatus;
    if (status == kCLAuthorizationStatusNotDetermined) {
        return;
    }
    int code = statusIsGranted(status) ? kCodeSuccess : kCodeDenied;
    deliver(&permissionCallback, code, 0, 0);
}

- (void)locationManager:(CLLocationManager *)locationManager didUpdateLocations:(NSArray<CLLocation *> *)locations {
    if (currentCallback == NULL) {
        return;
    }
    CLLocation *location = locations.lastObject;
    if (location == nil) {
        deliver(&currentCallback, kCodeUnavailable, 0, 0);
        return;
    }
    deliver(&currentCallback, kCodeSuccess, location.coordinate.latitude, location.coordinate.longitude);
}

- (void)locationManager:(CLLocationManager *)locationManager didFailWithError:(NSError *)error {
    if (currentCallback == NULL) {
        return;
    }
    int code = error.code == kCLErrorDenied ? kCodeDenied : kCodeUnavailable;
    deliver(&currentCallback, code, 0, 0);
}

@end

static void ensureManager(void) {
    if (manager != nil) {
        return;
    }
    manager = [[CLLocationManager alloc] init];
    manager.desiredAccuracy = kCLLocationAccuracyHundredMeters;
    delegate = [TowardsLocationDelegate new];
    manager.delegate = delegate;
}

int location_host_declares_location_usage(void) {
    NSBundle *bundle = [NSBundle mainBundle];
    id whenInUse = [bundle objectForInfoDictionaryKey:@"NSLocationWhenInUseUsageDescription"];
    id legacy = [bundle objectForInfoDictionaryKey:@"NSLocationUsageDescription"];
    return (whenInUse != nil || legacy != nil) ? 1 : 0;
}

int location_authorization_status(void) {
    __block int status = kStatusUnavailable;
    onMainSync(^{
        if (![CLLocationManager locationServicesEnabled]) {
            status = kStatusUnavailable;
            return;
        }
        ensureManager();
        status = mapStatus(manager.authorizationStatus);
    });
    return status;
}

int location_last_known(double *latitude, double *longitude) {
    if (latitude == NULL || longitude == NULL) {
        return 0;
    }
    __block int found = 0;
    onMainSync(^{
        CLLocation *location = manager.location;
        if (location == nil) {
            return;
        }
        *latitude = location.coordinate.latitude;
        *longitude = location.coordinate.longitude;
        found = 1;
    });
    return found;
}

void location_request_permission(towards_location_callback callback) {
    onMain(^{
        // Drop a superseded request without reporting it as a denial. The caller cancels that waiter.
        permissionCallback = NULL;
        if (![CLLocationManager locationServicesEnabled]) {
            postCallback(callback, kCodeUnavailable, 0, 0);
            return;
        }
        ensureManager();
        CLAuthorizationStatus status = manager.authorizationStatus;
        if (status != kCLAuthorizationStatusNotDetermined) {
            int code = statusIsGranted(status) ? kCodeSuccess : kCodeDenied;
            postCallback(callback, code, 0, 0);
            return;
        }
        permissionCallback = callback;
        [manager requestWhenInUseAuthorization];
    });
}

void location_request_current(towards_location_callback callback) {
    onMain(^{
        if (currentCallback != NULL) {
            deliver(&currentCallback, kCodeUnavailable, 0, 0);
        }
        currentGeneration += 1;
        int generation = currentGeneration;
        if (![CLLocationManager locationServicesEnabled]) {
            postCallback(callback, kCodeUnavailable, 0, 0);
            return;
        }
        ensureManager();
        if (!statusIsGranted(manager.authorizationStatus)) {
            postCallback(callback, kCodeDenied, 0, 0);
            return;
        }
        currentCallback = callback;
        [manager requestLocation];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 15 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            if (generation != currentGeneration || currentCallback == NULL) {
                return;
            }
            deliver(&currentCallback, kCodeUnavailable, 0, 0);
            [manager stopUpdatingLocation];
        });
    });
}

void location_cancel(void) {
    onMain(^{
        currentGeneration += 1;
        if (currentCallback != NULL) {
            deliver(&currentCallback, kCodeUnavailable, 0, 0);
        }
        [manager stopUpdatingLocation];
    });
}
