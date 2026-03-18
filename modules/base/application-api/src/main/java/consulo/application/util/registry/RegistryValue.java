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
package consulo.application.util.registry;

import consulo.disposer.Disposable;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.util.MissingResourceException;
import java.util.Properties;

/**
 * @author Kirill Kalishev
 * @author Konstantin Bulenkov
 */
@Deprecated
public class RegistryValue {
  private final String myKey;
  private @Nullable String myValue;
  
  private final Properties myProperties;

  private @Nullable String myStringCachedValue = null;
  private @Nullable Integer myIntCachedValue = null;
  private @Nullable Double myDoubleCachedValue = null;
  private @Nullable Boolean myBooleanCachedValue = null;

  RegistryValue(String key, @Nullable String value, Properties properties) {
    myKey = key;
    myValue = value;
    myProperties = properties;
  }

  public String getKey() {
    return myKey;
  }

  
  public String asString() {
    if (myStringCachedValue == null) {
      myStringCachedValue = get(myKey, "");
    }
    return myStringCachedValue;
  }

  public boolean asBoolean() {
     return asBoolean(false);
  }

  public boolean asBoolean(boolean defaultValue) {
    if (myBooleanCachedValue == null) {
      myBooleanCachedValue = Boolean.valueOf(get(myKey, String.valueOf(defaultValue)));
    }

    return myBooleanCachedValue;
  }

  public int asInteger() {
    return asInteger(0);
  }

  public int asInteger(int defaultValue) {
    if (myIntCachedValue == null) {
      myIntCachedValue = Integer.valueOf(get(myKey, String.valueOf(defaultValue)));
    }

    return myIntCachedValue;
  }

  public double asDouble() {
    if (myDoubleCachedValue == null) {
      myDoubleCachedValue = Double.valueOf(get(myKey, "0.0"));
    }

    return myDoubleCachedValue;
  }

  public Color asColor(Color defaultValue) {
    String s = get(myKey, null);
    if (s != null) {
      String[] rgb = s.split(",");
      if (rgb.length == 3) {
        try {
          return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
        }
        catch (Exception ignore) {
        }
      }
    }
    return defaultValue;
  }

  @Contract("_,!null -> !null")
  private @Nullable String get(String key, @Nullable String defaultValue) throws MissingResourceException {
    String systemProperty = myProperties.getProperty(key);
    if (systemProperty != null) {
      return systemProperty;
    }

    if (myValue != null) {
      return myValue;
    }

    return defaultValue;
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

  public void addListener(RegistryValueListener listener, Disposable parent) {
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
}
