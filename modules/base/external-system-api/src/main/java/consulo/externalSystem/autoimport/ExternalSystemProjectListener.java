// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import java.util.EventListener;

/**
 * Build tool listener for providing build tool lifecycle for auto-sync support.
 * Needed to highlight bounds of project refresh on the side of an external system.
 */
public interface ExternalSystemProjectListener extends EventListener {
    default void onProjectReloadStart() {
    }

    default void onProjectReloadFinish(ExternalSystemRefreshStatus status) {
    }

    default void onSettingsFilesListChange() {
    }
}
