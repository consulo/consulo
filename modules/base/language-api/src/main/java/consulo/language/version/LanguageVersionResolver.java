/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.version;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18:05/30.05.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageVersionResolver extends LanguageExtension {
  LanguageVersionResolver DEFAULT = new LanguageVersionResolver() {
    @Nonnull
    @Override
    public Language getLanguage() {
      return Language.ANY;
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable PsiElement element) {
      final LanguageVersion[] versions = language.getVersions();
      for (LanguageVersion version : versions) {
        if (version instanceof LanguageVersionWithDefinition && ((LanguageVersionWithDefinition)version).isMyElement(element)) {
          return version;
        }
      }
      return versions[0];
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile) {
      final LanguageVersion[] versions = language.getVersions();
      for (LanguageVersion version : versions) {
        if (version instanceof LanguageVersionWithDefinition && ((LanguageVersionWithDefinition)version).isMyFile(project, virtualFile)) {
          return version;
        }
      }
      return versions[0];
    }
  };

  @Nonnull
  static LanguageVersionResolver forLanguage(@Nonnull Language language) {
    ByLanguageValue<LanguageVersionResolver> value = Application.get().getExtensionPoint(LanguageVersionResolver.class).getOrBuildCache(KEY);
    return value.get(language);
  }

  ExtensionPointCacheKey<LanguageVersionResolver, ByLanguageValue<LanguageVersionResolver>> KEY =
          ExtensionPointCacheKey.create("LanguageVersionResolver", LanguageOneToOne.build(LanguageVersionResolver.DEFAULT));

  @Nonnull
  @RequiredReadAction
  LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable PsiElement element);

  @Nonnull
  @RequiredReadAction
  LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile);
}
