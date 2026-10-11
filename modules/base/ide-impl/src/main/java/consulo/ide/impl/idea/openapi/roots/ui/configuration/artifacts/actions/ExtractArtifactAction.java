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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ArtifactPointerUtil;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.ArtifactPackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutElementInfo;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Tree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.util.io.PathUtil;

import java.util.List;

/**
 * @author nik
 */
public class ExtractArtifactAction extends LayoutTreeActionBase {
    public ExtractArtifactAction(ArtifactEditorEx editor) {
        super(ProjectLocalize.actionNameExtractArtifact(), editor);
    }

    @Override
    protected boolean isEnabled() {
        return myArtifactEditor.getLayoutTreeComponent().getSelection().getCommonParentElement() != null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        LayoutTreeComponent treeComponent = myArtifactEditor.getLayoutTreeComponent();
        LayoutTreeSelection selection = treeComponent.getSelection();
        CompositePackagingElement<?> parent = selection.getCommonParentElement();
        if (parent == null) {
            return;
        }

        LayoutElementInfo parentNode = treeComponent.getParentNode(selection.getNodes().get(0));
        if (parentNode == null || !treeComponent.checkCanModifyChildren(parent, parentNode, selection.getNodes())) {
            return;
        }

        ArtifactEditorContext context = myArtifactEditor.getContext();
        List<PackagingElement<?>> selectedElements = selection.getElements();
        String initialName = "artifact";
        if (selectedElements.size() == 1) {
            initialName = PathUtil.suggestFileName(selectedElements.get(0).createPresentation(context).getPresentableName());
        }

        ExtractArtifactDialog descriptor = new ExtractArtifactDialog(context, initialName);
        DialogService dialogService = context.getProject().getApplication().getInstance(DialogService.class);
        Tree<?> tree = treeComponent.getTree();
        Dialog dialog = tree == null ? dialogService.build(descriptor) : dialogService.build(tree, descriptor);
        UIAccess uiAccess = UIAccess.current();
        dialog.showAsync().whenCompleteAsync((value, error) -> {
            if (error != null || !(value instanceof ExtractArtifactDialog.Result result) || myArtifactEditor.isDisposed()) {
                return;
            }

            Project project = context.getProject();
            ModifiableArtifactModel model = context.getOrCreateModifiableArtifactModel();
            ModifiableArtifact artifact = model.addArtifact(result.artifactName(), result.artifactType());
            treeComponent.editLayout(() -> {
                for (PackagingElement<?> element : selectedElements) {
                    artifact.getRootElement().addOrFindChild(ArtifactUtil.copyWithChildren(element, project));
                }
                for (PackagingElement<?> element : selectedElements) {
                    parent.removeChild(element);
                }
                ArtifactPointerManager pointerManager = ArtifactPointerUtil.getPointerManager(project);
                parent.addOrFindChild(new ArtifactPackagingElement(pointerManager, pointerManager.create(artifact, context.getArtifactModel())));
            });
            treeComponent.rebuildTree();
        }, uiAccess);
    }
}
