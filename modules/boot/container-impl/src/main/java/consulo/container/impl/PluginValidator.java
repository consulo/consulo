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
package consulo.container.impl;

import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorVersionValidator;
import consulo.container.plugin.PluginId;
import consulo.util.nodep.function.Condition;
import consulo.util.nodep.function.Function;
import consulo.util.nodep.io.FileUtilRt;

import java.io.*;
import java.util.*;

/**
 * @author VISTALL
 * @since 2020-05-23
 */
public class PluginValidator {

  public static PluginDescriptorVersionValidator VALIDATOR = new PluginDescriptorVersionValidator() {
    @Override
    public boolean validateVersion(PluginDescriptor pluginDescriptor) {
      return true;
    }
  };

  public static final String DISABLED_PLUGINS_FILENAME = "disabled_plugins.txt";

  static Set<PluginId> ourDisabledPlugins = null;

  public static Set<PluginId> getDisabledPlugins() {
    if (ourDisabledPlugins == null) {
      ourDisabledPlugins = new LinkedHashSet<>();
      if (System.getProperty("consulo.ignore.disabled.plugins") == null) {
        loadDisabledPlugins(ContainerPathManager.get().getConfigPath(), ourDisabledPlugins);
      }
    }
    return ourDisabledPlugins;
  }

  public static boolean disablePlugin(PluginId pluginId) {
    if (getDisabledPlugins().contains(pluginId)) return false;
    getDisabledPlugins().add(pluginId);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static boolean enablePlugin(PluginId pluginId) {
    if (!getDisabledPlugins().contains(pluginId)) return false;
    getDisabledPlugins().remove(pluginId);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static void saveDisabledPlugins(Set<PluginId> ids, boolean append) throws IOException {
    File plugins = new File(ContainerPathManager.get().getConfigPath(), DISABLED_PLUGINS_FILENAME);
    savePluginsList(ids, append, plugins);
    ourDisabledPlugins = null;
  }

  public static void replaceDisabledPlugins(Set<PluginId> ids) {
    try {
      saveDisabledPlugins(ids, false);
    }
    catch (IOException ignored) {
      // FIXME [VISTALL] log that?
    }
  }

  public static void savePluginsList(Collection<PluginId> ids, boolean append, File plugins) throws IOException {
    if (!plugins.isFile()) {
      FileUtilRt.ensureCanCreateFile(plugins);
    }
    PrintWriter printWriter = null;
    try {
      printWriter = new PrintWriter(new BufferedWriter(new FileWriter(plugins, append)));

      for (PluginId id : ids) {
        printWriter.println(id.getIdString());
      }
      printWriter.flush();
    }
    finally {
      if (printWriter != null) {
        printWriter.close();
      }
    }
  }

  public static boolean isIncompatible(final PluginDescriptor descriptor) {
    return !VALIDATOR.validateVersion(descriptor);
  }

  public static void loadDisabledPlugins(final String configPath, final Set<PluginId> disabledPlugins) {
    final File file = new File(configPath, DISABLED_PLUGINS_FILENAME);
    if (file.isFile()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(file));

        String id;
        while ((id = reader.readLine()) != null) {
          disabledPlugins.add(PluginId.getId(id.trim()));
        }
      }
      catch (IOException ignored) {
      }
      finally {
        if (reader != null) {
          try {
            reader.close();
          }
          catch (IOException ignored) {
          }
        }
      }
    }
  }

  public static void checkDependants(final PluginDescriptor pluginDescriptor, final Function<PluginId, PluginDescriptor> pluginId2Descriptor, final Condition<PluginId> check) {
    checkDependants(pluginDescriptor, pluginId2Descriptor, check, new HashSet<PluginId>());
  }

  private static boolean checkDependants(final PluginDescriptor pluginDescriptor,
                                         final Function<PluginId, PluginDescriptor> pluginId2Descriptor,
                                         final Condition<PluginId> check,
                                         final Set<PluginId> processed) {
    processed.add(pluginDescriptor.getPluginId());
    final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
    final Set<PluginId> optionalDependencies = new HashSet<PluginId>(Arrays.asList(pluginDescriptor.getOptionalDependentPluginIds()));
    for (final PluginId dependentPluginId : dependentPluginIds) {
      if (processed.contains(dependentPluginId)) continue;
      if (!optionalDependencies.contains(dependentPluginId)) {
        if (!check.value(dependentPluginId)) {
          return false;
        }
        final PluginDescriptor dependantPluginDescriptor = pluginId2Descriptor.fun(dependentPluginId);
        if (dependantPluginDescriptor != null && !checkDependants(dependantPluginDescriptor, pluginId2Descriptor, check, processed)) {
          return false;
        }
      }
    }
    return true;
  }
}
