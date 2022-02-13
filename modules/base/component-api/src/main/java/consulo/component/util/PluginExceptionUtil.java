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

import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-11-01
 */
public class PluginExceptionUtil {
  public static void logPluginError(Logger log, String message, Throwable t, Class<?> aClass) {
    PluginDescriptor plugin = PluginManager.getPlugin(aClass);

    if(plugin == null) {
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
