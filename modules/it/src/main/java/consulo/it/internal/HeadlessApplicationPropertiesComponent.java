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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationPropertiesComponent;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory application properties for the headless container — the production
 * implementation lives in ide-impl which is excluded here. Consumers in the indexing
 * stack ({@code GistManagerImpl} reindex-count persistence and friends) only need
 * plain get/set semantics within one test JVM.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessApplicationPropertiesComponent implements ApplicationPropertiesComponent {
    private final Map<String, Object> myValues = new ConcurrentHashMap<>();

    @Override
    public void unsetValue(String name) {
        myValues.remove(name);
    }

    @Override
    public boolean isValueSet(String name) {
        return myValues.containsKey(name);
    }

    @Override
    public @Nullable String getValue(String name) {
        Object value = myValues.get(name);
        return value == null ? null : value.toString();
    }

    @Override
    public void setValue(String name, @Nullable String value) {
        if (value == null) {
            myValues.remove(name);
        }
        else {
            myValues.put(name, value);
        }
    }

    @Override
    public void setValue(String name, @Nullable String value, @Nullable String defaultValue) {
        if (value == null || Objects.equals(value, defaultValue)) {
            myValues.remove(name);
        }
        else {
            myValues.put(name, value);
        }
    }

    @Override
    public void setValue(String name, float value, float defaultValue) {
        setValue(name, value == defaultValue ? null : String.valueOf(value));
    }

    @Override
    public void setValue(String name, long value, long defaultValue) {
        setValue(name, value == defaultValue ? null : String.valueOf(value));
    }

    @Override
    public void setValue(String name, int value, int defaultValue) {
        setValue(name, value == defaultValue ? null : String.valueOf(value));
    }

    @Override
    public void setValue(String name, boolean value, boolean defaultValue) {
        setValue(name, value == defaultValue ? null : String.valueOf(value));
    }

    @Override
    public @Nullable String[] getValues(String name) {
        Object value = myValues.get(name);
        return value instanceof String[] strings ? strings : null;
    }

    @Override
    public void setValues(String name, String[] values) {
        if (values == null) {
            myValues.remove(name);
        }
        else {
            myValues.put(name, values);
        }
    }
}
