// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.autoimport;

import java.util.Set;

/**
 * Reload context that describes modifications in settings files
 *
 * @see ExternalSystemProjectAware#getSettingsFiles() for details
 */
public interface ExternalSystemSettingsFilesReloadContext {
    /**
     * Paths of updated files since previous reload.
     */
    Set<String> getUpdated();

    /**
     * Paths of create files since previous reload.
     */
    Set<String> getCreated();

    /**
     * Paths of deleted files since previous reload.
     */
    Set<String> getDeleted();
}
