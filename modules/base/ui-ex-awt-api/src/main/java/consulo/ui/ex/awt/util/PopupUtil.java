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
package consulo.ui.ex.awt.util;

import consulo.application.ApplicationManager;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.awt.hacking.PopupFactoryHacking;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class PopupUtil {
  private static final Logger LOG = Logger.getInstance(PopupUtil.class);

  private PopupUtil() {
  }

  @Nullable
  public static Component getOwner(@Nullable Component c) {
    if (c == null) return null;

    final Window wnd = SwingUtilities.getWindowAncestor(c);
    if (wnd instanceof JWindow) {
      final JRootPane root = ((JWindow)wnd).getRootPane();
      final JBPopup popup = (JBPopup)root.getClientProperty(JBPopup.KEY);
      if (popup == null) return c;

      final Component owner = popup.getOwner();
      if (owner == null) return c;

      return getOwner(owner);
    }
    else {
      return c;
    }
  }

  public static JBPopup getPopupContainerFor(@Nullable Component c) {
    if (c == null) return null;

    final Window wnd = SwingUtilities.getWindowAncestor(c);
    if (wnd instanceof JWindow) {
      final JRootPane root = ((JWindow)wnd).getRootPane();
      return (JBPopup)root.getClientProperty(JBPopup.KEY);
    }

    return null;

  }

  public static void setPopupType(@Nonnull final PopupFactory factory, final int type) {
    PopupFactoryHacking.setPopupType(factory, type);
  }

  public static int getPopupType(@Nonnull final PopupFactory factory) {
    return PopupFactoryHacking.getPopupType(factory);
  }

  public static Component getActiveComponent() {
    Window[] windows = Window.getWindows();
    for (Window each : windows) {
      if (each.isActive()) {
        return each;
      }
    }

    final FocusableFrame frame = IdeFocusManager.findInstance().getLastFocusedFrame();
    if (frame != null) return frame.getComponent();
    return JOptionPane.getRootFrame();
  }

  public static void showBalloonForActiveFrame(@Nonnull final String message, final NotificationType type) {
    final Runnable runnable = new Runnable() {
      public void run() {
        final IdeFrame frame = (IdeFrame)IdeFocusManager.findInstance().getLastFocusedFrame();
        if (frame == null) {
          final Project[] projects = ProjectManager.getInstance().getOpenProjects();
          final Project project = projects == null || projects.length == 0 ? ProjectManager.getInstance().getDefaultProject() : projects[0];
          final JFrame jFrame = WindowManager.getInstance().getFrame(project);
          if (jFrame != null) {
            showBalloonForComponent(jFrame, message, type, true, project);
          }
          else {
            LOG.info("Can not get component to show message: " + message);
          }
          return;
        }
        showBalloonForComponent(frame.getComponent(), message, type, true, frame.getProject());
      }
    };
    UIUtil.invokeLaterIfNeeded(runnable);
  }

  public static void showBalloonForActiveComponent(@Nonnull final String message, final NotificationType type) {
    Runnable runnable = new Runnable() {
      public void run() {
        Window[] windows = Window.getWindows();
        Window targetWindow = null;
        for (Window each : windows) {
          if (each.isActive()) {
            targetWindow = each;
            break;
          }
        }

        if (targetWindow == null) {
          targetWindow = JOptionPane.getRootFrame();
        }

        if (targetWindow == null) {
          final IdeFrame frame = (IdeFrame)IdeFocusManager.findInstance().getLastFocusedFrame();
          if (frame == null) {
            final Project[] projects = ProjectManager.getInstance().getOpenProjects();
            final Project project = projects == null || projects.length == 0 ? ProjectManager.getInstance().getDefaultProject() : projects[0];
            final JFrame jFrame = WindowManager.getInstance().getFrame(project);
            if (jFrame != null) {
              showBalloonForComponent(jFrame, message, type, true, project);
            }
            else {
              LOG.info("Can not get component to show message: " + message);
            }
            return;
          }
          showBalloonForComponent(frame.getComponent(), message, type, true, frame.getProject());
        }
        else {
          showBalloonForComponent(targetWindow, message, type, true, null);
        }
      }
    };
    UIUtil.invokeLaterIfNeeded(runnable);
  }

  public static void showBalloonForComponent(@Nonnull Component component, @Nonnull final String message, final NotificationType type, final boolean atTop, @Nullable final Disposable disposable) {
    final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
    if (popupFactory == null) return;
    BalloonBuilder balloonBuilder = popupFactory.createHtmlTextBalloonBuilder(message, type, null);
    balloonBuilder.setDisposable(disposable == null ? ApplicationManager.getApplication() : disposable);
    Balloon balloon = balloonBuilder.createBalloon();
    Dimension size = component.getSize();
    Balloon.Position position;
    int x;
    int y;
    if (size == null) {
      x = y = 0;
      position = Balloon.Position.above;
    }
    else {
      x = Math.min(10, size.width / 2);
      y = size.height;
      position = Balloon.Position.below;
    }
    balloon.show(new RelativePoint(component, new Point(x, y)), position);
  }

  public static void showForActionButtonEvent(@Nonnull JBPopup popup, @Nonnull AnActionEvent e) {
    InputEvent inputEvent = e.getInputEvent();
    if (inputEvent == null) {
      popup.showInFocusCenter();
    }
    else {
      Component component = inputEvent.getComponent();
      if (component instanceof ActionButtonComponent) {
        popup.showUnderneathOf(component);
      }
      else {
        popup.showInCenterOf(component);
      }
    }
  }
}