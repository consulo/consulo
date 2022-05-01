// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.actions;

import consulo.execution.ExecutionBundle;
import consulo.application.AllIcons;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ide.impl.idea.openapi.ui.popup.ListItemDescriptorAdapter;
import consulo.ide.impl.idea.openapi.util.Pair;
import consulo.project.ui.wm.IdeFrame;
import consulo.ide.impl.idea.openapi.wm.ex.StatusBarEx;
import consulo.ide.impl.idea.openapi.wm.ex.WindowManagerEx;
import consulo.ui.ex.awt.JBList;
import consulo.ide.impl.idea.ui.popup.list.GroupedItemsListRenderer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopBackgroundProcessesAction extends DumbAwareAction implements AnAction.TransparentUpdate {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(!getCancellableProcesses(e.getData(CommonDataKeys.PROJECT)).isEmpty());
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);
    List<StopAction.HandlerItem> handlerItems = getItemsList(getCancellableProcesses(project));

    if (handlerItems.isEmpty()) {
      return;
    }

    final JBList<StopAction.HandlerItem> list = new JBList<>(handlerItems);
    list.setCellRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<StopAction.HandlerItem>() {
      @Nullable
      @Override
      public String getTextFor(StopAction.HandlerItem item) {
        return item.displayName;
      }

      @Nullable
      @Override
      public Image getIconFor(StopAction.HandlerItem item) {
        return item.icon;
      }

      @Override
      public boolean hasSeparatorAboveOf(StopAction.HandlerItem item) {
        return item.hasSeparator;
      }
    }));

    JBPopup popup = new PopupChooserBuilder<StopAction.HandlerItem>(list).setMovable(true)
            .setTitle(handlerItems.size() == 1 ? ExecutionBundle.message("confirm.background.process.stop") : ExecutionBundle.message("stop.background.process"))
            .setNamerForFiltering(o -> o.displayName).setItemChoosenCallback(() -> {
              List valuesList = list.getSelectedValuesList();
              for (Object o : valuesList) {
                if (o instanceof StopAction.HandlerItem) ((StopAction.HandlerItem)o).stop();
              }
            }).setRequestFocus(true).createPopup();

    InputEvent inputEvent = e.getInputEvent();
    Component component = inputEvent != null ? inputEvent.getComponent() : null;
    if (component != null && (ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace()) || ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(e.getPlace()))) {
      popup.showUnderneathOf(component);
    }
    else if (project == null) {
      popup.showInBestPositionFor(dataContext);
    }
    else {
      popup.showCenteredInCurrentWindow(project);
    }

  }

  @Nonnull
  private static List<Pair<TaskInfo, ProgressIndicator>> getCancellableProcesses(@Nullable Project project) {
    IdeFrame frame = WindowManagerEx.getInstanceEx().findFrameFor(project);
    StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
    if (statusBar == null) return Collections.emptyList();

    return ContainerUtil.findAll(statusBar.getBackgroundProcesses(), pair -> pair.first.isCancellable() && !pair.second.isCanceled());
  }

  @Nonnull
  private static List<StopAction.HandlerItem> getItemsList(@Nonnull List<? extends Pair<TaskInfo, ProgressIndicator>> tasks) {
    List<StopAction.HandlerItem> items = new ArrayList<>(tasks.size());
    for (final Pair<TaskInfo, ProgressIndicator> eachPair : tasks) {
      items.add(new StopAction.HandlerItem(eachPair.first.getTitle(), AllIcons.Process.Step_passive, false) {
        @Override
        void stop() {
          eachPair.second.cancel();
        }
      });
    }
    return items;
  }
}
