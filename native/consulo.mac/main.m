#import <dlfcn.h>

typedef int (launchConsulo)(int argc, char* argv[], NSString* workingDirectory, NSString* propertiesFile, NSString* vmOptionsFile);

void error(NSString* message) {
    NSAlert *alert = [[NSAlert alloc] init];
    [alert addButtonWithTitle:@"OK"];
    [alert setMessageText:@"Consulo"];
    [alert setInformativeText:message];
    [alert setAlertStyle:NSCriticalAlertStyle];
    [alert runModal];
    [alert release];
}

int main(int argc, char *argv[]) {
    NSFileManager* fileManager = [NSFileManager defaultManager];

    NSString* appPath = [[NSBundle mainBundle] bundlePath];

    NSString* platformDir = [NSString stringWithFormat:@"%@/Contents/%@", appPath, @"platform"];
    if (![fileManager fileExistsAtPath:platformDir]) {
        error(@"'platform' directory is not exists");
        return 1;
    }

    NSArray *dirFiles = [fileManager contentsOfDirectoryAtPath:platformDir error:nil];

    NSMutableArray* builds = [NSMutableArray arrayWithCapacity:dirFiles.count];
    for (NSString* dir in dirFiles) {
        if ([dir hasPrefix:@"build"]) {
            [builds addObject:dir];
        }
    }

    if(builds.count == 0) {
        error(@"No build for run");
        return 1;
    }

    NSArray* sortedArray = [builds sortedArrayUsingComparator:^(id a, id b) { return [a compare:b options:NSNumericSearch]; }];

    NSString* lastBuildNotAbsolute = sortedArray[sortedArray.count -1];

    NSString* lastBuild = [NSString stringWithFormat:@"%@/%@", platformDir, lastBuildNotAbsolute];

    NSString* propertiesFile = [NSString stringWithFormat:@"%@/Contents/consulo.properties", appPath];

    if (![fileManager fileExistsAtPath:propertiesFile]) {
        error(@"'consulo.properties' is not exists");
        return 1;
    }

    NSString* vmOptionsFile = [NSString stringWithFormat:@"%@/Contents/consulo.vmoptions", appPath];

    if (![fileManager fileExistsAtPath:vmOptionsFile]) {
        error(@"'consulo.vmoptions' is not exists");
        return 2;
    }

    void *libHandle = dlopen([[NSString stringWithFormat:@"%@/bin/libconsulo.dylib", lastBuild] UTF8String], RTLD_NOW + RTLD_GLOBAL);
    if (libHandle) {
        launchConsulo* main = dlsym(libHandle, "launchConsulo");
        if (!main) {
            error(@"Entry point for launch is not found");
            return 3;
        }

        return main(argc,argv, lastBuild, propertiesFile, vmOptionsFile);
    }
    else {
        error(@"Failed load launcher library");
        return 4;
    }
}
