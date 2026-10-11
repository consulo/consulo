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

import consulo.compiler.artifact.element.ComplexPackagingElementType;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ToggleShowElementContentAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ShowContentAction extends DumbAwareAction {
    private final ArtifactEditorImpl myArtifactEditor;

    public ShowContentAction(ArtifactEditorImpl artifactEditor) {
        super(ProjectLocalize.artifactActionShowContentText(), LocalizeValue.empty(), PlatformIconGroup.actionsShow());
        myArtifactEditor = artifactEditor;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new ShowContentOfAllElementsAction(myArtifactEditor));
        group.addSeparator();

        PackagingElementFactory factory = PackagingElementFactory.getInstance(myArtifactEditor.getContext().getProject());
        for (ComplexPackagingElementType<?> type : factory.getComplexElementTypes()) {
            group.add(new ToggleShowElementContentAction(type, myArtifactEditor));
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            ProjectLocalize.artifactActionShowContentText().get(),
            group.build(),
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        );
        popup.showUnderneathOf(e);
    }
}
