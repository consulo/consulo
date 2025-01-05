/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debug.impl.internal.action;

import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class XDebuggerActionBase extends AnAction implements AnAction.TransparentUpdate {
    private final boolean myHideDisabledInPopup;

    protected XDebuggerActionBase() {
        this(false);
    }

    protected XDebuggerActionBase(boolean hideDisabledInPopup) {
        myHideDisabledInPopup = hideDisabledInPopup;
    }

    protected XDebuggerActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        this(text, description, icon, false);
    }

    protected XDebuggerActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon, boolean hideDisabledInPopup) {
        super(text, description, icon);
        myHideDisabledInPopup = hideDisabledInPopup;
    }

    @Override
    @RequiredUIAccess
    public void update(final AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        boolean hidden = isHidden(event);
        if (hidden) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        boolean enabled = isEnabled(event);
        if (myHideDisabledInPopup && ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
        else {
            presentation.setVisible(true);
        }
        presentation.setEnabled(enabled);
    }

    protected boolean isEnabled(final AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project != null && isEnabled(project, e);
    }

    @Nonnull
    protected abstract DebuggerActionHandler getHandler();

    private boolean isEnabled(final Project project, final AnActionEvent event) {
        return getHandler().isEnabled(project, event);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull final AnActionEvent e) {
        performWithHandler(e);
    }

    protected boolean performWithHandler(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return true;
        }

        if (isEnabled(project, e)) {
            perform(project, e);
            return true;
        }

        return false;
    }

    private void perform(final Project project, final AnActionEvent e) {
        getHandler().perform(project, e);
    }

    protected boolean isHidden(AnActionEvent event) {
        final Project project = event.getData(Project.KEY);
        if (project != null) {
            if (!getHandler().isHidden(project, event)) {
                return false;
            }
        }
        return true;
    }
}
