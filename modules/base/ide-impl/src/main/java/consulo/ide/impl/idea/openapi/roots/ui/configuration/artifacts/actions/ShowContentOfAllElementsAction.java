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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ComplexElementSubstitutionParameters;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ShowContentOfAllElementsAction extends ToggleAction implements DumbAware {
    private final ArtifactEditorImpl myArtifactEditor;

    public ShowContentOfAllElementsAction(ArtifactEditorImpl artifactEditor) {
        super(ProjectLocalize.artifactActionShowContentAllText(), LocalizeValue.empty(), null);
        myArtifactEditor = artifactEditor;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myArtifactEditor.getSubstitutionParameters().isAllSubstituted();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        ComplexElementSubstitutionParameters parameters = myArtifactEditor.getSubstitutionParameters();
        if (state) {
            parameters.setSubstituteAll();
        }
        else {
            parameters.setSubstituteNone();
        }
        myArtifactEditor.onShowContentSettingsChanged();
        myArtifactEditor.getLayoutTreeComponent().rebuildTree();
    }
}
