// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionAPI(ComponentScope.PROJECT)
public interface SilentChangeVetoer {

    ThreeState canChangeFileSilently(VirtualFile virtualFile);

    /**
     * Queries all {@link SilentChangeVetoer} extensions about the status of the {@code virtualFile}.
     * Might access indexes (to determine the relevant VCS), so it must run on a background thread.
     *
     * @return {@link ThreeState#NO} or {@link ThreeState#YES} if at least one extension returned that
     * result; {@link ThreeState#UNSURE} otherwise
     */
    static ThreeState extensionsAllowToChangeFileSilently(Project project, VirtualFile virtualFile) {
        for (SilentChangeVetoer extension : project.getExtensionList(SilentChangeVetoer.class)) {
            ThreeState override = extension.canChangeFileSilently(virtualFile);
            if (override == ThreeState.NO || override == ThreeState.YES) {
                return override;
            }
        }
        return ThreeState.UNSURE;
    }
}
