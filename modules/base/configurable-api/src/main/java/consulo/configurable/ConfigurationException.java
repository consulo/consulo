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
import org.jspecify.annotations.Nullable;

public class ConfigurationException extends Exception {
    public static final LocalizeValue DEFAULT_TITLE = ConfigurableLocalize.cannotSaveSettingsDefaultDialogTitle();
    
    private LocalizeValue myTitle = DEFAULT_TITLE;
    private @Nullable Runnable myQuickFix = null;

    public ConfigurationException(LocalizeValue message) {
        this(message, LocalizeValue.empty());
    }

    public ConfigurationException(LocalizeValue message, LocalizeValue titleValue) {
        super(message.get());
        myTitle = titleValue.orIfEmpty(DEFAULT_TITLE);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfigurationException(String message) {
        this(message, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfigurationException(String message, @Nullable String title) {
        this(LocalizeValue.ofNullable(message), LocalizeValue.ofNullable(title));
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public void setQuickFix(Runnable quickFix) {
        myQuickFix = quickFix;
    }

    public @Nullable Runnable getQuickFix() {
        return myQuickFix;
    }
}