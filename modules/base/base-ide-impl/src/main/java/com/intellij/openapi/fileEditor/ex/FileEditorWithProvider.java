/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.fileEditor.ex;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;

import java.util.Objects;

/**
 * from kotlin
 */
public final class FileEditorWithProvider {
  private final FileEditor myFileEditor;
  private final FileEditorProvider myProvider;

  public FileEditorWithProvider(FileEditor fileEditor, FileEditorProvider provider) {
    myFileEditor = fileEditor;
    myProvider = provider;
  }

  public FileEditor getFileEditor() {
    return myFileEditor;
  }

  public FileEditorProvider getProvider() {
    return myProvider;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FileEditorWithProvider that = (FileEditorWithProvider)o;
    return Objects.equals(myFileEditor, that.myFileEditor) && Objects.equals(myProvider, that.myProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFileEditor, myProvider);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FileEditorWithProvider{");
    sb.append("myFileEditor=").append(myFileEditor);
    sb.append(", myProvider=").append(myProvider);
    sb.append('}');
    return sb.toString();
  }
}
