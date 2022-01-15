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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17:00/30.06.13
 */
public abstract class PredefinedBundlesProvider {
  public interface Context {
    @Nonnull
    Sdk createSdkWithName(@Nonnull SdkType sdkType, @Nonnull String suggestName);

    @Nonnull
    default Sdk createSdk(@Nonnull SdkType sdkType, @Nonnull String sdkHome) {
      return createSdkWithName(sdkType, sdkType.suggestSdkName(null, sdkHome));
    }
  }

  public static final ExtensionPointName<PredefinedBundlesProvider> EP_NAME = ExtensionPointName.create("com.intellij.predefinedBundlesProvider");

  public abstract void createBundles(@Nonnull Context context);
}
