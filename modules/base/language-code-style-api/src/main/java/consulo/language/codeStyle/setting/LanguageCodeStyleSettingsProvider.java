// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.configurable.Configurable;
import consulo.language.Language;
import consulo.language.codeStyle.*;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class and extension point for common code style settings for a specific language.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class LanguageCodeStyleSettingsProvider implements CodeStyleSettingsBase {
    public static final ExtensionPointName<LanguageCodeStyleSettingsProvider> EP_NAME =
        ExtensionPointName.create(LanguageCodeStyleSettingsProvider.class);

    public enum SettingsType {
        BLANK_LINES_SETTINGS,
        SPACING_SETTINGS,
        WRAPPING_AND_BRACES_SETTINGS,
        INDENT_SETTINGS,
        COMMENTER_SETTINGS,
        LANGUAGE_SPECIFIC
    }

    @Nullable
    public abstract String getCodeSample(@Nonnull SettingsType settingsType);

    public int getRightMargin(@Nonnull SettingsType settingsType) {
        return settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS ? 30 : -1;
    }

    public void customizeSettings(@Nonnull CodeStyleSettingsCustomizable consumer, @Nonnull SettingsType settingsType) {
    }

    /**
     * Override this method if file extension to be used with samples is different from the one returned by associated file type.
     *
     * @return The file extension for samples (null by default).
     */
    @Nullable
    public String getFileExt() {
        return null;
    }

    /**
     * Override this method if language name shown in preview tab must be different from the name returned by Language class itself.
     *
     * @return The language name to show in preview tab (null by default).
     */
    @Nullable
    public String getLanguageName() {
        return null;
    }

    /**
     * Allows to customize PSI file creation for a language settings preview panel.
     * <p>
     * <b>IMPORTANT</b>: The created file must be a non-physical one with PSI events disabled. For more information see
     * {@link PsiFileFactory#createFileFromText(String, Language, CharSequence, boolean, boolean)} where
     * {@code eventSystemEnabled} parameter must be {@code false}
     *
     * @param project current project
     * @param text    code sample to demonstrate formatting settings (see {@link #getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType)}
     * @return a PSI file instance with given text, or null for default implementation using provider's language.
     */
    @Nullable
    public PsiFile createFileFromText(final Project project, final String text) {
        return null;
    }

    /**
     * Creates an instance of {@code CommonCodeStyleSettings} and sets initial default values for those
     * settings which differ from the original.
     *
     * @return Created instance of {@code CommonCodeStyleSettings} or null if associated language doesn't
     * use its own language-specific common settings (the settings are shared with other languages).
     */
    @Nullable
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        return new CommonCodeStyleSettings(getLanguage());
    }

    /**
     * @deprecated use PredefinedCodeStyle extension point instead
     */
    @Nonnull
    @Deprecated
    public PredefinedCodeStyle[] getPredefinedCodeStyles() {
        return PredefinedCodeStyle.EMPTY_ARRAY;
    }

    @Nonnull
    public static Language[] getLanguagesWithCodeStyleSettings() {
        final ArrayList<Language> languages = new ArrayList<>();
        for (LanguageCodeStyleSettingsProvider provider : EP_NAME.getExtensionList()) {
            languages.add(provider.getLanguage());
        }
        return languages.toArray(new Language[0]);
    }

    @Nullable
    public static String getCodeSample(Language lang, @Nonnull SettingsType settingsType) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(lang);
        return provider != null ? provider.getCodeSample(settingsType) : null;
    }

    public static int getRightMargin(Language lang, @Nonnull SettingsType settingsType) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(lang);
        return provider != null ? provider.getRightMargin(settingsType) : -1;
    }

    /**
     * Searches a provider for a specific language or its base language.
     *
     * @param language The original language.
     * @return Found provider or {@code null} if it doesn't exist neither for the language itself nor for any of its base languages.
     */
    @Nullable
    public static LanguageCodeStyleSettingsProvider findUsingBaseLanguage(@Nonnull final Language language) {
        for (Language currLang = language; currLang != null; currLang = currLang.getBaseLanguage()) {
            LanguageCodeStyleSettingsProvider curr = forLanguage(currLang);
            if (curr != null) {
                return curr;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public abstract Language getLanguage();

    @Nullable
    public static Language getLanguage(String langName) {
        for (LanguageCodeStyleSettingsProvider provider : EP_NAME.getExtensionList()) {
            String name = provider.getLanguageName();
            if (name == null) {
                name = provider.getLanguage().getDisplayName();
            }
            if (langName.equals(name)) {
                return provider.getLanguage();
            }
        }
        return null;
    }

    @Nullable
    public static CommonCodeStyleSettings getDefaultCommonSettings(Language lang) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(lang);
        return provider != null ? provider.getDefaultCommonSettings() : null;
    }

    @Nullable
    public static String getFileExt(Language lang) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(lang);
        return provider != null ? provider.getFileExt() : null;
    }

    /**
     * Returns a language name to be shown in UI. Used to overwrite language's display name by another name to
     * be shown in UI.
     *
     * @param lang The language whose display name must be return.
     * @return Alternative UI name defined by provider.getLanguageName() method or (if the method returns null)
     * language's own display name.
     */
    @Nonnull
    public static String getLanguageName(Language lang) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(lang);
        String providerLangName = provider != null ? provider.getLanguageName() : null;
        return providerLangName != null ? providerLangName : lang.getDisplayName();
    }

    @Nullable
    public static PsiFile createFileFromText(final Language language, final Project project, final String text) {
        final LanguageCodeStyleSettingsProvider provider = forLanguage(language);
        return provider != null ? provider.createFileFromText(project, text) : null;
    }

    @Nullable
    public static LanguageCodeStyleSettingsProvider forLanguage(final Language language) {
        for (LanguageCodeStyleSettingsProvider provider : EP_NAME.getExtensionList()) {
            if (provider.getLanguage().equals(language)) {
                return provider;
            }
        }
        return null;
    }

    @Nullable
    public IndentOptionsEditor getIndentOptionsEditor() {
        return null;
    }

    public Set<String> getSupportedFields() {
        return new SupportedFieldCollector().collectFields();
    }

    public Set<String> getSupportedFields(SettingsType type) {
        return new SupportedFieldCollector().collectFields(type);
    }

    private final class SupportedFieldCollector implements CodeStyleSettingsCustomizable {
        private final Set<String> myCollectedFields = new HashSet<>();
        private SettingsType myCurrSettingsType;

        public Set<String> collectFields() {
            for (SettingsType settingsType : SettingsType.values()) {
                myCurrSettingsType = settingsType;
                customizeSettings(this, settingsType);
            }
            return myCollectedFields;
        }

        public Set<String> collectFields(SettingsType type) {
            myCurrSettingsType = type;
            customizeSettings(this, type);
            return myCollectedFields;
        }

        @Override
        public void showAllStandardOptions() {
            switch (myCurrSettingsType) {
                case BLANK_LINES_SETTINGS:
                    for (BlankLinesOption blankLinesOption : BlankLinesOption.values()) {
                        myCollectedFields.add(blankLinesOption.name());
                    }
                    break;
                case SPACING_SETTINGS:
                    for (SpacingOption spacingOption : SpacingOption.values()) {
                        myCollectedFields.add(spacingOption.name());
                    }
                    break;
                case WRAPPING_AND_BRACES_SETTINGS:
                    for (WrappingOrBraceOption wrappingOrBraceOption : WrappingOrBraceOption.values()) {
                        myCollectedFields.add(wrappingOrBraceOption.name());
                    }
                    break;
                case COMMENTER_SETTINGS:
                    for (CommenterOption commenterOption : CommenterOption.values()) {
                        myCollectedFields.add(commenterOption.name());
                    }
                    break;
                default:
                    // ignore
            }
        }

        @Override
        public void showStandardOptions(String... optionNames) {
            ContainerUtil.addAll(myCollectedFields, optionNames);
        }

        @Override
        public void showCustomOption(
            Class<? extends CustomCodeStyleSettings> settingsClass,
            String fieldName,
            String title,
            @Nullable String groupName,
            Object... options
        ) {
            myCollectedFields.add(fieldName);
        }

        @Override
        public void showCustomOption(
            Class<? extends CustomCodeStyleSettings> settingsClass,
            String fieldName,
            String title,
            @Nullable String groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFieldName,
            Object... options
        ) {
            myCollectedFields.add(fieldName);
        }
    }

    /**
     * Returns a wrapper around language's own code documentation comment settings from the given {@code rootSettings}.
     *
     * @param rootSettings Root code style setting to retrieve doc comment settings from.
     * @return {@code DocCommentSettings} wrapper object object which allows to retrieve and modify language's own
     * settings related to doc comment. The object is used then by common platform doc comment handling algorithms.
     */
    @Nonnull
    public DocCommentSettings getDocCommentSettings(@Nonnull CodeStyleSettings rootSettings) {
        return DocCommentSettings.DEFAULTS;
    }

    @Nullable
    public CodeStyleBean createBean() {
        return null;
    }

    @Nonnull
    @Override
    public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings modelSettings) {
        throw new RuntimeException(
            this.getClass().getCanonicalName() + " for language #" + getLanguage().getID() +
                " doesn't implement createSettingsPage()"
        );
    }


    /**
     * @return A list of providers implementing {@link #createSettingsPage(CodeStyleSettings, CodeStyleSettings)}
     */
    public static List<LanguageCodeStyleSettingsProvider> getSettingsPagesProviders() {
        return EP_NAME.getExtensionList();
    }
}
