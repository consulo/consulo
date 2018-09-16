/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.lang.folding;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedFoldingDescriptor extends FoldingDescriptor {
  private final String myPlaceholderText;

  public NamedFoldingDescriptor(@Nonnull PsiElement e, int start, int end, @Nullable FoldingGroup group, @Nonnull String placeholderText) {
    this(e.getNode(), new TextRange(start, end), group, placeholderText);
  }

  public NamedFoldingDescriptor(@Nonnull ASTNode node, int start, int end, @Nullable FoldingGroup group, @Nonnull String placeholderText) {
    this(node, new TextRange(start, end), group, placeholderText);
  }

  public NamedFoldingDescriptor(@Nonnull ASTNode node,
                                @Nonnull final TextRange range,
                                @Nullable FoldingGroup group,
                                @Nonnull String placeholderText) {
    super(node, range, group);
    myPlaceholderText = placeholderText;
  }

  @Override
  @Nonnull
  public String getPlaceholderText() {
    return myPlaceholderText;
  }
}
