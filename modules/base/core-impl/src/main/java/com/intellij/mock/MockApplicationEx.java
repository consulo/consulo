/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.IOException;

@Deprecated
public class MockApplicationEx extends MockApplication implements ApplicationEx {
  public MockApplicationEx(@Nonnull Disposable parentDisposable) {
    super(parentDisposable);
  }

  @Override
  public boolean holdsReadLock() {
    return false;
  }

  @Override
  public void load(String path) throws IOException {
  }

  @Override
  public boolean isLoaded() {
    return true;
  }

  @Override
  public void exit(boolean force, boolean exitConfirmed) {
  }

  @Override
  public void restart(boolean exitConfirmed) {
  }

  @Override
  public void doNotSave() {
  }

  @Override
  public void doNotSave(boolean value) {
  }

  @Override
  public boolean isDoNotSave() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull final Runnable process, @Nonnull final String progressTitle, final boolean canBeCanceled, @Nullable final Project project,
                                                     final JComponent parentComponent) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     Project project) {
    return false;
  }

  @Nonnull
  @Override
  public <T> T[] getExtensions(@Nonnull final ExtensionPointName<T> extensionPointName) {
    return Extensions.getRootArea().getExtensionPoint(extensionPointName).getExtensions();
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread(@Nullable final JComponent component) {
  }

  @Override
  public void assertTimeConsuming() {
  }

  @Override
  public boolean tryRunReadAction(@Nonnull Runnable runnable) {
    runReadAction(runnable);
    return true;
  }

  @Override
  public boolean isInImpatientReader() {
    return false;
  }

  @Override
  public boolean isWriteActionInProgress() {
    return false;
  }

  @Override
  public boolean isWriteActionPending() {
    return false;
  }
}
