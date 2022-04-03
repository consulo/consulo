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
package com.intellij.xdebugger.impl.ui;

import consulo.debugger.ui.DebuggerContentInfo;
import consulo.debugger.ui.XDebuggerUIConstants;
import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.runner.RunTab;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.ObservableConsoleView;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.layout.LayoutAttractionPolicy;
import consulo.execution.ui.layout.LayoutViewOptions;
import com.intellij.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ui.ex.action.ActionGroup;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.awt.UIUtil;
import consulo.debugger.XDebuggerBundle;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public abstract class DebuggerSessionTabBase extends RunTab {
  protected ExecutionConsole myConsole;

  public DebuggerSessionTabBase(@Nonnull Project project, @Nonnull String runnerId, @Nonnull String sessionName, @Nonnull GlobalSearchScope searchScope) {
    super(project, searchScope, runnerId, XDebuggerBundle.message("xdebugger.default.content.title"), sessionName);

    myUi.getDefaults().initTabDefaults(0, XDebuggerBundle.message("xdebugger.debugger.tab.title"), PlatformIconGroup.actionsStartdebugger())
            .initFocusContent(DebuggerContentInfo.FRAME_CONTENT, XDebuggerUIConstants.LAYOUT_VIEW_BREAKPOINT_CONDITION)
            .initFocusContent(DebuggerContentInfo.CONSOLE_CONTENT, LayoutViewOptions.STARTUP, new LayoutAttractionPolicy.FocusOnce(false));
  }

  public static ActionGroup getCustomizedActionGroup(final String id) {
    return (ActionGroup)CustomActionsSchemaImpl.getInstance().getCorrectedAction(id);
  }

  protected void attachNotificationTo(final Content content) {
    if (myConsole instanceof ObservableConsoleView) {
      ObservableConsoleView observable = (ObservableConsoleView)myConsole;
      observable.addChangeListener(types -> {
        if (types.contains(ConsoleViewContentType.ERROR_OUTPUT) || types.contains(ConsoleViewContentType.NORMAL_OUTPUT)) {
          content.fireAlert();
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

  @Nullable
  protected RunProfile getRunProfile() {
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
