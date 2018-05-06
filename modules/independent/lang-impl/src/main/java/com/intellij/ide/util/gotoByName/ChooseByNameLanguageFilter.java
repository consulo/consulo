/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ide.util.gotoByName;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class ChooseByNameLanguageFilter extends ChooseByNameFilter<Language> {
  public ChooseByNameLanguageFilter(@Nonnull ChooseByNamePopup popup,
                                    @Nonnull FilteringGotoByModel<Language> languageFilteringGotoByModel,
                                    @Nonnull ChooseByNameFilterConfiguration<Language> languageChooseByNameFilterConfiguration,
                                    @Nonnull Project project) {
    super(popup, languageFilteringGotoByModel, languageChooseByNameFilterConfiguration, project);
  }

  @Override
  protected String textForFilterValue(@Nonnull Language value) {
    return value.getDisplayName();
  }

  @Nullable
  @Override
  protected Image iconForFilterValue(@Nonnull Language value) {
    final LanguageFileType fileType = value.getAssociatedFileType();
    return fileType != null ? fileType.getIcon() : null;
  }

  @Nonnull
  @Override
  protected Collection<Language> getAllFilterValues() {
    final Collection<Language> registeredLanguages = Language.getRegisteredLanguages();
    List<Language> accepted = new ArrayList<Language>();
    for (Language language : registeredLanguages) {
      if (language != Language.ANY && !(language instanceof DependentLanguage)) {
        accepted.add(language);
      }
    }
    Collections.sort(accepted, LanguageUtil.LANGUAGE_COMPARATOR);
    return accepted;
  }
}
