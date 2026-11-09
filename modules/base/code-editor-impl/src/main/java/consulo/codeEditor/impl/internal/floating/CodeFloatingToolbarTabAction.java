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
package consulo.codeEditor.impl.internal.floating;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarDropdownGroup;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public abstract class CodeFloatingToolbarTabAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    private final boolean myForward;

    protected CodeFloatingToolbarTabAction(LocalizeValue text, boolean forward) {
        super(text);
        myForward = forward;
        setEnabledInModalContext(true);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.apply(ignored -> {
            e.getPresentation().setEnabledAndVisible(findFloatingToolbar(e) != null);
            return null;
        }));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        CodeFloatingToolbar toolbar = findFloatingToolbar(e);
        Editor editor = findEditor(e);
        if (toolbar == null || editor == null) {
            return;
        }

        AnAction next = toolbar.getNextMenuAction(myForward);
        if (next == null) {
            return;
        }

        toolbar.cancelActivePopup();

        DataContext dataContext = editor.getDataContext();
        if (next instanceof CodeFloatingToolbarDropdownGroup group) {
            group.actionPerformed(AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_FLOATING_TOOLBAR, null, dataContext));
            return;
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            new DefaultActionGroup(next),
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
            ActionPlaces.EDITOR_FLOATING_TOOLBAR
        );
        toolbar.attachPopup(next, popup);
        popup.showInBestPositionFor(dataContext);
    }

    @RequiredUIAccess
    private static @Nullable Editor findEditor(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return null;
        }

        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getProject() != project) {
                continue;
            }
            CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
            if (toolbar != null && toolbar.isShown()) {
                return editor;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private static @Nullable CodeFloatingToolbar findFloatingToolbar(AnActionEvent e) {
        return CodeFloatingToolbar.getToolbar(findEditor(e));
    }
}
