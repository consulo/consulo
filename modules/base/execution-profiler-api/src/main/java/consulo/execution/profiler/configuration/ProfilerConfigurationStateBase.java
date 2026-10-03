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
package consulo.execution.profiler.configuration;

import org.jspecify.annotations.Nullable;

/**
 * Base for configuration states: a bean with a public no-argument constructor whose getter/setter pairs are stored with
 * the XML serializer. A subclass adds its own properties and opens its package to {@code consulo.util.xml.serializer}.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class ProfilerConfigurationStateBase implements ProfilerConfigurationState {
    private @Nullable String myDisplayName;

    @Override
    public @Nullable String getDisplayName() {
        return myDisplayName;
    }

    @Override
    public void setDisplayName(@Nullable String displayName) {
        myDisplayName = displayName;
    }

    @Override
    public String toString() {
        String displayName = myDisplayName;
        return displayName == null ? getConfigurationTypeId() : displayName + " (" + getConfigurationTypeId() + ")";
    }
}
