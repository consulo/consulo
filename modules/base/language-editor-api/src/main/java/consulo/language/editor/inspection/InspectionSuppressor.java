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
package consulo.language.editor.inspection;

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
import javax.annotation.Nullable;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface InspectionSuppressor extends LanguageExtension {
  ExtensionPointCacheKey<InspectionSuppressor, ByLanguageValue<List<InspectionSuppressor>>> KEY = ExtensionPointCacheKey.create("InspectionSuppressor", LanguageOneToMany.build(false));

  @Nonnull
  static List<InspectionSuppressor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(InspectionSuppressor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * @see CustomSuppressableInspectionTool#isSuppressedFor(PsiElement)
   */
  boolean isSuppressedFor(@Nonnull PsiElement element, String toolId);

  /**
   * @see BatchSuppressableTool#getBatchSuppressActions(PsiElement)
   */
  SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, String toolShortName);
}
