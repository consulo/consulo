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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 17:38/30.03.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PsiElementFactory extends Predicate<IElementType> {
  ElementTypeEntryExtensionCollector<PsiElementFactory> EP = ElementTypeEntryExtensionCollector.create(PsiElementFactory.class);

  @Nullable
  @RequiredReadAction
  PsiElement createElement(@Nonnull ASTNode node);

  @Override
  boolean test(IElementType elementType);
}
