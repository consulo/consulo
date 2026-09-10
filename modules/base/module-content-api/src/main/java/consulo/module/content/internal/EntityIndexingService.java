// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.project.RootsChangeRescanningInfo;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public interface EntityIndexingService {

    static EntityIndexingService getInstance() {
        return Application.get().getInstance(EntityIndexingService.class);
    }

    void indexChanges(Project project, List<? extends RootsChangeRescanningInfo> changes);

    BuildableRootsChangeRescanningInfo createBuildableInfoBuilder();
}
