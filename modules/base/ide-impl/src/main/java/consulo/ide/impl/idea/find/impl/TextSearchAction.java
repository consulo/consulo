// Copyright 2013-2026 consulo.io
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.ide.impl.idea.find.impl;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

/**
 * Action to open Search Everywhere with the Text search tab selected.
 * Bound to Ctrl+Alt+Shift+E (or Cmd+Alt+Shift+E on Mac).
 */
@ActionImpl(id = "TextSearchAction")
public class TextSearchAction extends GotoActionBase implements DumbAware {
    public TextSearchAction() {
        super(FindLocalize.textSearchActionText(), FindLocalize.textSearchActionDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        showInSearchEverywherePopup(TextSearchContributor.ID, e, true, true);
    }

    @Override
    protected void gotoActionPerformed(AnActionEvent e) {
        // not used, actionPerformed directly calls showInSearchEverywherePopup
    }
}
