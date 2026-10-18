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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.log.impl.internal.ui.filter.FilterPopup;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

/**
 * A filter of the unified log toolbar: shows the filter and its value, and drops the choices of the filter.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class VcsLogFilterComboBoxAction extends ComboBoxAction implements DumbAware {
    private final FilterPopup<?> myPopup;

    VcsLogFilterComboBoxAction(FilterPopup<?> popup) {
        myPopup = popup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(VersionControlSystemLogLocalize.filterComboText(myPopup.getName(), myPopup.getCurrentText()));
        String tooltip = myPopup.getToolTip();
        presentation.setDescription(tooltip == null ? LocalizeValue.empty() : LocalizeValue.of(tooltip));
    }

    @Override
    protected ActionGroup createPopupActionGroup(DataContext context) {
        return myPopup.createActionGroup();
    }
}
