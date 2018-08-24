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
package com.intellij.ide.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.ObjectUtils;
import consulo.util.ApplicationPropertiesComponent;
import consulo.util.ProjectPropertiesComponent;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
public interface PropertiesComponent {
  static PropertiesComponent getInstance(Project project) {
    return ServiceManager.getService(project, ProjectPropertiesComponent.class);
  }

  static PropertiesComponent getInstance() {
    return ServiceManager.getService(ApplicationPropertiesComponent.class);
  }

  public abstract void unsetValue(String name);

  public abstract boolean isValueSet(String name);

  @Nullable
  public abstract String getValue(@NonNls String name);

  /**
   * Consider to use {@link #setValue(String, String, String)} to avoid write defaults.
   */
  public abstract void setValue(@Nonnull String name, @Nullable String value);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@Nonnull String name, @Nullable String value, @Nullable String defaultValue);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@Nonnull String name, float value, float defaultValue);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@Nonnull String name, int value, int defaultValue);

  /**
   * Set value or unset if equals to false
   */
  default void setValue(@Nonnull String name, boolean value) {
    setValue(name, value, false);
  }

  /**
   * Set value or unset if equals to default
   */
  public abstract void setValue(@Nonnull String name, boolean value, boolean defaultValue);

  @Nullable
  public abstract String[] getValues(@NonNls String name);

  public abstract void setValues(@NonNls String name, String[] values);

  default boolean isTrueValue(@NonNls String name) {
    return Boolean.valueOf(getValue(name)).booleanValue();
  }

  default boolean getBoolean(@Nonnull String name, boolean defaultValue) {
    return isValueSet(name) ? isTrueValue(name) : defaultValue;
  }

  default boolean getBoolean(@Nonnull String name) {
    return getBoolean(name, false);
  }

  @Nonnull
  default String getValue(@NonNls String name, @Nonnull String defaultValue) {
    if (!isValueSet(name)) {
      return defaultValue;
    }
    return ObjectUtils.notNull(getValue(name), defaultValue);
  }

  @SuppressWarnings("unused")
  @Deprecated
  /**
   * @deprecated Use {@link #getInt(String, int)}
   * Init was never performed and in any case is not recommended.
   */ default int getOrInitInt(@Nonnull String name, int defaultValue) {
    return getInt(name, defaultValue);
  }

  default int getInt(@Nonnull String name, int defaultValue) {
    return StringUtilRt.parseInt(getValue(name), defaultValue);
  }

  default long getOrInitLong(@NonNls String name, long defaultValue) {
    try {
      String value = getValue(name);
      return value == null ? defaultValue : Long.parseLong(value);
    }
    catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Deprecated
  /**
   * @deprecated Use {@link #getValue(String, String)}
   */ default String getOrInit(@NonNls String name, String defaultValue) {
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
          final String name = annotation.value();
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
        final PropertyName annotation = field.getAnnotation(PropertyName.class);
        if (annotation != null) {
          final Class<?> type = field.getType();

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
          final String stringValue = getValue(annotation.value(), defaultValue);
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
}
