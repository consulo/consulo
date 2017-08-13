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
package com.intellij.codeInsight.hints;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * convert from kotlin
 */
public interface InlayParameterHintsProvider {
  LanguageExtension<InlayParameterHintsProvider> EP = new LanguageExtension<>("com.intellij.codeInsight.parameterNameHints");

  /**
   * Hints for params to be shown
   */
  @NotNull
  List<InlayInfo> getParameterHints(@NotNull PsiElement element);

  /**
   * Provides fully qualified method name (e.g. "java.util.Map.put") and list of it's parameter names.
   * Used to obtain method information when adding it to blacklist
   */
  @Nullable
  MethodInfo getMethodInfo(@NotNull PsiElement element);

  /**
   * Default list of patterns for which hints should not be shown
   */
  @NotNull
  Set<String> getDefaultBlackList();

  /**
   * Returns language which blacklist will be appended to the resulting one
   * E.g. to prevent possible Groovy and Kotlin extensions from showing hints for blacklisted java methods.
   */
  @Nullable
  default Language getBlackListDependencyLanguage() {
    return null;
  }
}
