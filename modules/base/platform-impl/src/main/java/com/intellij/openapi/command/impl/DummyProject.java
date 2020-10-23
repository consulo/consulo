/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.command.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import consulo.annotation.access.RequiredWriteAction;
import consulo.container.plugin.ComponentConfig;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.UserDataHolderBase;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class DummyProject extends UserDataHolderBase implements Project {

  private static class DummyProjectHolder {
    private static final DummyProject ourInstance = new DummyProject();
  }

  @Nonnull
  public static Project getInstance() {
    return DummyProjectHolder.ourInstance;
  }

  private DummyProject() {
  }

  @Override
  public VirtualFile getProjectFile() {
    return null;
  }

  @Nonnull
  @Override
  public Application getApplication() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public String getName() {
    return "";
  }

  @Override
  @Nullable
  @NonNls
  public String getPresentableUrl() {
    return null;
  }

  @Override
  @Nonnull
  @NonNls
  public String getLocationHash() {
    return "dummy";
  }

  @Override
  @Nonnull
  public String getProjectFilePath() {
    return "";
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    return null;
  }

  @Override
  @Nullable
  public VirtualFile getBaseDir() {
    return null;
  }

  @Override
  public String getBasePath() {
    return null;
  }

  @Override
  public void save() {
  }

  @RequiredWriteAction
  @Nonnull
  @Override
  public AsyncResult<Void> saveAsync(UIAccess uiAccess) {
    return AsyncResult.resolved();
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> clazz) {
    return null;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  @Nonnull
  public Condition getDisposed() {
    return new Condition() {
      @Override
      public boolean value(final Object o) {
        return isDisposed();
      }
    };
  }

  @Nonnull
  public ComponentConfig[] getComponentConfigurations() {
    return new ComponentConfig[0];
  }

  @Nullable
  public Object getComponent(final ComponentConfig componentConfig) {
    return null;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isInitialized() {
    return false;
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Override
  public MessageBus getMessageBus() {
    return null;
  }

  @Override
  public void dispose() {
  }
}
