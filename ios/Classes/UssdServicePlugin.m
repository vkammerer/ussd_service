#import "UssdServicePlugin.h"
#import <ussd_service/ussd_service-Swift.h>

@implementation UssdServicePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftUssdServicePlugin registerWithRegistrar:registrar];
}
@end
