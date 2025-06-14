/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.Language;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-05-28
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class InlayHintsSettings {
    public static InlayHintsSettings getInstance() {
        return Application.get().getInstance(InlayHintsSettings.class);
    }

    public boolean hintsShouldBeShown(Language language) {
        return true;
    }

    public void saveLastViewedProviderId(String providerId) {
    }

    public String getLastViewedProviderId() {
        return null;
    }

    public boolean hintsEnabled(Language language) {
        return true;
    }

    public void setHintsEnabledForLanguage(Language language, boolean value) {
    }

    public void setEnabledGlobally(boolean enabledGlobally) {

    }

    public boolean hintsEnabledGlobally() {
        return true;
    }
}
