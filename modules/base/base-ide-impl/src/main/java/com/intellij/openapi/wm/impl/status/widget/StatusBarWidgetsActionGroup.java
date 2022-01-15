// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status.widget;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.ui.UIBundle;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

public class StatusBarWidgetsActionGroup extends ActionGroup {
  public static final String GROUP_ID = "ViewStatusBarWidgetsGroup";

  @Override
  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    Project project = e != null ? e.getProject() : null;
    if (project == null) return AnAction.EMPTY_ARRAY;

    StatusBarWidgetsManager manager = StatusBarWidgetsManager.getInstance(project);
    Collection<AnAction> toggleActions = new ArrayList<>(ContainerUtil.map(manager.getWidgetFactories(), ToggleWidgetAction::new));
    toggleActions.add(AnSeparator.getInstance());
    toggleActions.add(new HideCurrentWidgetAction());
    return toggleActions.toArray(AnAction.EMPTY_ARRAY);
  }

  private static final class ToggleWidgetAction extends DumbAwareToggleAction {
    private final StatusBarWidgetFactory myWidgetFactory;

    private ToggleWidgetAction(@Nonnull StatusBarWidgetFactory widgetFactory) {
      super(widgetFactory.getDisplayName());
      myWidgetFactory = widgetFactory;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      Project project = e.getProject();
      if (project == null) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }
      if (ActionPlaces.isMainMenuOrShortcut(e.getPlace())) {
        e.getPresentation().setEnabledAndVisible(myWidgetFactory.isConfigurable() && myWidgetFactory.isAvailable(project));
        return;
      }
      StatusBar statusBar = e.getData(PlatformDataKeys.STATUS_BAR);
      e.getPresentation().setEnabledAndVisible(statusBar != null && StatusBarWidgetsManager.getInstance(project).canBeEnabledOnStatusBar(myWidgetFactory, statusBar));
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return StatusBarWidgetSettings.getInstance().isEnabled(myWidgetFactory);
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      StatusBarWidgetSettings.getInstance().setEnabled(myWidgetFactory, state);
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        StatusBarWidgetsManager.getInstance(project).updateWidget(myWidgetFactory, UIAccess.current());
      }
    }
  }

  private static class HideCurrentWidgetAction extends DumbAwareAction {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      StatusBarWidgetFactory factory = getFactory(e);
      if (factory == null) return;

      StatusBarWidgetSettings.getInstance().setEnabled(factory, false);
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        StatusBarWidgetsManager.getInstance(project).updateWidget(factory, UIAccess.current());
      }
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      StatusBarWidgetFactory factory = getFactory(e);
      e.getPresentation().setEnabledAndVisible(factory != null && factory.isConfigurable());
      if (factory != null) {
        e.getPresentation().setText(UIBundle.message("status.bar.hide.widget.action.name", factory.getDisplayName()));
      }
    }

    @Nullable
    private static StatusBarWidgetFactory getFactory(@Nonnull AnActionEvent e) {
      Project project = e.getProject();
      String hoveredWidgetId = e.getData(StatusBarEx.HOVERED_WIDGET_ID);
      StatusBar statusBar = e.getData(PlatformDataKeys.STATUS_BAR);
      if (project != null && hoveredWidgetId != null && statusBar != null) {
        return StatusBarWidgetsManager.getInstance(project).findWidgetFactory(hoveredWidgetId);
      }
      return null;
    }
  }
}
