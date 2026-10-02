// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

@ExtensionAPI(ComponentScope.PROJECT)
public interface EndpointProjectModel {
    LocalizeValue getModuleDisplayName();

    String getModuleQueryTag();

    LocalizeValue getSelectModulesTitle();

    LocalizeValue getGroupByModuleTitleShort();

    LocalizeValue getGroupByModuleTitleFull();

    Collection<EndpointModuleEntity> getModuleEntities();

    boolean isTestModule(EndpointModuleEntity entity);

    EndpointFilter createFilter(EndpointModuleEntity entity, boolean fromLibraries, boolean fromTests);

    default @Nullable EndpointModuleEntity getModuleEntityForFile(PsiFile file) {
        return null;
    }
}
