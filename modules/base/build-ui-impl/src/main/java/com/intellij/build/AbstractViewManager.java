// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.events.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.LangBundle;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AtomicClearableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.SystemNotifications;
import com.intellij.ui.UIBundle;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.DisposableWrapperList;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.build.ExecutionNode.getEventResultIcon;

/**
 * Provides base implementation of the {@link ViewManager}
 *
 * @author Vladislav.Soroka
 */
public abstract class AbstractViewManager implements ViewManager, BuildProgressListener, BuildProgressObservable, Disposable {
  private static final Logger LOG = Logger.getInstance(ViewManager.class);
  private static final Key<Boolean> PINNED_EXTRACTED_CONTENT = new Key<>("PINNED_EXTRACTED_CONTENT");

  protected final Project myProject;
  protected final BuildContentManager myBuildContentManager;
  private final AtomicClearableLazyValue<MultipleBuildsView> myBuildsViewValue;
  private final Set<MultipleBuildsView> myPinnedViews;
  private final AtomicBoolean isDisposed = new AtomicBoolean(false);
  private final DisposableWrapperList<BuildProgressListener> myListeners = new DisposableWrapperList<>();

  public AbstractViewManager(Project project, BuildContentManager buildContentManager) {
    myProject = project;
    myBuildContentManager = buildContentManager;
    myBuildsViewValue = new AtomicClearableLazyValue<MultipleBuildsView>() {
      @Override
      @Nonnull
      protected MultipleBuildsView compute() {
        MultipleBuildsView buildsView = new MultipleBuildsView(myProject, myBuildContentManager, AbstractViewManager.this);
        Disposer.register(AbstractViewManager.this, buildsView);
        return buildsView;
      }
    };
    myPinnedViews = ContainerUtil.newConcurrentSet();
    //@Nullable BuildViewProblemsService buildViewProblemsService = project.getService(BuildViewProblemsService.class);
    //if (buildViewProblemsService != null) {
    //  buildViewProblemsService.listenToBuildView(this);
    //}
  }

  @Override
  public boolean isConsoleEnabledByDefault() {
    return false;
  }

  @Override
  public boolean isBuildContentView() {
    return true;
  }

  @Override
  //@ApiStatus.Experimental
  public void addListener(@Nonnull BuildProgressListener listener, @Nonnull Disposable disposable) {
    myListeners.add(listener, disposable);
  }

  @Nonnull
  protected abstract String getViewName();

  protected Map<BuildDescriptor, BuildView> getBuildsMap() {
    return myBuildsViewValue.getValue().getBuildsMap();
  }

  @Override
  public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    if (isDisposed.get()) return;

    MultipleBuildsView buildsView;
    if (event instanceof StartBuildEvent) {
      configurePinnedContent();
      buildsView = myBuildsViewValue.getValue();
    }
    else {
      buildsView = getMultipleBuildsView(buildId);
    }
    if (buildsView != null) {
      buildsView.onEvent(buildId, event);
    }

    for (BuildProgressListener listener : myListeners) {
      try {
        listener.onEvent(buildId, event);
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    }
  }

  private
  @Nullable
  MultipleBuildsView getMultipleBuildsView(@Nonnull Object buildId) {
    MultipleBuildsView buildsView = myBuildsViewValue.getValue();
    if (!buildsView.shouldConsume(buildId)) {
      buildsView = ContainerUtil.find(myPinnedViews, pinnedView -> pinnedView.shouldConsume(buildId));
    }
    return buildsView;
  }

  //@ApiStatus.Internal
  @Nullable
  public BuildView getBuildView(@Nonnull Object buildId) {
    MultipleBuildsView buildsView = getMultipleBuildsView(buildId);
    if (buildsView == null) return null;

    return buildsView.getBuildView(buildId);
  }

  void configureToolbar(@Nonnull DefaultActionGroup toolbarActions, @Nonnull MultipleBuildsView buildsView, @Nonnull BuildView view) {
    toolbarActions.removeAll();
    toolbarActions.addAll(view.createConsoleActions());
    toolbarActions.add(new PinBuildViewAction(buildsView));
    toolbarActions.add(BuildTreeFilters.createFilteringActionsGroup(view));
  }

  @Nullable
  protected Image getContentIcon() {
    return null;
  }

  protected void onBuildStart(BuildDescriptor buildDescriptor) {
  }

  protected void onBuildFinish(BuildDescriptor buildDescriptor) {
    BuildInfo buildInfo = (BuildInfo)buildDescriptor;
    if (buildInfo.result instanceof FailureResult) {
      boolean activate = buildInfo.isActivateToolWindowWhenFailed();
      myBuildContentManager.setSelectedContent(buildInfo.content, false, false, activate, null);
      List<? extends Failure> failures = ((FailureResult)buildInfo.result).getFailures();
      if (failures.isEmpty()) return;
      Failure failure = failures.get(0);
      Notification notification = failure.getNotification();
      if (notification != null) {
        String title = notification.getTitle();
        String content = notification.getContent();
        SystemNotifications.getInstance().notify(UIBundle.message("tool.window.name.build"), title, content);
      }
    }
  }

  @Override
  public void dispose() {
    isDisposed.set(true);
    myPinnedViews.clear();
    myBuildsViewValue.drop();
  }

  void onBuildsViewRemove(@Nonnull MultipleBuildsView buildsView) {
    if (isDisposed.get()) return;

    if (myBuildsViewValue.getValue() == buildsView) {
      myBuildsViewValue.drop();
    }
    else {
      myPinnedViews.remove(buildsView);
    }
  }

  static class BuildInfo extends DefaultBuildDescriptor {
    @BuildEventsNls.Message String message;
    @BuildEventsNls.Message String statusMessage;
    long endTime = -1;
    EventResult result;
    Content content;

    BuildInfo(@Nonnull BuildDescriptor descriptor) {
      super(descriptor);
    }

    public Image getIcon() {
      return getEventResultIcon(result);
    }

    public boolean isRunning() {
      return endTime == -1;
    }
  }

  private void configurePinnedContent() {
    MultipleBuildsView buildsView = myBuildsViewValue.getValue();
    Content content = buildsView.getContent();
    if (content != null && content.isPinned()) {
      String tabName = getPinnedTabName(buildsView);
      UIUtil.invokeLaterIfNeeded(() -> {
        content.setPinnable(false);
        if (content.getIcon() == null) {
          content.setIcon(Image.empty(8));
        }
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        ((BuildContentManagerImpl)myBuildContentManager).updateTabDisplayName(content, tabName);
      });
      myPinnedViews.add(buildsView);
      myBuildsViewValue.drop();
      content.putUserData(PINNED_EXTRACTED_CONTENT, Boolean.TRUE);
    }
  }

  private String getPinnedTabName(MultipleBuildsView buildsView) {
    Map<BuildDescriptor, BuildView> buildsMap = buildsView.getBuildsMap();

    BuildDescriptor buildInfo = buildsMap.keySet().stream().reduce((b1, b2) -> b1.getStartTime() <= b2.getStartTime() ? b1 : b2).orElse(null);
    if (buildInfo != null) {
      @BuildEventsNls.Title String title = buildInfo.getTitle();
      String viewName = getViewName().split(" ")[0];
      String tabName = viewName + ": " + StringUtil.trimStart(title, viewName);
      if (buildsMap.size() > 1) {
        return LangBundle.message("tab.title.more", tabName, buildsMap.size() - 1);
      }
      return tabName;
    }
    return getViewName();
  }

  private static class PinBuildViewAction extends DumbAwareAction implements Toggleable {
    private final Content myContent;

    PinBuildViewAction(MultipleBuildsView buildsView) {
      myContent = buildsView.getContent();
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      boolean selected = !myContent.isPinned();
      if (selected) {
        myContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
      }
      myContent.setPinned(selected);
      Toggleable.setSelected(e.getPresentation(), selected);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (!myContent.isValid()) return;
      Boolean isPinnedAndExtracted = myContent.getUserData(PINNED_EXTRACTED_CONTENT);
      if (isPinnedAndExtracted == Boolean.TRUE) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      ContentManager contentManager = myContent.getManager();
      boolean isActiveTab = contentManager != null && contentManager.getSelectedContent() == myContent;
      boolean selected = myContent.isPinned();

      e.getPresentation().setIcon(AllIcons.General.Pin_tab);
      Toggleable.setSelected(e.getPresentation(), selected);

      String text;
      if (!isActiveTab) {
        text = selected ? IdeBundle.message("action.unpin.active.tab") : IdeBundle.message("action.pin.active.tab");
      }
      else {
        text = selected ? IdeBundle.message("action.unpin.tab") : IdeBundle.message("action.pin.tab");
      }
      e.getPresentation().setText(text);
      e.getPresentation().setEnabledAndVisible(true);
    }
  }
}
