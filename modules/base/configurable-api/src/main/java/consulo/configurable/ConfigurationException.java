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
package consulo.configurable;

import consulo.annotation.DeprecationInfo;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public class ConfigurationException extends Exception {
    public static final LocalizeValue DEFAULT_TITLE = ConfigurableLocalize.cannotSaveSettingsDefaultDialogTitle();
    @Nonnull
    private LocalizeValue myTitle = DEFAULT_TITLE;
    private Runnable myQuickFix;

    public ConfigurationException(@Nonnull LocalizeValue message) {
        this(message, LocalizeValue.empty());
    }

    public ConfigurationException(@Nonnull LocalizeValue message, @Nonnull LocalizeValue title) {
        super(message.get());
        myTitle = title;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfigurationException(String message) {
        this(message, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfigurationException(String message, String title) {
        this(LocalizeValue.ofNullable(message), LocalizeValue.ofNullable(title));
    }

    //TODO: rename into getTitle after deprecation removal
    public LocalizeValue getTitleValue() {
        return myTitle;
    }

    @Deprecated
    @DeprecationInfo("Use getTitleValue()")
    public String getTitle() {
        LocalizeValue titleValue = getTitleValue();
        return titleValue == LocalizeValue.empty() ? null : titleValue.get();
    }

    public void setQuickFix(Runnable quickFix) {
        myQuickFix = quickFix;
    }

    public Runnable getQuickFix() {
        return myQuickFix;
    }
}