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
package consulo.ide.impl.psi.templateLanguages;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.ide.impl.idea.util.ui.tree.PerFileConfigurableBase;
import consulo.language.LangBundle;
import consulo.language.Language;
import consulo.language.impl.internal.template.TemplateDataLanguagePatterns;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.project.Project;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
@ExtensionImpl
public class TemplateDataLanguageConfigurable extends PerFileConfigurableBase<Language> implements ProjectConfigurable {
  @Inject
  public TemplateDataLanguageConfigurable(@Nonnull Project project, @Nonnull TemplateDataLanguageMappings templateDataLanguageMappings) {
    super(project, templateDataLanguageMappings);
  }

  @Nonnull
  @Override
  public String getId() {
    return "template.data.language.configurable";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return LangBundle.message("template.data.language.configurable");
  }

  @Override
  protected <S> Object getParameter(@Nonnull Key<S> key) {
    if (key == DESCRIPTION) return LangBundle.message("dialog.template.data.language.caption", ApplicationNamesInfo.getInstance().getFullProductName());
    if (key == MAPPING_TITLE) return LangBundle.message("template.data.language.configurable.tree.table.title");
    if (key == OVERRIDE_QUESTION) return LangBundle.message("template.data.language.override.warning.text");
    if (key == OVERRIDE_TITLE) return LangBundle.message("template.data.language.override.warning.title");
    return null;
  }

  @Override
  protected void renderValue(@Nullable Object target, @Nonnull Language language, @Nonnull ColoredTextContainer renderer) {
    renderer.append(language.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    renderer.setIcon(ObjectUtils.notNull(language.getAssociatedFileType(), UnknownFileType.INSTANCE).getIcon());
  }

  @Override
  protected void renderDefaultValue(Object target, @Nonnull ColoredTextContainer renderer) {
    Language language = TemplateDataLanguagePatterns.getInstance().getTemplateDataLanguageByFileName((VirtualFile)target);
    if (language == null) return;
    renderer.append(language.getDisplayName(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    renderer.setIcon(ObjectUtils.notNull(language.getAssociatedFileType(), UnknownFileType.INSTANCE).getIcon());
  }
}
