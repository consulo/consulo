/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.Factory;
import consulo.language.internal.ASTElementFactory;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jul-22
 */
@ServiceImpl
public class ASTElementFactoryImpl implements ASTElementFactory {
  @Nonnull
  @Override
  public ASTNode createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, int startOffset, int endOffset, @Nullable CharTable table, PsiManager manager) {
    return Factory.createSingleLeafElement(type, buffer, startOffset, endOffset, table, manager);
  }
}
