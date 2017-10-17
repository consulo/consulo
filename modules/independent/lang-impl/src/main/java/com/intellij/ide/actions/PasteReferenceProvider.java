/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.codeInsight.FileModificationService;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Producer;
import consulo.ide.actions.QualifiedNameProviders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;

public class PasteReferenceProvider implements PasteProvider {
  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (project == null || editor == null) return;

    final String fqn = getCopiedFqn(dataContext);

    Pair<PsiElement,QualifiedNameProvider> pair = QualifiedNameProviders.findElementByQualifiedName(fqn, project);
    if (pair != null) {
      insert(fqn, pair.getFirst(), editor, pair.getSecond());
    }
  }

  @Override
  public boolean isPastePossible(@NotNull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    return project != null && editor != null && getCopiedFqn(dataContext) != null;
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    String fqn = getCopiedFqn(dataContext);
    if (project == null || fqn == null) {
      return false;
    }
    return QualifiedNameProviders.findElementByQualifiedName(fqn, project) != null;
  }

  private static void insert(final String fqn, final PsiElement element, final Editor editor, final QualifiedNameProvider provider) {
    final Project project = editor.getProject();
    if (project == null) return;

    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    documentManager.commitDocument(editor.getDocument());

    final PsiFile file = documentManager.getPsiFile(editor.getDocument());
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      Document document = editor.getDocument();
      documentManager.doPostponedOperationsAndUnblockDocument(document);
      documentManager.commitDocument(document);
      EditorModificationUtil.deleteSelectedText(editor);
      provider.insertQualifiedName(fqn, element, editor, project);
    }), IdeBundle.message("command.pasting.reference"), null);
  }

  @Nullable
  private static String getCopiedFqn(final DataContext context) {
    Producer<Transferable> producer = context.getData(PasteAction.TRANSFERABLE_PROVIDER);

    if (producer != null) {
      Transferable transferable = producer.produce();
      if (transferable != null) {
        try {
          return (String)transferable.getTransferData(CopyReferenceAction.ourFlavor);
        }
        catch (Exception ignored) { }
      }
      return null;
    }

    return CopyPasteManager.getInstance().getContents(CopyReferenceAction.ourFlavor);
  }
}
