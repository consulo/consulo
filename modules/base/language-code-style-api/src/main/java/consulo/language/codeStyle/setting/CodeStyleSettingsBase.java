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
package consulo.language.codeStyle.setting;

import consulo.configurable.Configurable;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public interface CodeStyleSettingsBase {
    @Nullable
    default CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return null;
    }

    @Nonnull
    public abstract Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings);

    /**
     * Returns the name of the configurable page without creating a Configurable instance.
     *
     * @return the display name of the configurable page.
     */
    @Nonnull
    default LocalizeValue getConfigurableDisplayName() {
        Language lang = getLanguage();
        return lang == null ? LocalizeValue.empty() : lang.getDisplayName();
    }

    default boolean hasSettingsPage() {
        return true;
    }

    /**
     * Specifies a language this provider applies to. If the language is not null, its display name will
     * be used as a configurable name by default if <code>getConfigurableDisplayName()</code> is not
     * overridden.
     *
     * @return null by default.
     */
    @Nullable
    default Language getLanguage() {
        return null;
    }
}
