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

import consulo.content.internal.RemoteSdkNames;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.application.content.impl.internal.bundle.SdkPointerManagerImpl;
import consulo.application.content.impl.internal.bundle.SdkTableImpl;
import consulo.application.internal.PreloadingActivity;
import consulo.application.progress.ProgressIndicator;
import consulo.content.bundle.*;
import consulo.ide.impl.idea.openapi.projectRoots.impl.SdkConfigurationUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.SystemProperties;
import org.jspecify.annotations.Nullable;
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
    static class ContextImpl implements PredefinedBundlesProvider.Context {
        final List<Sdk> myBundles = new ArrayList<>();
        private final SdkTable mySdkTable;
        private final @Nullable String myPlatformTargetId;
        private final String myNameSuffix;

        public ContextImpl(SdkTable sdkTable) {
            this(sdkTable, null, SdkConfigurationUtil.PREDEFINED_PREFIX);
        }

        /**
         * @param platformTargetId the target the bundles live on, null for the local machine
         */
        ContextImpl(SdkTable sdkTable, @Nullable String platformTargetId, String nameSuffix) {
            mySdkTable = sdkTable;
            myPlatformTargetId = platformTargetId;
            myNameSuffix = nameSuffix;
        }

        @Override
        public Sdk createSdkWithName(PlatformAwareSdkType sdkType, Path homePath, String suggestName) {
            Sdk[] sdks = ArrayUtil.mergeArrayAndCollection(mySdkTable.getAllSdks(), myBundles, Sdk.ARRAY_FACTORY);
            String name = suggestName + myNameSuffix;
            // a bundle of a target is $remote:<target>$-<name> in the table - unique per target, project files keep <name>
            String uniqueSdkName = SdkUtil.createUniqueSdkName(
                myPlatformTargetId == null ? name : RemoteSdkNames.toTableName(myPlatformTargetId, name),
                sdks
            );
            Sdk sdk = mySdkTable.createSdk(homePath, uniqueSdkName, sdkType);
            if (myPlatformTargetId != null) {
                // the home is a path of that target - the bundle is resolved and run there
                SdkModificator modificator = sdk.getSdkModificator();
                modificator.setPlatformTargetId(myPlatformTargetId);
                modificator.commitChanges();
            }
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
                ((SdkImpl) bundle).setPredefined(true);
            }

            ((SdkTableImpl) sdkTable).addSdksUnsafe(bundles);
            ((SdkPointerManagerImpl) mySdkPointerManager.get()).updatePointers(bundles);
        }
    }
}
