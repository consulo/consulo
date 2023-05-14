/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ide.actions.SelectInContextImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.view.ProjectViewAutoScrollFromSourceHandler;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesAutoscrollFromSourceHandler extends ProjectViewAutoScrollFromSourceHandler {
  private final FavoritesViewSelectInTarget mySelectInTarget = new FavoritesViewSelectInTarget(myProject);

  public FavoritesAutoscrollFromSourceHandler(@Nonnull Project project, @Nonnull FavoritesViewTreeBuilder builder) {
    super(project, builder.getTree(), builder);
  }

  @Override
  protected boolean isAutoScrollEnabled() {
    return FavoritesManager.getInstance(myProject).getViewSettings().isAutoScrollFromSource();
  }

  @Override
  protected void setAutoScrollEnabled(boolean enabled) {
    FavoritesManager.getInstance(myProject).getViewSettings().setAutoScrollFromSource(enabled);
  }

  @Override
  protected void selectElementFromEditor(@Nonnull FileEditor editor) {
    final VirtualFile file = FileEditorManagerEx.getInstanceEx(myProject).getFile(editor);
    if (file != null) {
      final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
      if (psiFile != null) {
        final SelectInTarget target = mySelectInTarget;
        if (target != null) {
          final SelectInContext selectInContext = SelectInContextImpl.createEditorContext(myProject, editor);

          if (target.canSelect(selectInContext)) {
            target.selectIn(selectInContext, false);
          }
        }
      }
    }
  }
}
