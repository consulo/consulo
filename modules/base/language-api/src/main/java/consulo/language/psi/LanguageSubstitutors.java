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
package consulo.language.psi;

import consulo.application.Application;
import consulo.container.plugin.PluginIds;
import consulo.document.util.FileContentUtilCore;
import consulo.language.Language;
import consulo.language.LanguageExtension;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author peter
 */
public final class LanguageSubstitutors extends LanguageExtension<LanguageSubstitutor> {
  public static final LanguageSubstitutors INSTANCE = new LanguageSubstitutors();
  private static final Logger LOG = Logger.getInstance(LanguageSubstitutors.class);
  private static final Key<Language> SUBSTITUTED_LANG_KEY = Key.create("SUBSTITUTED_LANG_KEY");
  private static final Key<Boolean> REPARSING_SCHEDULED = Key.create("REPARSING_SCHEDULED");

  private LanguageSubstitutors() {
    super(PluginIds.CONSULO_BASE + ".lang.substitutor");
  }

  @Nonnull
  public Language substituteLanguage(@Nonnull Language lang, @Nonnull VirtualFile file, @Nonnull Project project) {
    for (LanguageSubstitutor substitutor : forKey(lang)) {
      Language language = substitutor.getLanguage(file, project);
      if (language != null) {
        processLanguageSubstitution(file, lang, language);
        return language;
      }
    }
    return lang;
  }

  private static void processLanguageSubstitution(@Nonnull final VirtualFile file,
                                                  @Nonnull Language originalLang,
                                                  @Nonnull final Language substitutedLang) {
    if (file instanceof VirtualFileWindow) {
      // Injected files are created with substituted language, no need to reparse:
      //   com.intellij.psi.impl.source.tree.injected.MultiHostRegistrarImpl#doneInjecting
      return;
    }
    Language prevSubstitutedLang = SUBSTITUTED_LANG_KEY.get(file);
    final Language prevLang = ObjectUtil.notNull(prevSubstitutedLang, originalLang);
    if (!prevLang.is(substitutedLang)) {
      if (file.replace(SUBSTITUTED_LANG_KEY, prevSubstitutedLang, substitutedLang)) {
        if (prevSubstitutedLang == null) {
          return; // no need to reparse for the first language substitution
        }

        Application application = Application.get();
        if (application.isUnitTestMode()) {
          return;
        }
        file.putUserData(REPARSING_SCHEDULED, true);
        application.invokeLater(() -> {
          if (file.replace(REPARSING_SCHEDULED, true, null)) {
            LOG.info("Reparsing " + file.getPath() + " because of language substitution " +
                     prevLang.getID() + "->" + substitutedLang.getID());
            FileContentUtilCore.reparseFiles(file);
          }
        }, application.getDefaultModalityState());
      }
    }
  }

  public static void cancelReparsing(@Nonnull VirtualFile file) {
    REPARSING_SCHEDULED.set(file, null);
  }
}
