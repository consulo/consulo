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
import consulo.language.ast.*;
import consulo.language.impl.ast.ASTLazyFactory;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.impl.psi.CodeFragmentElement;
import consulo.language.impl.psi.DummyHolderElement;
import consulo.language.impl.psi.LazyParseablePsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2013-04-02
 */
@ExtensionImpl(order = "last")
public class DefaultASTLazyFactory implements ASTLazyFactory {
  @Nonnull
  @Override
  public LazyParseableElement createLazy(@Nonnull ILazyParseableElementType type, @Nullable CharSequence text) {
    if (type instanceof IFileElementType) {
      final ASTNode node = type.createNode(text);
      return node instanceof  LazyParseableElement ? (LazyParseableElement) node : new FileElement(type, text);
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
  public boolean test(@Nullable IElementType input) {
    return true;
  }
}
