#import <AppKit/AppKit.h>
#import <CoreLocation/CoreLocation.h>

static NSString *resultPath;
static NSString *commandName;

static void finish(NSString *text) {
    static BOOL done = NO;
    if (done) {
        return;
    }
    done = YES;
    if (resultPath != nil) {
        [text writeToFile:resultPath atomically:YES encoding:NSUTF8StringEncoding error:nil];
    }
    printf("%s\n", text.UTF8String);
    fflush(stdout);
    exit(0);
}

static NSString *statusText(CLAuthorizationStatus status) {
    switch (status) {
        case kCLAuthorizationStatusNotDetermined:
            return @"0";
        case kCLAuthorizationStatusRestricted:
        case kCLAuthorizationStatusDenied:
            return @"1";
        case kCLAuthorizationStatusAuthorizedAlways:
            return @"2";
        default:
            return @"3";
    }
}

@interface Helper : NSObject <CLLocationManagerDelegate>
@property CLLocationManager *manager;
@property BOOL requestedPermission;
@property BOOL requestedFix;
@property BOOL granted;
@end

@implementation Helper

- (void)handleStatus:(CLAuthorizationStatus)status {
    if ([commandName isEqualToString:@"status"]) {
        finish(statusText(status));
        return;
    }
    if (status == kCLAuthorizationStatusNotDetermined) {
        if (!self.requestedPermission) {
            self.requestedPermission = YES;
            [self.manager requestWhenInUseAuthorization];
        }
        return;
    }
    self.granted = status == kCLAuthorizationStatusAuthorizedAlways;
    if (!self.granted) {
        finish([commandName isEqualToString:@"request"] ? @"denied" : @"error");
        return;
    }
    if (!self.requestedFix) {
        self.requestedFix = YES;
        [self.manager requestLocation];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(15 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            finish(self.granted && [commandName isEqualToString:@"request"] ? @"granted" : @"error");
        });
    }
}

- (void)locationManagerDidChangeAuthorization:(CLLocationManager *)manager {
    [self handleStatus:manager.authorizationStatus];
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
    [self handleStatus:status];
}

- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations {
    CLLocation *location = locations.lastObject;
    if (location == nil) {
        finish(@"error");
        return;
    }
    finish([NSString stringWithFormat:@"%.8f %.8f", location.coordinate.latitude, location.coordinate.longitude]);
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    if (error.code == kCLErrorLocationUnknown) {
        return;
    }
    finish(self.granted && [commandName isEqualToString:@"request"] ? @"granted" : @"error");
}

@end

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        commandName = argc > 1 ? [NSString stringWithUTF8String:argv[1]] : @"status";
        resultPath = argc > 2 ? [NSString stringWithUTF8String:argv[2]] : nil;
        NSApplication *app = [NSApplication sharedApplication];
        [app setActivationPolicy:NSApplicationActivationPolicyAccessory];
        Helper *helper = [Helper new];
        helper.manager = [[CLLocationManager alloc] init];
        helper.manager.desiredAccuracy = kCLLocationAccuracyHundredMeters;
        helper.manager.delegate = helper;
        dispatch_async(dispatch_get_main_queue(), ^{
            [helper handleStatus:helper.manager.authorizationStatus];
        });
        NSTimeInterval timeout = 120.0;
        if ([commandName isEqualToString:@"current"]) {
            timeout = 15.0;
        } else if ([commandName isEqualToString:@"status"]) {
            timeout = 2.0;
        }
        [app activateIgnoringOtherApps:YES];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(timeout * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (helper.granted && [commandName isEqualToString:@"request"]) {
                finish(@"granted");
            } else if ([commandName isEqualToString:@"status"]) {
                finish(@"0");
            } else {
                finish(@"error");
            }
        });
        [app run];
    }
    return 0;
}
