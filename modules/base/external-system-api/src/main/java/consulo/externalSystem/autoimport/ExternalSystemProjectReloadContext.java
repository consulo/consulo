// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.autoimport;

public interface ExternalSystemProjectReloadContext {
    /**
     * Project reload is submitted explicitly for user
     * <p>
     * Expected, project will be reloaded explicitly if this parameter is {@code true}
     */
    boolean isExplicitReload();

    /**
     * Project has undefined modifications
     * Undefined modifications are modifications, provided by {@link ExternalSystemProjectTracker#markDirty}
     * e.g. changes in settings from UI, cache invalidation and etc.
     * <p>
     * Project is expected to be fully reloaded when this flag is set to true
     */
    boolean hasUndefinedModifications();

    /**
     * Reload context that describes modifications in settings files
     *
     * @see ExternalSystemProjectAware#getSettingsFiles() for details
     */
    ExternalSystemSettingsFilesReloadContext getSettingsFilesContext();
}
