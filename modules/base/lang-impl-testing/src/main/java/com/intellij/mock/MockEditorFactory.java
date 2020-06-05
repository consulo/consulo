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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.CharArrayCharSequence;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

public class MockEditorFactory extends EditorFactory {
  public Document createDocument(String text) {
    return new DocumentImpl(text);
  }

  @Override
  public Editor createEditor(@Nonnull Document document) {
    return null;
  }

  @Override
  public Editor createViewer(@Nonnull Document document) {
    return null;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project) {
    return null;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind kind) {
    return null;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer) {
    return null;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer, @Nonnull EditorKind kind) {
    return null;
  }

  @Override
  public Editor createEditor(@Nonnull final Document document, final Project project, @Nonnull final FileType fileType, final boolean isViewer) {
    return null;
  }

  @Override
  public Editor createViewer(@Nonnull Document document, Project project) {
    return null;
  }

  @Override
  public Editor createViewer(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind editorKind) {
    return null;
  }

  @Override
  public void releaseEditor(@Nonnull Editor editor) {
  }

  @Override
  @Nonnull
  public Editor[] getEditors(@Nonnull Document document, Project project) {
    return Editor.EMPTY_ARRAY;
  }

  @Override
  @Nonnull
  public Editor[] getEditors(@Nonnull Document document) {
    return getEditors(document, null);
  }

  @Override
  @Nonnull
  public Editor[] getAllEditors() {
    return Editor.EMPTY_ARRAY;
  }

  @Override
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener) {
  }

  @Override
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeEditorFactoryListener(@Nonnull EditorFactoryListener listener) {
  }

  @Override
  @Nonnull
  public EditorEventMulticaster getEventMulticaster() {
    return new MockEditorEventMulticaster();
  }

  @Override
  @Nonnull
  public Document createDocument(@Nonnull CharSequence text) {
    return new DocumentImpl(text);
  }

  @Override
  @Nonnull
  public Document createDocument(@Nonnull char[] text) {
    return createDocument(new CharArrayCharSequence(text));
  }

  @Override
  public void refreshAllEditors() {
  }
}
