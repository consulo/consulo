/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.editor.completion;

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
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CompletionConfidence implements LanguageExtension {
  private static final ExtensionPointCacheKey<CompletionConfidence, ByLanguageValue<List<CompletionConfidence>>> KEY =
          ExtensionPointCacheKey.create("CompletionConfidence", LanguageOneToMany.build(false));

  @Nonnull
  public static List<CompletionConfidence> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(CompletionConfidence.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * This method is invoked first when a completion autopopup is scheduled. Extensions are able to cancel this completion process based on location.
   * For example, in string literals or comments completion autopopup may do more harm than good.
   */
  @Nonnull
  public ThreeState shouldSkipAutopopup(@Nonnull PsiElement contextElement, @Nonnull PsiFile psiFile, int offset) {
    return ThreeState.UNSURE;
  }
}
