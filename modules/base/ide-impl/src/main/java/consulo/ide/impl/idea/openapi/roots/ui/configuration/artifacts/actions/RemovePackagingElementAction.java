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
import consulo.project.ProjectBundle;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeSelection;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.ide.impl.idea.util.IconUtil;

/**
 * @author nik
 */
public class RemovePackagingElementAction extends LayoutTreeActionBase {

  public RemovePackagingElementAction(ArtifactEditorEx artifactEditor) {
    super(ProjectBundle.message("action.name.remove.packaging.element"), ProjectBundle.message("action.description.remove.packaging.elements"),
          IconUtil.getRemoveIcon(), artifactEditor);
  }

  @Override
  protected boolean isEnabled() {
    final LayoutTreeSelection selection = myArtifactEditor.getLayoutTreeComponent().getSelection();
    if (selection.getElements().isEmpty() || myArtifactEditor.getLayoutTreeComponent().isEditing()) {
      return false;
    }
    for (PackagingElementNode<?> node : selection.getNodes()) {
      if (node.getParentNode() == null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    myArtifactEditor.removeSelectedElements();
  }
}
