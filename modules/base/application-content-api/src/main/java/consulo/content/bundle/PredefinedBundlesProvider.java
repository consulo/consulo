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
package consulo.content.bundle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.platform.Platform;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 17:00/30.06.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PredefinedBundlesProvider {
  public interface Context {
    
    Sdk createSdkWithName(SdkType sdkType, String suggestName);

    
    Sdk createSdkWithName(BundleType bundleType, Path homePath, String suggestName);

    
    default Sdk createSdk(SdkType sdkType, String sdkHome) {
      return createSdkWithName(sdkType, sdkType.suggestSdkName(null, sdkHome));
    }

    
    default Sdk createSdk(Platform platform, BundleType sdkType, Path homePath) {
      return createSdkWithName(sdkType, homePath, sdkType.suggestSdkName(platform, null, homePath));
    }
  }

  public abstract void createBundles(Context context);
}
