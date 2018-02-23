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
package consulo.bundle;

import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 10.02.15
 */
public class DefaultPredefinedBundlesProvider extends PredefinedBundlesProvider {
  @Override
  public void createBundles(@Nonnull Consumer<SdkImpl> consumer) {
    for (SdkType sdkType : SdkType.EP_NAME.getExtensions()) {
      if(sdkType.canCreatePredefinedSdks()) {
        Collection<String> paths = sdkType.suggestHomePaths();

        for (String path : paths) {
          path = sdkType.adjustSelectedSdkHome(path);

          if(sdkType.isValidSdkHome(path)) {
            VirtualFile dirPath = LocalFileSystem.getInstance().findFileByPath(path);
            if(dirPath == null) {
              continue;
            }

            String sdkPath = sdkType.sdkPath(dirPath);

            SdkImpl sdk = createSdk(sdkType, sdkPath);
            sdk.setHomePath(sdkPath);
            sdk.setVersionString(sdkType.getVersionString(sdk));
            sdkType.setupSdkPaths(sdk);

            consumer.consume(sdk);
          }
        }
      }
    }
  }
}
