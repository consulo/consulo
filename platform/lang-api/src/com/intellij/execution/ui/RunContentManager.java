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
package com.intellij.execution.ui;

import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RunContentManager {
  Topic<RunContentWithExecutorListener> TOPIC = Topic.create("Run Content", RunContentWithExecutorListener.class);

  @SuppressWarnings("UnusedDeclaration")
  @Deprecated
  /**
   * @deprecated Use {@link LangDataKeys#RUN_CONTENT_DESCRIPTOR} instead
   */
  DataKey<RunContentDescriptor> RUN_CONTENT_DESCRIPTOR = LangDataKeys.RUN_CONTENT_DESCRIPTOR;

  @Nullable
  RunContentDescriptor getSelectedContent();

  @Nullable
  RunContentDescriptor getSelectedContent(Executor runnerInfo);

  @NotNull
  List<RunContentDescriptor> getAllDescriptors();

  @Nullable
  /**
   * To reduce number of open contents RunContentManager reuses
   * some of them during showRunContent (for ex. if a process was stopped)
   */
  RunContentDescriptor getReuseContent(@NotNull ExecutionEnvironment executionEnvironment);

  @Nullable
  RunContentDescriptor findContentDescriptor(Executor requestor, ProcessHandler handler);

  void showRunContent(@NotNull Executor executor, @NotNull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse);

  void showRunContent(@NotNull Executor executor, @NotNull RunContentDescriptor descriptor);

  void hideRunContent(@NotNull Executor executor, RunContentDescriptor descriptor);

  boolean removeRunContent(@NotNull Executor executor, RunContentDescriptor descriptor);

  void toFrontRunContent(Executor requestor, RunContentDescriptor descriptor);

  void toFrontRunContent(Executor requestor, ProcessHandler handler);

  @Nullable
  ToolWindow getToolWindowByDescriptor(@NotNull RunContentDescriptor descriptor);

  void selectRunContent(@NotNull RunContentDescriptor descriptor);

  /**
   * @return Tool window id where content should be shown. Null if content tool window is determined by executor.
   */
  @Nullable
  String getContentDescriptorToolWindowId(@Nullable RunnerAndConfigurationSettings settings);

  @NotNull
  String getToolWindowIdByEnvironment(@NotNull ExecutionEnvironment executionEnvironment);
}
