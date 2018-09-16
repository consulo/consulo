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
package com.intellij.openapi.editor.ex;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import javax.annotation.Nonnull;

import java.beans.PropertyChangeListener;

public interface EditorEventMulticasterEx extends EditorEventMulticaster{
  void addErrorStripeListener(@Nonnull ErrorStripeListener listener);
  void addErrorStripeListener(@Nonnull ErrorStripeListener listener, @Nonnull Disposable parentDisposable);
  void removeErrorStripeListener(@Nonnull ErrorStripeListener listener);

  void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener);
  void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener);

  void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);
  void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

  void addFocusChangeListner(@Nonnull FocusChangeListener listener);
  void addFocusChangeListner(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable);
  void removeFocusChangeListner(@Nonnull FocusChangeListener listener);
}
