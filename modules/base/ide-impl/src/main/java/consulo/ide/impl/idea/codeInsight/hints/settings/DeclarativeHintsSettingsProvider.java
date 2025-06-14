package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.DeclarativeInlayHintsSettings;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.impl.internal.inlay.setting.InlaySettingsProvider;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.project.Project;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ExtensionImpl
public class DeclarativeHintsSettingsProvider implements InlaySettingsProvider {
    @Override
    public List<InlayProviderSettingsModel> createModels(Project project, Language language) {
        List<DeclarativeInlayHintsProvider> providerDescriptions = Application.get().getExtensionList(DeclarativeInlayHintsProvider.class);
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        return providerDescriptions.stream()
            .filter(desc -> desc.getLanguage().equals(language))
            .map(desc -> {
                Boolean enabled = settings.isProviderEnabled(desc.getId());
                boolean isEnabled = enabled != null ? enabled : desc.isEnabledByDefault();
                return new DeclarativeHintsProviderSettingsModel(desc, isEnabled, language, project);
            })
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Language> getSupportedLanguages(Project project) {
        Set<Language> langs = Application.get().getExtensionList(DeclarativeInlayHintsProvider.class)
            .stream()
            .map(DeclarativeInlayHintsProvider::getLanguage)
            .collect(Collectors.toSet());
        return langs;
    }
}
