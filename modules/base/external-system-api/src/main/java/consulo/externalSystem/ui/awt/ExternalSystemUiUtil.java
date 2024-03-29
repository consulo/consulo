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
package consulo.externalSystem.ui.awt;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.internal.DefaultExternalSystemUiAware;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 4/8/13 7:29 PM
 */
public class ExternalSystemUiUtil {

  public static final int INSETS = 7;
  private static final int BALLOON_FADEOUT_TIME = 5000;

  private ExternalSystemUiUtil() {
  }

  /**
   * Asks to show balloon that contains information related to the given component.
   *
   * @param component    component for which we want to show information
   * @param messageType  balloon message type
   * @param message      message to show
   */
  public static void showBalloon(@Nonnull JComponent component, @Nonnull NotificationType messageType, @Nonnull String message) {
    final BalloonBuilder builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, messageType, null)
      .setDisposable(ApplicationManager.getApplication())
      .setFadeoutTime(BALLOON_FADEOUT_TIME);
    Balloon balloon = builder.createBalloon();
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

  @Nonnull
  public static GridBag getLabelConstraints(int indentLevel) {
    Insets insets = new Insets(INSETS, INSETS + INSETS * indentLevel, 0, INSETS);
    return new GridBag().anchor(GridBagConstraints.WEST).weightx(0).insets(insets);
  }

  @Nonnull
  public static GridBag getFillLineConstraints(int indentLevel) {
    Insets insets = new Insets(INSETS, INSETS + INSETS * indentLevel, 0, INSETS);
    return new GridBag().weightx(1).coverLine().fillCellHorizontally().anchor(GridBagConstraints.WEST).insets(insets);
  }

  public static void fillBottom(@Nonnull JComponent component) {
    component.add(Box.createVerticalGlue(), new GridBag().weightx(1).weighty(1).fillCell().coverLine());
  }

  /**
   * Applies data from the given settings object to the given model.
   * 
   * @param settings  target settings to use
   * @param model     UI model to be synced with the given settings
   */
  public static void apply(@Nonnull final AbstractExternalSystemLocalSettings settings, @Nonnull final ExternalSystemTasksTreeModel model) {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        Map<ExternalProjectPojo,Collection<ExternalProjectPojo>> projects = settings.getAvailableProjects();
        for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : projects.entrySet()) {
          model.ensureSubProjectsStructure(entry.getKey(), entry.getValue());
        }
        Map<String, Collection<ExternalTaskPojo>> tasks = settings.getAvailableTasks();
        for (Map.Entry<String, Collection<ExternalTaskPojo>> entry : tasks.entrySet()) {
          model.ensureTasks(entry.getKey(), entry.getValue());
        } 
      }
    });
  }

  public static void showUi(@Nonnull Object o, boolean show) {
    for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        field.setAccessible(true);
        try {
          Object v = field.get(o);
          if (v instanceof JComponent) {
            ((JComponent)v).setVisible(show);
          }
        }
        catch (IllegalAccessException e) {
          // Ignore
        }
      }
    }
  }

  public static void disposeUi(@Nonnull Object o) {
    for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        field.setAccessible(true);
        try {
          Object v = field.get(o);
          if (v instanceof JComponent) {
            field.set(o, null);
          }
        }
        catch (IllegalAccessException e) {
          // Ignore
        }
      }
    }
  }

  @Nonnull
  public static ExternalSystemUiAware getUiAware(@Nonnull ProjectSystemId externalSystemId) {
    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    return manager instanceof ExternalSystemUiAware ? (ExternalSystemUiAware)manager : DefaultExternalSystemUiAware.INSTANCE;
  }

  public static void executeAction(@Nonnull final String actionId, @Nonnull final InputEvent e) {
    final ActionManager actionManager = ActionManager.getInstance();
    final AnAction action = actionManager.getAction(actionId);
    if (action == null) {
      return;
    }
    final Presentation presentation = new Presentation();
    DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
    final AnActionEvent event = new AnActionEvent(e, context, "", presentation, actionManager, 0);
    action.update(event);
    if (presentation.isEnabled()) {
      action.actionPerformed(event);
    }
  }
}
