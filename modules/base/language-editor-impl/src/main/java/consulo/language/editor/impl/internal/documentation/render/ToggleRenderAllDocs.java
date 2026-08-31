// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

@ActionImpl(id = "ToggleRenderedDocPresentationForAll")
public class ToggleRenderAllDocs extends ToggleAction implements DumbAware {
    public ToggleRenderAllDocs() {
        super(
            ActionLocalize.actionTogglerendereddocpresentationforallText(),
            ActionLocalize.actionTogglerendereddocpresentationforallDescription()
        );
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        EditorSettingsExternalizable.getInstance().setDocCommentRenderingEnabled(state);
        resetAllEditorsToDefaultState();
    }

    /**
     * Sets all doc comments to their default state (rendered or not rendered) for all opened editors.
     */
    @RequiredUIAccess
    private static void resetAllEditorsToDefaultState() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            DocRenderManager.resetEditorToDefaultState(editor);
        }
    }
}
