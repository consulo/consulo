package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.extension.LanguageExtension;
import consulo.project.Project;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * Provider of a custom settings page for declarative inlay hints.
 * Responsible for UI component creation. Does not apply changes immediately—only in persistSettings().
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DeclarativeInlayHintsCustomSettingsProvider<T> extends LanguageExtension {
    @Nullable
    static DeclarativeInlayHintsCustomSettingsProvider<?> getCustomSettingsProvider(String providerId, Language language) {
        for (DeclarativeInlayHintsCustomSettingsProvider extension : Application.get().getExtensionPoint(DeclarativeInlayHintsCustomSettingsProvider.class)) {
            Language extLang = extension.getLanguage();
            if (providerId.equals(extension.getProviderId())
                && language.isKindOf(extLang)) {
                return extension;
            }
        }
        return null;
    }

    String getProviderId();

    /**
     * Creates the settings UI component.
     */
    JComponent createComponent(Project project, Language language);

    /**
     * Checks whether the provided settings differ from the persisted ones.
     */
    boolean isDifferentFrom(Project project, T settings);

    /**
     * Returns a copy of the current settings.
     */
    T getSettingsCopy();

    /**
     * Applies settings without persisting them.
     */
    void putSettings(Project project, T settings, Language language);

    /**
     * Persists settings changes.
     */
    void persistSettings(Project project, T settings, Language language);
}
