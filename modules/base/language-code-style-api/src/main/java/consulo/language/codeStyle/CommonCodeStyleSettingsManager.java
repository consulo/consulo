/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.application.Application;
import consulo.application.util.RecursionManager;
import consulo.language.Language;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.interner.JDOMInterner;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Content;
import org.jdom.Element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages common code style settings for every language using them.
 *
 * @author Rustam Vishnyakov
 */
class CommonCodeStyleSettingsManager {
    private volatile Map<Language, CommonCodeStyleSettings> myCommonSettingsMap;
    private volatile Map<String, Content> myUnknownSettingsMap;

    @Nonnull
    private final CodeStyleSettings myParentSettings;

    static final String COMMON_SETTINGS_TAG = "codeStyleSettings";
    private static final String LANGUAGE_ATTR = "language";

    private static final Logger LOG = Logger.getInstance(CommonCodeStyleSettingsManager.class);

    private static class DefaultsHolder {
        private final static CommonCodeStyleSettings SETTINGS = new CommonCodeStyleSettings(Language.ANY);

        static {
            SETTINGS.setRootSettings(CodeStyleSettings.getDefaults());
        }
    }

    CommonCodeStyleSettingsManager(@Nonnull CodeStyleSettings parentSettings) {
        myParentSettings = parentSettings;
    }

    @Nullable
    CommonCodeStyleSettings getCommonSettings(@Nullable Language lang) {
        Map<Language, CommonCodeStyleSettings> commonSettingsMap = getCommonSettingsMap();
        Language baseLang = ObjectUtil.notNull(lang, Language.ANY);
        while (baseLang != null) {
            CommonCodeStyleSettings settings = commonSettingsMap.get(baseLang);
            if (settings != null) {
                return settings;
            }
            baseLang = baseLang.getBaseLanguage();
        }
        return null;
    }

    CommonCodeStyleSettings getDefaults() {
        return DefaultsHolder.SETTINGS;
    }

    @Nonnull
    private Map<Language, CommonCodeStyleSettings> getCommonSettingsMap() {
        Map<Language, CommonCodeStyleSettings> commonSettingsMap = myCommonSettingsMap;
        if (commonSettingsMap == null) {
            synchronized (this) {
                commonSettingsMap = myCommonSettingsMap;
                if (commonSettingsMap == null) {
                    commonSettingsMap = initCommonSettingsMap();
                    initNonReadSettings();
                }
            }
        }
        return commonSettingsMap;
    }

    /**
     * Get common code style settings by language name. {@code getCommonSettings(Language)} is a preferred method but
     * sometimes (for example, in plug-ins which do not depend on a specific language support) language settings can be
     * obtained by name.
     *
     * @param langName The display name of the language whose settings must be returned.
     * @return Common code style settings for the given language or a new instance with default values if not found.
     */
    @Nonnull
    public CommonCodeStyleSettings getCommonSettings(@Nonnull String langName) {
        Map<Language, CommonCodeStyleSettings> map = getCommonSettingsMap();
        for (Map.Entry<Language, CommonCodeStyleSettings> entry : map.entrySet()) {
            if (langName.equals(entry.getKey().getDisplayName())) {
                return entry.getValue();
            }
        }
        return new CommonCodeStyleSettings(Language.ANY);
    }

    private void initNonReadSettings() {
        Application.get().getExtensionPoint(LanguageCodeStyleSettingsProvider.class).forEach(provider -> {
            Language target = provider.getLanguage();
            if (!myCommonSettingsMap.containsKey(target)) {
                CommonCodeStyleSettings initialSettings = safelyGetDefaults(provider);
                if (initialSettings != null) {
                    init(initialSettings, target);
                }
            }
        });
    }

    private void init(@Nonnull CommonCodeStyleSettings initialSettings, @Nonnull Language target) {
        initialSettings.setRootSettings(myParentSettings);
        registerCommonSettings(target, initialSettings);
    }

    private Map<Language, CommonCodeStyleSettings> initCommonSettingsMap() {
        Map<Language, CommonCodeStyleSettings> map = new LinkedHashMap<>();
        myCommonSettingsMap = map;
        myUnknownSettingsMap = new LinkedHashMap<>();
        return map;
    }

    private void registerCommonSettings(@Nonnull Language lang, @Nonnull CommonCodeStyleSettings settings) {
        synchronized (this) {
            if (!myCommonSettingsMap.containsKey(lang)) {
                myCommonSettingsMap.put(lang, settings);
                settings.getRootSettings(); // check not null
            }
        }
    }

    @Nonnull
    public CommonCodeStyleSettingsManager clone(@Nonnull CodeStyleSettings parentSettings) {
        synchronized (this) {
            CommonCodeStyleSettingsManager settingsManager = new CommonCodeStyleSettingsManager(parentSettings);
            if (myCommonSettingsMap != null && !myCommonSettingsMap.isEmpty()) {
                settingsManager.initCommonSettingsMap();
                for (Map.Entry<Language, CommonCodeStyleSettings> entry : myCommonSettingsMap.entrySet()) {
                    CommonCodeStyleSettings clonedSettings = entry.getValue().clone(parentSettings);
                    settingsManager.registerCommonSettings(entry.getKey(), clonedSettings);
                }
                // no need to clone, myUnknownSettingsMap contains immutable elements
                settingsManager.myUnknownSettingsMap.putAll(myUnknownSettingsMap);
            }
            return settingsManager;
        }
    }

    public void readExternal(@Nonnull Element element) throws InvalidDataException {
        synchronized (this) {
            initCommonSettingsMap();
            for (Element commonSettingsElement : element.getChildren(COMMON_SETTINGS_TAG)) {
                String languageId = commonSettingsElement.getAttributeValue(LANGUAGE_ATTR);
                if (!StringUtil.isEmpty(languageId)) {
                    Language target = Language.findLanguageByID(languageId);
                    boolean isKnownLanguage = target != null;
                    if (isKnownLanguage) {
                        LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(target);
                        if (provider != null) {
                            CommonCodeStyleSettings settings = safelyGetDefaults(provider);
                            if (settings != null) {
                                settings.readExternal(commonSettingsElement);
                                init(settings, target);
                            }
                        }
                        else {
                            isKnownLanguage = false;
                        }
                    }
                    if (!isKnownLanguage) {
                        myUnknownSettingsMap.put(languageId, JDOMInterner.internElement(commonSettingsElement));
                    }
                }
            }
            initNonReadSettings();
        }
    }

    private static CommonCodeStyleSettings safelyGetDefaults(LanguageCodeStyleSettingsProvider provider) {
        SimpleReference<CommonCodeStyleSettings> defaultSettingsRef = RecursionManager.doPreventingRecursion(
            provider,
            true,
            () -> SimpleReference.create(provider.getDefaultCommonSettings())
        );
        if (defaultSettingsRef == null) {
            LOG.error(provider.getClass().getCanonicalName() + ".getDefaultCommonSettings() recursively creates root settings.");
            return null;
        }
        else {
            CommonCodeStyleSettings defaultSettings = defaultSettingsRef.get();
            if (defaultSettings instanceof CodeStyleSettings) {
                LOG.error(
                    provider.getClass().getName() + ".getDefaultCommonSettings() creates root CodeStyleSettings " +
                        "instead of CommonCodeStyleSettings"
                );
            }
            return defaultSettings;
        }
    }

    public void writeExternal(@Nonnull Element element) throws WriteExternalException {
        synchronized (this) {
            if (myCommonSettingsMap == null) {
                return;
            }

            Map<String, Language> idToLang = new HashMap<>();
            for (Language language : myCommonSettingsMap.keySet()) {
                idToLang.put(language.getID(), language);
            }

            String[] languages = ArrayUtil.toStringArray(ContainerUtil.union(myUnknownSettingsMap.keySet(), idToLang.keySet()));
            Arrays.sort(languages);
            for (String id : languages) {
                Language language = idToLang.get(id);
                if (language != null) {
                    CommonCodeStyleSettings commonSettings = myCommonSettingsMap.get(language);
                    Element commonSettingsElement = new Element(COMMON_SETTINGS_TAG);
                    commonSettings.writeExternal(commonSettingsElement);
                    commonSettingsElement.setAttribute(LANGUAGE_ATTR, language.getID());
                    if (!commonSettingsElement.getChildren().isEmpty()) {
                        element.addContent(commonSettingsElement);
                    }
                }
                else {
                    Content unknown = myUnknownSettingsMap.get(id);
                    if (unknown != null) {
                        element.addContent(unknown.clone());
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals(Object obj) {
        if (obj instanceof CommonCodeStyleSettingsManager other) {
            if (getCommonSettingsMap().size() != other.getCommonSettingsMap().size()
                || myUnknownSettingsMap.size() != other.myUnknownSettingsMap.size()) {
                return false;
            }
            for (Language language : myCommonSettingsMap.keySet()) {
                CommonCodeStyleSettings theseSettings = myCommonSettingsMap.get(language);
                CommonCodeStyleSettings otherSettings = other.getCommonSettings(language);
                if (!theseSettings.equals(otherSettings)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
