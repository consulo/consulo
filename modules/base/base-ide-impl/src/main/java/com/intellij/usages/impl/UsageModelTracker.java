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
package com.intellij.usages.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author max
 */
class UsageModelTracker implements Disposable {
  @FunctionalInterface
  public interface UsageModelTrackerListener {
    void modelChanged(boolean isPropertyChange);
  }

  private final List<UsageModelTrackerListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  UsageModelTracker(@Nonnull Project project) {
    final PsiTreeChangeListener myPsiListener = new PsiTreeChangeAdapter() {
      @Override
      public void childAdded(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, false);
      }

      @Override
      public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, false);
      }

      @Override
      public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, false);
      }

      @Override
      public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, false);
      }

      @Override
      public void childMoved(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, false);
      }

      @Override
      public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
        doFire(event, true);
      }
    };
    PsiManager.getInstance(project).addPsiTreeChangeListener(myPsiListener, this);
  }

  private void doFire(@Nonnull PsiTreeChangeEvent event, boolean propertyChange) {
    if (!(event.getFile() instanceof PsiCodeFragment)) {
      for (UsageModelTrackerListener listener : myListeners) {
        listener.modelChanged(propertyChange);
      }
    }
  }

  @Override
  public void dispose() {
  }

  void addListener(@Nonnull UsageModelTrackerListener listener, @Nonnull Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, () -> myListeners.remove(listener));
  }
}
