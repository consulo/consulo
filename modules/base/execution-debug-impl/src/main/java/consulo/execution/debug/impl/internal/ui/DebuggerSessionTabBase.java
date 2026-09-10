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
package consulo.execution.debug.impl.internal.ui;

import consulo.logging.Logger;
import consulo.application.ApplicationManager;
import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerContentInfo;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.runner.RunTab;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.ObservableConsoleView;
import consulo.execution.ui.layout.LayoutAttractionPolicy;
import consulo.execution.ui.layout.LayoutViewOptions;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
public abstract class DebuggerSessionTabBase extends RunTab {
    private static final Logger LOG = Logger.getInstance(DebuggerSessionTabBase.class);

  protected ExecutionConsole myConsole;

  public DebuggerSessionTabBase(Project project, String runnerId, String sessionName, GlobalSearchScope searchScope) {
    super(project, searchScope, runnerId, XDebuggerLocalize.xdebuggerDefaultContentTitle().get(), sessionName);

    myUi.getDefaults().initTabDefaults(0, XDebuggerLocalize.xdebuggerThreadsVarsTabTitle().get(), ExecutionDebugIconGroup.actionStartdebugger())
            .initFocusContent(DebuggerContentInfo.FRAME_CONTENT, XDebuggerUIConstants.LAYOUT_VIEW_BREAKPOINT_CONDITION)
            .initFocusContent(DebuggerContentInfo.CONSOLE_CONTENT, LayoutViewOptions.STARTUP, new LayoutAttractionPolicy.FocusOnce(false));
  }

  public static CompletableFuture<@Nullable ActionGroup> getCustomizedActionGroupAsync(String id) {
    return CustomActionsSchema.getCorrectedGroupAsync(id);
  }

  /**
   * Fills a toolbar group from the customization schema, which resolves asynchronously. The target group is
   * returned right away and filled once the schema answers, so the toolbar picks the actions up on its next update.
   */
  public static DefaultActionGroup customizedActionGroup(String id) {
    DefaultActionGroup group = new DefaultActionGroup();
    addCustomizedActions(group, id);
    return group;
  }

  public static void addCustomizedActions(DefaultActionGroup target, String id) {
    getCustomizedActionGroupAsync(id).whenComplete((group, throwable) -> {
      if (throwable != null) {
        LOG.error("Failed to resolve a debugger toolbar group", throwable);
        return;
      }
      if (group != null) {
        target.addAll(group);
      }
    });
  }

  protected void attachNotificationTo(Content content) {
    if (myConsole instanceof ObservableConsoleView) {
      ObservableConsoleView observable = (ObservableConsoleView)myConsole;
      observable.addChangeListener(new ObservableConsoleView.ChangeListener() {
          @Override
          public void contentAdded(Collection<ConsoleViewContentType> types) {
              if (types.contains(ConsoleViewContentType.ERROR_OUTPUT) || types.contains(ConsoleViewContentType.NORMAL_OUTPUT)) {
                  content.fireAlert();
              }
          }
      }, content);

      RunProfile profile = getRunProfile();

      if (profile instanceof RunConfigurationBase && !ApplicationManager.getApplication().isUnitTestMode()) {
        observable.addChangeListener(
                new RunContentBuilder.ConsoleToFrontListener((RunConfigurationBase)profile, myProject, DefaultDebugExecutor.getDebugExecutorInstance(),
                                                             myRunContentDescriptor, myUi), content);
      }
    }
  }

  protected @Nullable RunProfile getRunProfile() {
    return myEnvironment != null ? myEnvironment.getRunProfile() : null;
  }

  public void select() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    UIUtil.invokeLaterIfNeeded(() -> {
      if (myRunContentDescriptor != null) {
        RunContentManager manager = ExecutionManager.getInstance(myProject).getContentManager();
        ToolWindow toolWindow = manager.getToolWindowByDescriptor(myRunContentDescriptor);
        Content content = myRunContentDescriptor.getAttachedContent();
        if (toolWindow == null || content == null) return;
        manager.selectRunContent(myRunContentDescriptor);
      }
    });
  }
}
