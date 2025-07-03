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
package consulo.language.editor.structureView;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Should be registered as language extension
 *
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PsiStructureViewFactory extends LanguageExtension {
  ExtensionPointCacheKey<PsiStructureViewFactory, ByLanguageValue<PsiStructureViewFactory>> KEY = ExtensionPointCacheKey.create("PsiStructureViewFactory", LanguageOneToOne.build());

  @Nullable
  static PsiStructureViewFactory forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(PsiStructureViewFactory.class).getOrBuildCache(KEY).get(language);
  }

  @Nullable
  @RequiredReadAction
  static StructureViewBuilder createBuilderForFile(PsiFile file) {
    PsiStructureViewFactory factory = forLanguage(file.getLanguage());
    if (factory != null) {
      return factory.getStructureViewBuilder(file);
    }
    return null;
  }

  @Nullable
  StructureViewBuilder getStructureViewBuilder(PsiFile psiFile);
}