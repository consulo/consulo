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
package consulo.externalService.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.SystemHealthMonitor;
import consulo.application.util.SystemInfo;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateChecker;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-04-05
 */
@ExtensionImpl
public class ExternalWrongJdkSystemHealthMonitor implements SystemHealthMonitor {
    private final ApplicationPropertiesComponent myProperties;

    private static final int MIN_JRE_VERSION = 25;

    @Inject
    public ExternalWrongJdkSystemHealthMonitor(ApplicationPropertiesComponent properties) {
        myProperties = properties;
    }

    @Override
    public void check(Reporter reporter) {
        // if jre is bundled - skip, we will update it with platform
        if (PlatformOrPluginUpdateChecker.isJreBuild()) {
            return;
        }

        if (SystemInfo.isJavaVersionAtLeast(MIN_JRE_VERSION)) {
            return;
        }

        String checkJavaLower25RuntimeIgnoreKey = "java.lower." + MIN_JRE_VERSION + ".runtime";

        if (myProperties.getBoolean(checkJavaLower25RuntimeIgnoreKey)) {
            return;
        }

        LocalizeValue locValue = ExternalServiceLocalize.lowerJreVersions0Message(MIN_JRE_VERSION);

        reporter.warning(locValue.get(), new String[]{"Use Bundled JRE", "Ignore"}, new Runnable[]{
            PlatformOrPluginUpdateChecker::setForceBundledJreAtUpdate,
            () -> myProperties.setValue(checkJavaLower25RuntimeIgnoreKey, true)
        });
    }
}
