/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 05-Apr-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface EditorHistoryManager {
  public static EditorHistoryManager getInstance(@Nonnull Project project) {
    return project.getInstance(EditorHistoryManager.class);
  }

  boolean hasBeenOpen(@Nonnull VirtualFile f);

  @Nonnull
  List<VirtualFile> getFileList();

  @Nullable
  FileEditorState getState(@Nonnull VirtualFile file, final FileEditorProvider provider);

  void removeFile(@Nonnull final VirtualFile file);

  @Nullable
  FileEditorProvider getSelectedProvider(final VirtualFile file);
}
