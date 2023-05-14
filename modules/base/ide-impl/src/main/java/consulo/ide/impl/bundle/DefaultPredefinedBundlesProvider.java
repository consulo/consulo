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
import consulo.content.bundle.*;
import consulo.platform.Platform;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 10.02.15
 */
@ExtensionImpl(id = "default", order = "first")
public class DefaultPredefinedBundlesProvider extends PredefinedBundlesProvider {
  @Override
  public void createBundles(@Nonnull Context context) {
    Platform platform = Platform.current();

    SdkType.EP_NAME.forEachExtensionSafe(sdkType -> {
      if (sdkType instanceof BundleType bundleType) {
        createBundles(context, bundleType, platform);
      }
      else {
        createLegacySdks(context, sdkType);
      }
    });
  }

  private void createBundles(@Nonnull Context context, BundleType sdkType, Platform platform) {
    if (!sdkType.canCreatePredefinedSdks(platform)) {
      return;
    }

    List<Path> paths = new ArrayList<>();
    sdkType.collectHomePaths(platform, paths::add);

    for (Path path : paths) {
      path = sdkType.adjustSelectedSdkHome(platform, path);

      if (sdkType.isValidSdkHome(platform, path)) {
        VirtualFile dirPath = LocalFileSystem.getInstance().findFileByNioFile(path);
        if (dirPath == null) {
          continue;
        }

        String versionString = sdkType.getVersionString(platform, path);

        Sdk sdk = context.createSdk(platform, sdkType, path);
        SdkModificator sdkModificator = sdk.getSdkModificator();
        sdkModificator.setVersionString(versionString);
        sdkModificator.commitChanges();

        sdkType.setupSdkPaths(sdk);
      }
    }
  }

  private void createLegacySdks(@Nonnull Context context, SdkType sdkType) {
    if (!sdkType.canCreatePredefinedSdks()) {
      return;
    }

    Collection<String> paths = sdkType.suggestHomePaths();

    for (String path : paths) {
      path = sdkType.adjustSelectedSdkHome(path);

      if (sdkType.isValidSdkHome(path)) {
        VirtualFile dirPath = LocalFileSystem.getInstance().findFileByPath(path);
        if (dirPath == null) {
          continue;
        }

        String sdkPath = sdkType.sdkPath(dirPath);

        Sdk sdk = context.createSdk(sdkType, sdkPath);
        SdkModificator sdkModificator = sdk.getSdkModificator();
        sdkModificator.setHomePath(sdkPath);
        sdkModificator.setVersionString(sdkType.getVersionString(sdkPath));
        sdkModificator.commitChanges();

        sdkType.setupSdkPaths(sdk);
      }
    }
  }
}
