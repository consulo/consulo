// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

@ServiceAPI(ComponentScope.PROJECT)
public interface SuggestedRefactoringProvider {
    static SuggestedRefactoringProvider getInstance(Project project) {
        return project.getInstance(SuggestedRefactoringProvider.class);
    }

    /**
     * Resets state of accumulated signature changes used for suggesting refactoring.
     */
    void reset();
}
