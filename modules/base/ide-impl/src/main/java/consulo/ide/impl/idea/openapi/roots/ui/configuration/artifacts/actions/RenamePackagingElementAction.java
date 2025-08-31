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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.ProjectBundle;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.RenameablePackagingElement;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreePath;

/**
 * @author nik
 */
public class RenamePackagingElementAction extends DumbAwareAction {
  private final ArtifactEditorEx myArtifactEditor;

  public RenamePackagingElementAction(ArtifactEditorEx artifactEditor) {
    super(ProjectBundle.message("action.name.rename.packaging.element"));
    registerCustomShortcutSet(CommonShortcuts.getRename(), artifactEditor.getLayoutTreeComponent().getTreePanel());
    myArtifactEditor = artifactEditor;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    LayoutTreeSelection selection = myArtifactEditor.getLayoutTreeComponent().getSelection();
    e.getPresentation().setEnabledAndVisible(
        selection.getElementIfSingle() instanceof RenameablePackagingElement renameablePackagingElement
            && renameablePackagingElement.canBeRenamed()
    );
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    LayoutTreeSelection selection = myArtifactEditor.getLayoutTreeComponent().getSelection();
    PackagingElementNode<?> node = selection.getNodeIfSingle();
    PackagingElement<?> element = selection.getElementIfSingle();
    if (node == null || element == null) return;
    if (!myArtifactEditor.getLayoutTreeComponent().checkCanModify(element, node)) return;
    
    TreePath path = selection.getPath(node);
    myArtifactEditor.getLayoutTreeComponent().startRenaming(path);
  }
}
