/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.impl.internal.template;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.Language;
import consulo.language.impl.util.LanguagePerFileMappings;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.module.content.FilePropertyPusher;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author peter
 */
@Singleton
@State(name = "TemplateDataLanguageMappings", storages = {@Storage("templateLanguages.xml")})
@ServiceImpl
public class TemplateDataLanguageMappingsImpl extends LanguagePerFileMappings<Language> implements TemplateDataLanguageMappings {
  @Inject
  public TemplateDataLanguageMappingsImpl(final Project project) {
    super(project);
  }

  @Override
  protected String serialize(final Language language) {
    return language.getID();
  }

  @Override
  @Nonnull
  public List<Language> getAvailableValues() {
    return TemplateDataLanguageMappings.getTemplateableLanguages();
  }

  @Nullable
  @Override
  public Language getMapping(@Nullable VirtualFile file) {
    Language t = getConfiguredMapping(file);
    return t == null || t == Language.ANY ? getDefaultMapping(file) : t;
  }

  @Override
  public Language getDefaultMapping(@Nullable VirtualFile file) {
    return getDefaultMappingForFile(file);
  }

  @Nullable
  public static Language getDefaultMappingForFile(@Nullable VirtualFile file) {
    return file == null ? null : TemplateDataLanguagePatterns.getInstance().getTemplateDataLanguageByFileName(file);
  }

  private final FilePropertyPusher<Language> myPropertyPusher = new TemplateDataLanguagePusher();

  @Nonnull
  @Override
  protected FilePropertyPusher<Language> getFilePropertyPusher() {
    return myPropertyPusher;
  }
}
