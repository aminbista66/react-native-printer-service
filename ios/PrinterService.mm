#import "PrinterService.h"

@implementation PrinterService
- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativePrinterServiceSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"PrinterService";
}

@end
