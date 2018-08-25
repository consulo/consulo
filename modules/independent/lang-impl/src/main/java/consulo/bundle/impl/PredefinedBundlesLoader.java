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
package consulo.bundle.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.projectRoots.impl.SdkTableImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SystemProperties;
import consulo.bundle.PredefinedBundlesProvider;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:05/22.11.13
 */
public class PredefinedBundlesLoader {
  private static class ContextImpl implements PredefinedBundlesProvider.Context {
    private final List<Sdk> myBundles = new ArrayList<>();
    private final SdkTable mySdkTable;

    public ContextImpl(SdkTable sdkTable) {
      mySdkTable = sdkTable;
    }

    @Override
    @Nonnull
    public Sdk createSdkWithName(@Nonnull SdkType sdkType, @Nonnull String suggestName) {
      Sdk[] sdks = ArrayUtil.mergeArrayAndCollection(mySdkTable.getAllSdks(), myBundles, Sdk.ARRAY_FACTORY);
      String uniqueSdkName = SdkConfigurationUtil.createUniqueSdkName(suggestName + SdkConfigurationUtil.PREDEFINED_PREFIX, sdks);
      Sdk sdk = mySdkTable.createSdk(uniqueSdkName, sdkType);
      myBundles.add(sdk);
      return sdk;
    }

    @Override
    @Nonnull
    public Sdk createSdk(@Nonnull SdkType sdkType, @Nonnull String sdkHome) {
      return createSdkWithName(sdkType, sdkType.suggestSdkName(null, sdkHome));
    }
  }

  @Inject
  public PredefinedBundlesLoader(SdkTable sdkTable) {
    if (SystemProperties.is("consulo.disable.predefined.bundles")) {
      return;
    }

    ContextImpl context = new ContextImpl(sdkTable);
    for (PredefinedBundlesProvider provider : PredefinedBundlesProvider.EP_NAME.getExtensions()) {
      provider.createBundles(context);
    }

    List<Sdk> bundles = context.myBundles;

    if (!bundles.isEmpty()) {
      for (Sdk bundle : bundles) {
        ((SdkImpl)bundle).setPredefined(true);
      }

      ((SdkTableImpl)sdkTable).addSdksUnsafe(bundles);
    }
  }
}
