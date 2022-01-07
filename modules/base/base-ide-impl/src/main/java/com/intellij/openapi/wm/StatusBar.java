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
package com.intellij.openapi.wm;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
public interface StatusBar extends StatusBarInfo, Disposable {

  @SuppressWarnings({"AbstractClassNeverImplemented"})
  abstract class Info implements StatusBarInfo {
    public static final Topic<StatusBarInfo> TOPIC = Topic.create("IdeStatusBar.Text", StatusBarInfo.class);

    private Info() {
    }

    public static void set(@Nullable final String text, @Nullable final Project project) {
      set(text, project, null);
    }

    public static void set(@Nullable final String text, @Nullable final Project project, @Nullable final String requestor) {
      if (project != null) {
        if (project.isDisposed()) return;
        if (!project.isInitialized()) {
          StartupManager.getInstance(project).runWhenProjectIsInitialized((p, ui) -> p.getMessageBus().syncPublisher(TOPIC).setInfo(text, requestor));
          return;
        }
      }

      final MessageBus bus = project == null ? ApplicationManager.getApplication().getMessageBus() : project.getMessageBus();
      bus.syncPublisher(TOPIC).setInfo(text, requestor);
    }
  }


  final class Anchors {
    public static final String DEFAULT_ANCHOR = after(StandardWidgets.COLUMN_SELECTION_MODE_PANEL);

    public static String before(String widgetId) {
      return "before " + widgetId;
    }

    public static String after(String widgetId) {
      return "after " + widgetId;
    }
  }

  final class StandardWidgets {
    public static final String ENCODING_PANEL = "Encoding";
    public static final String COLUMN_SELECTION_MODE_PANEL = "InsertOverwrite"; // Keep the old ID for backwards compatibility
    public static final String READONLY_ATTRIBUTE_PANEL = "ReadOnlyAttribute";
    public static final String POSITION_PANEL = "Position";
    public static final String LINE_SEPARATOR_PANEL = "LineSeparator";
  }

  void addWidget(@Nonnull StatusBarWidget widget);

  void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor);

  void addWidget(@Nonnull StatusBarWidget widget, @Nonnull Disposable parentDisposable);

  void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor, @Nonnull Disposable parentDisposable);

  void removeWidget(@Nonnull String id);

  void updateWidget(@Nonnull String id);

  @Nullable
  StatusBarWidget getWidget(String id);

  void fireNotificationPopup(@Nonnull JComponent content, Color backgroundColor);

  StatusBar createChild();

  StatusBar findChild(Component c);

  IdeFrame getFrame();

  void install(IdeFrame frame);

  @Nullable
  Project getProject();

  @Nonnull
  default consulo.ui.Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @Deprecated
  @DeprecationInfo("AWT Dependency")
  default JComponent getComponent() {
    // override isUnified() too
    throw new AbstractMethodError();
  }

  default boolean isUnified() {
    return false;
  }
}
