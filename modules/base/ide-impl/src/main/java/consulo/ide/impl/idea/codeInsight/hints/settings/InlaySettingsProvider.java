// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.Language;
import consulo.project.Project;

import java.util.Collection;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlaySettingsProvider {
    ExtensionPointName<InlaySettingsProvider> EXTENSION_POINT_NAME = ExtensionPointName.create(InlaySettingsProvider.class);

    public static List<InlaySettingsProvider> getExtensions() {
        return EXTENSION_POINT_NAME.getExtensionList();
    }

    /**
     * Returns list of hint provider models to be shown in Preferences | Editor | Inlay Hints.
     * Languages are expected to be only from getSupportedLanguages().
     * <p>
     * WARNING! Make sure you are not creating Swing components inside.
     * It is not guaranteed to run in EDT!
     */
    List<InlayProviderSettingsModel> createModels(Project project, Language language);

    /**
     * Returns list of supported languages. Every language must have a model in createModels().
     */
    Collection<Language> getSupportedLanguages(Project project);
}
