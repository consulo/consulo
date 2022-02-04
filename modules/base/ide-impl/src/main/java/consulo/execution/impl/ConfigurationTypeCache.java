/*
 * Copyright 2013-2020 consulo.io
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
package consulo.execution.impl;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.UnknownConfigurationType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-08-05
 */
public class ConfigurationTypeCache {
  private List<ConfigurationType> myTypes;
  private final Map<String, ConfigurationType> myTypesByName = new LinkedHashMap<>();

  public ConfigurationTypeCache(@Nonnull final List<ConfigurationType> factories) {
    final ArrayList<ConfigurationType> types = new ArrayList<>(factories);
    types.add(UnknownConfigurationType.INSTANCE);
    myTypes = types;

    for (final ConfigurationType type : factories) {
      myTypesByName.put(type.getId(), type);
    }

    final UnknownConfigurationType broken = UnknownConfigurationType.INSTANCE;
    myTypesByName.put(broken.getId(), broken);
  }

  @Nonnull
  public List<ConfigurationType> getTypes() {
    return myTypes;
  }

  @Nullable
  public ConfigurationType getConfigurationType(final String typeName) {
    return myTypesByName.get(typeName);
  }

  @Nullable
  public ConfigurationFactory findFactoryOfTypeNameByName(final String typeName, final String factoryName) {
    ConfigurationType type = myTypesByName.get(typeName);
    if (type == null) {
      type = myTypesByName.get(UnknownConfigurationType.NAME);
    }

    return findFactoryOfTypeByName(type, factoryName);
  }

  @Nullable
  private static ConfigurationFactory findFactoryOfTypeByName(final ConfigurationType type, final String factoryName) {
    if (factoryName == null) return null;

    if (type instanceof UnknownConfigurationType) {
      return type.getConfigurationFactories()[0];
    }

    final ConfigurationFactory[] factories = type.getConfigurationFactories();
    for (final ConfigurationFactory factory : factories) {
      if (factoryName.equals(factory.getName())) return factory;
    }

    return null;
  }
}
