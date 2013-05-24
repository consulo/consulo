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
package com.intellij.lang.properties.psi.impl;

import com.intellij.lang.ASTLeafFactory;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author cdr
 */
public class PropertiesASTLeafFactory implements ASTLeafFactory{
  @Override
  @NotNull
  public LeafElement createLeaf(final IElementType type, CharSequence text) {
    if (type == PropertiesTokenTypes.VALUE_CHARACTERS) {
      return new PropertyValueImpl(type, text);
    }

    if (type == PropertiesTokenTypes.END_OF_LINE_COMMENT) {
      return new PsiCommentImpl(type, text);
    }

    return null;
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return input == PropertiesTokenTypes.VALUE_CHARACTERS || input == PropertiesTokenTypes.END_OF_LINE_COMMENT;
  }
}
