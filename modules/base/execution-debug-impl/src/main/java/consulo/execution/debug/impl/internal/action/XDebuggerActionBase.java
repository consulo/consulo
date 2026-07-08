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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * @author nik
 */
public abstract class XDebuggerActionBase extends AnAction implements AnActionWithAsyncUpdate {
    private final boolean myHideDisabledInPopup;

    protected XDebuggerActionBase() {
        this(false);
    }

    protected XDebuggerActionBase(boolean hideDisabledInPopup) {
        myHideDisabledInPopup = hideDisabledInPopup;
    }

    protected XDebuggerActionBase(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        this(text, description, icon, false);
    }

    protected XDebuggerActionBase(LocalizeValue text, LocalizeValue description, @Nullable Image icon, boolean hideDisabledInPopup) {
        super(text, description, icon);
        myHideDisabledInPopup = hideDisabledInPopup;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> updateInReadAction(e)).toCoroutine();
    }

    @RequiredReadAction
    public void updateInReadAction(AnActionEvent event) {
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

    @RequiredReadAction
    protected boolean isEnabled(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project != null && isEnabled(project, e);
    }

    protected abstract DebuggerActionHandler getHandler();

    @RequiredReadAction
    private boolean isEnabled(Project project, AnActionEvent event) {
        return getHandler().isEnabled(project, event);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
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

    private void perform(Project project, AnActionEvent e) {
        getHandler().perform(project, e);
    }

    protected boolean isHidden(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        if (project != null) {
            if (!getHandler().isHidden(project, event)) {
                return false;
            }
        }
        return true;
    }
}
