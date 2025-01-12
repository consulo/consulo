/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.inject.advanced;

import consulo.language.Language;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Represents an element for the UI-driven language-injection. Each element specifies
 * the following properties:
 * <ul>
 * <li>language-id
 * <li>prefix
 * <li>suffix
 * <li>injection range (based on value-pattern for XML-related injections)
 * <li>friendly name for displaying the entry
 * </ul>
 */
public interface Injection {

  @Nonnull
  String getInjectedLanguageId();

  @Nullable
  Language getInjectedLanguage();

  @Nonnull
  String getPrefix();

  @Nonnull
  String getSuffix();

  @Nonnull
  List<TextRange> getInjectedArea(PsiElement element);

  /**
   * Determines how the injection would like being displayed (e.g. attributes
   * return a qualified TAG-NAME/@ATT-NAME combination name instead of just
   * the plain name.
   */
  @Nonnull
  String getDisplayName();

  boolean acceptsPsiElement(final PsiElement element);
}
