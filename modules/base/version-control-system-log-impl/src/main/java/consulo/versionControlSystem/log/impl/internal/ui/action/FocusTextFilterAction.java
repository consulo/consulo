/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.UnifiedVcsLogUi;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

@ActionImpl(id = VcsLogActionPlaces.VCS_LOG_FOCUS_TEXT_FILTER)
public class FocusTextFilterAction extends LegacyDumbAwareAction {
    public FocusTextFilterAction() {
        super(VersionControlSystemLogLocalize.actionFocusTextFilterText(), VersionControlSystemLogLocalize.actionFocusTextFilterDescription());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getData(VcsLogUi.KEY) instanceof UnifiedVcsLogUi);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        if (e.getData(VcsLogUi.KEY) instanceof UnifiedVcsLogUi logUi) {
            logUi.toggleTextFilterFocus();
        }
    }
}
