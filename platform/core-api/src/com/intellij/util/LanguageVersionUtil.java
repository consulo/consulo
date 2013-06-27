/*
 * Copyright 2013 Consulo.org
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
package com.intellij.util;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.LanguageVersionResolvers;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14:09/27.06.13
 */
public class LanguageVersionUtil {
  public static LanguageVersion findLanguageVersion(@NotNull Language language,
                                                    @Nullable Project project,
                                                    @Nullable VirtualFile virtualFile) {

    final LanguageVersion languageVersion = LanguageVersion.KEY.get(virtualFile);
    if(languageVersion != null) {
      return languageVersion;
    }
    else {
      return LanguageVersionResolvers.INSTANCE.forLanguage(language).getLanguageVersion(language, project, virtualFile);
    }
  }
}
