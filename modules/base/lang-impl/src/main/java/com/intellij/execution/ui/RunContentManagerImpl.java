/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.ui;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.dashboard.RunDashboardContributor;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.layout.impl.DockableGridContainerFactory;
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.docking.DockManager;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.impl.RunToolWindowManager;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RunContentManagerImpl implements RunContentManager, Disposable {
  public static final Key<Boolean> ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY = Key.create("ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY");
  private static final Logger LOG = Logger.getInstance(RunContentManagerImpl.class);
  private static final Key<Executor> EXECUTOR_KEY = Key.create("Executor");

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Provider<ToolWindowManager> myToolWindowManager;

  private final RunToolWindowManager myRunToolWindowManager;

  public RunContentManagerImpl(@Nonnull Project project, @Nonnull Provider<ToolWindowManager> toolWindowManager, @Nonnull DockManager dockManager) {
    myProject = project;
    myToolWindowManager = toolWindowManager;
    myRunToolWindowManager = new RunToolWindowManager(project, myToolWindowManager, this);

    DockableGridContainerFactory containerFactory = new DockableGridContainerFactory();
    dockManager.register(DockableGridContainerFactory.TYPE, containerFactory);
    Disposer.register(myProject, containerFactory);
  }

  @Override
  public void dispose() {
  }

  private RunContentWithExecutorListener getSyncPublisher() {
    return myProject.getMessageBus().syncPublisher(TOPIC);
  }

  @Override
  public void toFrontRunContent(final Executor requestor, final ProcessHandler handler) {
    final RunContentDescriptor descriptor = getDescriptorBy(handler, requestor);
    if (descriptor == null) {
      return;
    }
    toFrontRunContent(requestor, descriptor);
  }

  @Override
  public void toFrontRunContent(final Executor requestor, final RunContentDescriptor descriptor) {
    myProject.getApplication().invokeLater(() -> {
      ContentManager contentManager = getContentManagerForRunner(requestor, descriptor);
      Content content = getRunContentByDescriptor(contentManager, descriptor);
      if (content != null) {
        contentManager.setSelectedContent(content);
        myToolWindowManager.get().getToolWindow(getToolWindowIdForRunner(requestor, descriptor)).show(null);
      }
    }, myProject.getDisposed());
  }

  @Override
  public void hideRunContent(@Nonnull final Executor executor, final RunContentDescriptor descriptor) {
    myProject.getApplication().invokeLater(() -> {
      ToolWindow toolWindow = myToolWindowManager.get().getToolWindow(getToolWindowIdForRunner(executor, descriptor));
      if (toolWindow != null) {
        toolWindow.hide(null);
      }
    }, myProject.getDisposed());
  }

  @Override
  @Nullable
  public RunContentDescriptor getSelectedContent(final Executor executor) {
    final Content selectedContent = getContentManagerForRunner(executor, null).getSelectedContent();
    return selectedContent != null ? getRunContentDescriptorByContent(selectedContent) : null;
  }

  @Override
  @Nullable
  public RunContentDescriptor getSelectedContent() {
    for (String activeWindow : myRunToolWindowManager.getToolwindowIdZBuffer()) {
      final ContentManager contentManager = myRunToolWindowManager.get(activeWindow);
      if (contentManager == null) {
        continue;
      }

      final Content selectedContent = contentManager.getSelectedContent();
      if (selectedContent == null) {
        if (contentManager.getContentCount() == 0) {
          // continue to the next window if the content manager is empty
          continue;
        }
        else {
          // stop iteration over windows because there is some content in the window and the window is the last used one
          break;
        }
      }
      // here we have selected content
      return getRunContentDescriptorByContent(selectedContent);
    }

    return null;
  }

  @Override
  public boolean removeRunContent(@Nonnull final Executor executor, final RunContentDescriptor descriptor) {
    final ContentManager contentManager = getContentManagerForRunner(executor, descriptor);
    final Content content = getRunContentByDescriptor(contentManager, descriptor);
    return content != null && contentManager.removeContent(content, true);
  }

  @Override
  public void showRunContent(@Nonnull Executor executor, @Nonnull RunContentDescriptor descriptor) {
    showRunContent(executor, descriptor, descriptor.getExecutionId());
  }

  private void showRunContent(@Nonnull final Executor executor, @Nonnull final RunContentDescriptor descriptor, final long executionId) {
    if (myProject.getApplication().isUnitTestMode()) {
      return;
    }

    final ContentManager contentManager = getContentManagerForRunner(executor, descriptor);
    RunContentDescriptor oldDescriptor = chooseReuseContentForDescriptor(contentManager, descriptor, executionId, descriptor.getDisplayName());
    final Content content;
    if (oldDescriptor == null) {
      content = createNewContent(descriptor, executor);
    }
    else {
      content = oldDescriptor.getAttachedContent();
      LOG.assertTrue(content != null);
      getSyncPublisher().contentRemoved(oldDescriptor, executor);
      Disposer.dispose(oldDescriptor); // is of the same category, can be reused
    }

    content.setExecutionId(executionId);
    content.setComponent(descriptor.getComponent());
    content.setPreferredFocusedComponent(descriptor.getPreferredFocusComputable());
    content.putUserData(RunContentDescriptor.DESCRIPTOR_KEY, descriptor);
    content.putUserData(EXECUTOR_KEY, executor);
    content.setDisplayName(descriptor.getDisplayName());
    descriptor.setAttachedContent(content);

    String toolWindowId = getToolWindowIdForRunner(executor, descriptor);
    final ToolWindow toolWindow = myToolWindowManager.get().getToolWindow(toolWindowId);
    final ProcessHandler processHandler = descriptor.getProcessHandler();
    if (processHandler != null) {
      final ProcessAdapter processAdapter = new ProcessAdapter() {
        @Override
        public void startNotified(final ProcessEvent event) {
          UIUtil.invokeLaterIfNeeded(() -> {
            content.setIcon(ExecutionUtil.getIconWithLiveIndicator(descriptor.getIcon()));
            toolWindow.setIcon(ExecutionUtil.getIconWithLiveIndicator(myRunToolWindowManager.getImage(toolWindowId)));
          });
        }

        @Override
        public void processTerminated(final ProcessEvent event) {
          Application.get().invokeLater(() -> {
            boolean alive = false;
            ContentManager manager = myRunToolWindowManager.get(toolWindowId);
            if (manager == null) return;
            for (Content content1 : manager.getContents()) {
              RunContentDescriptor descriptor1 = getRunContentDescriptorByContent(content1);
              if (descriptor1 != null) {
                ProcessHandler handler = descriptor1.getProcessHandler();
                if (handler != null && !handler.isProcessTerminated()) {
                  alive = true;
                  break;
                }
              }
            }
            Image base = myRunToolWindowManager.getImage(toolWindowId);
            toolWindow.setIcon(alive ? ExecutionUtil.getIconWithLiveIndicator(base) : base);

            Image icon = descriptor.getIcon();
            content.setIcon(icon == null ? executor.getDisabledIcon() : ImageEffects.transparent(icon));
          });
        }
      };
      processHandler.addProcessListener(processAdapter);
      final Disposable disposer = content.getDisposer();
      if (disposer != null) {
        Disposer.register(disposer, new Disposable() {
          @Override
          public void dispose() {
            processHandler.removeProcessListener(processAdapter);
          }
        });
      }
    }

    if (oldDescriptor == null) {
      contentManager.addContent(content);
      new CloseListener(myProject, content, executor);
    }
    content.getManager().setSelectedContent(content);

    if (!descriptor.isActivateToolWindowWhenAdded()) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
      // let's activate tool window, but don't move focus
      //
      // window.show() isn't valid here, because it will not
      // mark the window as "last activated" windows and thus
      // some action like navigation up/down in stacktrace wont
      // work correctly
      descriptor.getPreferredFocusComputable();
      window.activate(descriptor.getActivationCallback(), descriptor.isAutoFocusContent(), descriptor.isAutoFocusContent());
    }, myProject.getDisposed());
  }


  @Nullable
  @Override
  public RunContentDescriptor getReuseContent(@Nonnull ExecutionEnvironment executionEnvironment) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;
    RunContentDescriptor contentToReuse = executionEnvironment.getContentToReuse();
    if (contentToReuse != null) {
      return contentToReuse;
    }

    // TODO [konstantin.aleev] Should content be reused in case of dashboard?
    String toolWindowId = getContentDescriptorToolWindowId(executionEnvironment.getRunnerAndConfigurationSettings());
    final ContentManager contentManager = toolWindowId == null ? getContentManagerForRunner(executionEnvironment.getExecutor(), null) : myRunToolWindowManager.get(toolWindowId);
    return chooseReuseContentForDescriptor(contentManager, null, executionEnvironment.getExecutionId(), executionEnvironment.toString());
  }

  @Override
  public RunContentDescriptor findContentDescriptor(final Executor requestor, final ProcessHandler handler) {
    return getDescriptorBy(handler, requestor);
  }

  @Override
  public void showRunContent(@Nonnull Executor info, @Nonnull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse) {
    copyContentAndBehavior(descriptor, contentToReuse);
    showRunContent(info, descriptor, descriptor.getExecutionId());
  }

  public static void copyContentAndBehavior(@Nonnull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse) {
    if (contentToReuse != null) {
      Content attachedContent = contentToReuse.getAttachedContent();
      if (attachedContent != null && attachedContent.isValid()) {
        descriptor.setAttachedContent(attachedContent);
      }
      if (contentToReuse.isReuseToolWindowActivation()) {
        descriptor.setActivateToolWindowWhenAdded(contentToReuse.isActivateToolWindowWhenAdded());
      }
      descriptor.setContentToolWindowId(contentToReuse.getContentToolWindowId());
    }
  }

  @Nullable
  private static RunContentDescriptor chooseReuseContentForDescriptor(@Nonnull ContentManager contentManager,
                                                                      @Nullable RunContentDescriptor descriptor,
                                                                      long executionId,
                                                                      @Nullable String preferredName) {
    Content content = null;
    if (descriptor != null) {
      //Stage one: some specific descriptors (like AnalyzeStacktrace) cannot be reused at all
      if (descriptor.isContentReuseProhibited()) {
        return null;
      }
      //Stage two: try to get content from descriptor itself
      final Content attachedContent = descriptor.getAttachedContent();

      if (attachedContent != null &&
          attachedContent.isValid() &&
          contentManager.getIndexOfContent(attachedContent) != -1 &&
          (Comparing.equal(descriptor.getDisplayName(), attachedContent.getDisplayName()) || !attachedContent.isPinned())) {
        content = attachedContent;
      }
    }
    //Stage three: choose the content with name we prefer
    if (content == null) {
      content = getContentFromManager(contentManager, preferredName, executionId);
    }
    if (content == null || !isTerminated(content) || (content.getExecutionId() == executionId && executionId != 0)) {
      return null;
    }
    final RunContentDescriptor oldDescriptor = getRunContentDescriptorByContent(content);
    if (oldDescriptor != null && !oldDescriptor.isContentReuseProhibited()) {
      //content.setExecutionId(executionId);
      return oldDescriptor;
    }

    return null;
  }

  @Nullable
  private static Content getContentFromManager(ContentManager contentManager, @Nullable String preferredName, long executionId) {
    ArrayList<Content> contents = new ArrayList<>(Arrays.asList(contentManager.getContents()));
    Content first = contentManager.getSelectedContent();
    if (first != null && contents.remove(first)) {//selected content should be checked first
      contents.add(0, first);
    }
    if (preferredName != null) {//try to match content with specified preferred name
      for (Content c : contents) {
        if (canReuseContent(c, executionId) && preferredName.equals(c.getDisplayName())) {
          return c;
        }
      }
    }
    for (Content c : contents) {//return first "good" content
      if (canReuseContent(c, executionId)) {
        return c;
      }
    }
    return null;
  }

  private static boolean canReuseContent(Content c, long executionId) {
    return c != null && !c.isPinned() && isTerminated(c) && !(c.getExecutionId() == executionId && executionId != 0);
  }

  @Nonnull
  private ContentManager getContentManagerForRunner(final Executor executor, final RunContentDescriptor descriptor) {
    final ContentManager contentManager = myRunToolWindowManager.get(getToolWindowIdForRunner(executor, descriptor));
    if (contentManager == null) {
      LOG.error("Runner " + executor.getId() + " is not registered");
    }
    //noinspection ConstantConditions
    return contentManager;
  }

  private static String getToolWindowIdForRunner(final Executor executor, final RunContentDescriptor descriptor) {
    if (descriptor != null && descriptor.getContentToolWindowId() != null) {
      return descriptor.getContentToolWindowId();
    }
    return executor.getToolWindowId();
  }

  private static Content createNewContent(final RunContentDescriptor descriptor, Executor executor) {
    final String processDisplayName = descriptor.getDisplayName();
    final Content content = ContentFactory.getInstance().createContent(descriptor.getComponent(), processDisplayName, true);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    Image icon = descriptor.getIcon();
    content.setIcon(icon == null ? executor.getToolWindowIcon() : icon);
    return content;
  }

  public static boolean isTerminated(@Nonnull Content content) {
    RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    return processHandler == null || processHandler.isProcessTerminated();
  }

  @Nullable
  public static RunContentDescriptor getRunContentDescriptorByContent(@Nonnull Content content) {
    return content.getUserData(RunContentDescriptor.DESCRIPTOR_KEY);
  }

  @Nullable
  public static Executor getExecutorByContent(@Nonnull Content content) {
    return content.getUserData(EXECUTOR_KEY);
  }

  @Override
  @Nullable
  public ToolWindow getToolWindowByDescriptor(@Nonnull RunContentDescriptor descriptor) {
    for (Map.Entry<String, ContentManager> entry : myRunToolWindowManager.entrySet()) {
      if (getRunContentByDescriptor(entry.getValue(), descriptor) != null) {
        return ToolWindowManager.getInstance(myProject).getToolWindow(entry.getKey());
      }
    }
    return null;
  }

  @Nullable
  private static Content getRunContentByDescriptor(@Nonnull ContentManager contentManager, @Nonnull RunContentDescriptor descriptor) {
    for (Content content : contentManager.getContents()) {
      if (descriptor.equals(getRunContentDescriptorByContent(content))) {
        return content;
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public List<RunContentDescriptor> getAllDescriptors() {
    Set<Map.Entry<String, ContentManager>> entries = myRunToolWindowManager.entrySet();
    if (entries.isEmpty()) {
      return List.of();
    }

    List<RunContentDescriptor> descriptors = new SmartList<>();
    for (Map.Entry<String, ContentManager> entry : entries) {
      ContentManager value = entry.getValue();

      for (Content content : value.getContents()) {
        RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
        if (descriptor != null) {
          descriptors.add(descriptor);
        }
      }
    }
    return descriptors;
  }

  @Override
  public void selectRunContent(@Nonnull RunContentDescriptor descriptor) {
    for (Map.Entry<String, ContentManager> entry : myRunToolWindowManager.entrySet()) {
      Content content = getRunContentByDescriptor(entry.getValue(), descriptor);
      if (content != null) {
        entry.getValue().setSelectedContent(content);
      }
    }
  }

  @Override
  @Nullable
  public String getContentDescriptorToolWindowId(@Nullable RunnerAndConfigurationSettings settings) {
    if (settings != null) {
      ConfigurationType type = settings.getType();
      if (RunDashboardContributor.isShowInDashboard(type)) {
        return RunDashboardManager.getInstance(myProject).getToolWindowId();
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public String getToolWindowIdByEnvironment(@Nonnull ExecutionEnvironment executionEnvironment) {
    // TODO [konstantin.aleev] Check remaining places where Executor.getToolWindowId() is used
    // Also there are some places where ToolWindowId.RUN or ToolWindowId.DEBUG are used directly.
    // For example, HotSwapProgressImpl.NOTIFICATION_GROUP. All notifications for this group is shown in Debug tool window,
    // however such notifications should be shown in Run Dashboard tool window, if run content is redirected to Run Dashboard tool window.
    String toolWindowId = getContentDescriptorToolWindowId(executionEnvironment.getRunnerAndConfigurationSettings());
    return toolWindowId != null ? toolWindowId : executionEnvironment.getExecutor().getToolWindowId();
  }

  @Nullable
  private RunContentDescriptor getDescriptorBy(ProcessHandler handler, Executor runnerInfo) {
    List<Content> contents = new ArrayList<>();
    ContainerUtil.addAll(contents, getContentManagerForRunner(runnerInfo, null).getContents());
    ContainerUtil.addAll(contents, myRunToolWindowManager.get(RunDashboardManager.getInstance(myProject).getToolWindowId()).getContents());
    for (Content content : contents) {
      RunContentDescriptor runContentDescriptor = getRunContentDescriptorByContent(content);
      assert runContentDescriptor != null;
      if (runContentDescriptor.getProcessHandler() == handler) {
        return runContentDescriptor;
      }
    }
    return null;
  }

  private class CloseListener extends BaseContentCloseListener {
    private Content myContent;
    private final Executor myExecutor;

    private CloseListener(@Nonnull Project project, @Nonnull final Content content, @Nonnull Executor executor) {
      super(content, project);
      myContent = content;
      myExecutor = executor;
    }

    @Override
    protected void disposeContent(@Nonnull Content content) {
      try {
        RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
        getSyncPublisher().contentRemoved(descriptor, myExecutor);
        if (descriptor != null) {
          Disposer.dispose(descriptor);
        }
      }
      finally {
        content.release();
      }
    }

    @Override
    protected boolean closeQuery(@Nonnull Content content, boolean modal) {
      final RunContentDescriptor descriptor = getRunContentDescriptorByContent(myContent);
      if (descriptor == null) {
        return true;
      }

      final ProcessHandler processHandler = descriptor.getProcessHandler();
      if (processHandler == null || processHandler.isProcessTerminated() || processHandler.isProcessTerminating()) {
        return true;
      }
      GeneralSettings.ProcessCloseConfirmation rc = TerminateRemoteProcessDialog.show(myProject, descriptor.getDisplayName(), processHandler);
      if (rc == null) { // cancel
        return false;
      }
      boolean destroyProcess = rc == GeneralSettings.ProcessCloseConfirmation.TERMINATE;
      if (destroyProcess) {
        processHandler.destroyProcess();
      }
      else {
        processHandler.detachProcess();
      }
      waitForProcess(descriptor, modal);
      return true;
    }
  }

  private void waitForProcess(final RunContentDescriptor descriptor, final boolean modal) {
    final ProcessHandler processHandler = descriptor.getProcessHandler();
    final boolean killable = !modal && (processHandler instanceof KillableProcess) && ((KillableProcess)processHandler).canKillProcess();

    String title = ExecutionBundle.message("terminating.process.progress.title", descriptor.getDisplayName());
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, title, true) {

      {
        if (killable) {
          String cancelText = ExecutionBundle.message("terminating.process.progress.kill");
          setCancelText(cancelText);
          setCancelTooltipText(cancelText);
        }
      }

      @Override
      public boolean isConditionalModal() {
        return modal;
      }

      @Override
      public boolean shouldStartInBackground() {
        return !modal;
      }

      @Override
      public void run(@Nonnull final ProgressIndicator progressIndicator) {
        final Semaphore semaphore = new Semaphore();
        semaphore.down();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          final ProcessHandler processHandler1 = descriptor.getProcessHandler();
          try {
            if (processHandler1 != null) {
              processHandler1.waitFor();
            }
          }
          finally {
            semaphore.up();
          }
        });

        progressIndicator.setText(ExecutionBundle.message("waiting.for.vm.detach.progress.text"));
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
          @Override
          public void run() {
            while (true) {
              if (progressIndicator.isCanceled() || !progressIndicator.isRunning()) {
                semaphore.up();
                break;
              }
              try {
                //noinspection SynchronizeOnThis
                synchronized (this) {
                  //noinspection SynchronizeOnThis
                  wait(2000L);
                }
              }
              catch (InterruptedException ignore) {
              }
            }
          }
        });

        semaphore.waitFor();
      }

      @Override
      public void onCancel() {
        if (killable && !processHandler.isProcessTerminated()) {
          ((KillableProcess)processHandler).killProcess();
        }
      }
    });
  }
}