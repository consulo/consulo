/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.lang;

import com.intellij.psi.impl.source.CharTableImpl;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.util.CharTable;
import consulo.lang.LanguageVersion;
import consulo.lang.util.LanguageVersionUtil;
import consulo.psi.tree.ASTCompositeFactory;
import consulo.psi.tree.ASTLazyFactory;
import consulo.psi.tree.ASTLeafFactory;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public final class ASTFactory {
  private static final CharTable WHITESPACES = new CharTableImpl();

  // factory methods
  @Nonnull
  public static LazyParseableElement lazy(@Nonnull final ILazyParseableElementType type, final CharSequence text) {
    return ASTLazyFactory.EP.getValue(type).createLazy(type, text);
  }

  @Nonnull
  public static CompositeElement composite(@Nonnull final IElementType type) {
    return ASTCompositeFactory.EP.getValue(type).createComposite(type);
  }

  @Nonnull
  public static LeafElement leaf(@Nonnull final IElementType type, final CharSequence text) {
    return leaf(type, LanguageVersionUtil.findDefaultVersion(type.getLanguage()), text);
  }

  @Nonnull
  public static LeafElement leaf(@Nonnull final IElementType type, @Nonnull LanguageVersion languageVersion, final CharSequence text) {
    return ASTLeafFactory.EP.getValue(type).createLeaf(type, languageVersion, text);
  }

  @Nonnull
  public static LeafElement whitespace(final CharSequence text) {
    final PsiWhiteSpaceImpl w = new PsiWhiteSpaceImpl(WHITESPACES.intern(text));
    CodeEditUtil.setNodeGenerated(w, true);
    return w;
  }
}
