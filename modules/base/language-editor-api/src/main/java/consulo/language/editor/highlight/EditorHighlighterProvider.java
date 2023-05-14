/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.editor.internal.DefaultEditorHighlighterProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EditorHighlighterProvider {
  ExtensionPointCacheKey<EditorHighlighterProvider, Map<FileType, EditorHighlighterProvider>> KEY = ExtensionPointCacheKey.groupBy("EditorHighlighterProvider", EditorHighlighterProvider::getFileType);

  @Nonnull
  static EditorHighlighterProvider forFileType(@Nonnull FileType fileType) {
    Map<FileType, EditorHighlighterProvider> map = Application.get().getExtensionPoint(EditorHighlighterProvider.class).getOrBuildCache(KEY);
    return map.getOrDefault(fileType, DefaultEditorHighlighterProvider.INSTANCE);
  }

  /**
   * Lower level API for customizing language's file syntax highlighting in editor component.
   *
   * @param project     The project in which the highlighter will work, or null if the highlighter is not tied to any project.
   * @param fileType    the file type of the file to be highlighted
   * @param virtualFile The file to be highlighted
   * @param colors      color scheme highlighter shall be initialized with.   @return EditorHighlighter implementation
   */
  @Nonnull
  EditorHighlighter getEditorHighlighter(@Nullable Project project, @Nonnull FileType fileType, @Nullable final VirtualFile virtualFile, @Nonnull EditorColorsScheme colors);

  @Nonnull
  FileType getFileType();
}
