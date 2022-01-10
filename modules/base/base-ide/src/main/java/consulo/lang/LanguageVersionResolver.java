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
package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18:05/30.05.13
 */
public interface LanguageVersionResolver {
  LanguageVersionResolver DEFAULT = new LanguageVersionResolver() {
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
  @RequiredReadAction
  LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable PsiElement element);

  @Nonnull
  @RequiredReadAction
  LanguageVersion getLanguageVersion(@Nonnull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile);
}
