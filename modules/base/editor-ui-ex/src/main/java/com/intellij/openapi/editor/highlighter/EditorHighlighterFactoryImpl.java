/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.lang.Language;
import consulo.logging.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.testFramework.LightVirtualFile;
import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class EditorHighlighterFactoryImpl extends EditorHighlighterFactory {
  private static final Logger LOG = Logger.getInstance(EditorHighlighterFactoryImpl.class);

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(SyntaxHighlighter highlighter, @Nonnull final EditorColorsScheme colors) {
    if (highlighter == null) highlighter = new PlainSyntaxHighlighter();
    return new LexerEditorHighlighter(highlighter, colors);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull final FileType fileType, @Nonnull final EditorColorsScheme settings, final Project project) {
    if (fileType instanceof LanguageFileType) {
      return FileTypeEditorHighlighterProviders.INSTANCE.forFileType(fileType).getEditorHighlighter(project, fileType, null, settings);
    }

    SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, null);
    return createEditorHighlighter(highlighter, settings);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(final Project project, @Nonnull final FileType fileType) {
    return createEditorHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull VirtualFile vFile, @Nonnull EditorColorsScheme settings, @javax.annotation.Nullable Project project) {
    FileType fileType = vFile.getFileType();
    if (fileType instanceof LanguageFileType) {
      LanguageFileType substFileType = substituteFileType(((LanguageFileType)fileType).getLanguage(), vFile, project);
      if (substFileType != null) {
        EditorHighlighterProvider provider = FileTypeEditorHighlighterProviders.INSTANCE.forFileType(substFileType);
        EditorHighlighter editorHighlighter = provider.getEditorHighlighter(project, fileType, vFile, settings);
        boolean isPlain = editorHighlighter.getClass() == LexerEditorHighlighter.class && ((LexerEditorHighlighter)editorHighlighter).isPlain();
        if (!isPlain) {
          return editorHighlighter;
        }
      }
      try {
        return FileTypeEditorHighlighterProviders.INSTANCE.forFileType(fileType).getEditorHighlighter(project, fileType, vFile, settings);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, vFile);
    return createEditorHighlighter(highlighter, settings);
  }

  @javax.annotation.Nullable
  private static LanguageFileType substituteFileType(Language language, VirtualFile vFile, Project project) {
    LanguageFileType fileType = null;
    if (vFile != null && project != null) {
      Language substLanguage = LanguageSubstitutors.INSTANCE.substituteLanguage(language, vFile, project);
      if (substLanguage != language) {
        fileType = substLanguage.getAssociatedFileType();
      }
    }
    return fileType;
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(final Project project, @Nonnull final VirtualFile file) {
    return createEditorHighlighter(file, EditorColorsManager.getInstance().getGlobalScheme(), project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(final Project project, @Nonnull final String fileName) {
    return createEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme(), fileName, project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull final EditorColorsScheme settings,
                                                   @Nonnull final String fileName,
                                                   @javax.annotation.Nullable final Project project) {
    return createEditorHighlighter(new LightVirtualFile(fileName), settings, project);
  }
}
