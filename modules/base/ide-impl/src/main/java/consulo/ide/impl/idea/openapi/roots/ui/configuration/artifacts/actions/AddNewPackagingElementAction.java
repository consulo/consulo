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
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.compiler.artifact.element.PackagingElementType;

/**
* @author nik
*/
public class AddNewPackagingElementAction extends DumbAwareAction {
  private final PackagingElementType<?> myType;
  private final ArtifactEditorEx myArtifactEditor;

  public AddNewPackagingElementAction(PackagingElementType<?> type, ArtifactEditorEx artifactEditor) {
    super(type.getPresentableName(), null, type.getIcon());
    myType = type;
    myArtifactEditor = artifactEditor;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    myArtifactEditor.addNewPackagingElement(myType);
  }
}
