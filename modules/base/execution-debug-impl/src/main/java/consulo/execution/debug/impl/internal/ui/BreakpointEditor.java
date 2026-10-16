/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui;

import consulo.codeEditor.Editor;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CompositeShortcutSet;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author zajac
 * @since 2012-04-04
 */
public class BreakpointEditor {
    public interface Delegate {
        void done();

        void more();
    }

    private final DockLayout myMainPanel = DockLayout.create();
    private final Hyperlink myShowMoreOptionsLink;
    private @Nullable Delegate myDelegate;

    @RequiredUIAccess
    public BreakpointEditor() {
        AnAction viewBreakpoints = ActionManager.getInstance().getAction(XDebuggerActions.VIEW_BREAKPOINTS);
        String shortcutText = viewBreakpoints != null ? KeymapUtil.getFirstKeyboardShortcutText(viewBreakpoints) : null;
        LocalizeValue moreText = StringUtil.isEmpty(shortcutText)
            ? XDebuggerLocalize.xbreakpointEditorMoreLink()
            : XDebuggerLocalize.xbreakpointEditorMoreLinkWithShortcut(shortcutText);
        myShowMoreOptionsLink = Hyperlink.create(moreText, event -> {
            Delegate delegate = myDelegate;
            if (delegate != null) {
                delegate.more();
            }
        });

        Button doneButton = Button.create(XDebuggerLocalize.xbreakpointEditorDoneButton(), event -> done());

        myMainPanel.bottom(DockLayout.create()
            .left(myShowMoreOptionsLink)
            .right(HorizontalLayout.create().add(doneButton)));

        myMainPanel.paddingBuilder().horizontalSet(Space.LARGE).verticalSet(Space.MEDIUM).apply();

        AnAction doneAction = new DoneAction();
        doneAction.registerCustomShortcutSet(
            new CompositeShortcutSet(CustomShortcutSet.fromString("ESCAPE"), CustomShortcutSet.fromString("ENTER")),
            myMainPanel
        );
    }

    private class DoneAction extends AnAction implements AnActionWithUIUpdate {
        @RequiredUIAccess
        @Override
        public void updateAtUI(AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            boolean lookup = project != null && LookupManager.getInstance(project).getActiveLookup() != null;
            Editor editor = e.getData(Editor.KEY);
            e.getPresentation()
                .setEnabled(!lookup && (editor == null || StringUtil.isEmpty(editor.getSelectionModel().getSelectedText())));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            done();
        }
    }

    public Component getMainPanel() {
        return myMainPanel;
    }

    public void setShowMoreOptionsLink(boolean visible) {
        myShowMoreOptionsLink.setVisible(visible);
    }

    private void done() {
        Delegate delegate = myDelegate;
        if (delegate != null) {
            delegate.done();
        }
    }

    public void setPropertiesPanel(Component panel) {
        myMainPanel.center(panel);
    }

    public void setDelegate(@Nullable Delegate delegate) {
        myDelegate = delegate;
    }
}
