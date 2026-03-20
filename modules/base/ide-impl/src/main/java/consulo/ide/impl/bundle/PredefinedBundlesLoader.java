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
import consulo.application.progress.ProgressIndicator;
import consulo.content.bundle.*;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.application.content.impl.internal.bundle.SdkPointerManagerImpl;
import consulo.application.content.impl.internal.bundle.SdkTableImpl;
import consulo.application.internal.PreloadingActivity;
import consulo.ide.impl.idea.openapi.projectRoots.impl.SdkConfigurationUtil;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.SystemProperties;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2013-11-22
 */
@ExtensionImpl
public class PredefinedBundlesLoader extends PreloadingActivity {
    private static final Logger LOG = Logger.getInstance(PredefinedBundlesLoader.class);

    private static class ContextImpl implements PredefinedBundlesProvider.Context {
        private final List<Sdk> myBundles = new ArrayList<>();
        private final SdkTable mySdkTable;

        public ContextImpl(SdkTable sdkTable) {
            mySdkTable = sdkTable;
        }

        
        @Override
        public Sdk createSdkWithName(
            BundleType sdkType,
            Path homePath,
            String suggestName
        ) {
            // TODO [VISTALL] path can be remote - handle it

            Sdk[] sdks = ArrayUtil.mergeArrayAndCollection(mySdkTable.getAllSdks(), myBundles, Sdk.ARRAY_FACTORY);
            String uniqueSdkName = SdkUtil.createUniqueSdkName(suggestName + SdkConfigurationUtil.PREDEFINED_PREFIX, sdks);
            Sdk sdk = mySdkTable.createSdk(homePath, uniqueSdkName, sdkType);
            myBundles.add(sdk);
            return sdk;
        }

        @Override
        
        @SuppressWarnings("deprecation")
        public Sdk createSdkWithName(SdkType sdkType, String suggestName) {
            Sdk[] sdks = ArrayUtil.mergeArrayAndCollection(mySdkTable.getAllSdks(), myBundles, Sdk.ARRAY_FACTORY);
            String uniqueSdkName = SdkUtil.createUniqueSdkName(suggestName + SdkConfigurationUtil.PREDEFINED_PREFIX, sdks);
            Sdk sdk = mySdkTable.createSdk(uniqueSdkName, sdkType);
            myBundles.add(sdk);
            return sdk;
        }

        @Override
        
        public Sdk createSdk(SdkType sdkType, String sdkHome) {
            return createSdkWithName(sdkType, sdkType.suggestSdkName(null, sdkHome));
        }
    }

    private final Application myApplication;
    private final Provider<SdkTable> mySdkTable;
    private final Provider<SdkPointerManager> mySdkPointerManager;

    @Inject
    public PredefinedBundlesLoader(Application application, Provider<SdkTable> sdkTable, Provider<SdkPointerManager> sdkPointerManager) {
        mySdkTable = sdkTable;
        myApplication = application;
        mySdkPointerManager = sdkPointerManager;
    }

    @Override
    public void preload(ProgressIndicator indicator) {
        if (SystemProperties.is("consulo.disable.predefined.bundles")) {
            return;
        }

        SdkTable sdkTable = mySdkTable.get();

        ContextImpl context = new ContextImpl(sdkTable);

        myApplication.getExtensionPoint(PredefinedBundlesProvider.class).forEach(provider -> provider.createBundles(context));

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
