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
package consulo.language.editor.inlay;

import consulo.localize.LocalizeValue;

import java.util.Objects;

/**
 * Describes a single inlay option.
 */
public final class InlayOptionInfo {
    private final String id;
    private final boolean isEnabledByDefault;
    private final LocalizeValue name;

    public InlayOptionInfo(String id,
                           boolean isEnabledByDefault,
                           LocalizeValue name) {
        this.id = id;
        this.isEnabledByDefault = isEnabledByDefault;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabledByDefault() {
        return isEnabledByDefault;
    }

    public LocalizeValue getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InlayOptionInfo)) {
            return false;
        }
        InlayOptionInfo that = (InlayOptionInfo) other;
        return isEnabledByDefault == that.isEnabledByDefault &&
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isEnabledByDefault, name);
    }
}