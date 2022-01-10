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

import com.intellij.openapi.util.AtomicNotNullLazyValue;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@DeprecationInfo(value = "Use `EarlyAccessProgramDescriptor` or `PropertiesComponent`")
public class Registry {
  private static final String REGISTRY_BUNDLE = "misc.registry";

  // copy jvm properties, disable override in runtime
  private static final Properties ourJvmProperties = new Properties(System.getProperties());

  private static AtomicNotNullLazyValue<Map<String, RegistryValue>> ourRegistry = AtomicNotNullLazyValue.createValue(() -> {
    Map<String, RegistryValue> properties = new ConcurrentHashMap<>();

    ResourceBundle resourceBundle = ResourceBundle.getBundle(REGISTRY_BUNDLE);

    Enumeration<String> keys = resourceBundle.getKeys();

    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      if(key.endsWith(".description") || key.equals(".restartRequired")) {
        continue;
      }

      String value = resourceBundle.getString(key);

      properties.put(key, new RegistryValue(key, value, ourJvmProperties));
    }

    return properties;
  });

  @Nonnull
  public static RegistryValue get(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    return ourRegistry.get().computeIfAbsent(key, s -> new RegistryValue(s, null, ourJvmProperties));
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    return get(key).asBoolean();
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, boolean defaultValue) {
    try {
      return get(key).asBoolean();
    }
    catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    return get(key).asInteger();
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, int defaultValue) {
    return get(key).asInteger(defaultValue);
  }

  public static double doubleValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    return get(key).asDouble();
  }

  @Nonnull
  public static String stringValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    return get(key).asString();
  }

  public static Color getColor(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, Color defaultValue) {
    return get(key).asColor(defaultValue);
  }
}
