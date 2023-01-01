/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 08/12/2022
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CollapseBlockHandler extends LanguageExtension {
  ExtensionPointCacheKey<CollapseBlockHandler, ByLanguageValue<List<CollapseBlockHandler>>> KEY = ExtensionPointCacheKey.create("CollapseBlockHandler", LanguageOneToMany.build(false));

  @Nonnull
  static List<CollapseBlockHandler> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(CollapseBlockHandler.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  PsiElement findParentBlock(@Nullable PsiElement element);

  boolean isEndBlockToken(@Nullable PsiElement element);

  @Nonnull
  default String getPlaceholderText() {
    return "{...}";
  }

  @Nonnull
  @RequiredReadAction
  default TextRange getFoldingRange(@Nonnull PsiElement element) {
    return element.getTextRange();
  }
}
