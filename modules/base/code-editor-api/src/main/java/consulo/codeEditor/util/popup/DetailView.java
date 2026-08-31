/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.codeEditor.util.popup;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.colorScheme.TextAttributes;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * @author zajac
 * @since 2012-05-09
 */
public interface DetailView extends UserDataHolder {
  Editor getEditor();

  void navigateInPreviewEditor(PreviewEditorState editorState);

  JPanel getPropertiesPanel();

  void setPropertiesPanel(@Nullable JPanel panel);

  void clearEditor();

  PreviewEditorState getEditorState();

  ItemWrapper getCurrentItem();

  boolean hasEditorOnly();

  void setCurrentItem(@Nullable ItemWrapper item);

  class PreviewEditorState {
    public static PreviewEditorState EMPTY = new PreviewEditorState(null, null, null);

    public static PreviewEditorState create(VirtualFile file, int line) {
      return new PreviewEditorState(file, line < 0 ? null : new LogicalPosition(line, 0), null);
    }

    public static PreviewEditorState create(VirtualFile file, int line, TextAttributes attributes) {
      return new PreviewEditorState(file, new LogicalPosition(line, 0), attributes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PreviewEditorState that = (PreviewEditorState)o;

      return Objects.equals(myAttributes, that.myAttributes)
          && Objects.equals(myFile, that.myFile)
          && Objects.equals(myNavigate, that.myNavigate);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(myFile);
      result = 31 * result + Objects.hashCode(myNavigate);
      result = 31 * result + Objects.hashCode(myAttributes);
      return result;
    }

    public @Nullable VirtualFile getFile() {
      return myFile;
    }

    public @Nullable LogicalPosition getNavigate() {
      return myNavigate;
    }

    public @Nullable TextAttributes getAttributes() {
      return myAttributes;
    }

    private final @Nullable VirtualFile myFile;
    private final @Nullable LogicalPosition myNavigate;
    private final @Nullable TextAttributes myAttributes;

    public PreviewEditorState(@Nullable VirtualFile file, @Nullable LogicalPosition navigate, @Nullable TextAttributes attributes) {
      myFile = file;
      myNavigate = navigate;
      myAttributes = attributes;
    }
  }
}
