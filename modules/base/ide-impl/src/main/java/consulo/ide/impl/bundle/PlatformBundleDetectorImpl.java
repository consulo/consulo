/*
 * Copyright 2013-2026 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.application.content.impl.internal.bundle.SdkPointerManagerImpl;
import consulo.content.bundle.PredefinedBundlesProvider;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkPointerManager;
import consulo.content.bundle.SdkTable;
import consulo.content.internal.PlatformBundleDetector;
import consulo.ide.impl.idea.openapi.projectRoots.impl.SdkConfigurationUtil;
import consulo.platform.Platform;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * The bundles of a platform, with the same providers the local machine's predefined bundles come from -
 * {@link DefaultPredefinedBundlesProvider} for the platform aware bundle types.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@Singleton
@ServiceImpl
public class PlatformBundleDetectorImpl implements PlatformBundleDetector {
    private final Application myApplication;
    private final SdkTable mySdkTable;
    private final SdkPointerManager mySdkPointerManager;

    @Inject
    public PlatformBundleDetectorImpl(Application application, SdkTable sdkTable, SdkPointerManager sdkPointerManager) {
        myApplication = application;
        mySdkTable = sdkTable;
        mySdkPointerManager = sdkPointerManager;
    }

    @Override
    public List<Sdk> detect(String platformTargetId, Platform platform, boolean predefined) {
        // predefined ones are named like the local predefined bundles, kept ones like bundles added by hand - the table
        // knows them by $remote:<target>$-<name>, see RemoteSdkNames
        PredefinedBundlesLoader.ContextImpl context =
            new PredefinedBundlesLoader.ContextImpl(mySdkTable, platformTargetId, predefined ? SdkConfigurationUtil.PREDEFINED_PREFIX : "");

        PredefinedBundlesProvider provider = myApplication.getExtensionPoint(PredefinedBundlesProvider.class)
            .findExtension(DefaultPredefinedBundlesProvider.class);
        if (provider instanceof DefaultPredefinedBundlesProvider defaultProvider) {
            defaultProvider.createBundles(context, platform);
        }
        return List.copyOf(context.myBundles);
    }

    @Override
    public void add(List<Sdk> bundles, boolean predefined) {
        for (Sdk bundle : bundles) {
            if (predefined) {
                ((SdkImpl) bundle).setPredefined(true);
            }
            mySdkTable.addSdk(bundle);
        }
        if (predefined) {
            ((SdkPointerManagerImpl) mySdkPointerManager).updatePointers(bundles);
        }
    }
}
