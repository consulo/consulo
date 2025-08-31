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
package consulo.language.impl.ast;

import consulo.language.ast.IElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.PsiWhiteSpaceImpl;
import consulo.language.util.CharTable;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public final class ASTFactory {
  private static final CharTable WHITESPACES = new CharTableImpl();

  // factory methods
  @Nonnull
  public static LazyParseableElement lazy(@Nonnull ILazyParseableElementType type, CharSequence text) {
    return ASTLazyFactory.EP.getValue(type).createLazy(type, text);
  }

  @Nonnull
  public static CompositeElement composite(@Nonnull IElementType type) {
    return ASTCompositeFactory.EP.getValue(type).createComposite(type);
  }

  @Nonnull
  public static LeafElement leaf(@Nonnull IElementType type, CharSequence text) {
    return leaf(type, LanguageVersionUtil.findDefaultVersion(type.getLanguage()), text);
  }

  @Nonnull
  public static LeafElement leaf(@Nonnull IElementType type, @Nonnull LanguageVersion languageVersion, CharSequence text) {
    return ASTLeafFactory.EP.getValue(type).createLeaf(type, languageVersion, text);
  }

  @Nonnull
  public static LeafElement whitespace(CharSequence text) {
    PsiWhiteSpaceImpl w = new PsiWhiteSpaceImpl(WHITESPACES.intern(text));
    CodeEditUtil.setNodeGenerated(w, true);
    return w;
  }
}
