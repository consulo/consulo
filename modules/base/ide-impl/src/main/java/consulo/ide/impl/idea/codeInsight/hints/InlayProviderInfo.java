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
import consulo.language.editor.inlay.InlayOptionInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Set;

/**
 * Information about an inlay hints provider.
 */
public final class InlayProviderInfo {
    private final InlayHintsProvider provider;
    private final String providerId;
    private final Set<InlayOptionInfo> options;
    private final boolean isEnabledByDefault;
    private final LocalizeValue providerName;

    public InlayProviderInfo(InlayHintsProvider provider,
                             String providerId,
                             Set<InlayOptionInfo> options,
                             boolean isEnabledByDefault,
                             LocalizeValue providerName) {
        this.provider = provider;
        this.providerId = providerId;
        this.options = options;
        this.isEnabledByDefault = isEnabledByDefault;
        this.providerName = providerName;
    }

    public InlayHintsProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public Set<InlayOptionInfo> getOptions() {
        return options;
    }

    public boolean isEnabledByDefault() {
        return isEnabledByDefault;
    }

    @Nonnull
    public LocalizeValue getProviderName() {
        return providerName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InlayProviderInfo)) return false;
        InlayProviderInfo that = (InlayProviderInfo) other;
        return isEnabledByDefault == that.isEnabledByDefault &&
            Objects.equals(provider, that.provider) &&
            Objects.equals(providerId, that.providerId) &&
            Objects.equals(options, that.options) &&
            Objects.equals(providerName, that.providerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, providerId, options, isEnabledByDefault, providerName);
    }
}
