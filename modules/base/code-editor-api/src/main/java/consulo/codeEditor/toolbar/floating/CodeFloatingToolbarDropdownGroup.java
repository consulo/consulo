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
package consulo.codeEditor.toolbar.floating;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AlwaysPerformingActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

/**
 * A button of the floating code toolbar which opens its actions in a popup under itself.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public abstract class CodeFloatingToolbarDropdownGroup extends DefaultActionGroup
    implements AlwaysPerformingActionGroup, AnActionWithSyncUpdate, DumbAware {
    protected CodeFloatingToolbarDropdownGroup(LocalizeValue text) {
        super(text, true);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setPerformGroup(true);
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            this,
            editor != null ? editor.getDataContext() : e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
            e.getPlace()
        );

        CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
        if (toolbar != null) {
            toolbar.attachPopup(this, popup);
        }

        popup.showUnderneathOf(e);
    }
}
