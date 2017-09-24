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

import com.intellij.ide.ClassUtilCore;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.Main;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.extensions.impl.PicoPluginExtensionInitializationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author mike
 */
public class PluginManager extends PluginManagerCore {
  @NonNls public static final String INSTALLED_TXT = "installed.txt";

  public static long startupStart;

  /**
   * Called via reflection
   */
  @SuppressWarnings({"UnusedDeclaration", "HardCodedStringLiteral"})
  protected static void start(final String mainClass, final String methodName, final String[] args) {
    startupStart = System.nanoTime();

    Main.setFlags(args);

    if (!Main.isHeadless()) {
      UIUtil.initDefaultLAF();
    }

    ThreadGroup threadGroup = new ThreadGroup("Idea Thread Group") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        processException(e);
      }
    };

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          ClassUtilCore.clearJarURLCache();

          Class<?> aClass = Class.forName(mainClass);
          Method method = aClass.getDeclaredMethod(methodName, ArrayUtil.EMPTY_STRING_ARRAY.getClass());
          method.setAccessible(true);
          Object[] argsArray = {args};
          method.invoke(null, argsArray);
        }
        catch (Throwable t) {
          throw new StartupAbortedException(t);
        }
      }
    };

    new Thread(threadGroup, runnable, "Idea Main Thread").start();
  }

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
          if (Logger.isInitialized() && !(t instanceof ProcessCanceledException)) {
            getLogger().error(t);
          }
        }
        catch (Throwable ignore) {
        }

        Main.showMessage("Start Failed", t);
      }

      System.exit(se.exitCode());
    }

    if (!(t instanceof ProcessCanceledException)) {
      getLogger().error(t);
    }
  }

  private static Thread.UncaughtExceptionHandler HANDLER = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      processException(e);
    }
  };

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
          public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            notification.expire();

            String description = event.getDescription();
            if (EDIT.equals(description)) {
              PluginManagerConfigurable configurable = new PluginManagerConfigurable(PluginManagerUISettings.getInstance());
              IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
              ShowSettingsUtil.getInstance().editConfigurable((JFrame)ideFrame, configurable);
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
    final IdeaPluginDescriptor[] plugins = getPlugins();
    for (final IdeaPluginDescriptor plugin : plugins) {
      if (Comparing.equal(id, plugin.getPluginId())) {
        return plugin;
      }
    }
    return null;
  }

  @Nullable
  public static File getPluginPath(@NotNull Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    assert temp instanceof PluginClassLoader : "classloader is not plugin";
    PluginClassLoader classLoader = (PluginClassLoader)temp;
    PluginId pluginId = classLoader.getPluginId();
    IdeaPluginDescriptor plugin = getPlugin(pluginId);
    assert plugin != null : "plugin is not found";
    return plugin.getPath();
  }

  public static void handleComponentError(@NotNull Throwable t, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    if (t instanceof StartupAbortedException) {
      throw (StartupAbortedException)t;
    }

    PluginId pluginId = null;
    if (config != null) {
      pluginId = config.getPluginId();
    }
    if (pluginId == null || CORE_PLUGIN.equals(pluginId)) {
      pluginId = componentClassName == null ? null : getPluginByClassName(componentClassName);
    }
    if (pluginId == null || CORE_PLUGIN.equals(pluginId)) {
      if (t instanceof PicoPluginExtensionInitializationException) {
        pluginId = ((PicoPluginExtensionInitializationException)t).getPluginId();
      }
    }

    if (pluginId != null && !isSystemPlugin(pluginId)) {
      getLogger().warn(t);

      if(!SandboxUtil.isInsideSandbox()) {
        disablePlugin(pluginId.getIdString());
      }

      StringWriter message = new StringWriter();
      message.append("Plugin '").append(pluginId.getIdString()).append("' failed to initialize and will be disabled. ");
      message.append(" Please restart ").append(ApplicationNamesInfo.getInstance().getFullProductName()).append('.');
      message.append("\n\n");
      t.printStackTrace(new PrintWriter(message));
      Main.showMessage("Plugin Error", message.toString(), false);

      throw new StartupAbortedException(t).exitCode(Main.PLUGIN_ERROR).logError(false);
    }
    else {
      throw new StartupAbortedException("Fatal error initializing '" + componentClassName + "'", t);
    }
  }

  private static class StartupAbortedException extends RuntimeException {
    private int exitCode = Main.STARTUP_EXCEPTION;
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
}
