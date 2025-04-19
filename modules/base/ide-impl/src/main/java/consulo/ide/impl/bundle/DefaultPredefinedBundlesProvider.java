/*
 * Copyright 2013-2016 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.bundle;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.bundle.*;
import consulo.platform.Platform;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2015-02-10
 */
@ExtensionImpl(id = "default", order = "first")
public class DefaultPredefinedBundlesProvider extends PredefinedBundlesProvider {
    private record LegacySDKPath(String path, String envVarName) {
    }

    private record BundlePath(Path path, String envVarName) {
    }

    private final Application myApplication;

    @Inject
    public DefaultPredefinedBundlesProvider(Application application) {
        myApplication = application;
    }

    @Override
    public void createBundles(@Nonnull Context context) {
        Platform platform = Platform.current();

        myApplication.getExtensionPoint(SdkType.class).forEach(sdkType -> {
            if (sdkType instanceof BundleType bundleType) {
                createBundles(context, bundleType, platform);
            }
            else {
                createLegacySdks(context, sdkType, platform);
            }
        });
    }

    private void createBundles(@Nonnull Context context, BundleType sdkType, Platform platform) {
        if (!sdkType.canCreatePredefinedSdks(platform)) {
            return;
        }

        List<BundlePath> paths = new ArrayList<>();
        sdkType.collectHomePaths(platform, path -> paths.add(new BundlePath(path, null)));
        for (String envVar : sdkType.getEnviromentVariables(platform)) {
            String varValue = platform.os().getEnvironmentVariable(envVar);
            if (!StringUtil.isEmptyOrSpaces(varValue)) {
                paths.add(new BundlePath(platform.fs().getPath(varValue), envVar));
            }
        }

        for (BundlePath bundlePath : paths) {
            Path path = sdkType.adjustSelectedSdkHome(platform, bundlePath.path());

            if (sdkType.isValidSdkHome(platform, path)) {
                VirtualFile dirPath = LocalFileSystem.getInstance().findFileByNioFile(path);
                if (dirPath == null) {
                    continue;
                }

                String versionString = sdkType.getVersionString(platform, path);

                Sdk sdk;
                if (bundlePath.envVarName() != null) {
                    sdk = context.createSdkWithName(sdkType, path, bundlePath.envVarName());
                }
                else {
                    sdk = context.createSdk(platform, sdkType, path);
                }

                SdkModificator sdkModificator = sdk.getSdkModificator();
                sdkModificator.setVersionString(versionString);
                sdkModificator.commitChanges();

                sdkType.setupSdkPaths(sdk);
            }
        }
    }

    private void createLegacySdks(@Nonnull Context context, SdkType sdkType, Platform platform) {
        if (!sdkType.canCreatePredefinedSdks()) {
            return;
        }

        List<LegacySDKPath> paths = new ArrayList<>();
        for (String path : sdkType.suggestHomePaths()) {
            paths.add(new LegacySDKPath(path, null));
        }

        for (String envVar : sdkType.getEnviromentVariables(platform)) {
            String varValue = platform.os().getEnvironmentVariable(envVar);
            if (!StringUtil.isEmptyOrSpaces(varValue)) {
                paths.add(new LegacySDKPath(varValue, envVar));
            }
        }

        for (LegacySDKPath legacySDKPath : paths) {
            String path = sdkType.adjustSelectedSdkHome(legacySDKPath.path());

            if (sdkType.isValidSdkHome(path)) {
                VirtualFile dirPath = LocalFileSystem.getInstance().findFileByPath(path);
                if (dirPath == null) {
                    continue;
                }

                String sdkPath = sdkType.sdkPath(dirPath);

                Sdk sdk;
                if (legacySDKPath.envVarName() != null) {
                    sdk = context.createSdkWithName(sdkType, legacySDKPath.envVarName());
                }
                else {
                    sdk = context.createSdk(sdkType, sdkPath);
                }

                SdkModificator sdkModificator = sdk.getSdkModificator();
                sdkModificator.setHomePath(sdkPath);
                sdkModificator.setVersionString(sdkType.getVersionString(sdkPath));
                sdkModificator.commitChanges();

                sdkType.setupSdkPaths(sdk);
            }
        }
    }
}
