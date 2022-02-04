/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.model.execution;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

/**
 * Essentially this class is {@link ExternalSystemTaskExecutionSettings} plus auxiliary information like execution type (run/debug etc).
 * 
 * @author Denis Zhdanov
 * @since 6/9/13 4:14 PM
 */
public class ExternalTaskExecutionInfo {
  
  @Nonnull
  private ExternalSystemTaskExecutionSettings mySettings;
  @Nonnull
  private String myExecutorId;

  @SuppressWarnings("UnusedDeclaration")
  public ExternalTaskExecutionInfo() {
    this(new ExternalSystemTaskExecutionSettings(), "___DUMMY___");
  }

  public ExternalTaskExecutionInfo(@Nonnull ExternalSystemTaskExecutionSettings settings, @Nonnull String executorId) {
    mySettings = settings;
    myExecutorId = executorId;
  }

  @Nonnull
  public ExternalSystemTaskExecutionSettings getSettings() {
    return mySettings;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setSettings(@Nonnull ExternalSystemTaskExecutionSettings settings) {
    // Required by IJ serialization.
    mySettings = settings;
  }

  @Nonnull
  public String getExecutorId() {
    return myExecutorId;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setExecutorId(@Nonnull String executorId) {
    // Required by IJ serialization.
    myExecutorId = executorId;
  }

  public String getDescription() {
    return StringUtil.join(mySettings.getTaskDescriptions(), "\n");
  }

  @Override
  public int hashCode() {
    int result = mySettings.hashCode();
    result = 31 * result + myExecutorId.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalTaskExecutionInfo task = (ExternalTaskExecutionInfo)o;

    if (!myExecutorId.equals(task.myExecutorId)) return false;
    if (!mySettings.equals(task.mySettings)) return false;

    return true;
  }

  @Override
  public String toString() {
    return StringUtil.join(mySettings.getTaskNames(), " ");
  }
}
