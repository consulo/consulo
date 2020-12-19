//
// Created by max on 5/4/12.
//
// To change the template use AppCode | Preferences | File Templates.
//


#import "Launcher.h"
#import "VMOptionsReader.h"
#import "utils.h"
#import <dlfcn.h>

@class NSAlert;

typedef jint (JNICALL* fun_ptr_t_CreateJavaVM)(JavaVM** pvm, void** env, void* args);

NSBundle* vm;
NSString* minRequiredJavaVersion = @"1.8";

NSString* ourBootclasspath = @"$CONSULO_HOME/boot/consulo-bootstrap.jar:$CONSULO_HOME/boot/consulo-container-api.jar:$CONSULO_HOME/boot/consulo-container-impl.jar:$CONSULO_HOME/boot/consulo-desktop-bootstrap.jar:$CONSULO_HOME/boot/consulo-util-nodep.jar";

@interface NSString (CustomReplacements)
- (NSString*)replaceAll:(NSString*)pattern to:(NSString*)replacement;

@end

@implementation NSString (CustomReplacements)
- (NSString*)replaceAll:(NSString*)pattern to:(NSString*)replacement {
    if ([self rangeOfString:pattern].length == 0) return self;

    NSMutableString* answer = [[self mutableCopy] autorelease];
    [answer replaceOccurrencesOfString:pattern withString:replacement options:0 range:NSMakeRange(0, [self length])];
    return answer;
}
@end

@interface NSDictionary (TypedGetters)
- (NSDictionary*)dictionaryForKey:(id)key;

- (id)valueForKey:(NSString*)key inDictionary:(NSString*)dictKey defaultObject:(NSString*)defaultValue;
@end

@implementation NSDictionary (TypedGetters)
- (NSDictionary*)dictionaryForKey:(id)key {
    id answer = self[key];
    if ([answer isKindOfClass:[NSDictionary class]]) {
        return answer;
    }
    return nil;
}

- (id)valueForKey:(NSString*)key inDictionary:(NSString*)dictKey defaultObject:(NSString*)defaultValue {
    NSDictionary* dict = [self dictionaryForKey:dictKey];
    if (dict == nil) return nil;
    id answer = [dict valueForKey:key];
    return answer != nil ? answer : defaultValue;
}
@end

@implementation Launcher

- (id)initWithArgc:(int)anArgc argv:(char**)anArgv workDirectory:(NSString*)anWorkDirectory propertiesFile:(NSString*)anPropertiesFile vmOptionsFile:(NSString*)anVmOptionsFile appHome:(NSString*)anAppHome {
    self = [super init];
    if (self) {
        argc = anArgc;
        argv = anArgv;
        myWorkingDirectory = anWorkDirectory;
        myPropertiesFile = anPropertiesFile;
        myVmOptionsFile = anVmOptionsFile;
        myAppHome = anAppHome;
    }

    return self;
}

NSString* getOSXVersion() {
    NSString* versionString;
    NSDictionary* sv = [NSDictionary dictionaryWithContentsOfFile:@"/System/Library/CoreServices/SystemVersion.plist"];
    versionString = sv[@"ProductVersion"];
    //NSLog(@"OS X: %@", versionString);
    return versionString;
}

void showWarning(NSString* messageText) {
    NSAlert* alert = [[NSAlert alloc] init];
    [alert addButtonWithTitle:@"OK"];
    NSString* message_description = [NSString stringWithFormat:@"Java 11 or later is required."];
    NSString* informativeText = [NSString stringWithFormat:@"%@", message_description];
    [alert setMessageText:messageText];
    [alert setInformativeText:informativeText];
    [alert setAlertStyle:NSWarningAlertStyle];
    [alert runModal];
    [alert release];
}


BOOL appendBundle(NSString* path, NSMutableArray* sink) {
    if ([path hasSuffix:@"jdk"] || [path hasSuffix:@".jre"]) {
        NSBundle* bundle = [NSBundle bundleWithPath:path];
        if (bundle != nil) {
            [sink addObject:bundle];
            return true;
        }
    }
    return false;
}

BOOL appendJvmBundlesAt(NSString* path, NSMutableArray* sink) {
    NSError* error = nil;
    NSArray* names = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:path error:&error];

    BOOL res = false;
    if (names != nil) {
        for (NSString* name in names) {
            res |= appendBundle([path stringByAppendingPathComponent:name], sink);
        }
    }
    return res;
}

NSArray* allVms(NSString* workingDirectory) {
    NSMutableArray* jvmBundlePaths = [NSMutableArray array];

    NSString* appDir = workingDirectory;

    if (!appendJvmBundlesAt([appDir stringByAppendingPathComponent:@"/jre"], jvmBundlePaths)) {
        appendBundle([appDir stringByAppendingPathComponent:@"/jdk"], jvmBundlePaths);
    }
    if (jvmBundlePaths.count > 0) return jvmBundlePaths;

    appendJvmBundlesAt([NSHomeDirectory() stringByAppendingPathComponent:@"Library/Java/JavaVirtualMachines"], jvmBundlePaths);
    appendJvmBundlesAt(@"/Library/Java/JavaVirtualMachines", jvmBundlePaths);
    appendJvmBundlesAt(@"/System/Library/Java/JavaVirtualMachines", jvmBundlePaths);

    return jvmBundlePaths;
}

NSString* jvmVersion(NSBundle* bundle) {
    NSString* javaVersion = [bundle.infoDictionary valueForKey:@"JVMVersion" inDictionary:@"JavaVM" defaultObject:@"0"];
    //NSLog(@"jvmVersion: %@", javaVersion);
    return javaVersion;
}

BOOL meetMinRequirements(NSString* vmVersion) {
    return [minRequiredJavaVersion compare:vmVersion options:NSNumericSearch] <= 0;
}

BOOL satisfies(NSString* vmVersion) {
    BOOL meetRequirement = meetMinRequirements(vmVersion);
    if (!meetRequirement) {
        return meetRequirement;
    }

    return true;
}

NSComparisonResult compareVMVersions(id vm1, id vm2, void* context) {
    return [jvmVersion(vm2) compare:jvmVersion(vm1) options:NSNumericSearch];
}

NSBundle* getJDKBundle(NSString* jdkVersion, NSString* source) {
    if (jdkVersion != nil) {
        NSBundle* jdkBundle = [NSBundle bundleWithPath:jdkVersion];
        if (jdkBundle != nil && ![jvmVersion(jdkBundle) isEqualToString:@"0"]) {
            NSString* javaVersion = jvmVersion(jdkBundle);
            if (javaVersion != nil && meetMinRequirements(javaVersion)) {
                debugLog(@"VM from:");
                debugLog(source);
                debugLog([jdkBundle bundlePath]);
                return jdkBundle;
            }
        }
    }
    return nil;
}

NSBundle* findMatchingVm(NSString* workingDirectory) {
    //the environment variable.
    NSString* variable = @"CONSULO_JRE";
    // The explicitly set JRE to use.
    NSString* explicit = [[NSProcessInfo processInfo] environment][variable];
    if (explicit != nil) {
        NSLog(@"Value of %@: %@", variable, explicit);
        NSBundle* jdkBundle = getJDKBundle(explicit, @"environment variable");
        if (jdkBundle != nil) {
            return jdkBundle;
        }
    }

    NSArray* vmBundles = [allVms(workingDirectory) sortedArrayUsingFunction:compareVMVersions context:NULL];

    if (isDebugEnabled()) {
        debugLog(@"Found Java Virtual Machines:");
        for (NSBundle* vm in vmBundles) {
            debugLog([vm bundlePath]);
        }
    }

    for (NSBundle* vm in vmBundles) {
        if (satisfies(jvmVersion(vm))) {
            debugLog(@"Chosen VM:");
            debugLog([vm bundlePath]);
            return vm;
        }
    }

    NSLog(@"No matching VM found.");
    showWarning(@"No matching VM found.");
    return nil;
}

CFBundleRef NSBundle2CFBundle(NSBundle* bundle) {
    CFURLRef bundleURL = (CFURLRef) ([NSURL fileURLWithPath:bundle.bundlePath]);
    return CFBundleCreate(kCFAllocatorDefault, bundleURL);
}

- (NSString*)expandMacros:(NSString*)str {
    return [str replaceAll:@"$CONSULO_HOME" to:myWorkingDirectory];
}

NSArray* parseAppVMOptions(NSString* vmOptionsFile) {
    NSMutableArray* options = [NSMutableArray array];

    NSLog(@"Processing AppVMOptions file at %@", vmOptionsFile);
    NSArray* contents = [VMOptionsReader readFile:vmOptionsFile];
    if (contents != nil) {
        NSLog(@"Done");
        [options addObjectsFromArray:contents];
    } else {
        NSLog(@"No content found at %@ ", vmOptionsFile);
    }
    return options;
}

NSArray* parseVMOptions(NSString* vmOptionsFile) {
    NSMutableArray* options = [NSMutableArray array];
    NSMutableArray* used = [NSMutableArray array];

    NSLog(@"Processing VMOptions file at %@", vmOptionsFile);
    NSArray* contents = [VMOptionsReader readFile:vmOptionsFile];
    if (contents != nil) {
        NSLog(@"Done");
        [used addObject:vmOptionsFile];
        [options addObjectsFromArray:contents];
    } else {
        NSLog(@"No content found at %@ ", vmOptionsFile);
    }
    // deprecated option
    [options addObject:[NSString stringWithFormat:@"-Djb.vmOptionsFile=%@", [used componentsJoinedByString:@","]]];

    [options addObject:[NSString stringWithFormat:@"-Dconsulo.vm.options.files=%@", [used componentsJoinedByString:@","]]];

    return options;
}

- (JavaVMInitArgs)buildArgsFor:(NSBundle*)jvm {
    NSMutableArray* args_array = [NSMutableArray array];

    //[args_array addObject:[NSString stringWithFormat:@"-Djava.class.path=%@", ourBootclasspath]];
    [args_array addObject:[NSString stringWithFormat:@"--module-path=%@/boot:%@/boot/spi", myWorkingDirectory, myWorkingDirectory]];
    [args_array addObject:@"-Djdk.module.main=consulo.desktop.bootstrap"];
    [args_array addObject:@"-Dconsulo.module.path.boot=true"];

    [args_array addObjectsFromArray:parseAppVMOptions([NSString stringWithFormat:@"%@/bin/app.vmoptions", myWorkingDirectory])];

    [args_array addObjectsFromArray:parseVMOptions(myVmOptionsFile)];
    [args_array addObjectsFromArray:[@"-Dfile.encoding=UTF-8 -ea -Dsun.io.useCanonCaches=false -Djava.net.preferIPv4Stack=true -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Xverify:none" componentsSeparatedByString:@" "]];
    [args_array addObject:[NSString stringWithFormat:@"-Dconsulo.properties.file=%@", myPropertiesFile]];
    [args_array addObject:[NSString stringWithFormat:@"-Dconsulo.home.path=%@", myWorkingDirectory]];
    [args_array addObject:[NSString stringWithFormat:@"-Dconsulo.app.home.path=%@/Contents", myAppHome]];

    // deprecated options
    [args_array addObject:[NSString stringWithFormat:@"-Didea.properties.file=%@", myPropertiesFile]];
    [args_array addObject:[NSString stringWithFormat:@"-Didea.home.path=%@", myWorkingDirectory]];

    JavaVMInitArgs args;
    args.version = JNI_VERSION_9;
    args.ignoreUnrecognized = JNI_TRUE;

    args.nOptions = (jint) [args_array count];
    args.options = calloc((size_t) args.nOptions, sizeof(JavaVMOption));
    for (NSUInteger idx = 0; idx < args.nOptions; idx++) {
        id obj = args_array[idx];
        NSString* jvmProperty = [self expandMacros:[obj description]];
        debugLog([NSString stringWithFormat:@"Adding property %@", jvmProperty]);
        args.options[idx].optionString = strdup([jvmProperty UTF8String]);
    }
    return args;
}

- (void)process_cwd {
    NSString* cwd = myWorkingDirectory;
    cwd = [self expandMacros:cwd];

    if (chdir([cwd UTF8String]) != 0) {
        NSLog(@"Cannot chdir to working directory at %@", cwd);
    }
}

BOOL validationJavaVersion(NSString* workingDirectory) {
    vm = findMatchingVm(workingDirectory);
    if (vm == nil) {
        return false;
    }
    return true;
}

- (void)alert:(NSArray*)values {
    NSAlert* alert = [[[NSAlert alloc] init] autorelease];
    [alert setMessageText:values[0]];
    [alert setInformativeText:values[1]];

    if ([values count] > 2) {
        NSTextView* accessory = [[NSTextView alloc] initWithFrame:NSMakeRect(0, 0, 300, 15)];
        [accessory setFont:[NSFont systemFontOfSize:[NSFont smallSystemFontSize]]];
        NSMutableAttributedString* str = [[NSMutableAttributedString alloc] initWithString:values[2]];
        [str addAttribute:NSLinkAttributeName value:values[2] range:NSMakeRange(0, str.length)];
        [accessory insertText:str];
        [accessory setEditable:NO];
        [accessory setDrawsBackground:NO];
        [alert setAccessoryView:accessory];
    }

    [alert runModal];
}

- (void)launch {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    if (vm == nil) {
        showWarning(@"No matching VM found.");
        NSLog(@"Cannot find matching VM, aborting");
        exit(-1);
    }

    NSError* error = nil;
    BOOL ok = [vm loadAndReturnError:&error];
    if (!ok) {
        NSLog(@"Cannot load JVM bundle: %@", error);
        exit(-1);
    }

    CFBundleRef cfvm = NSBundle2CFBundle(vm);

    fun_ptr_t_CreateJavaVM create_vm = CFBundleGetFunctionPointerForName(cfvm, CFSTR("JNI_CreateJavaVM"));

    if (create_vm == NULL) {
        // We have Apple VM chosen here...
/*
        [self execCommandLineJava:vm];
        return;
*/

        NSString* serverLibUrl = [vm.bundlePath stringByAppendingPathComponent:@"Contents/Libraries/libserver.dylib"];

        void* libHandle = dlopen(serverLibUrl.UTF8String, RTLD_NOW + RTLD_GLOBAL);
        if (libHandle) {
            create_vm = dlsym(libHandle, "JNI_CreateJavaVM_Impl");
        }
    }

    if (create_vm == NULL) {
        NSLog(@"Cannot find JNI_CreateJavaVM in chosen JVM bundle at %@", vm.bundlePath);
        exit(-1);
    }

    [self process_cwd];

    JNIEnv* env;
    JavaVM* jvm;

    JavaVMInitArgs args = [self buildArgsFor:vm];

    jint create_vm_rc = create_vm(&jvm, &env, &args);
    if (create_vm_rc != JNI_OK || jvm == NULL) {
        NSLog(@"JNI_CreateJavaVM (%@) failed: %ld", vm.bundlePath, create_vm_rc);
        exit(-1);
    }

    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    if (string_class == NULL) {
        NSLog(@"No java.lang.String in classpath!");
        exit(-1);
    }

    const char* mainClassName = "consulo/desktop/boot/main/Main";
    jclass mainClass = (*env)->FindClass(env, mainClassName);
    if (mainClass == NULL || (*env)->ExceptionOccurred(env)) {
        NSLog(@"Main class %s not found", mainClassName);
        (*env)->ExceptionDescribe(env);
        exit(-1);
    }

    jmethodID mainMethod = (*env)->GetStaticMethodID(env, mainClass, "main", "([Ljava/lang/String;)V");
    if (mainMethod == NULL || (*env)->ExceptionOccurred(env)) {
        NSLog(@"Cant't find main() method");
        (*env)->ExceptionDescribe(env);
        exit(-1);
    }

    // See http://stackoverflow.com/questions/10242115/os-x-strange-psn-command-line-parameter-when-launched-from-finder
    // about psn_ stuff
    int arg_count = 0;
    for (int i = 1; i < argc; i++) {
        if (memcmp(argv[i], "-psn_", 4) != 0) arg_count++;
    }

    jobject jni_args = (*env)->NewObjectArray(env, arg_count, string_class, NULL);

    arg_count = 0;
    for (int i = 1; i < argc; i++) {
        if (memcmp(argv[i], "-psn_", 4) != 0) {
            jstring jni_arg = (*env)->NewStringUTF(env, argv[i]);
            (*env)->SetObjectArrayElement(env, jni_args, arg_count, jni_arg);
            arg_count++;
        }
    }

    (*env)->CallStaticVoidMethod(env, mainClass, mainMethod, jni_args);

    (*jvm)->DetachCurrentThread(jvm);
    (*jvm)->DestroyJavaVM(jvm);

    [pool release];
}

@end
