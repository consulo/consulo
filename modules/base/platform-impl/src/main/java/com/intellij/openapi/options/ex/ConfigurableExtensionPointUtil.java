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
package com.intellij.openapi.options.ex;

import consulo.logging.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.OptionalConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class ConfigurableExtensionPointUtil {

  private final static Logger LOG = Logger.getInstance(ConfigurableExtensionPointUtil.class);

  private ConfigurableExtensionPointUtil() {
  }


  public static List<Configurable> buildConfigurablesList(List<ConfigurableEP<Configurable>> extensions, @Nullable Condition<Configurable> filter) {
    final List<Configurable> result = new ArrayList<>();

    final Map<String, ConfigurableWrapper> idToConfigurable = new HashMap<>();
    for (ConfigurableEP<Configurable> ep : extensions) {
      final Configurable configurable = ConfigurableWrapper.wrapConfigurable(ep);
      if (isSuppressed(configurable, filter)) continue;
      if (configurable instanceof ConfigurableWrapper) {
        final ConfigurableWrapper wrapper = (ConfigurableWrapper)configurable;
        idToConfigurable.put(wrapper.getId(), wrapper);
      }
      else {
//        dumpConfigurable(configurablesExtensionPoint, ep, configurable);
        ContainerUtil.addIfNotNull(result, configurable);
      }
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

  private static boolean isSuppressed(Configurable each, Condition<Configurable> filter) {
    OptionalConfigurable configurable = ConfigurableWrapper.cast(each, OptionalConfigurable.class);
    if(configurable != null && !configurable.needDisplay()) {
      return true;
    }
    return filter != null && !filter.value(each);
  }

  /*
  private static void dumpConfigurable(ExtensionPointName<ConfigurableEP<Configurable>> configurablesExtensionPoint,
                                       ConfigurableEP<Configurable> ep,
                                       Configurable configurable) {
    if (configurable != null && !(configurable instanceof ConfigurableGroup)) {
      if (ep.instanceClass != null && (configurable instanceof SearchableConfigurable) && (configurable instanceof Configurable.Composite)) {
        Element element = dump(ep, configurable, StringUtil.getShortName(configurablesExtensionPoint.getName()));
        final Configurable[] configurables = ((Configurable.Composite)configurable).getConfigurables();
        for (Configurable child : configurables) {
          final Element dump = dump(null, child, "configurable");
          element.addContent(dump);
        }
        final StringWriter out = new StringWriter();
        try {
          new XMLOutputter(Format.getPrettyFormat()).output(element, out);
        }
        catch (IOException e) {
        }
        System.out.println(out);
      }
    }
  }

  private static Element dump(@Nullable ConfigurableEP ep,
                              Configurable configurable, String name) {
    Element element = new Element(name);
    if (ep != null) {
      element.setAttribute("instance", ep.instanceClass);
      String id = ep.id == null ? ((SearchableConfigurable)configurable).getId() : ep.id;
      element.setAttribute("id", id);
    }
    else {
      element.setAttribute("instance", configurable.getClass().getName());
      if (configurable instanceof SearchableConfigurable) {
        element.setAttribute("id", ((SearchableConfigurable)configurable).getId());
      }
    }

    CommonBundle.lastKey = null;
    String displayName = configurable.getDisplayName();
    if (CommonBundle.lastKey != null) {
      element.setAttribute("key", CommonBundle.lastKey).setAttribute("bundle", CommonBundle.lastBundle);
    }
    else {
      element.setAttribute("displayName", displayName);
    }
    if (configurable instanceof NonDefaultProjectConfigurable) {
      element.setAttribute("nonDefaultProject", "true");
    }
    return element;
  }
  */

  /**
   * @deprecated create a new instance of configurable instead
   */
  @Nonnull
  public static <T extends Configurable> T findProjectConfigurable(@Nonnull Project project, @Nonnull Class<T> configurableClass) {
    return findConfigurable(project.getExtensions(Configurable.PROJECT_CONFIGURABLE), configurableClass);
  }

  @Nonnull
  public static <T extends Configurable> T findApplicationConfigurable(@Nonnull Class<T> configurableClass) {
    return findConfigurable(Configurable.APPLICATION_CONFIGURABLE.getExtensions(), configurableClass);
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
    return createConfigurableForProvider(project.getExtensions(Configurable.PROJECT_CONFIGURABLE), providerClass);
  }

  @Nullable
  public static Configurable createApplicationConfigurableForProvider(Class<? extends ConfigurableProvider> providerClass) {
    return createConfigurableForProvider(Configurable.APPLICATION_CONFIGURABLE.getExtensions(), providerClass);
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
