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
package consulo.ide.impl.idea.ide.actions;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class QuickChangeSchemesAction extends QuickSwitchSchemeAction implements DumbAware {
    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
        AnAction[] actions = getGroup().getChildren(null);
        for (AnAction action : actions) {
            group.add(action);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        FeatureUsageTracker.getInstance().triggerFeatureUsed("ui.scheme.quickswitch");
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    private DefaultActionGroup getGroup() {
        return (DefaultActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_CHANGE_SCHEME);
    }
}
