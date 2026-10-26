/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionWithDelegate;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * A top-level popup row showing the presentation an action computed in its update.
 */
final class DvcsBranchesTreeUpdatedAction extends AnAction implements ActionWithDelegate<AnAction> {
    private final AnAction myDelegate;

    DvcsBranchesTreeUpdatedAction(AnAction delegate, Presentation presentation) {
        myDelegate = delegate;
        getTemplatePresentation().copyFrom(presentation);
    }

    @Override
    public AnAction getDelegate() {
        return myDelegate;
    }

    @Override
    public boolean isDumbAware() {
        return myDelegate.isDumbAware();
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        myDelegate.actionPerformed(e);
    }
}
