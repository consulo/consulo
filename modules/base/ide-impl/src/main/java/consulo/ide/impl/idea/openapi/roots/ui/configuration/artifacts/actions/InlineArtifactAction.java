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

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.ArtifactPackagingElement;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutElementInfo;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import java.util.List;

/**
 * @author nik
 */
public class InlineArtifactAction extends LayoutTreeActionBase {
    public InlineArtifactAction(ArtifactEditorEx editor) {
        super(ProjectLocalize.actionNameInlineArtifact(), editor);
    }

    @Override
    protected boolean isEnabled() {
        LayoutTreeSelection selection = myArtifactEditor.getLayoutTreeComponent().getSelection();
        LayoutElementInfo node = selection.getNodeIfSingle();
        PackagingElement<?> element = selection.getElementIfSingle();
        return element instanceof ArtifactPackagingElement && node != null && node.getParentElement(element) != null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        LayoutTreeComponent treeComponent = myArtifactEditor.getLayoutTreeComponent();
        LayoutTreeSelection selection = treeComponent.getSelection();
        PackagingElement<?> element = selection.getElementIfSingle();
        LayoutElementInfo node = selection.getNodeIfSingle();
        if (node == null || !(element instanceof ArtifactPackagingElement artifactElement)) {
            return;
        }

        CompositePackagingElement<?> parent = node.getParentElement(element);
        LayoutElementInfo parentNode = treeComponent.getParentNode(node);
        if (parent == null || parentNode == null || !treeComponent.checkCanModifyChildren(parent, parentNode, List.of(node))) {
            return;
        }

        treeComponent.editLayout(() -> {
            parent.removeChild(element);
            ArtifactEditorContext context = myArtifactEditor.getContext();
            Artifact artifact = artifactElement.findArtifact(context);
            if (artifact != null) {
                CompositePackagingElement<?> rootElement = artifact.getRootElement();
                if (rootElement instanceof ArtifactRootElement<?>) {
                    for (PackagingElement<?> child : rootElement.getChildren()) {
                        parent.addOrFindChild(ArtifactUtil.copyWithChildren(child, context.getProject()));
                    }
                }
                else {
                    parent.addOrFindChild(ArtifactUtil.copyWithChildren(rootElement, context.getProject()));
                }
            }
        });
        treeComponent.rebuildTree();
    }
}
