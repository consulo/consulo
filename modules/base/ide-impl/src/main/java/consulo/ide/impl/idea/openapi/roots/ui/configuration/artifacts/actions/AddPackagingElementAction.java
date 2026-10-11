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

import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
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
public class AddPackagingElementAction extends DumbAwareAction {
    private final ArtifactEditorEx myArtifactEditor;

    public AddPackagingElementAction(ArtifactEditorEx artifactEditor) {
        super(ProjectLocalize.artifactsAddCopyAction(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        myArtifactEditor = artifactEditor;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        PackagingElementFactory factory = PackagingElementFactory.getInstance(myArtifactEditor.getContext().getProject());
        for (PackagingElementType<?> type : factory.getAllElementTypes()) {
            if (type.isAvailableForAdd(myArtifactEditor.getContext(), myArtifactEditor.getArtifact())) {
                group.add(new AddNewPackagingElementAction(type, myArtifactEditor));
            }
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            ProjectLocalize.artifactsAddCopyAction().get(),
            group.build(),
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
        if (e.isFromActionToolbar()) {
            popup.showUnderneathOf(e);
        }
        else {
            popup.showInBestPositionFor(e.getDataContext());
        }
    }
}
