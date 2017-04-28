#import <dlfcn.h>

typedef int (launchConsulo)(int argc, char* argv[], NSString* workingDirectory, NSString* propertiesFile, NSString* vmOptionsFile, NSString* appHome);

void error(NSString* message) {
    NSAlert* alert = [[NSAlert alloc] init];
    [alert addButtonWithTitle:@"OK"];
    [alert setMessageText:@"Consulo"];
    [alert setInformativeText:[NSString stringWithFormat:@"%@.\nVisit 'https://consulo.io/trouble.html' for identify problem", message]];
    [alert setAlertStyle:NSCriticalAlertStyle];
    [alert addButtonWithTitle:@"Visit consulo.io"];
    if ([alert runModal] == NSAlertSecondButtonReturn) {
        [[NSWorkspace sharedWorkspace] openURL:[NSURL URLWithString:@"https://consulo.io/trouble.html"]];
    }
    [alert release];
}

int main(int argc, char* argv[]) {
    NSFileManager* fileManager = [NSFileManager defaultManager];

    NSString* appPath = [[NSBundle mainBundle] bundlePath];

    NSString* platformDir = [NSString stringWithFormat:@"%@/Contents/%@", appPath, @"platform"];
    if (![fileManager fileExistsAtPath:platformDir]) {
        error(@"'platform' directory is not found");
        return 1;
    }

    NSMutableArray* rootBuilds = [NSMutableArray arrayWithCapacity:1];
    NSArray* dirFiles = [fileManager contentsOfDirectoryAtPath:platformDir error:nil];

    // collect build from .app directory
    NSMutableArray* builds = [NSMutableArray arrayWithCapacity:dirFiles.count + 1];
    for (NSString* dir in dirFiles) {
        if ([dir hasPrefix:@"build"]) {
            [builds addObject:dir];
            [rootBuilds addObject:dir];
        }
    }

    // collect builds from app support
    NSString* appSupportDir = [NSString stringWithFormat:@"%@/%@", NSHomeDirectory(), @"Library/Application Support/Consulo Platform"];
    if ([fileManager fileExistsAtPath:appSupportDir]) {
        for (NSString* dir in [fileManager contentsOfDirectoryAtPath:appSupportDir error:nil]) {
            if ([dir hasPrefix:@"build"]) {
                [builds addObject:dir];
            }
        }
    }

    if (builds.count == 0) {
        error(@"No build for run");
        return 1;
    }

    NSArray* sortedArray = [builds sortedArrayUsingComparator:^(id a, id b) {
        return [a compare:b options:NSNumericSearch];
    }];

    NSString* lastBuildNotAbsolute = sortedArray[sortedArray.count - 1];

    BOOL isBootBuild = [rootBuilds containsObject:lastBuildNotAbsolute];

    NSString* selectedBuild = [NSString stringWithFormat:@"%@/%@", isBootBuild ? platformDir : appSupportDir, lastBuildNotAbsolute];

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

    void* libHandle = dlopen([[NSString stringWithFormat:@"%@/bin/libconsulo.dylib", selectedBuild] UTF8String], RTLD_NOW + RTLD_GLOBAL);
    if (libHandle) {
        launchConsulo* main = dlsym(libHandle, "launchConsulo");
        if (!main) {
            error(@"Entry point for launch is not found");
            return 3;
        }

        return main(argc, argv, selectedBuild, propertiesFile, vmOptionsFile, appPath);
    }
    else {
        error(@"Failed load launcher library");
        return 4;
    }
}
