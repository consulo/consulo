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
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.FileElement;
import consulo.language.ast.ICompositeElementType;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.impl.ast.ASTCompositeFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2:23/02.04.13
 */
@ExtensionImpl(order = "last")
public class DefaultASTCompositeFactory implements ASTCompositeFactory {
  @Nonnull
  @Override
  public CompositeElement createComposite(@Nonnull IElementType type) {
    if (type instanceof IFileElementType) {
      return new FileElement(type, null);
    }
    if (type instanceof ICompositeElementType) {
      return (CompositeElement)((ICompositeElementType)type).createCompositeNode();
    }
    return new CompositeElement(type);
  }

  @Override
  public boolean test(@Nullable IElementType input) {
    return true;
  }
}
