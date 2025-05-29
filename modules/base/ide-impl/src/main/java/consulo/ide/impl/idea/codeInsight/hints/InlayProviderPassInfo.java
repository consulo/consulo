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
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.language.editor.inlay.InlayHintsProvider;

import java.util.Map;

/**
 * @param optionToEnabled exhaustive set of options for a given provider
 */
public final class InlayProviderPassInfo {
    private final InlayHintsProvider provider;
    private final String providerId;
    private final Map<String, Boolean> optionToEnabled;

    public InlayProviderPassInfo(InlayHintsProvider provider,
                                 String providerId,
                                 Map<String, Boolean> optionToEnabled) {
        this.provider = provider;
        this.providerId = providerId;
        this.optionToEnabled = optionToEnabled;
    }

    public InlayHintsProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public Map<String, Boolean> getOptionToEnabled() {
        return optionToEnabled;
    }

    @Override
    public String toString() {
        return "InlayProviderPassInfo(providerId='" + providerId + "', provider=" + provider + ")";
    }
}
