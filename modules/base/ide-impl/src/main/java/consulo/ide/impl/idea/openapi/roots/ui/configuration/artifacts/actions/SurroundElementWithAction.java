/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElementType;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutElementInfo;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.io.PathUtil;

import java.util.List;

/**
 * @author nik
 */
public class SurroundElementWithAction extends LayoutTreeActionBase {
    public SurroundElementWithAction(ArtifactEditorEx artifactEditor) {
        super(ActionLocalize.actionSurroundwithText(), artifactEditor);
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

        Project project = myArtifactEditor.getContext().getProject();
        CompositePackagingElementType<?>[] types = PackagingElementFactory.getInstance(project).getCompositeElementTypes();
        List<PackagingElement<?>> selected = selection.getElements();
        if (types.length == 1) {
            surroundWith(types[0], parent, selected, treeComponent);
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        for (CompositePackagingElementType<?> type : types) {
            group.add(new DumbAwareAction(type.getPresentableName(), type.getPresentableName(), type.getIcon()) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent event) {
                    surroundWith(type, parent, selected, treeComponent);
                }
            });
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            getTemplatePresentation().getText(),
            group.build(),
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
        popup.showInBestPositionFor(e.getDataContext());
    }

    @RequiredUIAccess
    private void surroundWith(
        CompositePackagingElementType<?> type,
        CompositePackagingElement<?> parent,
        List<PackagingElement<?>> selected,
        LayoutTreeComponent treeComponent
    ) {
        if (myArtifactEditor.isDisposed() || selected.isEmpty()) {
            return;
        }

        Project project = myArtifactEditor.getContext().getProject();
        String elementName = selected.get(0).createPresentation(myArtifactEditor.getContext()).getPresentableName();
        String baseName = PathUtil.suggestFileName(elementName);
        UIAccess uiAccess = UIAccess.current();
        type.createComposite(parent, baseName, myArtifactEditor.getContext()).whenCompleteAsync((newParent, error) -> {
            if (error != null || newParent == null || myArtifactEditor.isDisposed()) {
                return;
            }

            treeComponent.editLayout(() -> {
                for (PackagingElement<?> element : selected) {
                    newParent.addOrFindChild(ArtifactUtil.copyWithChildren(element, project));
                }
                for (PackagingElement<?> element : selected) {
                    parent.removeChild(element);
                }
                parent.addOrFindChild(newParent);
            });
            treeComponent.updateAndSelect(List.of(newParent));
        }, uiAccess);
    }
}
