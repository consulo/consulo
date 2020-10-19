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

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.projectRoots.impl.SdkTableImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SystemProperties;
import consulo.bundle.PredefinedBundlesProvider;
import consulo.bundle.SdkPointerManager;
import consulo.logging.Logger;
import consulo.module.bundle.SdkPointerManagerImpl;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:05/22.11.13
 */
public class PredefinedBundlesLoader extends PreloadingActivity {
  private static final Logger LOG = Logger.getInstance(PredefinedBundlesLoader.class);

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

  private final Provider<SdkTable> mySdkTable;
  private final Provider<SdkPointerManager> mySdkPointerManager;

  @Inject
  public PredefinedBundlesLoader(Provider<SdkTable> sdkTable, Provider<SdkPointerManager> sdkPointerManager) {
    mySdkTable = sdkTable;
    mySdkPointerManager = sdkPointerManager;
  }

  @Override
  public void preload(@Nonnull ProgressIndicator indicator) {
    if (SystemProperties.is("consulo.disable.predefined.bundles")) {
      return;
    }

    SdkTable sdkTable = mySdkTable.get();

    ContextImpl context = new ContextImpl(sdkTable);
    for (PredefinedBundlesProvider provider : PredefinedBundlesProvider.EP_NAME.getExtensionList()) {
      try {
        provider.createBundles(context);
      }
      catch (Error e) {
        LOG.error(e);
      }
    }

    List<Sdk> bundles = context.myBundles;

    if (!bundles.isEmpty()) {
      for (Sdk bundle : bundles) {
        ((SdkImpl)bundle).setPredefined(true);
      }

      ((SdkTableImpl)sdkTable).addSdksUnsafe(bundles);
      ((SdkPointerManagerImpl)mySdkPointerManager.get()).updatePointers(bundles);
    }
  }
}
