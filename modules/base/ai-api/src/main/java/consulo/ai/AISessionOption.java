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

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A setting of a chat session the provider offers - the model, the mode, the effort, the context size. The values of a
 * grouped option are flattened.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public record AISessionOption(String id,
                              String name,
                              @Nullable String description,
                              @Nullable String category,
                              String type,
                              @Nullable String currentValue,
                              List<AISessionOptionValue> values) {
    public static final String CATEGORY_MODEL = "model";
    public static final String CATEGORY_MODE = "mode";
    public static final String CATEGORY_THOUGHT_LEVEL = "thought_level";
    public static final String CATEGORY_MODEL_CONFIG = "model_config";

    public boolean isModel() {
        return CATEGORY_MODEL.equals(category) || "model".equals(id);
    }

    public boolean isMode() {
        return CATEGORY_MODE.equals(category) || "mode".equals(id);
    }

    /**
     * An on and off switch - a boolean, or a select of two values such as {@code true} and {@code false}.
     */
    public boolean isToggle() {
        if ("boolean".equals(type) || "toggle".equals(type)) {
            return true;
        }
        if (values.size() != 2) {
            return false;
        }
        String first = values.get(0).value().toLowerCase();
        String second = values.get(1).value().toLowerCase();
        return first.equals("false") && second.equals("true") || first.equals("true") && second.equals("false");
    }

    /**
     * How hard the model thinks, as levels to choose from - not the switch which turns thinking on and off.
     */
    public boolean isEffort() {
        return CATEGORY_THOUGHT_LEVEL.equals(category) && !isToggle() && !values.isEmpty();
    }

    public @Nullable AISessionOptionValue findValue(String value) {
        for (AISessionOptionValue candidate : values) {
            if (candidate.value().equals(value)) {
                return candidate;
            }
        }
        return null;
    }
}
