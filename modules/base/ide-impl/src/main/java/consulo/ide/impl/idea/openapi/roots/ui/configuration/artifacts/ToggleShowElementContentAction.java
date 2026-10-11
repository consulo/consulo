/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.application.dumb.DumbAware;
import consulo.compiler.artifact.element.ComplexPackagingElementType;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author nik
 */
public class ToggleShowElementContentAction extends ToggleAction implements DumbAware {
    private final ComplexPackagingElementType<?> myType;
    private final ArtifactEditorImpl myEditor;

    public ToggleShowElementContentAction(ComplexPackagingElementType<?> type, ArtifactEditorImpl editor) {
        super(type.getShowContentActionText(), LocalizeValue.empty(), null);
        myType = type;
        myEditor = editor;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myEditor.getSubstitutionParameters().isShowContentForType(myType);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        myEditor.getSubstitutionParameters().setShowContent(myType, state);
        myEditor.onShowContentSettingsChanged();
        myEditor.getLayoutTreeComponent().rebuildTree();
    }
}
