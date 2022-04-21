/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.generation.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.AutoIndentLinesHandler;
import consulo.language.codeStyle.LanguageFormatting;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.LanguageFileType;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.document.util.DocumentUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AutoIndentLinesAction extends BaseCodeInsightAction implements DumbAware {
  @Nullable
  @Override
  protected Editor getEditor(@Nonnull DataContext dataContext, @Nonnull Project project, boolean forUpdate) {
    Editor editor = getBaseEditor(dataContext, project);
    if (editor == null) return null;
    Document document = editor.getDocument();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    PsiFile psiFile = documentManager.getCachedPsiFile(document);
    if (psiFile == null) return editor;
    if (!forUpdate) documentManager.commitDocument(document);
    int startLineOffset = DocumentUtil.getLineStartOffset(editor.getSelectionModel().getSelectionStart(), document);
    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile, startLineOffset);
  }

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new AutoIndentLinesHandler();
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    final FileType fileType = file.getFileType();
    return fileType instanceof LanguageFileType && LanguageFormatting.INSTANCE.forContext(((LanguageFileType)fileType).getLanguage(), file) != null;
  }
}