// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.markup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ErrorStripeUpdateManager {
  public static ErrorStripeUpdateManager getInstance(Project project) {
    return project.getInstance(ErrorStripeUpdateManager.class);
  }

  /**
   * Repaint the error stripe panel. Reads PsiFile asynchronously on background thread
   * and dispatches UI update to EDT. Can be called from any thread.
   */
  public abstract void repaintErrorStripePanel(Editor editor);

  /**
   * Repaint the error stripe panel with a pre-read PsiFile. Must be called on EDT.
   */
  @RequiredUIAccess
  public abstract void repaintErrorStripePanel(Editor editor, @Nullable PsiFile psiFile);

  @RequiredUIAccess
  public abstract void setOrRefreshErrorStripeRenderer(EditorMarkupModel editorMarkupModel, @Nullable PsiFile file);
}
