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
package com.intellij.psi;

import javax.annotation.Nonnull;

/**
 * Default empty implementation of {@link PsiTreeChangeListener}.
 */
public abstract class PsiTreeChangeAdapter implements PsiTreeChangeListener {
  @Override
  public void beforeChildAddition(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildRemoval(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildReplacement(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void beforePropertyChange(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void childAdded(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void childMoved(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
  }

  @Override
  public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
  }
}
