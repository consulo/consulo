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
package consulo.psi.tree;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 17:38/30.03.13
 */
public interface PsiElementFactory extends Predicate<IElementType> {
  ElementTypeEntryExtensionCollector<PsiElementFactory> EP = ElementTypeEntryExtensionCollector.create("com.intellij.lang.psi.elementFactory");

  @Nullable
  @RequiredReadAction
  PsiElement createElement(@Nonnull ASTNode node);

  @Deprecated
  default boolean apply(IElementType elementType) {
    return false;
  }

  @Override
  @SuppressWarnings("deprecation")
  default boolean test(IElementType elementType) {
    return apply(elementType);
  }
}
