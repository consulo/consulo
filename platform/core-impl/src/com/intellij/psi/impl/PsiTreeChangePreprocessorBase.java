/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package com.intellij.psi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public abstract class PsiTreeChangePreprocessorBase implements PsiTreeChangePreprocessor {
  private final PsiModificationTrackerImpl myModificationTracker;

  public PsiTreeChangePreprocessorBase(@NotNull Project project) {
    myModificationTracker = (PsiModificationTrackerImpl)PsiModificationTracker.SERVICE.getInstance(project);
  }

  @Override
  public final void treeChanged(@NotNull PsiTreeChangeEventImpl event) {
    boolean modifyOutOfCodeCounter = false;

    switch (event.getCode()) {
      case BEFORE_CHILDREN_CHANGE:
        if (event.getParent() instanceof PsiFile) {
          modifyOutOfCodeCounter = false;
          break; // May be caused by fake PSI event from PomTransaction. A real event will anyway follow.
        }

      case CHILDREN_CHANGED:
        if (event.isGenericChange()) {
          return;
        }
        modifyOutOfCodeCounter = modifyOutOfCodeCounter(event.getFile(), event.getParent());
        break;

      case BEFORE_CHILD_ADDITION:
      case BEFORE_CHILD_REMOVAL:
      case CHILD_ADDED:
      case CHILD_REMOVED:
        modifyOutOfCodeCounter = modifyOutOfCodeCounter(event.getFile(), event.getParent());
        break;

      case BEFORE_PROPERTY_CHANGE:
      case PROPERTY_CHANGED:
        modifyOutOfCodeCounter = true;
        break;

      case BEFORE_CHILD_REPLACEMENT:
      case CHILD_REPLACED:
        modifyOutOfCodeCounter = modifyOutOfCodeCounter(event.getFile(), event.getParent());
        break;

      case BEFORE_CHILD_MOVEMENT:
      case CHILD_MOVED:
        modifyOutOfCodeCounter = modifyOutOfCodeCounter(event.getFile(), event.getOldParent()) && modifyOutOfCodeCounter(event.getFile(), event.getNewParent());
        break;
    }

    if (modifyOutOfCodeCounter) {
      myModificationTracker.incOutOfCodeBlockModificationCounter();
    }
  }

  protected abstract boolean isMyFile(@NotNull PsiFile file);

  protected boolean isMaybeMyElement(@Nullable PsiElement element) {
    return false;
  }

  protected boolean modifyOutOfCodeCounter(@Nullable PsiFile file, @Nullable PsiElement element) {
    if(file == null) {
      return isMaybeMyElement(element);
    }
    else if(isMyFile(file)) {
      return !isInsideCodeBlock(element);
    }
    return false;
  }

  protected abstract boolean isInsideCodeBlock(@Nullable PsiElement element);
}
