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
package consulo.ide.impl.idea.openapi.editor.highlighter;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.ProcessCanceledException;
import consulo.language.Language;
import consulo.language.editor.highlight.*;
import consulo.language.file.LanguageFileType;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.psi.LanguageSubstitutors;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class EditorHighlighterFactoryImpl extends EditorHighlighterFactory {
  private static final Logger LOG = Logger.getInstance(EditorHighlighterFactoryImpl.class);

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(SyntaxHighlighter highlighter, @Nonnull EditorColorsScheme colors) {
    if (highlighter == null) highlighter = new DefaultSyntaxHighlighter();
    return new LexerEditorHighlighter(highlighter, colors);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull FileType fileType, @Nonnull EditorColorsScheme settings, Project project) {
    if (fileType instanceof LanguageFileType) {
      return EditorHighlighterProvider.forFileType(fileType).getEditorHighlighter(project, fileType, null, settings);
    }

    SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, null);
    return createEditorHighlighter(highlighter, settings);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(Project project, @Nonnull FileType fileType) {
    return createEditorHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull VirtualFile vFile, @Nonnull EditorColorsScheme settings, @Nullable Project project) {
    FileType fileType = vFile.getFileType();
    if (fileType instanceof LanguageFileType) {
      LanguageFileType substFileType = substituteFileType(((LanguageFileType)fileType).getLanguage(), vFile, project);
      if (substFileType != null) {
        EditorHighlighterProvider provider = EditorHighlighterProvider.forFileType(substFileType);
        EditorHighlighter editorHighlighter = provider.getEditorHighlighter(project, fileType, vFile, settings);
        boolean isPlain = editorHighlighter.getClass() == LexerEditorHighlighter.class && ((LexerEditorHighlighter)editorHighlighter).isPlain();
        if (!isPlain) {
          return editorHighlighter;
        }
      }
      try {
        return EditorHighlighterProvider.forFileType(fileType).getEditorHighlighter(project, fileType, vFile, settings);
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

  @Nullable
  private static LanguageFileType substituteFileType(Language language, VirtualFile vFile, Project project) {
    LanguageFileType fileType = null;
    if (vFile != null && project != null) {
      Language substLanguage = LanguageSubstitutors.substituteLanguage(language, vFile, project);
      if (substLanguage != language) {
        fileType = substLanguage.getAssociatedFileType();
      }
    }
    return fileType;
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(Project project, @Nonnull VirtualFile file) {
    return createEditorHighlighter(file, EditorColorsManager.getInstance().getGlobalScheme(), project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(Project project, @Nonnull String fileName) {
    return createEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme(), fileName, project);
  }

  @Nonnull
  @Override
  public EditorHighlighter createEditorHighlighter(@Nonnull EditorColorsScheme settings, @Nonnull String fileName, @Nullable Project project) {
    return createEditorHighlighter(new LightVirtualFile(fileName), settings, project);
  }
}
