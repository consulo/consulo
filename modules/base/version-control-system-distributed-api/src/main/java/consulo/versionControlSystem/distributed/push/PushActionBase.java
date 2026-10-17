/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.push;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;

/**
 * Any OK-action in the push dialog must inherit from this base class.
 */
public abstract class PushActionBase extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public PushActionBase(LocalizeValue actionName) {
        super(actionName);
        setEnabledInModalContext(true);
    }

    protected PushActionBase() {
        setEnabledInModalContext(true);
    }

    @RequiredUIAccess
    protected abstract boolean isEnabled(VcsPushUi dialog);

    @RequiredUIAccess
    protected LocalizeValue getText(VcsPushUi dialog, boolean enabled) {
        return getTemplatePresentation().getTextValue();
    }

    @RequiredUIAccess
    protected abstract LocalizeValue getDescription(VcsPushUi dialog, boolean enabled);

    @RequiredUIAccess
    protected abstract void actionPerformed(Project project, VcsPushUi dialog);

    @RequiredUIAccess
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        VcsPushUi ui = e.getData(VcsPushUi.VCS_PUSH_DIALOG);
        if (ui == null) {
            return;
        }
        actionPerformed(project, ui);
    }

    @Override
    public final Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.apply(input -> {
            update(e);
            return null;
        }));
    }

    @RequiredUIAccess
    public final void update(AnActionEvent e) {
        VcsPushUi dialog = e.getData(VcsPushUi.VCS_PUSH_DIALOG);
        if (dialog == null || e.getData(Project.KEY) == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);

        boolean enabled = isEnabled(dialog);
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText(getText(dialog, enabled));
        e.getPresentation().setDescription(getDescription(dialog, enabled));
    }
}
