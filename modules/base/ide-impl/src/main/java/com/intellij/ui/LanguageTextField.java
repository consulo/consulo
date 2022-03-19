/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ui;

import com.intellij.ide.highlighter.HighlighterFactory;
import consulo.language.Language;
import consulo.document.Document;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorEx;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.util.lang.LocalTimeCounter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LanguageTextField extends EditorTextField {
  private final Language myLanguage;
  private final Project myProject;

  public LanguageTextField(@Nullable Language language, @Nonnull Project project, @Nonnull String value) {
    this(language, project, value, true);
  }

  public LanguageTextField(@Nullable Language language, @Nonnull Project project, @Nonnull String value, boolean oneLineMode) {
    this(language, project, value, new SimpleDocumentCreator(), oneLineMode);
  }

  public LanguageTextField(@Nullable Language language,
                           @Nonnull Project project,
                           @Nonnull String value,
                           @Nonnull DocumentCreator documentCreator)
  {
    this(language, project, value, documentCreator, true);
  }

  public LanguageTextField(@Nullable Language language,
                           @Nonnull Project project,
                           @Nonnull String value,
                           @Nonnull DocumentCreator documentCreator,
                           boolean oneLineMode) {
    super(documentCreator.createDocument(value, language, project), project,
          language != null ? language.getAssociatedFileType() : PlainTextFileType.INSTANCE, language == null, oneLineMode);

    myLanguage = language;
    myProject = project;

    setEnabled(language != null);
  }

  public interface DocumentCreator {
    Document createDocument(String value, @Nullable Language language, Project project);
  }

  public static class SimpleDocumentCreator implements DocumentCreator {
    @Override
    public Document createDocument(String value, @Nullable Language language, Project project) {
      return LanguageTextField.createDocument(value, language, project, this);
    }

    public void customizePsiFile(PsiFile file) {
    }
  }

  private static Document createDocument(String value, @Nullable Language language, Project project,
                                         @Nonnull SimpleDocumentCreator documentCreator) {
    if (language != null) {
      final PsiFileFactory factory = PsiFileFactory.getInstance(project);
      final FileType fileType = language.getAssociatedFileType();
      assert fileType != null;

      final long stamp = LocalTimeCounter.currentTime();
      final PsiFile psiFile = factory.createFileFromText("Dummy." + fileType.getDefaultExtension(), fileType, value, stamp, true, false);
      documentCreator.customizePsiFile(psiFile);
      final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
      assert document != null;
      return document;
    }
    else {
      return EditorFactory.getInstance().createDocument(value);
    }
  }

  @Override
  protected EditorEx createEditor() {
    final EditorEx ex = super.createEditor();

    if (myLanguage != null) {
      final FileType fileType = myLanguage.getAssociatedFileType();
      ex.setHighlighter(HighlighterFactory.createHighlighter(myProject, fileType));
    }
    ex.setEmbeddedIntoDialogWrapper(true);

    return ex;
  }
}
