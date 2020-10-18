//
// Created by max on 5/4/12.
//
// To change the template use AppCode | Preferences | File Templates.
//


#import <Foundation/Foundation.h>
#import <JavaVM/jni.h>

#ifndef JNI_VERSION_9
#define JNI_VERSION_9   0x00090000
#endif

@interface Launcher : NSObject {
    int argc;
    char** argv;
    NSString* myPropertiesFile;
    NSString* myVmOptionsFile;
    NSString* myWorkingDirectory;
    NSString* myAppHome;
}
- (id)initWithArgc:(int)anArgc argv:(char**)anArgv workDirectory:(NSString*)anWorkDirectory propertiesFile:(NSString*)anPropertiesFile vmOptionsFile:(NSString*)anVmOptionsFile appHome:(NSString*)anAppHome;

BOOL validationJavaVersion(NSString* workingDirectory);

- (void)launch;
@end