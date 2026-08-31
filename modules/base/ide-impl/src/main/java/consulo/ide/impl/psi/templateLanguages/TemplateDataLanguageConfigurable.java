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
import consulo.application.Application;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.ide.impl.idea.util.ui.tree.PerFileConfigurableBase;
import consulo.language.Language;
import consulo.language.impl.internal.template.TemplateDataLanguagePatterns;
import consulo.language.localize.LanguageLocalize;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
@ExtensionImpl
public class TemplateDataLanguageConfigurable extends PerFileConfigurableBase<Language> implements ProjectConfigurable {
    @Inject
    public TemplateDataLanguageConfigurable(Project project, TemplateDataLanguageMappings templateDataLanguageMappings) {
        super(project, templateDataLanguageMappings);
    }

    @Override
    public String getId() {
        return "template.data.language.configurable";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LanguageLocalize.templateDataLanguageConfigurable();
    }

    @Override
    public LocalizeValue getDescription() {
        return LanguageLocalize.dialogTemplateDataLanguageCaption(Application.get().getName());
    }

    @Override
    public LocalizeValue getMappingTitle() {
        return LanguageLocalize.templateDataLanguageConfigurableTreeTableTitle();
    }

    @Override
    public LocalizeValue getOverrideQuestion() {
        return LanguageLocalize.templateDataLanguageOverrideWarningText();
    }

    @Override
    public LocalizeValue getOverrideTitle() {
        return LanguageLocalize.templateDataLanguageOverrideWarningTitle();
    }

    @Override
    protected void renderValue(@Nullable Object target, Language language, ColoredTextContainer renderer) {
        renderer.append(language.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        renderer.setIcon(ObjectUtil.notNull(language.getAssociatedFileType(), UnknownFileType.INSTANCE).getIcon());
    }

    @Override
    protected void renderDefaultValue(Object target, ColoredTextContainer renderer) {
        Language language = TemplateDataLanguagePatterns.getInstance().getTemplateDataLanguageByFileName((VirtualFile) target);
        if (language == null) {
            return;
        }
        renderer.append(language.getDisplayName(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        renderer.setIcon(ObjectUtil.notNull(language.getAssociatedFileType(), UnknownFileType.INSTANCE).getIcon());
    }
}
