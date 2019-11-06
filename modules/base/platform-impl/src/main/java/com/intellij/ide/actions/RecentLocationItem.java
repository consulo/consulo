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
package com.intellij.ide.actions;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl;

import java.util.Objects;

/**
 * from kotlin
 */
public final class RecentLocationItem {
  private final EditorEx myEditor;
  private final IdeDocumentHistoryImpl.PlaceInfo myInfo;

  public RecentLocationItem(EditorEx editor, IdeDocumentHistoryImpl.PlaceInfo info) {
    myEditor = editor;
    myInfo = info;
  }

  public EditorEx getEditor() {
    return myEditor;
  }

  public IdeDocumentHistoryImpl.PlaceInfo getInfo() {
    return myInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecentLocationItem that = (RecentLocationItem)o;
    return Objects.equals(myEditor, that.myEditor) && Objects.equals(myInfo, that.myInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myEditor, myInfo);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RecentLocationItem{");
    sb.append("myEditor=").append(myEditor);
    sb.append(", myInfo=").append(myInfo);
    sb.append('}');
    return sb.toString();
  }
}
