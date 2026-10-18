/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.InputBoxBuilder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.util.VcsLogUtil;

@ActionImpl(id = "Vcs.Log.GoToRef", shortcutFrom = @ActionRef(id = IdeActions.ACTION_FIND))
public class GoToHashOrRefAction extends LegacyDumbAwareAction {
    public GoToHashOrRefAction() {
        super(
            VersionControlSystemLogLocalize.actionGoToRefText(),
            VersionControlSystemLogLocalize.actionGoToRefDescription(),
            PlatformIconGroup.actionsSearch()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        VcsLogUtil.triggerUsage(e);

        VcsLog log = e.getRequiredData(VcsLog.KEY);
        InputBoxBuilder.text()
            .title(VersionControlSystemLogLocalize.actionGoToRefText())
            .text(VersionControlSystemLogLocalize.actionGoToRefDescription())
            .showAsync()
            .whenComplete((reference, error) -> {
                if (reference != null && !StringUtil.isEmptyOrSpaces(reference)) {
                    log.jumpToReference(reference.trim());
                }
            });
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(VcsLog.KEY));
    }
}
