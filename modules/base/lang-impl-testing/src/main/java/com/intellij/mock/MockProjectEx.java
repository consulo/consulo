/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.openapi.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import consulo.components.impl.stores.IProjectStore;
import com.intellij.openapi.project.ex.ProjectEx;
import javax.annotation.Nonnull;

public class MockProjectEx  extends MockProject implements ProjectEx {
  public MockProjectEx(@Nonnull Disposable parentDisposable) {
    super((MockComponentManager)ApplicationManager.getApplication(), parentDisposable);
  }

  @Override
  public void setProjectName(@Nonnull String name) {
  }

  @Override
  @Nonnull
  public IProjectStore getStateStore() {
    return new MockProjectStore();
  }

  @Override
  public void initNotLazyServices(ProgressIndicator progressIndicator) {
  }

  @Override
  public boolean isOptimiseTestLoadSpeed() {
    return false;
  }

  @Override
  public void setOptimiseTestLoadSpeed(final boolean optimiseTestLoadSpeed) {
    throw new UnsupportedOperationException("Method setOptimiseTestLoadSpeed not implemented in " + getClass());
  }

  }
