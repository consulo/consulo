/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.hint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;

/**
 * Returns the subset of the text range of the specified element which is considered its declaration.
 * For example, the declaration range of a method includes its modifiers, return type, name and
 * parameter list.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DeclarationRangeHandler<T extends PsiElement> {
  ExtensionPointCacheKey<DeclarationRangeHandler, Function<Class, DeclarationRangeHandler>> KEY =
          ExtensionPointCacheKey.create("DeclarationRangeHandler", ByClassGrouper.build(DeclarationRangeHandler::getElementClass));

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends PsiElement> DeclarationRangeHandler<T> findDeclarationHandler(@Nonnull T element) {
    Function<Class, DeclarationRangeHandler> call = Application.get().getExtensionPoint(DeclarationRangeHandler.class).getOrBuildCache(KEY);
    return call.apply(element.getClass());
  }

  /**
   * @return element class
   */
  @Nonnull
  Class<T> getElementClass();

  /**
   * Returns the declaration range for the specified container.
   *
   * @param container the container
   * @return the declaration range for it.
   */
  @Nonnull
  TextRange getDeclarationRange(@Nonnull T container);
}
