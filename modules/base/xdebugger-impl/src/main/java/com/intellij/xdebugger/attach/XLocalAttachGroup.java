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
package com.intellij.xdebugger.attach;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import javax.annotation.Nonnull;

import javax.swing.*;

public interface XLocalAttachGroup {
  int getOrder();

  @Nonnull
  String getGroupName();

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in method @{@link XLocalAttachDebuggerProvider#getAvailableDebuggers(Project, ProcessInfo, UserDataHolder)}
   * and use it for presentation
   * @return an icon to be shown in popup menu for your debugger item
   */
  @Nonnull
  Icon getProcessIcon(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder);

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in method @{@link XLocalAttachDebuggerProvider#getAvailableDebuggers(Project, ProcessInfo, UserDataHolder)}
   * and use it for presentation
   * @return a text to be shown on your debugger item
   */
  @Nonnull
  String getProcessDisplayText(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder);

  /**
   * Specifies process order in your group
   * @param dataHolder you may put your specific data into the holder at previous step in method @{@link XLocalAttachDebuggerProvider#getAvailableDebuggers(Project, ProcessInfo, UserDataHolder)}
   * and use it for comparison
   */
  int compare(@Nonnull Project project, @Nonnull ProcessInfo a, @Nonnull ProcessInfo b, @Nonnull UserDataHolder dataHolder);

  XLocalAttachGroup DEFAULT = new XDefaultLocalAttachGroup();
}
