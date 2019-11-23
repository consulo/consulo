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

import com.intellij.ide.IdeBundle;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.StartupUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationNamesInfo;
import consulo.container.plugin.ComponentConfig;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.extensions.impl.PluginExtensionInitializationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.annotations.Exported;
import consulo.application.ApplicationProperties;
import consulo.awt.TargetAWT;
import consulo.container.ExitCodes;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.logging.internal.LoggerFactoryInitializer;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author mike
 */
public class PluginManager extends PluginManagerCore {
  public static class StartupAbortedException extends RuntimeException {
    private int exitCode = ExitCodes.STARTUP_EXCEPTION;
    private boolean logError = true;

    public StartupAbortedException(Throwable cause) {
      super(cause);
    }

    public StartupAbortedException(String message, Throwable cause) {
      super(message, cause);
    }

    public int exitCode() {
      return exitCode;
    }

    public StartupAbortedException exitCode(int exitCode) {
      this.exitCode = exitCode;
      return this;
    }

    public boolean logError() {
      return logError;
    }

    public StartupAbortedException logError(boolean logError) {
      this.logError = logError;
      return this;
    }
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

  private static Thread.UncaughtExceptionHandler HANDLER = (t, e) -> processException(e);

  public static void installExceptionHandler() {
    Thread.currentThread().setUncaughtExceptionHandler(HANDLER);
  }

  public static void reportPluginError() {
    if (ourPluginErrors != null) {
      for (String pluginError : ourPluginErrors) {
        String message = IdeBundle.message("title.plugin.notification.title");
        Notifications.Bus.notify(new Notification(message, message, pluginError, NotificationType.ERROR, new NotificationListener() {
          @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
          @Override
          public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
            notification.expire();

            String description = event.getDescription();
            if (EDIT.equals(description)) {
              PluginManagerConfigurable configurable = new PluginManagerConfigurable(PluginManagerUISettings.getInstance());
              IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
              ShowSettingsUtil.getInstance().editConfigurable(ideFrame == null ? null : TargetAWT.to(ideFrame.getWindow()), configurable);
              return;
            }

            List<String> disabledPlugins = getDisabledPlugins();
            if (myPlugins2Disable != null && DISABLE.equals(description)) {
              for (String pluginId : myPlugins2Disable) {
                if (!disabledPlugins.contains(pluginId)) {
                  disabledPlugins.add(pluginId);
                }
              }
            }
            else if (myPlugins2Enable != null && ENABLE.equals(description)) {
              disabledPlugins.removeAll(myPlugins2Enable);
            }

            try {
              saveDisabledPlugins(disabledPlugins, false);
            }
            catch (IOException ignore) {
            }

            myPlugins2Enable = null;
            myPlugins2Disable = null;
          }
        }));
      }
      ourPluginErrors = null;
    }
  }

  public static boolean isPluginInstalled(PluginId id) {
    return getPlugin(id) != null;
  }

  @Nullable
  public static IdeaPluginDescriptor getPlugin(PluginId id) {
    return (IdeaPluginDescriptor)consulo.container.plugin.PluginManager.findPlugin(id);
  }

  @Nullable
  @Exported
  public static File getPluginPath(@Nonnull Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    assert temp instanceof PluginClassLoader : "classloader is not plugin";
    PluginClassLoader classLoader = (PluginClassLoader)temp;
    PluginId pluginId = classLoader.getPluginId();
    PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(pluginId);
    assert plugin != null : "plugin is not found";
    return plugin.getPath();
  }

  public static void handleComponentError(@Nonnull Throwable t, @Nullable Class componentClass, @Nullable ComponentConfig config) {
    if (t instanceof StartupAbortedException) {
      throw (StartupAbortedException)t;
    }

    PluginId pluginId = null;
    if (config != null) {
      pluginId = config.getPluginId();
    }
    if (pluginId == null || CORE_PLUGIN.equals(pluginId)) {
      pluginId = componentClass == null ? null : consulo.container.plugin.PluginManager.getPluginId(componentClass);
    }
    if (pluginId == null || CORE_PLUGIN.equals(pluginId)) {
      if (t instanceof PluginExtensionInitializationException) {
        pluginId = ((PluginExtensionInitializationException)t).getPluginId();
      }
    }

    if (pluginId != null && !isSystemPlugin(pluginId)) {
      getLogger().warn(t);

      if(!ApplicationProperties.isInSandbox()) {
        disablePlugin(pluginId.getIdString());
      }

      StringWriter message = new StringWriter();
      message.append("Plugin '").append(pluginId.getIdString()).append("' failed to initialize and will be disabled. ");
      message.append(" Please restart ").append(ApplicationNamesInfo.getInstance().getFullProductName()).append('.');
      message.append("\n\n");
      t.printStackTrace(new PrintWriter(message));
      StartupUtil.showMessage("Plugin Error", message.toString(), false, false);

      throw new StartupAbortedException(t).exitCode(ExitCodes.PLUGIN_ERROR).logError(false);
    }
    else {
      throw new StartupAbortedException("Fatal error initializing '" + (componentClass == null ? null : componentClass.getName()) + "'", t);
    }
  }
}
