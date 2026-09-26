// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

@ServiceAPI(ComponentScope.PROJECT)
public interface ExternalSystemProjectTrackerSettings {
    static ExternalSystemProjectTrackerSettings getInstance(Project project) {
        return project.getInstance(ExternalSystemProjectTrackerSettings.class);
    }

    AutoReloadType getAutoReloadType();

    void setAutoReloadType(AutoReloadType autoReloadType);

    enum AutoReloadType {
        /**
         * Reloads a project after any changes made to build script files
         */
        ALL,

        /**
         * Reloads a project after VCS updates and changes made to build script files outside the IDE
         */
        SELECTIVE,

        /**
         * Reloads a project only if cached data is corrupted, invalid or missing
         */
        NONE
    }
}
