/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.options.ex;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.configurable.OptionalConfigurable;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.idea.openapi.options.ConfigurableEP;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class ConfigurableExtensionPointUtil {

  private final static Logger LOG = Logger.getInstance(ConfigurableExtensionPointUtil.class);

  private ConfigurableExtensionPointUtil() {
  }


  public static List<Configurable> buildConfigurablesList(List<Configurable> extensions, @Nullable Predicate<Configurable> filter) {
    final List<Configurable> result = new ArrayList<>();

    final Map<String, ConfigurableWrapper> idToConfigurable = new HashMap<>();
    for (Configurable configurable : extensions) {
      // do not disable if disable
      if (configurable instanceof OptionalConfigurable && !((OptionalConfigurable)configurable).needDisplay()) {
        continue;
      }

      if (filter == null || !filter.test(configurable)) {
        continue;
      }

      idToConfigurable.put(configurable.getId(), ConfigurableWrapper.wrapConfigurable(configurable));
    }

    //modify configurables (append children)
    for (final String id : idToConfigurable.keySet()) {
      final ConfigurableWrapper wrapper = idToConfigurable.get(id);
      final String parentId = wrapper.getParentId();
      if (parentId != null) {
        final ConfigurableWrapper parent = idToConfigurable.get(parentId);
        if (parent != null) {
          idToConfigurable.put(parentId, parent.addChild(wrapper));
        }
        else {
          LOG.error("Can't find parent for " + parentId + " (" + wrapper + ")");
        }
      }
    }
    //leave only roots (i.e. configurables without parents)
    for (final Iterator<String> iterator = idToConfigurable.keySet().iterator(); iterator.hasNext(); ) {
      final String key = iterator.next();
      final ConfigurableWrapper wrapper = idToConfigurable.get(key);
      if (wrapper.getParentId() != null) {
        iterator.remove();
      }
    }
    ContainerUtil.addAll(result, idToConfigurable.values());

    return result;
  }

  /**
   * @deprecated create a new instance of configurable instead
   */
  @Nonnull
  public static <T extends Configurable> T findProjectConfigurable(@Nonnull Project project, @Nonnull Class<T> configurableClass) {
    return findConfigurable(project.getExtensions(BaseShowSettingsUtil.PROJECT_CONFIGURABLE), configurableClass);
  }

  @Nonnull
  public static <T extends Configurable> T findApplicationConfigurable(@Nonnull Class<T> configurableClass) {
    return findConfigurable(BaseShowSettingsUtil.APPLICATION_CONFIGURABLE.getExtensions(), configurableClass);
  }

  @Nonnull
  private static <T extends Configurable> T findConfigurable(ConfigurableEP<Configurable>[] extensions, Class<T> configurableClass) {
    for (ConfigurableEP<Configurable> extension : extensions) {
      if (extension.providerClass != null || extension.instanceClass != null || extension.implementationClass != null) {
        final Configurable configurable = extension.createConfigurable();
        if (configurableClass.isInstance(configurable)) {
          return configurableClass.cast(configurable);
        }
      }
    }
    throw new IllegalArgumentException("Cannot find configurable of " + configurableClass);
  }

  @Nullable
  public static Configurable createProjectConfigurableForProvider(@Nonnull Project project, Class<? extends ConfigurableProvider> providerClass) {
    return createConfigurableForProvider(project.getExtensions(BaseShowSettingsUtil.PROJECT_CONFIGURABLE), providerClass);
  }

  @Nullable
  public static Configurable createApplicationConfigurableForProvider(Class<? extends ConfigurableProvider> providerClass) {
    return createConfigurableForProvider(BaseShowSettingsUtil.APPLICATION_CONFIGURABLE.getExtensions(), providerClass);
  }

  @Nullable
  private static Configurable createConfigurableForProvider(ConfigurableEP<Configurable>[] extensions, Class<? extends ConfigurableProvider> providerClass) {
    for (ConfigurableEP<Configurable> extension : extensions) {
      if (extension.providerClass != null) {
        final Class<Object> aClass = extension.findClassNoExceptions(extension.providerClass);
        if (aClass != null && providerClass.isAssignableFrom(aClass)) {
          return extension.createConfigurable();
        }
      }
    }
    return null;
  }
}
