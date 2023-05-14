/*
 * Copyright 2013-2019 consulo.io
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
package consulo.component.util;

import consulo.component.extension.ExtensionException;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 2019-11-01
 */
public class PluginExceptionUtil {
  private static final Logger LOG = Logger.getInstance(PluginExceptionUtil.class);

  /**
   * Creates an exception caused by a problem in a plugin's code.
   *
   * @param pluginClass a problematic class which caused the error
   */
  @Nonnull
  public static PluginException createByClass(@Nonnull String errorMessage, @Nullable Throwable cause, @Nonnull Class<?> pluginClass) {
    PluginId pluginId = PluginManager.getPluginId(pluginClass);
    return new PluginException(errorMessage, cause, pluginId);
  }

  /**
   * Creates an exception caused by a problem in a plugin's code, takes error message from the cause exception.
   *
   * @param pluginClass a problematic class which caused the error
   */
  @Nonnull
  public static PluginException createByClass(@Nonnull Throwable cause, @Nonnull Class<?> pluginClass) {
    String message = cause.getMessage();

    PluginId pluginId = PluginManager.getPluginId(pluginClass);
    return new PluginException(message != null ? message : "", cause, pluginId);
  }

  @Nonnull
  public static Set<PluginId> findAllPluginIds(@Nonnull Throwable t) {
    if (t instanceof PluginException) {
      PluginId pluginId = ((PluginException)t).getPluginId();
      return Set.of(pluginId);
    }

    if (t instanceof ExtensionException) {
      Class extensionClass = ((ExtensionException)t).getExtensionClass();
      PluginId pluginId = PluginManager.getPluginId(extensionClass);
      if (pluginId == null) {
        LOG.error("There no plugin for extension class: " + extensionClass);
        return Set.of();
      }
      return Set.of(pluginId);
    }

    Set<PluginId> pluginIds = new TreeSet<>();

    for (StackTraceElement element : t.getStackTrace()) {
      String classLoaderName = element.getClassLoaderName();
      if (classLoaderName == null) {
        continue;
      }

      PluginDescriptor plugin = PluginManager.findPlugin(PluginId.getId(classLoaderName));
      if (plugin == null) {
        continue;
      }
      pluginIds.add(plugin.getPluginId());
    }
    return pluginIds;
  }

  @Nullable
  public static PluginId findFirstPluginId(@Nonnull Throwable t) {
    Set<PluginId> pluginIds = findAllPluginIds(t);
    return pluginIds.stream().filter(pluginId -> !PluginIds.isPlatformPlugin(pluginId)).findFirst().orElse(null);
  }

  public static void logPluginError(Logger log, String message, Throwable t, Class<?> aClass) {
    PluginDescriptor plugin = PluginManager.getPlugin(aClass);

    if (plugin == null) {
      log.error(message, t);
    }
    else {
      log.error(new PluginException(message, t, plugin.getPluginId()));
    }
  }

  public static void reportDeprecatedUsage(@Nonnull String signature, @Nonnull String details) {
    String message = "'" + signature + "' is deprecated and going to be removed soon. " + details;
    Logger.getInstance(PluginException.class).error(message);
  }

  public static void reportDeprecatedDefault(@Nonnull Class<?> violator, @Nonnull String methodName, @Nonnull String details) {
    String message = "The default implementation of method '" + methodName + "' is deprecated, you need to override it in '" + violator + "'. " + details;
    Logger logger = Logger.getInstance(violator);
    logPluginError(logger, message, null, violator);
  }
}
