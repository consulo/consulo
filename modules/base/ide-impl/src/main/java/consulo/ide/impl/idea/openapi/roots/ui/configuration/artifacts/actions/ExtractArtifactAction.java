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

import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.ArtifactPackagingElement;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.compiler.artifact.ArtifactPointerUtil;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author nik
 */
public class ExtractArtifactAction extends LayoutTreeActionBase {
  public ExtractArtifactAction(ArtifactEditorEx editor) {
    super(ProjectBundle.message("action.name.extract.artifact"), editor);
  }

  @Override
  protected boolean isEnabled() {
    return myArtifactEditor.getLayoutTreeComponent().getSelection().getCommonParentElement() != null;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final LayoutTreeComponent treeComponent = myArtifactEditor.getLayoutTreeComponent();
    final LayoutTreeSelection selection = treeComponent.getSelection();
    final CompositePackagingElement<?> parent = selection.getCommonParentElement();
    if (parent == null) return;
    final PackagingElementNode<?> parentNode = selection.getNodes().get(0).getParentNode();
    if (parentNode == null) return;

    if (!treeComponent.checkCanModifyChildren(parent, parentNode, selection.getNodes())) {
      return;
    }


    final Collection<? extends PackagingElement> selectedElements = selection.getElements();
    String initialName = "artifact";
    if (selectedElements.size() == 1) {
      initialName = PathUtil.suggestFileName(ContainerUtil.getFirstItem(selectedElements, null).createPresentation(myArtifactEditor.getContext()).getPresentableName());
    }
    IExtractArtifactDialog dialog = showDialog(treeComponent, initialName);
    if (dialog == null) return;

    final Project project = myArtifactEditor.getContext().getProject();
    final ModifiableArtifactModel model = myArtifactEditor.getContext().getOrCreateModifiableArtifactModel();
    final ModifiableArtifact artifact = model.addArtifact(dialog.getArtifactName(), dialog.getArtifactType());
    treeComponent.editLayout(new Runnable() {
      @Override
      public void run() {
        for (PackagingElement<?> element : selectedElements) {
          artifact.getRootElement().addOrFindChild(ArtifactUtil.copyWithChildren(element, project));
        }
        for (PackagingElement element : selectedElements) {
          parent.removeChild(element);
        }
        ArtifactPointerManager pointerManager = ArtifactPointerUtil.getPointerManager(project);
        parent.addOrFindChild(new ArtifactPackagingElement(pointerManager, pointerManager.create(artifact, myArtifactEditor.getContext().getArtifactModel())));
      }
    });
    treeComponent.rebuildTree();
  }

  @Nullable
  protected IExtractArtifactDialog showDialog(LayoutTreeComponent treeComponent, String initialName) {
    final ExtractArtifactDialog dialog = new ExtractArtifactDialog(myArtifactEditor.getContext(), treeComponent, initialName);
    dialog.show();
    if (!dialog.isOK()) {
      return null;
    }
    return dialog;
  }
}
