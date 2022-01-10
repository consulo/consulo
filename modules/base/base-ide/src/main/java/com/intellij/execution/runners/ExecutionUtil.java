/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.execution.runners;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.UIUtil;
import consulo.execution.ExecutionDataKeys;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class ExecutionUtil {
  private static final Logger LOG = Logger.getInstance(ExecutionUtil.class);

  private static final NotificationGroup ourNotificationGroup = NotificationGroup.logOnlyGroup("Execution");

  private ExecutionUtil() {
  }

  public static void handleExecutionError(@Nonnull Project project, @Nonnull String toolWindowId, @Nonnull RunProfile runProfile, @Nonnull ExecutionException e) {
    handleExecutionError(project, toolWindowId, runProfile.getName(), e);
  }

  public static void handleExecutionError(@Nonnull ExecutionEnvironment environment, @Nonnull ExecutionException e) {
    handleExecutionError(environment.getProject(), environment.getExecutor().getToolWindowId(), environment.getRunProfile().getName(), e);
  }

  public static void handleExecutionError(@Nonnull final Project project, @Nonnull final String toolWindowId, @Nonnull String taskName, @Nonnull ExecutionException e) {
    if (e instanceof RunCanceledByUserException) {
      return;
    }

    LOG.debug(e);

    String description = e.getMessage();
    if (description == null) {
      LOG.warn("Execution error without description", e);
      description = "Unknown error";
    }

    HyperlinkListener listener = null;
    if ((description.contains("87") || description.contains("111") || description.contains("206")) &&
        e instanceof ProcessNotCreatedException &&
        !PropertiesComponent.getInstance(project).isTrueValue("dynamic.classpath")) {
      final String commandLineString = ((ProcessNotCreatedException)e).getCommandLine().getCommandLineString();
      if (commandLineString.length() > 1024 * 32) {
        description = "Command line is too long. In order to reduce its length classpath file can be used.<br>" +
                      "Would you like to enable classpath file mode for all run configurations of your project?<br>" +
                      "<a href=\"\">Enable</a>";

        listener = new HyperlinkListener() {
          @Override
          public void hyperlinkUpdate(HyperlinkEvent event) {
            PropertiesComponent.getInstance(project).setValue("dynamic.classpath", "true");
          }
        };
      }
    }
    final String title = ExecutionBundle.message("error.running.configuration.message", taskName);
    final String fullMessage = title + ":<br>" + description;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.error(fullMessage, e);
    }

    if (listener == null && e instanceof HyperlinkListener) {
      listener = (HyperlinkListener)e;
    }

    final HyperlinkListener finalListener = listener;
    final String finalDescription = description;
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (project.isDisposed()) {
          return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindowManager.canShowNotification(toolWindowId)) {
          //noinspection SSBasedInspection
          toolWindowManager.notifyByBalloon(toolWindowId, MessageType.ERROR, fullMessage, null, finalListener);
        }
        else {
          Messages.showErrorDialog(project, UIUtil.toHtml(fullMessage), "");
        }
        NotificationListener notificationListener = finalListener == null ? null : new NotificationListener() {
          @Override
          public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
            finalListener.hyperlinkUpdate(event);
          }
        };
        ourNotificationGroup.createNotification(title, finalDescription, NotificationType.ERROR, notificationListener).notify(project);
      }
    });
  }

  public static void restartIfActive(@Nonnull RunContentDescriptor descriptor) {
    ProcessHandler processHandler = descriptor.getProcessHandler();
    if (processHandler != null && processHandler.isStartNotified() && !processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
      restart(descriptor);
    }
  }

  public static void restart(@Nonnull RunContentDescriptor descriptor) {
    restart(descriptor.getComponent());
  }

  public static void restart(@Nonnull Content content) {
    restart(content.getComponent());
  }

  private static void restart(@Nullable JComponent component) {
    if (component != null) {
      ExecutionEnvironment environment = DataManager.getInstance().getDataContext(component).getData(ExecutionDataKeys.EXECUTION_ENVIRONMENT);
      if (environment != null) {
        restart(environment);
      }
    }
  }

  public static void restart(@Nonnull ExecutionEnvironment environment) {
    if (!ExecutorRegistry.getInstance().isStarting(environment)) {
      ExecutionManager.getInstance(environment.getProject()).restartRunProfile(environment);
    }
  }

  public static void runConfiguration(@Nonnull RunnerAndConfigurationSettings configuration, @Nonnull Executor executor) {
    ExecutionEnvironmentBuilder builder = createEnvironment(executor, configuration);
    if (builder != null) {
      ExecutionManager.getInstance(configuration.getConfiguration().getProject()).restartRunProfile(builder.activeTarget().build());
    }
  }

  @Nullable
  public static ExecutionEnvironmentBuilder createEnvironment(@Nonnull Executor executor, @Nonnull RunnerAndConfigurationSettings settings) {
    try {
      return ExecutionEnvironmentBuilder.create(executor, settings);
    }
    catch (ExecutionException e) {
      handleExecutionError(settings.getConfiguration().getProject(), executor.getToolWindowId(), settings.getConfiguration().getName(), e);
      return null;
    }
  }

  @Nonnull
  public static Image getIconWithLiveIndicator(@Nullable final Image base) {
    int width = base == null ? 13 : base.getWidth();
    int height = base == null ? 13 : base.getHeight();
    return ImageEffects.layered(base, ImageEffects.canvas(width, height, ctx -> {
      int iSize = 2;

      ctx.setFillStyle(StandardColors.GREEN);
      ctx.arc(width - iSize - 1, height - iSize - 1, iSize, 0, 2 * Math.PI);
      ctx.fill();

      ctx.setStrokeStyle(StandardColors.BLACK.withAlpha(0.4f));
      ctx.arc(width - iSize - 1, height - iSize - 1, iSize, 0, 2 * Math.PI);
      ctx.stroke();
    }));
  }
}
