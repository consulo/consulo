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

import com.intellij.lang.ASTLazyFactory;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.CodeFragmentElement;
import com.intellij.psi.impl.source.DummyHolderElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2:21/02.04.13
 */
public class DefaultASTLazyFactory implements ASTLazyFactory {
  @NotNull
  @Override
  public LazyParseableElement createLazy(ILazyParseableElementType type, CharSequence text) {
    if (type instanceof IFileElementType) {
      return new FileElement(type, text);
    }
    final ASTNode node = type.createNode(text);
    if (node != null) {
      return (LazyParseableElement)node;
    }
    if (type == TokenType.CODE_FRAGMENT) {
      return new CodeFragmentElement(null);
    }
    else if (type == TokenType.DUMMY_HOLDER) {
      return new DummyHolderElement(text);
    }
    return new LazyParseablePsiElement(type, text);
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return true;
  }
}
