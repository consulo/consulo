/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.util.ObjectUtil;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.Lists;
import consulo.util.collection.impl.map.ConcurrentHashMap;
import consulo.util.lang.ShutDownTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class NotificationProjectTracker implements Disposable {
  private final Map<String, EventLogConsole> myCategoryMap = new ConcurrentHashMap<>();
  private final List<Notification> myInitial = Lists.newLockFreeCopyOnWriteList();
  private final Project myProject;
  private final EventLog myEventLog;

  protected LogModel myProjectModel;

  @Inject
  public NotificationProjectTracker(@Nonnull final Project project, EventLog eventLog) {
    myProject = project;
    myEventLog = eventLog;


    myProjectModel = new LogModel(project, project);

    for (Notification notification : myEventLog.myModel.takeNotifications()) {
      printNotification(notification);
    }
  }

  void initDefaultContent() {
    createNewContent(EventLog.DEFAULT_CATEGORY);

    for (Notification notification : myInitial) {
      doPrintNotification(notification, ObjectUtil.assertNotNull(getConsole(notification)));
    }
    myInitial.clear();
  }

  @Override
  public void dispose() {
    myEventLog.myModel.setStatusMessage(null, 0);
    StatusBar.Info.set("", null, EventLog.LOG_REQUESTOR);
  }

  protected void printNotification(Notification notification) {
    if (!NotificationsConfigurationImpl.getSettings(notification.getGroupId()).isShouldLog()) {
      return;
    }
    myProjectModel.addNotification(notification);

    EventLogConsole console = getConsole(notification);
    if (console == null) {
      myInitial.add(notification);
    }
    else {
      doPrintNotification(notification, console);
    }
  }

  private void doPrintNotification(@Nonnull final Notification notification, @Nonnull final EventLogConsole console) {
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new DumbAwareRunnable() {
      @Override
      public void run() {
        if (!ShutDownTracker.isShutdownHookRunning() && !myProject.isDisposed()) {
          ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
              console.doPrintNotification(notification);
            }
          });
        }
      }
    });
  }

  protected void showNotification(@Nonnull final String groupId, @Nonnull final List<String> ids) {
    ToolWindow eventLog = EventLog.getEventLog(myProject);
    if (eventLog != null) {
      EventLog.activate(eventLog, groupId, new Runnable() {
        @Override
        public void run() {
          EventLogConsole console = NotificationProjectTracker.this.getConsole(groupId);
          if (console != null) {
            console.showNotification(ids);
          }
        }
      });
    }
  }

  protected void clearNMore(@Nonnull Collection<String> groups) {
    for (String group : groups) {
      EventLogConsole console = myCategoryMap.get(EventLog.getContentName(group));
      if (console != null) {
        console.clearNMore();
      }
    }
  }

  @Nullable
  protected EventLogConsole getConsole(@Nonnull Notification notification) {
    return getConsole(notification.getGroupId());
  }

  @Nullable
  private EventLogConsole getConsole(@Nonnull String groupId) {
    if (myCategoryMap.get(EventLog.DEFAULT_CATEGORY) == null) return null; // still not initialized

    String name = EventLog.getContentName(groupId);
    EventLogConsole console = myCategoryMap.get(name);
    return console != null ? console : createNewContent(name);
  }

  @Nonnull
  private EventLogConsole createNewContent(String name) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    EventLogConsole newConsole = new EventLogConsole(myProjectModel);
    EventLogToolWindowFactory.createContent(myProject, EventLog.getEventLog(myProject), newConsole, name);
    myCategoryMap.put(name, newConsole);

    return newConsole;
  }

}
