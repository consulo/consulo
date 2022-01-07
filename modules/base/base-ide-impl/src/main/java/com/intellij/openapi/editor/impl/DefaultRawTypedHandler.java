// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.CommandProcessorEx;
import com.intellij.openapi.command.CommandToken;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.*;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

public class DefaultRawTypedHandler implements TypedActionHandlerEx {
  private final TypedAction myAction;
  private CommandToken myCurrentCommandToken;
  private boolean myInOuterCommand;

  DefaultRawTypedHandler(TypedAction action) {
    myAction = action;
  }

  @Override
  public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
    if (editor.isViewer() || !editor.getDocument().isWritable()) return;

    TypedActionHandler handler = myAction.getHandler();

    if (handler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)handler).beforeExecute(editor, c, context, plan);
    }
  }

  @Override
  public void execute(@Nonnull final Editor editor, final char charTyped, @Nonnull final DataContext dataContext) {
    CommandProcessorEx commandProcessorEx = (CommandProcessorEx)CommandProcessor.getInstance();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (myCurrentCommandToken != null) {
      throw new IllegalStateException("Unexpected reentrancy of DefaultRawTypedHandler");
    }
    myCurrentCommandToken = commandProcessorEx.startCommand(project, "", editor.getDocument(), UndoConfirmationPolicy.DEFAULT);
    myInOuterCommand = myCurrentCommandToken == null;
    try {
      if (!EditorModificationUtil.requestWriting(editor)) {
        return;
      }
      ApplicationManager.getApplication().runWriteAction(new DocumentRunnable(editor.getDocument(), editor.getProject()) {
        @Override
        public void run() {
          Document doc = editor.getDocument();
          doc.startGuardedBlockChecking();
          try {
            myAction.getHandler().execute(editor, charTyped, dataContext);
          }
          catch (ReadOnlyFragmentModificationException e) {
            EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
          }
          finally {
            doc.stopGuardedBlockChecking();
          }
        }
      });
    }
    finally {
      if (!myInOuterCommand) {
        commandProcessorEx.finishCommand(myCurrentCommandToken, null);
        myCurrentCommandToken = null;
      }
      myInOuterCommand = false;
    }
  }

  public void beginUndoablePostProcessing() {
    if (myInOuterCommand) {
      return;
    }
    if (myCurrentCommandToken == null) {
      throw new IllegalStateException("Not in a typed action at this time");
    }
    CommandProcessorEx commandProcessorEx = (CommandProcessorEx)CommandProcessor.getInstance();
    Project project = myCurrentCommandToken.getProject();
    commandProcessorEx.finishCommand(myCurrentCommandToken, null);
    myCurrentCommandToken = commandProcessorEx.startCommand(project, "", null, UndoConfirmationPolicy.DEFAULT);
  }
}
