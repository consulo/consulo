// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.markup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ErrorStripeUpdateManager {
  public static ErrorStripeUpdateManager getInstance(Project project) {
    return project.getInstance(ErrorStripeUpdateManager.class);
  }

  @RequiredUIAccess
  public abstract void repaintErrorStripePanel(@Nonnull Editor editor);

  @RequiredUIAccess
  public abstract void setOrRefreshErrorStripeRenderer(@Nonnull EditorMarkupModel editorMarkupModel, @Nullable PsiFile file);
}
