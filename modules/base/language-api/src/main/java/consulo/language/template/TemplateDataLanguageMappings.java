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
package consulo.language.template;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.DependentLanguage;
import consulo.language.InjectableLanguage;
import consulo.language.Language;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.util.PerFileMappingsEx;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 13-Aug-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface TemplateDataLanguageMappings extends PerFileMappingsEx<Language> {
  @Nonnull
  public static TemplateDataLanguageMappings getInstance(@Nonnull Project project) {
    return project.getInstance(TemplateDataLanguageMappings.class);
  }

  @Nonnull
  public static List<Language> getTemplateableLanguages() {
    return ContainerUtil.findAll(Language.getRegisteredLanguages(), new Predicate<>() {
      @Override
      public boolean test(final Language language) {
        if (language == Language.ANY) return false;
        if (language instanceof TemplateLanguage || language instanceof DependentLanguage || language instanceof InjectableLanguage) return false;
        if (language.getBaseLanguage() != null) return test(language.getBaseLanguage());
        return true;
      }
    });
  }
}
