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

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.ProjectBundle;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.compiler.artifact.element.CompositePackagingElementType;
import consulo.compiler.artifact.element.PackagingElementFactory;

import java.util.List;

/**
 * @author nik
 */
public class AddCompositeElementAction extends DumbAwareAction {
  private final ArtifactEditorEx myArtifactEditor;
  private final CompositePackagingElementType<?> myElementType;

  public AddCompositeElementAction(ArtifactEditorEx artifactEditor, CompositePackagingElementType elementType) {
    super(ProjectBundle.message("artifacts.create.action", elementType.getPresentableName()));
    myArtifactEditor = artifactEditor;
    myElementType = elementType;
    getTemplatePresentation().setIcon(elementType.getIcon());
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    myArtifactEditor.addNewPackagingElement(myElementType);
  }

  public static void addCompositeCreateActions(List<AnAction> actions, final ArtifactEditorEx artifactEditor) {
    for (CompositePackagingElementType packagingElementType : PackagingElementFactory.getInstance(artifactEditor.getContext().getProject()).getCompositeElementTypes()) {
      actions.add(new AddCompositeElementAction(artifactEditor, packagingElementType));
    }
  }
}