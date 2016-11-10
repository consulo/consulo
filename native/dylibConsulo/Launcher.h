//
// Created by max on 5/4/12.
//
// To change the template use AppCode | Preferences | File Templates.
//


#import <Foundation/Foundation.h>
#import <JavaVM/jni.h>


@interface Launcher : NSObject {
    int argc;
    char** argv;
    NSString* myPropertiesFile;
    NSString* myVmOptionsFile;
    NSString* myWorkingDirectory;
}
- (id)initWithArgc:(int)anArgc argv:(char**)anArgv workDirectory:(NSString*)anWorkDirectory propertiesFile:(NSString*)anPropertiesFile vmOptionsFile:(NSString*)anVmOptionsFile;

BOOL validationJavaVersion(NSString* workingDirectory);

- (void)launch;
@end