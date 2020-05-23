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
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.util.nodep.io.FileUtilRt;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-05-23
 */
public class PluginValidator {

  public static PluginDescriptorVersionValidator VALIDATOR = new PluginDescriptorVersionValidator() {
    @Override
    public boolean validateVersion(@Nonnull PluginDescriptor pluginDescriptor) {
      return true;
    }
  };

  public static final String DISABLED_PLUGINS_FILENAME = "disabled_plugins.txt";

  static List<String> ourDisabledPlugins = null;

  @Nonnull
  static List<String> getDisabledPlugins() {
    if (ourDisabledPlugins == null) {
      ourDisabledPlugins = new ArrayList<String>();
      if (System.getProperty("consulo.ignore.disabled.plugins") == null) {
        loadDisabledPlugins(ContainerPathManager.get().getConfigPath(), ourDisabledPlugins);
      }
    }
    return ourDisabledPlugins;
  }

  public static boolean disablePlugin(String id) {
    if (getDisabledPlugins().contains(id)) return false;
    getDisabledPlugins().add(id);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static boolean enablePlugin(String id) {
    if (!getDisabledPlugins().contains(id)) return false;
    getDisabledPlugins().remove(id);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static void saveDisabledPlugins(Collection<String> ids, boolean append) throws IOException {
    File plugins = new File(ContainerPathManager.get().getConfigPath(), DISABLED_PLUGINS_FILENAME);
    savePluginsList(ids, append, plugins);
    ourDisabledPlugins = null;
  }

  public static void replaceDisabledPlugins(List<String> ids) {
    try {
      saveDisabledPlugins(ids, false);
    }
    catch (IOException ignored) {
      // FIXME [VISTALL] log that?
    }
  }

  public static void savePluginsList(Collection<String> ids, boolean append, File plugins) throws IOException {
    if (!plugins.isFile()) {
      FileUtilRt.ensureCanCreateFile(plugins);
    }
    PrintWriter printWriter = null;
    try {
      printWriter = new PrintWriter(new BufferedWriter(new FileWriter(plugins, append)));

      for (String id : ids) {
        printWriter.println(id);
      }
      printWriter.flush();
    }
    finally {
      if (printWriter != null) {
        printWriter.close();
      }
    }
  }

  @Nonnull
  static PluginManager.PluginSkipReason calcPluginSkipReason(final PluginDescriptor descriptor) {
    final String idString = descriptor.getPluginId().getIdString();
    if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
      return PluginManager.PluginSkipReason.NO;
    }

    if (!descriptor.isEnabled()) return PluginManager.PluginSkipReason.DISABLED;

    boolean shouldLoad = !getDisabledPlugins().contains(idString);
    if (shouldLoad && descriptor instanceof PluginDescriptorImpl) {
      if (isIncompatible(descriptor)) return PluginManager.PluginSkipReason.INCOMPATIBLE;
    }

    return !shouldLoad ? PluginManager.PluginSkipReason.DEPENDENCY_IS_NOT_RESOLVED : PluginManager.PluginSkipReason.NO;
  }

  public static boolean isIncompatible(final PluginDescriptor descriptor) {
    return !VALIDATOR.validateVersion(descriptor);
  }

  public static void loadDisabledPlugins(final String configPath, final Collection<String> disabledPlugins) {
    final File file = new File(configPath, DISABLED_PLUGINS_FILENAME);
    if (file.isFile()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(file));

        String id;
        while ((id = reader.readLine()) != null) {
          disabledPlugins.add(id.trim());
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

  static boolean shouldSkipPlugin(@Nonnull PluginDescriptor descriptor) {
    if (descriptor instanceof PluginDescriptorImpl) {
      PluginDescriptorImpl descriptorImpl = (PluginDescriptorImpl)descriptor;
      Boolean skipped = descriptorImpl.getSkipped();
      if (skipped != null) {
        return skipped;
      }
      boolean result = shouldSkipPlugin0(descriptor);
      descriptorImpl.setSkipped(result);
      return result;
    }
    return shouldSkipPlugin0(descriptor);
  }

  static boolean shouldSkipPlugin0(final PluginDescriptor descriptor) {
    return calcPluginSkipReason(descriptor) != PluginManager.PluginSkipReason.NO;
  }
}
