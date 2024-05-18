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
package consulo.execution.impl.internal.configuration;

import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-08-05
 */
public class ConfigurationTypeCache {
  static final ExtensionPointCacheKey<ConfigurationType, ConfigurationTypeCache> CACHE_KEY =
    ExtensionPointCacheKey.create("ConfigurationTypeCache", ConfigurationTypeCache::new);
  private List<ConfigurationType> myTypes;
  private final Map<String, ConfigurationType> myTypesById = new LinkedHashMap<>();

  private ConfigurationTypeCache(@Nonnull ExtensionWalker<ConfigurationType> walker) {
    List<ConfigurationType> types = new ArrayList<>();
    walker.walk(types::add);

    types.add(UnknownConfigurationType.INSTANCE);
    myTypes = types;

    for (ConfigurationType type : types) {
      myTypesById.put(type.getId(), type);
    }
  }

  @Nonnull
  public List<ConfigurationType> getTypes() {
    return myTypes;
  }

  @Nullable
  public ConfigurationType getConfigurationType(final String typeName) {
    return myTypesById.get(typeName);
  }

  @Nullable
  public ConfigurationFactory findFactoryOfTypeNameId(final String typeId, final String factoryName) {
    ConfigurationType type = myTypesById.get(typeId);
    if (type == null) {
      type = myTypesById.get(UnknownConfigurationType.NAME);
    }

    return findFactoryOfTypeById(type, factoryName);
  }

  @Nullable
  private static ConfigurationFactory findFactoryOfTypeById(final ConfigurationType type, final String facotryId) {
    if (facotryId == null) return null;

    if (type instanceof UnknownConfigurationType) {
      return type.getConfigurationFactories()[0];
    }

    final ConfigurationFactory[] factories = type.getConfigurationFactories();
    for (final ConfigurationFactory factory : factories) {
      if (facotryId.equals(factory.getId())) return factory;
    }

    return null;
  }
}
