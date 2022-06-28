/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.util.IncorrectOperationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * User: ik
 * Date: 03.04.2003
 * Time: 11:22:05
 *
 * @see AbstractElementManipulator
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ElementManipulator<T extends PsiElement> {
  ExtensionPointCacheKey<ElementManipulator, Function<Class, ElementManipulator>> KEY =
          ExtensionPointCacheKey.create("ItemPresentationProvider", ByClassGrouper.build(ElementManipulator::getElementClass));

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends PsiElement> ElementManipulator<T> getManipulator(@Nonnull T element) {
    Function<Class, ElementManipulator> call = Application.get().getExtensionPoint(ElementManipulator.class).getOrBuildCache(KEY);
    return call.apply(element.getClass());
  }

  /**
   * Changes the element's text to a new value
   *
   * @param element element to be changed
   * @param range range within the element
   * @param newContent new element text
   * @return changed element
   * @throws IncorrectOperationException if something goes wrong
   */
  T handleContentChange(@Nonnull T element, @Nonnull TextRange range, String newContent) throws IncorrectOperationException;

  T handleContentChange(@Nonnull T element, String newContent) throws IncorrectOperationException;

  @Nonnull
  TextRange getRangeInElement(@Nonnull T element);

  @Nonnull
  Class<T> getElementClass();
}
