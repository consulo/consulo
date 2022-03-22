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
package com.intellij.ide.plugins;

import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.StartupAbortedException;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.component.impl.extension.PluginExtensionInitializationException;
import consulo.component.ProcessCanceledException;
import consulo.annotation.DeprecationInfo;
import consulo.annotation.UsedInPlugin;
import consulo.application.ApplicationProperties;
import consulo.container.ExitCodes;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.ComponentConfig;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactoryInitializer;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author mike
 */
@Deprecated
@DeprecationInfo("Use consulo.container.plugin.PluginManager")
public class PluginManager extends PluginManagerCore {
  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance(PluginManagerCore.class);
  }

  @NonNls
  public static final String INSTALLED_TXT = "installed.txt";

  public static void processException(Throwable t) {
    StartupAbortedException se = null;

    if (t instanceof StartupAbortedException) {
      se = (StartupAbortedException)t;
    }
    else if (t.getCause() instanceof StartupAbortedException) {
      se = (StartupAbortedException)t.getCause();
    }
    else if (!ApplicationStarter.isLoaded()) {
      se = new StartupAbortedException(t);
    }

    if (se != null) {
      if (se.logError()) {
        try {
          if (LoggerFactoryInitializer.isInitialized() && !(t instanceof ProcessCanceledException)) {
            getLogger().error(t);
          }
        }
        catch (Throwable ignore) {
        }

        StartupUtil.showMessage("Start Failed", t);
      }

      System.exit(se.exitCode());
    }

    if (!(t instanceof ProcessCanceledException)) {
      getLogger().error(t);
    }
  }


  public static Logger getLogger() {
    return LoggerHolder.ourLogger;
  }

  private static Thread.UncaughtExceptionHandler HANDLER = (t, e) -> processException(e);

  public static void installExceptionHandler() {
    Thread.currentThread().setUncaughtExceptionHandler(HANDLER);
  }

  public static boolean isPluginInstalled(PluginId id) {
    return getPlugin(id) != null;
  }

  @Nullable
  public static PluginDescriptor getPlugin(PluginId id) {
    return consulo.container.plugin.PluginManager.findPlugin(id);
  }

  @Nullable
  @UsedInPlugin
  @Deprecated
  public static File getPluginPath(@Nonnull Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    assert temp instanceof PluginClassLoader : "classloader is not plugin";
    PluginClassLoader classLoader = (PluginClassLoader)temp;
    PluginId pluginId = classLoader.getPluginId();
    PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(pluginId);
    assert plugin != null : "plugin is not found";
    return plugin.getPath();
  }

  @Deprecated
  public static void handleComponentError(@Nonnull Throwable t, @Nullable Class componentClass, @Nullable ComponentConfig config) {
    StartupUtil.handleComponentError(t, componentClass, config);
  }
}
