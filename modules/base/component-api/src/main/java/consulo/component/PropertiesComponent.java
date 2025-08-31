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
package consulo.component;

import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
public interface PropertiesComponent {
    void unsetValue(String name);

    boolean isValueSet(String name);

    @Nullable
    String getValue(String name);

    /**
     * Consider to use {@link #setValue(String, String, String)} to avoid write defaults.
     */
    void setValue(@Nonnull String name, @Nullable String value);

    /**
     * Set value or unset if equals to default value
     */
    void setValue(@Nonnull String name, @Nullable String value, @Nullable String defaultValue);

    /**
     * Set value or unset if equals to default value
     */
    void setValue(@Nonnull String name, float value, float defaultValue);

    /**
     * Set value or unset if equals to default value
     */
    void setValue(@Nonnull String name, long value, long defaultValue);

    /**
     * Set value or unset if equals to default value
     */
    void setValue(@Nonnull String name, int value, int defaultValue);

    /**
     * Set value or unset if equals to false
     */
    default void setValue(@Nonnull String name, boolean value) {
        setValue(name, value, false);
    }

    /**
     * Set value or unset if equals to default
     */
    void setValue(@Nonnull String name, boolean value, boolean defaultValue);

    @Nullable
    String[] getValues(String name);

    void setValues(String name, String[] values);

    default boolean isTrueValue(String name) {
        return Boolean.valueOf(getValue(name));
    }

    default boolean getBoolean(@Nonnull String name, boolean defaultValue) {
        return isValueSet(name) ? isTrueValue(name) : defaultValue;
    }

    default boolean getBoolean(@Nonnull String name) {
        return getBoolean(name, false);
    }

    @Nonnull
    default String getValue(String name, @Nonnull String defaultValue) {
        if (!isValueSet(name)) {
            return defaultValue;
        }
        return ObjectUtil.notNull(getValue(name), defaultValue);
    }

    default int getInt(@Nonnull String name, int defaultValue) {
        return StringUtil.parseInt(getValue(name), defaultValue);
    }

    default long getLong(@Nonnull String name, int defaultValue) {
        return StringUtil.parseLong(getValue(name), defaultValue);
    }

    default float getFloat(String name, float defaultValue) {
        if (isValueSet(name)) {
            try {
                return Float.parseFloat(getValue(name));
            }
            catch (NumberFormatException ignore) {
            }
        }
        return defaultValue;
    }

    @Deprecated
    default long getOrInitLong(String name, long defaultValue) {
        try {
            String value = getValue(name);
            return value == null ? defaultValue : Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unused")
    /**
     * @deprecated Use {@link #getInt(String, int)}
     * Init was never performed and in any case is not recommended.
     */
    @Deprecated
    default int getOrInitInt(@Nonnull String name, int defaultValue) {
        return getInt(name, defaultValue);
    }

    @Deprecated
    /**
     * @deprecated Use {@link #getValue(String, String)}
     */
    default String getOrInit(String name, String defaultValue) {
        if (!isValueSet(name)) {
            setValue(name, defaultValue);
            return defaultValue;
        }
        return getValue(name);
    }

    default boolean saveFields(@Nonnull Object object) {
        try {
            for (Field field : object.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                PropertyName annotation = field.getAnnotation(PropertyName.class);
                if (annotation != null) {
                    String name = annotation.value();
                    setValue(name, String.valueOf(field.get(object)));
                }
            }
            return true;
        }
        catch (IllegalAccessException e) {
            return false;
        }
    }

    default boolean loadFields(@Nonnull Object object) {
        try {
            for (Field field : object.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                PropertyName annotation = field.getAnnotation(PropertyName.class);
                if (annotation != null) {
                    Class<?> type = field.getType();

                    String defaultValue = annotation.defaultValue();
                    if (PropertyName.NOT_SET.equals(defaultValue)) {
                        if (type.equals(boolean.class)) {
                            defaultValue = String.valueOf(field.getBoolean(object));
                        }
                        else if (type.equals(long.class)) {
                            defaultValue = String.valueOf(field.getLong(object));
                        }
                        else if (type.equals(int.class)) {
                            defaultValue = String.valueOf(field.getInt(object));
                        }
                        else if (type.equals(short.class)) {
                            defaultValue = String.valueOf(field.getShort(object));
                        }
                        else if (type.equals(byte.class)) {
                            defaultValue = String.valueOf(field.getByte(object));
                        }
                        else if (type.equals(double.class)) {
                            defaultValue = String.valueOf(field.getDouble(object));
                        }
                        else if (type.equals(float.class)) {
                            defaultValue = String.valueOf(field.getFloat(object));
                        }
                        else if (type.equals(String.class)) {
                            defaultValue = String.valueOf(field.get(object));
                        }

                    }
                    String stringValue = getValue(annotation.value(), defaultValue);
                    Object value = null;

                    if (type.equals(boolean.class)) {
                        value = Boolean.valueOf(stringValue);
                    }
                    else if (type.equals(long.class)) {
                        value = Long.parseLong(stringValue);
                    }
                    else if (type.equals(int.class)) {
                        value = Integer.parseInt(stringValue);
                    }
                    else if (type.equals(short.class)) {
                        value = Short.parseShort(stringValue);
                    }
                    else if (type.equals(byte.class)) {
                        value = Byte.parseByte(stringValue);
                    }
                    else if (type.equals(double.class)) {
                        value = Double.parseDouble(stringValue);
                    }
                    else if (type.equals(float.class)) {
                        value = Float.parseFloat(stringValue);
                    }
                    else if (type.equals(String.class)) {
                        value = stringValue;
                    }

                    if (value != null) {
                        field.set(object, value);
                    }
                }
            }
            return true;
        }
        catch (IllegalAccessException e) {
            return false;
        }
    }
}
