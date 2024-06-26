// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.build.ui.BuildContentManager;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.localize.BuildLocalize;
import consulo.build.ui.process.BuildProcessHandler;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ExecutionUtil;
import consulo.execution.impl.internal.ui.BaseContentCloseListener;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.ide.impl.idea.util.ContentUtilEx;
import consulo.language.LangBundle;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.internal.StartupManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.TabbedContent;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.util.ContentUtilEx.getFullName;

/**
 * @author Vladislav.Soroka
 */
@Singleton
@ServiceImpl
public final class BuildContentManagerImpl implements BuildContentManager {
  /**
   * @deprecated use Build_Tab_Title_Supplier instead
   */
  @SuppressWarnings("SSBasedInspection")
  // @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @Deprecated
  public static final String Build = BuildLocalize.tabTitleBuild().get();

  public static final Supplier<String> Build_Tab_Title_Supplier = () -> BuildLocalize.tabTitleBuild().get();

  private static final List<Supplier<String>> ourPresetOrder = Arrays.asList(
    LangBundle.messagePointer("tab.title.sync"),
    Build_Tab_Title_Supplier,
    LangBundle.messagePointer("tab.title.run"),
    LangBundle.messagePointer("tab.title.debug")
  );

  private static final Key<Map<Object, CloseListener>> CONTENT_CLOSE_LISTENERS = Key.create("CONTENT_CLOSE_LISTENERS");

  private final Project myProject;
  private final Map<Content, Pair<Image, AtomicInteger>> liveContentsMap = new ConcurrentHashMap<>();

  @Inject
  public BuildContentManagerImpl(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  @Nonnull
  public ToolWindow getOrCreateToolWindow() {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    if (toolWindow != null) {
      return toolWindow;
    }

    toolWindow = toolWindowManager.registerToolWindow(
      TOOL_WINDOW_ID,
      true,
      ToolWindowAnchor.BOTTOM,
      false
    );
    toolWindow.setIcon(PlatformIconGroup.toolwindowsToolwindowbuild());
    ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addDataProvider(new DataProvider() {
      private int myInsideGetData = 0;

      @Override
      public Object getData(@Nonnull Key dataId) {
        myInsideGetData++;
        try {
          return myInsideGetData == 1
            ? DataManager.getInstance().getDataContext(contentManager.getComponent()).getData(dataId) : null;
        }
        finally {
          myInsideGetData--;
        }
      }
    });

    ContentManagerWatcher.watchContentManager(toolWindow, contentManager);
    return toolWindow;
  }

  private void invokeLaterIfNeeded(@Nonnull Runnable runnable) {
    if (myProject.isDefault()) {
      return;
    }
    StartupManagerEx.getInstanceEx(myProject).runAfterOpened(() -> {
      GuiUtils.invokeLaterIfNeeded(runnable, IdeaModalityState.defaultModalityState(), myProject.getDisposed());
    });
  }

  @Override
  public void addContent(Content content) {
    invokeLaterIfNeeded(() -> {
      ContentManager contentManager = getOrCreateToolWindow().getContentManager();
      final String name = content.getTabName();
      final String category = StringUtil.trimEnd(StringUtil.split(name, " ").get(0), ':');
      int idx = -1;
      for (int i = 0; i < ourPresetOrder.size(); i++) {
        final String s = ourPresetOrder.get(i).get();
        if (s.equals(category)) {
          idx = i;
          break;
        }
      }
      final Content[] existingContents = contentManager.getContents();
      if (idx != -1) {
        MultiMap<String, String> existingCategoriesNames = new MultiMap<>();
        for (Content existingContent : existingContents) {
          String tabName = existingContent.getTabName();
          existingCategoriesNames.putValue(
            StringUtil.trimEnd(StringUtil.split(tabName, " ").get(0), ':'),
            tabName
          );
        }

        int place = 0;
        for (int i = 0; i <= idx; i++) {
          String key = ourPresetOrder.get(i).get();
          Collection<String> tabNames = existingCategoriesNames.get(key);
          place += tabNames.size();
        }
        contentManager.addContent(content, place);
      }
      else {
        contentManager.addContent(content);
      }

      for (Content existingContent : existingContents) {
        existingContent.setDisplayName(existingContent.getTabName());
      }
      String tabName = content.getTabName();
      updateTabDisplayName(content, tabName);
    });
  }

  public void updateTabDisplayName(Content content, String tabName) {
    invokeLaterIfNeeded(() -> {
      if (!tabName.equals(content.getDisplayName())) {
        // we are going to adjust display name, so we need to ensure tab name is not retrieved based on display name
        content.setTabName(tabName);
        content.setDisplayName(tabName);
      }
    });
  }

  @Override
  public void removeContent(Content content) {
    invokeLaterIfNeeded(() -> {
      ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(TOOL_WINDOW_ID);
      ContentManager contentManager = toolWindow == null ? null : toolWindow.getContentManager();
      if (contentManager != null && (!contentManager.isDisposed())) {
        contentManager.removeContent(content, true);
      }
    });
  }

  @Override
  public void setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus, boolean activate, @Nullable Runnable activationCallback) {
    invokeLaterIfNeeded(() -> {
      ToolWindow toolWindow = getOrCreateToolWindow();
      if (!toolWindow.isAvailable()) {
        return;
      }
      if (activate) {
        toolWindow.show(activationCallback);
      }
      toolWindow.getContentManager().setSelectedContent(content, requestFocus, forcedFocus, false);
    });
  }

  @Override
  public Content addTabbedContent(
    @Nonnull JComponent contentComponent,
    @Nonnull String groupPrefix,
    @Nonnull String tabName,
    @Nullable Image icon,
    @Nullable Disposable childDisposable
  ) {
    ContentManager contentManager = getOrCreateToolWindow().getContentManager();
    ContentUtilEx.addTabbedContent(contentManager, contentComponent, groupPrefix, tabName, false, childDisposable);
    Content content = contentManager.findContent(getFullName(groupPrefix, tabName));
    if (icon != null) {
      TabbedContent tabbedContent = ContentUtilEx.findTabbedContent(contentManager, groupPrefix);
      if (tabbedContent != null) {
        tabbedContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        tabbedContent.setIcon(icon);
      }
    }
    return content;
  }

  public void startBuildNotified(
    @Nonnull BuildDescriptor buildDescriptor,
    @Nonnull Content content,
    @Nullable BuildProcessHandler processHandler
  ) {
    if (processHandler != null) {
      Map<Object, CloseListener> closeListenerMap = content.getUserData(CONTENT_CLOSE_LISTENERS);
      if (closeListenerMap == null) {
        closeListenerMap = new HashMap<>();
        content.putUserData(CONTENT_CLOSE_LISTENERS, closeListenerMap);
      }
      closeListenerMap.put(buildDescriptor.getId(), new CloseListener(content, processHandler));
    }
    Pair<Image, AtomicInteger> pair = liveContentsMap.computeIfAbsent(content, c -> Pair.pair(c.getIcon(), new AtomicInteger(0)));
    pair.second.incrementAndGet();
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    if (pair.first == null) {
      content.putUserData(Content.TAB_LABEL_ORIENTATION_KEY, ComponentOrientation.RIGHT_TO_LEFT);
    }
    content.setIcon(ExecutionUtil.getIconWithLiveIndicator(
      pair.first == null ? PlatformIconGroup.toolwindowsToolwindowbuild() : pair.first
    ));
    invokeLaterIfNeeded(() -> {
      JComponent component = content.getComponent();
      component.invalidate();
      if (!liveContentsMap.isEmpty()) {
        getOrCreateToolWindow().setIcon(ExecutionUtil.getIconWithLiveIndicator(
          PlatformIconGroup.toolwindowsToolwindowbuild()
        ));
      }
    });
  }

  public void finishBuildNotified(@Nonnull BuildDescriptor buildDescriptor, @Nonnull Content content) {
    Map<Object, CloseListener> closeListenerMap = content.getUserData(CONTENT_CLOSE_LISTENERS);
    if (closeListenerMap != null) {
      CloseListener closeListener = closeListenerMap.remove(buildDescriptor.getId());
      if (closeListener != null) {
        closeListener.dispose();
        if (closeListenerMap.isEmpty()) {
          content.putUserData(CONTENT_CLOSE_LISTENERS, null);
        }
      }
    }

    Pair<Image, AtomicInteger> pair = liveContentsMap.get(content);
    if (pair != null && pair.second.decrementAndGet() == 0) {
      content.setIcon(pair.first);
      if (pair.first == null) {
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.FALSE);
      }
      liveContentsMap.remove(content);
    }

    invokeLaterIfNeeded(() -> {
      if (liveContentsMap.isEmpty()) {
        getOrCreateToolWindow().setIcon(PlatformIconGroup.toolwindowsToolwindowbuild());
      }
    });
  }

  private final class CloseListener extends BaseContentCloseListener {
    private
    @Nullable
    BuildProcessHandler myProcessHandler;

    private CloseListener(final @Nonnull Content content, @Nonnull BuildProcessHandler processHandler) {
      super(content, myProject);
      myProcessHandler = processHandler;
    }

    @Override
    protected void disposeContent(@Nonnull Content content) {
      if (myProcessHandler instanceof Disposable) {
        Disposer.dispose((Disposable)myProcessHandler);
      }
      myProcessHandler = null;
    }

    @Override
    protected boolean closeQuery(@Nonnull Content content, boolean modal) {
      if (myProcessHandler == null || myProcessHandler.isProcessTerminated() || myProcessHandler.isProcessTerminating()) {
        return true;
      }
      myProcessHandler.putUserData(RunContentManagerImpl.ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY, Boolean.TRUE);
      final String sessionName = myProcessHandler.getExecutionName();
      final WaitForProcessTask task = new WaitForProcessTask(myProcessHandler, sessionName, modal, myProject) {
        @Override
        public void onCancel() {
          // stop waiting for the process
          myProcessHandler.forceProcessDetach();
        }
      };
      return askUserAndWait(myProcessHandler, sessionName, task);
    }
  }
}
