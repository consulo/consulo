// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * Provides default auto reload type used in {@code AutoImportProjectTrackerSettings}.
 * This extension point will provide auto reload type for all build systems even
 * if it is implemented in the plugin of the specific language, so it should be
 * implemented with caution.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DefaultAutoReloadTypeProvider {
    ExternalSystemProjectTrackerSettings.AutoReloadType getAutoReloadType();
}
