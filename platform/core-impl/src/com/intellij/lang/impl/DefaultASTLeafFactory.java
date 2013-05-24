/*
 * Copyright 2013 Consulo.org
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
package com.intellij.lang.impl;

import com.intellij.lang.ASTLeafFactory;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.PsiCoreCommentImpl;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILeafElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2:22/02.04.13
 */
public class DefaultASTLeafFactory implements ASTLeafFactory {
  @NotNull
  @Override
  public LeafElement createLeaf(IElementType type, CharSequence text) {
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(type.getLanguage());
    if(parserDefinition != null) {
      if(parserDefinition.getCommentTokens().contains(type)) {
        return new PsiCoreCommentImpl(type, text);
      }
    }

    // this is special case, then type is WHITE_SPACE, but no parser definition
    if(type == TokenType.WHITE_SPACE) {
      return new PsiWhiteSpaceImpl(text);
    }

    if (type instanceof ILeafElementType) {
      return (LeafElement)((ILeafElementType)type).createLeafNode(text);
    }
    return new LeafPsiElement(type, text);
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return true;
  }
}
