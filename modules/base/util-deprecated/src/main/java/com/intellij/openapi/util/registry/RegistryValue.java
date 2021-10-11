/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.util.registry;

import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.MissingResourceException;

/**
 * @author Kirill Kalishev
 * @author Konstantin Bulenkov
 */
@Deprecated
public class RegistryValue {
  @Nonnull
  private final String myKey;
  @Nullable
  private String myValue;

  private String myStringCachedValue;
  private Integer myIntCachedValue;
  private Double myDoubleCachedValue;
  private Boolean myBooleanCachedValue;

  RegistryValue(@Nonnull String key, @Nullable String value) {
    myKey = key;
    myValue = value;
  }

  @Nonnull
  public String getKey() {
    return myKey;
  }

  @Nonnull
  public String asString() {
    final String value = get(myKey, null, true);
    assert value != null : myKey;
    return value;
  }

  public boolean asBoolean() {
    if (myBooleanCachedValue == null) {
      myBooleanCachedValue = Boolean.valueOf(get(myKey, "false", true));
    }

    return myBooleanCachedValue;
  }

  public int asInteger() {
    if (myIntCachedValue == null) {
      myIntCachedValue = Integer.valueOf(get(myKey, "0", true));
    }

    return myIntCachedValue;
  }

  public double asDouble() {
    if (myDoubleCachedValue == null) {
      myDoubleCachedValue = Double.valueOf(get(myKey, "0.0", true));
    }

    return myDoubleCachedValue;
  }

  public Color asColor(Color defaultValue) {
    final String s = get(myKey, null, true);
    if (s != null) {
      final String[] rgb = s.split(",");
      if (rgb.length == 3) {
        try {
          return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
        }
        catch (Exception e) {//
        }
      }
    }
    return defaultValue;
  }

  @Nonnull
  public String getDescription() {
    return get(myKey + ".description", "", false);
  }

  private String get(@Nonnull String key, String defaultValue, boolean isValue) throws MissingResourceException {
    if (isValue) {
      if (myStringCachedValue == null) {
        myStringCachedValue = _get(key, defaultValue, isValue);
        if (isBoolean()) {
          myStringCachedValue = Boolean.valueOf(myStringCachedValue).toString();
        }
      }
      return myStringCachedValue;
    }
    return _get(key, defaultValue, isValue);
  }

  private String _get(@Nonnull String key, String defaultValue, boolean mustExistInBundle) throws MissingResourceException {
    String systemProperty = System.getProperty(key);
    if (systemProperty != null) {
      return systemProperty;
    }
    final String bundleValue = getBundleValue(key, mustExistInBundle);
    if (bundleValue != null) {
      return bundleValue;
    }
    return defaultValue;
  }

  private String getBundleValue(@Nonnull String key, boolean mustExist) throws MissingResourceException {
    return myValue;
  }

  public void setValue(boolean value) {
    setValue(Boolean.valueOf(value).toString());
  }

  public void setValue(int value) {
    setValue(Integer.valueOf(value).toString());
  }

  public void setValue(String value) {
    myValue = value;

    resetCache();
  }

  public void addListener(@Nonnull final RegistryValueListener listener, @Nonnull Disposable parent) {
  }

  @Override
  public String toString() {
    return myKey + "=" + asString();
  }

  void resetCache() {
    myStringCachedValue = null;
    myIntCachedValue = null;
    myBooleanCachedValue = null;
  }

  public boolean isBoolean() {
    return "true".equals(asString()) || "false".equals(asString());
  }
}
