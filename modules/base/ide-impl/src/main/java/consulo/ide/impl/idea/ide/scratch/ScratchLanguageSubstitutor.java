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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.LanguageSubstitutor;
import consulo.language.psi.LanguageSubstitutors;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl(order = "first")
public class ScratchLanguageSubstitutor extends LanguageSubstitutor {
  @Nullable
  @Override
  public Language getLanguage(@Nonnull VirtualFile file, @Nonnull Project project) {
    return substituteLanguage(project, file);
  }

  @Nullable
  public static Language substituteLanguage(@Nonnull Project project, @Nonnull VirtualFile file) {
    RootType rootType = ScratchFileService.getInstance().getRootType(file);
    if (rootType == null) return null;
    Language language = rootType.substituteLanguage(project, file);
    Language adjusted = language != null ? language : ScratchFileServiceImpl.getLanguageByFileName(file);
    Language result = adjusted != null && adjusted != PlainTextLanguage.INSTANCE ? LanguageSubstitutors.substituteLanguage(adjusted, file, project) : adjusted;
    return result == Language.ANY ? null : result;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
