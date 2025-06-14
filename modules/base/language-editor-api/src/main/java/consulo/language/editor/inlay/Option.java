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

import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * Represents a configurable option for hints.
 */
public class Option {
    @Nonnull
    private final String id;
    private final LocalizeValue nameSupplier;
    private final boolean defaultValue;
    private final LocalizeValue extendedDescription;

    public Option(@Nonnull String id,
                  LocalizeValue nameSupplier,
                  boolean defaultValue) {
        this(id, nameSupplier, defaultValue, LocalizeValue.of());
    }

    public Option(@Nonnull String id,
                  LocalizeValue nameSupplier,
                  boolean defaultValue,
                  @Nonnull LocalizeValue extendedDescription) {
        this.id = id;
        this.nameSupplier = nameSupplier;
        this.defaultValue = defaultValue;
        this.extendedDescription = extendedDescription;
    }

    @Nonnull
    public LocalizeValue getExtendedDescription() {
        return extendedDescription;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public String getName() {
        return nameSupplier.get();
    }

    public boolean isEnabled() {
        Boolean v = ParameterNameHintsSettings.getInstance().getOption(id);
        return v != null ? v : defaultValue;
    }

    public boolean get() {
        Boolean v = ParameterNameHintsSettings.getInstance().getOption(id);
        return v != null ? v : defaultValue;
    }

    public void set(boolean newValue) {
        ParameterNameHintsSettings settings = ParameterNameHintsSettings.getInstance();
        if (newValue == defaultValue) {
            settings.setOption(id, null);
        }
        else {
            settings.setOption(id, newValue);
        }
    }
}
