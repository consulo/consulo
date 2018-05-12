/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityInvokator;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Condition;

import javax.annotation.Nonnull;

/**
 * @author max
 */
class ModalityInvokatorImpl implements ModalityInvokator {
  ModalityInvokatorImpl() {
  }

  @Nonnull
  @Override
  public AsyncResult<Void> invokeLater(@Nonnull Runnable runnable) {
    return invokeLater(runnable, ApplicationManager.getApplication().getDisposed());
  }

  @Nonnull
  @Override
  public AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    return LaterInvocator.invokeLater(runnable, expired);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    return LaterInvocator.invokeLater(runnable, state, expired);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    return invokeLater(runnable, state, ApplicationManager.getApplication().getDisposed());
  }
}