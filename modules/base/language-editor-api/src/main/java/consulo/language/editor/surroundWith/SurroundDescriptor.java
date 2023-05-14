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
package consulo.language.editor.surroundWith;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Defines a code fragment type on which the Surround With action can be used for files
 * in a custom language. All surround descriptors registered for a language are queried
 * sequentially, and as soon as one is found that returns a non-empty list of elements
 * from {@link #getElementsToSurround(PsiFile, int, int)}, the user
 * is prompted to choose a specific surrounder for that surround descriptor.
 *
 * @author ven
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SurroundDescriptor extends LanguageExtension {
  ExtensionPointCacheKey<SurroundDescriptor, ByLanguageValue<List<SurroundDescriptor>>> KEY = ExtensionPointCacheKey.create("SurroundDescriptor", LanguageOneToMany.build(false));

  @Nonnull
  static List<SurroundDescriptor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(SurroundDescriptor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Returns the list of elements which will be included in the surrounded region for
   * the specified selection in the specified file, or an empty array if no surrounders
   * from this surround descriptor are applicable to the specified selection.
   *
   * @param file        the file where elements are to be surrounded.
   * @param startOffset the selection start offset, with whitespaces skipped
   * @param endOffset   the selection end offset, with whitespaces skipped
   * @return the elements to be surrounded, or an empty array if cannot surround
   */
  @Nonnull
  @RequiredReadAction
  PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset);

  /**
   * Returns the list of surrounders (surround templates) which can be used for this
   * code fragment type.
   *
   * @return the list of surrounders.
   */
  @Nonnull
  Surrounder[] getSurrounders();

  boolean isExclusive();
}
