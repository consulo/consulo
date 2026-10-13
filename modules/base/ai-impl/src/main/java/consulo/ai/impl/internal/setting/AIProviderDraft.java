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
package consulo.ai.impl.internal.setting;

import consulo.ai.AIProviderType;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An account as the settings page edits it - nothing reaches the provider table before apply.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class AIProviderDraft {
    /**
     * The name the account is stored under, null for an account added on the page.
     */
    @Nullable String originalName;
    String name;
    final AIProviderType type;

    /**
     * Null until the stored key was read or the key was typed.
     */
    @Nullable String apiKey;
    boolean apiKeyChanged;

    boolean isDefault;
    @Nullable String defaultModelId;

    /**
     * Edited by the account component of the type; compared with {@link #originalOptions} for a modification.
     */
    final Map<String, String> options = new LinkedHashMap<>();
    final Map<String, String> originalOptions = new LinkedHashMap<>();

    AIProviderDraft(@Nullable String originalName, String name, AIProviderType type) {
        this.originalName = originalName;
        this.name = name;
        this.type = type;
    }
}
