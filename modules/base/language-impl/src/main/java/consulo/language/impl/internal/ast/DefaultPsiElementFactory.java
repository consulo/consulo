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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.IElementTypeAsPsiFactory;
import consulo.language.parser.LanguageParserDefinitions;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 1:38/02.04.13
 */
@ExtensionImpl(order = "last")
public class DefaultPsiElementFactory implements PsiElementFactory {
  @RequiredReadAction
  @Nullable
  @Override
  public PsiElement createElement(@Nonnull ASTNode node) {
    IElementType elementType = node.getElementType();
    if(elementType instanceof IElementTypeAsPsiFactory) {
      return ((IElementTypeAsPsiFactory)elementType).createElement(node);
    }

    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(elementType.getLanguage());
    if (parserDefinition != null) {
      return parserDefinition.createElement(node);
    }
    return null;
  }

  @Override
  public boolean test(@Nonnull IElementType type) {
    return true;
  }
}
