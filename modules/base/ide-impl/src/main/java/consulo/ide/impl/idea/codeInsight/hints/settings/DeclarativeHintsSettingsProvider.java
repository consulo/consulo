// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.impl.internal.inlay.setting.InlaySettingsProvider;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactoryUtil;
import consulo.language.editor.inlay.DeclarativeInlayHintsSettings;
import consulo.language.editor.inlay.InlayProviderInfo;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ExtensionImpl
public class DeclarativeHintsSettingsProvider implements InlaySettingsProvider {
    @Override
    public List<InlayProviderSettingsModel> createModels(Project project, Language language) {
        List<InlayProviderInfo> providerInfos = DeclarativeInlayHintsProviderFactoryUtil.getProvidersForLanguage(language);
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        List<InlayProviderSettingsModel> models = new ArrayList<>(providerInfos.size());
        for (InlayProviderInfo info : providerInfos) {
            if (info.provider().getLanguage() != language) {
                continue;
            }
            boolean isEnabled = settings.isProviderEnabled(info.providerId(), info.isEnabledByDefault());
            models.add(new DeclarativeHintsProviderSettingsModel(info, isEnabled, language, project));
        }
        return models;
    }

    @Override
    public Collection<Language> getSupportedLanguages(Project project) {
        return DeclarativeInlayHintsProviderFactoryUtil.getSupportedLanguages();
    }
}
