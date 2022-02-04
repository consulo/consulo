// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.document.Document;
import com.intellij.openapi.editor.Editor;
import consulo.document.FileDocumentManager;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

public class WrapWithCustomTemplateAction extends AnAction {
  private final CustomLiveTemplate myTemplate;
  private final Editor myEditor;
  @Nullable
  private final Runnable myAfterExecutionCallback;
  private final PsiFile myFile;

  public WrapWithCustomTemplateAction(CustomLiveTemplate template, final Editor editor, final PsiFile file, final Set<Character> usedMnemonicsSet) {
    this(template, editor, file, usedMnemonicsSet, null);
  }

  public WrapWithCustomTemplateAction(CustomLiveTemplate template, final Editor editor, final PsiFile file, final Set<Character> usedMnemonicsSet, @Nullable Runnable afterExecutionCallback) {
    super(InvokeTemplateAction.extractMnemonic(template.getTitle(), usedMnemonicsSet));
    myTemplate = template;
    myFile = file;
    myEditor = editor;
    myAfterExecutionCallback = afterExecutionCallback;
  }


  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    perform();
  }

  public void perform() {
    final Document document = myEditor.getDocument();
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      ReadonlyStatusHandler.getInstance(myFile.getProject()).ensureFilesWritable(Collections.singletonList(file));
    }

    String selection = myEditor.getSelectionModel().getSelectedText(true);

    if (selection != null) {
      selection = selection.trim();
      PsiDocumentManager.getInstance(myFile.getProject()).commitAllDocuments();
      myTemplate.wrap(selection, new CustomTemplateCallback(myEditor, myFile));
      if (myAfterExecutionCallback != null) {
        myAfterExecutionCallback.run();
      }
    }
  }
}
