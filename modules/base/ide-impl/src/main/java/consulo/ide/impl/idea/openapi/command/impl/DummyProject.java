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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

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

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  @Nonnull
  public BooleanSupplier getDisposed() {
    return this::isDisposed;
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
