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
package consulo.language.editor.internal;

import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.EditorHighlighterProvider;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26-Jun-22
 */
public class DefaultEditorHighlighterProvider implements EditorHighlighterProvider {
  public static final DefaultEditorHighlighterProvider INSTANCE = new DefaultEditorHighlighterProvider();

  @Nonnull
  @Override
  public EditorHighlighter getEditorHighlighter(@Nullable Project project, @Nonnull FileType fileType, @Nullable VirtualFile virtualFile, @Nonnull EditorColorsScheme colors) {
    return EditorHighlighterFactory.getInstance().createEditorHighlighter(SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, virtualFile), colors);
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    throw new UnsupportedOperationException();
  }
}
