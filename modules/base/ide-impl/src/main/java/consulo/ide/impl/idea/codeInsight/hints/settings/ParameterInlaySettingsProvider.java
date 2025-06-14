// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.impl.internal.inlay.setting.InlaySettingsProvider;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.extension.LanguageExtension;
import consulo.project.Project;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class ParameterInlaySettingsProvider implements InlaySettingsProvider {
    @Override
    public List<InlayProviderSettingsModel> createModels(Project project, Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider != null) {
            return Collections.singletonList(new ParameterInlayProviderSettingsModel(provider, language));
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<Language> getSupportedLanguages(Project project) {
        List<Language> languages = project
            .getApplication()
            .getExtensionPoint(InlayParameterHintsProvider.class)
            .collectMapped(LanguageExtension::getLanguage);
        return languages;
    }
}
