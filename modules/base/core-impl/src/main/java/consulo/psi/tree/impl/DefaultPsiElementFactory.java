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
package consulo.psi.tree.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import consulo.annotation.access.RequiredReadAction;
import consulo.psi.tree.PsiElementFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import consulo.psi.tree.IElementTypeAsPsiFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 1:38/02.04.13
 */
public class DefaultPsiElementFactory implements PsiElementFactory {
  @RequiredReadAction
  @Nullable
  @Override
  public PsiElement createElement(@Nonnull ASTNode node) {
    IElementType elementType = node.getElementType();
    if(elementType instanceof IElementTypeAsPsiFactory) {
      return ((IElementTypeAsPsiFactory)elementType).createElement(node);
    }

    assert elementType != null;
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
