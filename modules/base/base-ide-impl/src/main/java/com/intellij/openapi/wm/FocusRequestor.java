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
package com.intellij.openapi.wm;

import consulo.disposer.Disposable;
import com.intellij.openapi.util.AsyncResult;

import javax.annotation.Nonnull;

/**
 * Basic interface for requesting sending focus commands to <code>IdeFocusManager</code>
 */
public interface FocusRequestor extends Disposable {

  /**
   * Requests focus on a component
   *
   * @param c      - component to reqiest focus to
   * @param forced - if true - focus request is explicit, must be fulfilled, if false - can be dropped
   * @return action callback that either notifies when the focus was obtained or focus request was droppped
   */
  @Nonnull
  AsyncResult<Void> requestFocus(@Nonnull consulo.ui.Component c, boolean forced);

  // TODO [VISTALL] AWT & Swing dependency

  // region AWT & Swing dependency

  /**
   * Requests focus on a component
   *
   * @param c      - component to reqiest focus to
   * @param forced - if true - focus request is explicit, must be fulfilled, if false - can be dropped
   * @return action callback that either notifies when the focus was obtained or focus request was droppped
   */
  @Nonnull
  AsyncResult<Void> requestFocus(@Nonnull java.awt.Component c, boolean forced);

  // endregion
}
