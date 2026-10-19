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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.versionControlSystem.distributed.branch.DvcsBranchDataKeys;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
abstract class DvcsBranchesDelegatingAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    protected DvcsBranchesDelegatingAction(LocalizeValue text, Image icon) {
        super(text, LocalizeValue.empty(), icon);
    }

    protected abstract @Nullable AnAction createDelegate(DvcsBranchesTreePopupModel<?> model, @Nullable DvcsRef selectedRef);

    @Override
    public void update(AnActionEvent e) {
        AnAction delegate = findDelegate(e);
        e.getPresentation().setVisible(e.hasData(DvcsBranchesLogSidePanel.KEY));
        e.getPresentation().setEnabled(delegate != null);
        Image icon = delegate == null ? null : delegate.getTemplatePresentation().getIcon();
        if (icon != null) {
            e.getPresentation().setIcon(icon);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        AnAction delegate = findDelegate(e);
        if (delegate != null) {
            delegate.actionPerformed(e);
        }
    }

    private @Nullable AnAction findDelegate(AnActionEvent e) {
        DvcsBranchesLogSidePanel panel = e.getData(DvcsBranchesLogSidePanel.KEY);
        return panel == null ? null : createDelegate(panel.getModel(), e.getData(DvcsBranchDataKeys.SELECTED_REF));
    }
}
