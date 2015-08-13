/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.vfs.backgroundTask.ui.widget;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeManager;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProvider;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProviders;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author VISTALL
 * @since 01.05.14
 */
public class BackgroundTaskWidget extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
  private Icon myIcon;
  private VirtualFile myVirtualFile;

  public BackgroundTaskWidget(@NotNull Project project) {
    super(project);
  }

  @NotNull
  @Override
  public String ID() {
    return "BackgroundTaskWidget";
  }

  @Nullable
  @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return this;
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent e) {
    update(myVirtualFile = e.getNewFile());
  }

  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    update(myVirtualFile = file);
  }

  private void update(VirtualFile file) {
    if(DumbService.isDumb(myProject)) {
      return;
    }
    if (file == null) {
      myIcon = null;
    }
    else {
      List<BackgroundTaskByVfsChangeProvider> providers = BackgroundTaskByVfsChangeProviders.getProviders(myProject, file);
      if(providers.isEmpty()) {
        myIcon = null;
      }
      else {
        List<BackgroundTaskByVfsChangeTask> tasks = BackgroundTaskByVfsChangeManager.getInstance(myProject).findTasks(file);
        if(tasks.isEmpty()) {
          myIcon = IconLoader.getDisabledIcon(AllIcons.RunConfigurations.TrackTests);
        }
        else {
          myIcon = AllIcons.RunConfigurations.TrackTests;
        }
      }
    }

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (myStatusBar != null) {
          myStatusBar.updateWidget(ID());
        }
      }
    });
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    super.install(statusBar);
    myProject.getMessageBus().connect().subscribe(BackgroundTaskByVfsChangeManager.TOPIC, new BackgroundTaskByVfsChangeManager.ListenerAdapter() {
      @Override
      public void taskChanged(@NotNull BackgroundTaskByVfsChangeTask task) {
        VirtualFile file = task.getVirtualFilePointer().getFile();
        if(file == null) {
          return;
        }
        update(file);
      }
    });
  }

  @Override
  public StatusBarWidget copy() {
    return new BackgroundTaskWidget(myProject);
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return "Background Tasks";
  }

  @Nullable
  @Override
  public Consumer<MouseEvent> getClickConsumer() {
    return new Consumer<MouseEvent>() {
      @Override
      public void consume(MouseEvent mouseEvent) {
        if(myVirtualFile == null) {
          return;
        }
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Force Run", null, AllIcons.Actions.Resume) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            BackgroundTaskByVfsChangeManager.getInstance(myProject).runTasks(myVirtualFile);
          }

          @Override
          public void update(AnActionEvent e) {

            e.getPresentation().setEnabled(!BackgroundTaskByVfsChangeManager.getInstance(myProject).findEnabledTasks(myVirtualFile).isEmpty());
          }
        });
        group.add(new AnAction("Manage", null, AllIcons.General.Settings) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            BackgroundTaskByVfsChangeManager.getInstance(myProject).openManageDialog(myVirtualFile);
          }
        });
        ListPopup choose = JBPopupFactory.getInstance()
                .createActionGroupPopup(getTooltipText(), group, DataManager.getInstance().getDataContext(mouseEvent.getComponent()), null, true);

        Dimension dimension = choose.getContent().getPreferredSize();
        Point at = new Point(0, -dimension.height);
        choose.show(new RelativePoint(mouseEvent.getComponent(), at));
      }
    };
  }
}
