/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.impl.internal.ast;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILeafElementType;
import consulo.language.ast.TokenType;
import consulo.language.impl.ast.ASTLeafFactory;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.impl.psi.PsiCoreCommentImpl;
import consulo.language.impl.psi.PsiWhiteSpaceImpl;
import consulo.language.parser.LanguageParserDefinitions;
import consulo.language.parser.ParserDefinition;
import consulo.language.version.LanguageVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2:22/02.04.13
 */
@ExtensionImpl(order = "last")
public class DefaultASTLeafFactory implements ASTLeafFactory {
  @Nonnull
  @Override
  public LeafElement createLeaf(@Nonnull IElementType type, @Nonnull LanguageVersion languageVersion, @Nonnull CharSequence text) {
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(type.getLanguage());
    if(parserDefinition != null) {
      if(parserDefinition.getCommentTokens(languageVersion).contains(type)) {
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
  public boolean test(@Nullable IElementType input) {
    return true;
  }
}
