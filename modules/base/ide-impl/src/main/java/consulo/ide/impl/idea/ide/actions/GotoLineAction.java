// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.ide.impl.idea.ide.util.EditorGotoLineNumberDialog;
import consulo.ide.impl.idea.ide.util.GotoLineNumberDialog;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class GotoLineAction extends AnAction implements DumbAware {
    public GotoLineAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        if (Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT))) {
            GotoLineNumberDialog dialog = new EditorGotoLineNumberDialog(project, editor);
            dialog.show();
        }
        else {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(IdeLocalize.commandGoToLine())
                .run(() -> {
                    GotoLineNumberDialog dialog = new EditorGotoLineNumberDialog(project, editor);
                    dialog.show();
                    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                });
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.hasData(Project.KEY) && event.hasData(EditorKeys.EDITOR_EVEN_IF_INACTIVE));
    }
}
