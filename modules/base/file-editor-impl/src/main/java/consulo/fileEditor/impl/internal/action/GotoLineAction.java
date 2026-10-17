// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.fileEditor.internal.largeFileEditor.LargeEditorActionUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PlatformDataKeys;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;

@ActionImpl(id = "GotoLine")
public class GotoLineAction extends LegacyDumbAwareAction {
    public GotoLineAction() {
        super(ActionLocalize.actionGotolineText(), ActionLocalize.actionGotolineDescription());
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getRequiredData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        boolean modalContext = Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT));
        GotoLineNumberDialog.show(project, editor, editor.getContentUIComponent(), modalContext ? null : IdeLocalize.commandGoToLine());
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(
            event.hasData(Project.KEY)
                && event.hasData(EditorKeys.EDITOR_EVEN_IF_INACTIVE)
                && LargeEditorActionUtil.tryGetLargeFileEditorManager(event) == null
        );
    }
}
