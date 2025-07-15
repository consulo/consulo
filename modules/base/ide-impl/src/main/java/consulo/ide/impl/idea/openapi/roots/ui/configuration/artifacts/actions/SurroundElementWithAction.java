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

import consulo.application.ApplicationManager;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElementType;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class SurroundElementWithAction extends LayoutTreeActionBase {
  public SurroundElementWithAction(ArtifactEditorEx artifactEditor) {
    super("Surround With...", artifactEditor);
    CustomShortcutSet shortcutSet = new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts("SurroundWith"));
    registerCustomShortcutSet(shortcutSet, artifactEditor.getLayoutTreeComponent().getLayoutTree());
  }

  @Override
  protected boolean isEnabled() {
    return myArtifactEditor.getLayoutTreeComponent().getSelection().getCommonParentElement() != null;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final LayoutTreeComponent treeComponent = myArtifactEditor.getLayoutTreeComponent();
    LayoutTreeSelection selection = treeComponent.getSelection();
    final CompositePackagingElement<?> parent = selection.getCommonParentElement();
    if (parent == null) return;
    PackagingElementNode<?> parentNode = selection.getNodes().get(0).getParentNode();
    if (parentNode == null) return;

    if (!treeComponent.checkCanModifyChildren(parent, parentNode, selection.getNodes())) {
      return;
    }

    final CompositePackagingElementType<?>[] types = PackagingElementFactory.getInstance(e.getRequiredData(Project.KEY)).getCompositeElementTypes();
    final List<PackagingElement<?>> selected = selection.getElements();
    if (types.length == 1) {
      surroundWith(types[0], parent, selected, treeComponent);
    }
    else {
      JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<CompositePackagingElementType>("Surround With...", types) {
        @Override
        public Image getIconFor(CompositePackagingElementType aValue) {
          return aValue.getIcon();
        }

        @Nonnull
        @Override
        public String getTextFor(CompositePackagingElementType value) {
          return value.getPresentableName().get();
        }

        @Override
        public PopupStep onChosen(CompositePackagingElementType selectedValue, boolean finalChoice) {
          ApplicationManager.getApplication().invokeLater(() -> surroundWith(selectedValue, parent, selected, treeComponent));
          return FINAL_CHOICE;
        }
      }).showInBestPositionFor(e.getDataContext());
    }
  }

  private void surroundWith(CompositePackagingElementType<?> type, CompositePackagingElement<?> parent, List<PackagingElement<?>> selected,
                            LayoutTreeComponent treeComponent) {
    if (myArtifactEditor.isDisposed() || selected.isEmpty()) return;

    Project project = myArtifactEditor.getContext().getProject();
    String elementName = ContainerUtil.getFirstItem(selected, null).createPresentation(myArtifactEditor.getContext()).getPresentableName();
    String baseName = PathUtil.suggestFileName(elementName);
    CompositePackagingElement<?> newParent = type.createComposite(parent, baseName, myArtifactEditor.getContext());
    if (newParent != null) {
      treeComponent.editLayout(() -> {
        for (PackagingElement<?> element : selected) {
          newParent.addOrFindChild(ArtifactUtil.copyWithChildren(element, project));
        }
        for (PackagingElement<?> element : selected) {
          parent.removeChild(element);
        }
        parent.addOrFindChild(newParent);
      });
      treeComponent.rebuildTree();
    }
  }
}
