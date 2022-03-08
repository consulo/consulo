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
package com.intellij.openapi.editor.highlighter;

import consulo.ide.ServiceManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.editor.highlight.EditorHighlighter;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public abstract class EditorHighlighterFactory {

  public static EditorHighlighterFactory getInstance() {
    return ServiceManager.getService(EditorHighlighterFactory.class);
  }

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(final SyntaxHighlighter syntaxHighlighter, @Nonnull EditorColorsScheme colors);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(@Nonnull FileType fileType, @Nonnull EditorColorsScheme settings, final Project project);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(final Project project, @Nonnull FileType fileType);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(@Nonnull final VirtualFile file,
                                                            @Nonnull EditorColorsScheme globalScheme,
                                                            @javax.annotation.Nullable final Project project);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(final Project project, @Nonnull VirtualFile file);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(final Project project, @Nonnull String fileName);

  @Nonnull
  public abstract EditorHighlighter createEditorHighlighter(@Nonnull EditorColorsScheme settings, @Nonnull String fileName, @javax.annotation.Nullable final Project project);
}