/*
 * Copyright 2013-2020 consulo.io
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
package consulo.execution.ui.impl;

import com.intellij.execution.Executor;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ThreeState;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.execution.ui.RunContentManagerImpl.getExecutorByContent;
import static com.intellij.execution.ui.RunContentManagerImpl.getRunContentDescriptorByContent;

/**
 * @author VISTALL
 * @since 2020-11-01
 */
public class RunToolWindowManager {
  private static final Logger LOG = Logger.getInstance(RunToolWindowManager.class);

  private final Map<String, ContentManager> myToolwindowIdToContentManagerMap = new ConcurrentHashMap<>();
  private final Map<String, Image> myToolwindowIdToBaseIconMap = new HashMap<>();
  private final LinkedList<String> myToolwindowIdZBuffer = new LinkedList<>();
  @Nonnull
  private final Project myProject;
  private final Provider<ToolWindowManager> myToolWindowManager;
  private final Disposable myParentDisposable;

  public RunToolWindowManager(Project project, Provider<ToolWindowManager> toolWindowManager, Disposable parentDisposable) {
    myProject = project;
    myToolWindowManager = toolWindowManager;
    myParentDisposable = parentDisposable;
    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      @Override
      public void stateChanged(ToolWindowManager tw) {
        if (project.isDisposed()) {
          return;
        }

        Set<String> currentWindows = new HashSet<>();
        ContainerUtil.addAll(currentWindows, tw.getToolWindowIds());
        myToolwindowIdZBuffer.retainAll(currentWindows);

        final String activeToolWindowId = tw.getActiveToolWindowId();
        if (activeToolWindowId != null) {
          if (myToolwindowIdZBuffer.remove(activeToolWindowId)) {
            myToolwindowIdZBuffer.addFirst(activeToolWindowId);
          }
        }
      }
    });
  }

  public Image getImage(@Nonnull String toolWindowId) {
    return myToolwindowIdToBaseIconMap.get(toolWindowId);
  }

  public List<String> getToolwindowIdZBuffer() {
    return myToolwindowIdZBuffer;
  }

  public Set<Map.Entry<String, ContentManager>> entrySet() {
    return myToolwindowIdToContentManagerMap.entrySet();
  }

  @Nullable
  @RequiredUIAccess
  public ContentManager get(@Nonnull String toolWindowId) {
    // if project started disposing, return null
    if(myProject.getDisposeState().get() != ThreeState.NO) {
      return null;
    }
    
    UIAccess.assertIsUIThread();
    return myToolwindowIdToContentManagerMap.computeIfAbsent(toolWindowId, this::createToolWindow);
  }

  @RequiredUIAccess
  private ContentManager createToolWindow(@Nonnull String toolWindowId) {
    if (ToolWindowId.RUN_DASHBOARD.equals(toolWindowId)) {
      RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(myProject);
      return registerToolWindow(toolWindowId, runDashboardManager.getToolWindowIcon(), null);
    }
    else {
      Executor executor = Executor.EP_NAME.getExtensionList().stream().filter(it -> it.getToolWindowId().equals(toolWindowId)).findFirst().get();
      return registerToolWindow(executor.getToolWindowId(), executor.getToolWindowIcon(), null);
    }
  }

  @RequiredUIAccess
  private ContentManager registerToolWindow(@Nonnull String toolWindowId, @Nonnull Image toolWindowIcon, @Nullable Executor executor) {
    ToolWindowManager toolWindowManager = myToolWindowManager.get();

    if (toolWindowManager.getToolWindow(toolWindowId) != null) {
      throw new IllegalArgumentException("Already registered: " + toolWindowId);
    }

    final ToolWindow toolWindow = toolWindowManager.registerToolWindow(toolWindowId, true, ToolWindowAnchor.BOTTOM, myParentDisposable, true);
    final ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addDataProvider(new DataProvider() {
      private int myInsideGetData = 0;

      @Override
      public Object getData(@Nonnull Key<?> dataId) {
        myInsideGetData++;
        try {
          if (PlatformDataKeys.HELP_ID == dataId) {
            return executor != null ? executor.getHelpId() : null;
          }
          else {
            return myInsideGetData == 1 ? DataManager.getInstance().getDataContext(contentManager.getComponent()).getData(dataId) : null;
          }
        }
        finally {
          myInsideGetData--;
        }
      }
    });

    toolWindow.setIcon(toolWindowIcon);
    new ContentManagerWatcher(toolWindow, contentManager);
    initToolWindow(executor, toolWindowId, toolWindowIcon, contentManager);

    return contentManager;
  }

  private void initToolWindow(@Nullable final Executor executor, String toolWindowId, Image toolWindowIcon, ContentManager contentManager) {
    myToolwindowIdToBaseIconMap.put(toolWindowId, toolWindowIcon);
    contentManager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          Content content = event.getContent();
          Executor contentExecutor = executor;
          if (contentExecutor == null) {
            // Content manager contains contents related with different executors.
            // Try to get executor from content.
            contentExecutor = getExecutorByContent(content);
            // Must contain this user data since all content is added by this class.
            LOG.assertTrue(contentExecutor != null);
          }
          myProject.getMessageBus().syncPublisher(RunContentManager.TOPIC).contentSelected(getRunContentDescriptorByContent(content), contentExecutor);
        }
      }
    });
    Disposer.register(contentManager, () -> {
      myToolwindowIdToContentManagerMap.remove(toolWindowId).removeAllContents(true);
      myToolwindowIdZBuffer.remove(toolWindowId);
      myToolwindowIdToBaseIconMap.remove(toolWindowId);
    });
    myToolwindowIdZBuffer.addLast(toolWindowId);
  }
}
