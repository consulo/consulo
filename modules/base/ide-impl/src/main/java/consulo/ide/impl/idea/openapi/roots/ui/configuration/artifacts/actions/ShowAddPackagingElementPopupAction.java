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

import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class ShowAddPackagingElementPopupAction extends DumbAwareAction {
  private final ArtifactEditorEx myArtifactEditor;

  public ShowAddPackagingElementPopupAction(ArtifactEditorEx artifactEditor) {
    super("Add...");
    myArtifactEditor = artifactEditor;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DefaultActionGroup group = new DefaultActionGroup();
    for (PackagingElementType type : PackagingElementFactory.getInstance(e.getRequiredData(Project.KEY)).getAllElementTypes()) {
      group.add(new AddNewPackagingElementAction((PackagingElementType<?>)type, myArtifactEditor));
    }
    DataContext dataContext = e.getDataContext();
    ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup("Add", group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    popup.showInBestPositionFor(dataContext);
  }
}
