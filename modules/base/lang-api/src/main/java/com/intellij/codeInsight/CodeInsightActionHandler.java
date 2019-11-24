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

package com.intellij.codeInsight;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import consulo.annotations.DeprecationInfo;
import consulo.annotations.RequiredWriteAction;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;

public interface CodeInsightActionHandler extends FileModifier {
  abstract class WriteActionAdapter implements CodeInsightActionHandler{
    @Override
    @RequiredUIAccess
    public final void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
      Application.get().runWriteAction(() -> invokeInWriteAction(project, editor, file));
    }

    @RequiredWriteAction
    public abstract void invokeInWriteAction(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file);

    @Override
    public final boolean startInWriteAction() {
      return false;
    }
  }

  @RequiredUIAccess
  void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file);

  @Deprecated
  @DeprecationInfo("Please return 'false' always, if u need wrap into write action - use WriteActionAdapter")
  default boolean startInWriteAction() {
    return true;
  }
}