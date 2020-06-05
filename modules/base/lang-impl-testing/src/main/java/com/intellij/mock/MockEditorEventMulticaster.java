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
package com.intellij.mock;

import consulo.disposer.Disposable;
import com.intellij.openapi.editor.event.*;
import javax.annotation.Nonnull;

public class MockEditorEventMulticaster implements EditorEventMulticaster {
  public MockEditorEventMulticaster() {
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener) {
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable) {
      }

  @Override
  public void removeDocumentListener(@Nonnull DocumentListener listener) {
  }

  @Override
  public void addEditorMouseListener(@Nonnull EditorMouseListener listener) {
  }

  @Override
  public void addEditorMouseListener(@Nonnull final EditorMouseListener listener, @Nonnull final Disposable parentDisposable) {
  }

  @Override
  public void removeEditorMouseListener(@Nonnull EditorMouseListener listener) {
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
  }

  @Override
  public void addCaretListener(@Nonnull CaretListener listener) {
  }

  @Override
  public void addCaretListener(@Nonnull CaretListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeCaretListener(@Nonnull CaretListener listener) {
  }

  @Override
  public void addSelectionListener(@Nonnull SelectionListener listener) {
  }

  @Override
  public void addSelectionListener(@Nonnull SelectionListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeSelectionListener(@Nonnull SelectionListener listener) {
  }

  @Override
  public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
  }

  @Override
  public void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
  }

}
