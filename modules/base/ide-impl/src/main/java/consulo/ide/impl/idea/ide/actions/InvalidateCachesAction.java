// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.actions.InvalidateCacheDialog;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ActionImpl(id = "InvalidateCaches")
public class InvalidateCachesAction extends AnAction implements DumbAware {
    @Nonnull
    private final Application myApplication;

    @Inject
    public InvalidateCachesAction(@Nonnull Application application) {
        super(invalidateCachesTitle(application), ActionLocalize.actionInvalidatecachesDescription());
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        new InvalidateCacheDialog(myApplication, project).showAsync();
    }

    @Nonnull
    private static LocalizeValue invalidateCachesTitle(@Nonnull Application application) {
        return application.isRestartCapable()
            ? ActionLocalize.actionInvalidatecachesText()
            : ActionLocalize.actionInvalidatecachesNorestartText();
    }
}
