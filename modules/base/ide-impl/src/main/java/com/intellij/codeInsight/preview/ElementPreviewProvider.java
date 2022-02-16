/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.preview;

import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

public interface ElementPreviewProvider {
  ExtensionPointName<ElementPreviewProvider> EP_NAME = ExtensionPointName.create("consulo.elementPreviewProvider");

  boolean isSupportedFile(@Nonnull PsiFile psiFile);

  void show(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull Point point, boolean keyTriggered);

  /**
   * @param element if disposed
   * @param editor
   */
  void hide(@Nullable PsiElement element, @Nonnull Editor editor);
}