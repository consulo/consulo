// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

/**
 * Provides external system specific icons for actions and other common external system UI elements that should be visually identified.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemIconProvider {
    ProjectSystemId getSystemId();

    /**
     * Icon for auto-reload action in editor floating toolbar (ExternalSystem.ProjectRefreshAction).
     */
    default Image getReloadIcon() {
        return PlatformIconGroup.actionsBuildloadchanges();
    }

    /**
     * Icon for project selector in dependency analyzer.
     */
    default Image getProjectIcon() {
        return Image.empty(Image.DEFAULT_ICON_SIZE);
    }

    static ExternalSystemIconProvider getExtension(ProjectSystemId systemId) {
        ExternalSystemIconProvider iconProvider = Application.get().getExtensionPoint(ExternalSystemIconProvider.class)
            .findFirstSafe(it -> systemId.equals(it.getSystemId()));
        if (iconProvider != null) {
            return iconProvider;
        }
        Logger.getInstance(ExternalSystemIconProvider.class)
            .debug("Cannot find ExternalSystemIconProvider for " + systemId + ". Fallback to default provider");
        return () -> systemId;
    }
}
