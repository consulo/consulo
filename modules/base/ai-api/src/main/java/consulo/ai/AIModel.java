/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ai;

import consulo.localize.LocalizeValue;

import java.util.Set;

/**
 * One model offered by an {@link AIProviderType}. Models belong to the type, not to a configured
 * instance, so they are known before any credentials are entered.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public interface AIModel {
    /**
     * Type-scoped identifier, for example {@code claude-opus-4-7}. Stable across releases, since it is
     * what gets persisted in settings.
     */
    String getId();

    String getProviderTypeId();

    LocalizeValue getDisplayName();

    /**
     * Total context window in tokens, or {@code -1} when the vendor does not publish one.
     */
    int getContextWindow();

    Set<AIModelCapability> getCapabilities();

    default boolean hasCapability(AIModelCapability capability) {
        return getCapabilities().contains(capability);
    }
}
