/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author gregsh
 * @see ShowExpressionTypeAction
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ExpressionTypeProvider<T extends PsiElement> implements LanguageExtension {
  private static final ExtensionPointCacheKey<ExpressionTypeProvider, ByLanguageValue<List<ExpressionTypeProvider>>> KEY =
          ExtensionPointCacheKey.create("ExpressionTypeProvider", LanguageOneToMany.build(false));

  @Nonnull
  public static List<ExpressionTypeProvider> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ExpressionTypeProvider.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Returns HTML string for type info hint.
   *
   * @see consulo.util.lang.StringUtil#escapeXmlEntities(String)
   */
  @Nonnull
  public abstract String getInformationHint(@Nonnull T element);

  /**
   * Returns HTML string if no target found at position.
   */
  @Nonnull
  public abstract String getErrorHint();

  /**
   * Returns the list of all possible targets at specified position.
   */
  @Nonnull
  public abstract List<T> getExpressionsAt(@Nonnull PsiElement elementAt);

  /**
   * @return true if this type provider can provide more useful information (e.g. value range, nullability, etc.)
   * on elements via {@link #getAdvancedInformationHint(PsiElement)}.
   */
  public boolean hasAdvancedInformation() {
    return false;
  }

  /**
   * Returns HTML string containing advanced type information hint (e.g. nullability, values range, etc.)
   *
   * @param element an element to provide information about
   * @return an advanced information hint. Should return the same result as {@link #getInformationHint(PsiElement)}
   * if no additional information is available for given element.
   * @throws UnsupportedOperationException if this provider does not provide any advanced information
   *                                       (in this case {@link #hasAdvancedInformation()} method must return false).
   */
  @Nonnull
  public String getAdvancedInformationHint(@Nonnull T element) {
    throw new UnsupportedOperationException();
  }
}
