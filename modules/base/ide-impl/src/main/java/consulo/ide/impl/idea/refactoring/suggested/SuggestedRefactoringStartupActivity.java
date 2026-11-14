// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringProvider;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

@ExtensionImpl
public class SuggestedRefactoringStartupActivity implements PostStartupActivity, DumbAware {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (Application.get().isHeadlessEnvironment()) {
            return;
        }
        SuggestedRefactoringProvider.getInstance(project);
    }
}
