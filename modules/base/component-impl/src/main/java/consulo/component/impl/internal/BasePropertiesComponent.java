/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.component.impl.internal;

import consulo.component.PropertiesComponent;
import consulo.component.persist.PersistentStateComponent;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasePropertiesComponent implements PropertiesComponent, PersistentStateComponent<PropertiesState> {
    private final Map<String, String> myMap = new LinkedHashMap<>();

    @TestOnly
    @Deprecated
    public static BasePropertiesComponent create() {
        return new BasePropertiesComponent();
    }

    @Override
    public PropertiesState getState() {
        PropertiesState state = new PropertiesState();
        for (Map.Entry<String, String> entry : myMap.entrySet()) {
            state.properties.put(entry.getKey(), entry.getValue());
        }
        return state;
    }

    @Override
    public void loadState(PropertiesState state) {
        myMap.clear();
        for (Map.Entry<String, String> entry : state.properties.entrySet()) {
            myMap.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getValue(String name) {
        return myMap.get(name);
    }

    @Override
    public void setValue(String name, @Nullable String value) {
        if (value == null) {
            myMap.remove(name);
        }
        else {
            myMap.put(name, value);
        }
    }

    @Override
    public void setValue(String name, @Nullable String value, @Nullable String defaultValue) {
        if (value == null || value.equals(defaultValue)) {
            myMap.remove(name);
        }
        else {
            myMap.put(name, value);
        }
    }

    @Override
    public void setValue(String name, float value, float defaultValue) {
        if (value == defaultValue) {
            myMap.remove(name);
        }
        else {
            myMap.put(name, String.valueOf(value));
        }
    }

    @Override
    public void setValue(String name, long value, long defaultValue) {
        if (value == defaultValue) {
            myMap.remove(name);
        }
        else {
            myMap.put(name, String.valueOf(value));
        }
    }

    @Override
    public void setValue(String name, int value, int defaultValue) {
        if (value == defaultValue) {
            myMap.remove(name);
        }
        else {
            myMap.put(name, String.valueOf(value));
        }
    }

    @Override
    public void setValue(String name, boolean value, boolean defaultValue) {
        if (value == defaultValue) {
            myMap.remove(name);
        }
        else {
            setValue(name, String.valueOf(value));
        }
    }

    @Override
    public void unsetValue(String name) {
        myMap.remove(name);
    }

    @Override
    public boolean isValueSet(String name) {
        return myMap.containsKey(name);
    }

    @Override
    public @Nullable String[] getValues(String name) {
        String value = getValue(name);
        return value != null ? value.split("\n") : null;
    }

    @Override
    public void setValues(String name, String[] values) {
        if (values == null) {
            setValue(name, null);
        }
        else {
            setValue(name, StringUtil.join(values, "\n"));
        }
    }
}