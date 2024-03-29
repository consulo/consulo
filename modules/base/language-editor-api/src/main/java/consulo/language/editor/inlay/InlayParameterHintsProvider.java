/*
 * Copyright 2013-2017 consulo.io
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
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * convert from kotlin
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlayParameterHintsProvider extends LanguageExtension {
  ExtensionPointCacheKey<InlayParameterHintsProvider, ByLanguageValue<InlayParameterHintsProvider>> KEY = ExtensionPointCacheKey.create("InlayParameterHintsProvider", LanguageOneToOne.build());

  @Nullable
  static InlayParameterHintsProvider forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(InlayParameterHintsProvider.class).getOrBuildCache(KEY).get(language);
  }

  /**
   * Hints for params to be shown
   */
  @Nonnull
  List<InlayInfo> getParameterHints(@Nonnull PsiElement element);

  /**
   * Provides fully qualified method name (e.g. "java.util.Map.put") and list of it's parameter names.
   * Used to obtain method information when adding it to blacklist
   */
  @Nullable
  MethodInfo getMethodInfo(@Nonnull PsiElement element);

  /**
   * Default list of patterns for which hints should not be shown
   */
  @Nonnull
  Set<String> getDefaultBlackList();

  /**
   * Returns language which blacklist will be appended to the resulting one
   * E.g. to prevent possible Groovy and Kotlin extensions from showing hints for blacklisted java methods.
   */
  @Nullable
  default Language getBlackListDependencyLanguage() {
    return null;
  }

  /**
   * Customise hints presentation
   */
  @Nonnull
  default String getInlayPresentation(@Nonnull String inlayText) {
    return inlayText + ":";
  }
}
