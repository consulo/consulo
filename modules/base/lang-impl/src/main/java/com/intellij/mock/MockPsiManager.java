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

package com.intellij.mock;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.util.PsiModificationTracker;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MockPsiManager extends PsiManagerEx {
  private final Project myProject;
  private final Map<VirtualFile,PsiDirectory> myDirectories = new HashMap<VirtualFile, PsiDirectory>();
  private MockFileManager myMockFileManager;
  private PsiModificationTrackerImpl myPsiModificationTracker;

  public MockPsiManager(@Nonnull Project project) {
    myProject = project;
  }

  public void addPsiDirectory(VirtualFile file, PsiDirectory psiDirectory) {
    myDirectories.put(file, psiDirectory);
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public PsiFile findFile(@Nonnull VirtualFile file) {
    return null;
  }

  @Override
  @Nullable
  public
  FileViewProvider findViewProvider(@Nonnull VirtualFile file) {
    return null;
  }

  @Override
  public PsiDirectory findDirectory(@Nonnull VirtualFile file) {
    return myDirectories.get(file);
  }

  @Override
  public boolean areElementsEquivalent(PsiElement element1, PsiElement element2) {
    return Comparing.equal(element1, element2);
  }

  @Override
  public void reloadFromDisk(@Nonnull PsiFile file) {
  }

  @Override
  public void addPsiTreeChangeListener(@Nonnull PsiTreeChangeListener listener) {
  }

  @Override
  public void addPsiTreeChangeListener(@Nonnull PsiTreeChangeListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removePsiTreeChangeListener(@Nonnull PsiTreeChangeListener listener) {
  }

  @Override
  @Nonnull
  public PsiModificationTracker getModificationTracker() {
    if (myPsiModificationTracker == null) {
      myPsiModificationTracker = new PsiModificationTrackerImpl(Application.get(), myProject);
    }
    return myPsiModificationTracker;
  }

  @Override
  public void startBatchFilesProcessingMode() {
  }

  @Override
  public void finishBatchFilesProcessingMode() {
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, T value) {
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dropResolveCaches() {
    getFileManager().cleanupForNextTest();
  }

  @RequiredUIAccess
  @Override
  public void dropPsiCaches() {
    dropResolveCaches();
  }

  @Override
  public boolean isInProject(@Nonnull PsiElement element) {
    return false;
  }

  @Override
  public boolean isBatchFilesProcessingMode() {
    return false;
  }

  @Override
  public boolean isAssertOnFileLoading(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public void beforeChange(boolean isPhysical) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void afterChange(boolean isPhysical) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerRunnableToRunOnChange(@Nonnull Runnable runnable) {
  }

  @Override
  public void registerRunnableToRunOnAnyChange(@Nonnull Runnable runnable) {
  }

  @Override
  public void registerRunnableToRunAfterAnyChange(@Nonnull Runnable runnable) {
    throw new UnsupportedOperationException("Method registerRunnableToRunAfterAnyChange is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public FileManager getFileManager() {
    if (myMockFileManager == null) {
      myMockFileManager = new MockFileManager(this);
    }
    return myMockFileManager;
  }

  @Override
  public void beforeChildRemoval(@Nonnull final PsiTreeChangeEventImpl event) {
  }

  @Override
  public void beforeChildReplacement(@Nonnull final PsiTreeChangeEventImpl event) {
  }

  @Override
  public void beforeChildAddition(@Nonnull PsiTreeChangeEventImpl event) {
  }

  @Override
  public void setAssertOnFileLoadingFilter(@Nonnull VirtualFileFilter filter, @Nonnull Disposable parentDisposable) {

  }
}
