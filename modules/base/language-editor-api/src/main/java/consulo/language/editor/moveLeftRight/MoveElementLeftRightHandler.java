/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.moveLeftRight;

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

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Instances of this class implement language-specific logic of 'move element left/right' actions
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface MoveElementLeftRightHandler extends LanguageExtension {
  ExtensionPointCacheKey<MoveElementLeftRightHandler, ByLanguageValue<List<MoveElementLeftRightHandler>>> KEY =
          ExtensionPointCacheKey.create("MoveElementLeftRightHandler", LanguageOneToMany.build(false));

  @Nonnull
  public static List<MoveElementLeftRightHandler> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(MoveElementLeftRightHandler.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Returns a list of sub-elements (usually children) of given PSI element, which can be moved using 'move element left/right' actions.
   * Should return an empty array if there are no such elements.
   */
  @Nonnull
  @RequiredReadAction
  PsiElement[] getMovableSubElements(@Nonnull PsiElement element);
}
