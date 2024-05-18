/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ProcessHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public interface RunContentManager {
  Key<Boolean> LIGHTWEIGHT_CONTENT_MARKER = Key.create("LightweightContent");

  @Nullable
  RunContentDescriptor getSelectedContent();

  @Nullable
  RunContentDescriptor getSelectedContent(Executor runnerInfo);

  @Nonnull
  List<RunContentDescriptor> getAllDescriptors();

  /**
   * To reduce number of open contents RunContentManager reuses
   * some of them during showRunContent (for ex. if a process was stopped)
   */
  @RequiredUIAccess
  @Nullable
  RunContentDescriptor getReuseContent(@Nonnull ExecutionEnvironment executionEnvironment);

  @Nullable
  RunContentDescriptor findContentDescriptor(Executor requestor, ProcessHandler handler);

  void showRunContent(@Nonnull Executor executor, @Nonnull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse);

  void showRunContent(@Nonnull Executor executor, @Nonnull RunContentDescriptor descriptor);

  void hideRunContent(@Nonnull Executor executor, RunContentDescriptor descriptor);

  boolean removeRunContent(@Nonnull Executor executor, RunContentDescriptor descriptor);

  void toFrontRunContent(Executor requestor, RunContentDescriptor descriptor);

  void toFrontRunContent(Executor requestor, ProcessHandler handler);

  @Nullable
  ToolWindow getToolWindowByDescriptor(@Nonnull RunContentDescriptor descriptor);

  void selectRunContent(@Nonnull RunContentDescriptor descriptor);

  /**
   * @return Tool window id where content should be shown. Null if content tool window is determined by executor.
   */
  @Nullable
  default String getContentDescriptorToolWindowId(@Nonnull ExecutionEnvironment environment) {
    RunProfile runProfile = environment.getRunProfile();
    if (runProfile instanceof RunConfiguration) {
      return getContentDescriptorToolWindowId((RunConfiguration)runProfile);
    }

    RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
    if (settings != null) {
      return getContentDescriptorToolWindowId(settings.getConfiguration());
    }
    return null;
  }

  String getContentDescriptorToolWindowId(@Nullable RunConfiguration settings);

  @Nonnull
  String getToolWindowIdByEnvironment(@Nonnull ExecutionEnvironment executionEnvironment);

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
}
