/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.codeEditor.util.SoftWrapUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

@ActionImpl(id = "EditorToggleUseSoftWrapsInPreview")
public final class ToggleUseSoftWrapsInPreviewAction extends ToggleAction implements DumbAware {
    public ToggleUseSoftWrapsInPreviewAction() {
        super(
            CodeEditorLocalize.actionToggleUseSoftWrapsInPreviewText(),
            CodeEditorLocalize.actionToggleUseSoftWrapsInPreviewDescription(),
            PlatformIconGroup.actionsTogglesoftwrap()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        if (editor == null || editor.getEditorKind() != EditorKind.PREVIEW) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        super.update(e);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        return editor != null && editor.getSettings().isUseSoftWraps();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            SoftWrapUtil.toggleSoftWraps(editor, SoftWrapAppliancePlaces.PREVIEW, state);
        }
    }
}
