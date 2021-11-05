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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import javax.annotation.Nonnull;

public interface HighlightVisitor {
  ExtensionPointName<HighlightVisitor> EP_HIGHLIGHT_VISITOR = ExtensionPointName.create("com.intellij.highlightVisitor");

  boolean suitableForFile(@Nonnull PsiFile file);

  void visit(@Nonnull PsiElement element);

  boolean analyze(@Nonnull PsiFile file, final boolean updateWholeFile, @Nonnull HighlightInfoHolder holder, @Nonnull Runnable action);

  @Nonnull
  HighlightVisitor clone();

  /**
   * @deprecated unused, left for binary compatibility
   */
  @Deprecated
  default int order() {
    return -1;
  }
}
