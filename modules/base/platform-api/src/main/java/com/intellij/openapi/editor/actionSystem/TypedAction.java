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
package com.intellij.openapi.editor.actionSystem;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.reporting.FreezeLogger;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides services for registering actions which are activated by typing in the editor.
 *
 * @see EditorActionManager#getTypedAction()
 */
public class TypedAction {
  @Nonnull
  private TypedActionHandler myRawHandler;
  private TypedActionHandler myHandler;
  private boolean myHandlersLoaded;

  public TypedAction() {
    myHandler = new Handler();
    myRawHandler = new DefaultRawHandler();
  }

  private void ensureHandlersLoaded() {
    if (!myHandlersLoaded) {
      myHandlersLoaded = true;
      for (EditorTypedHandlerBean handlerBean : EditorTypedHandlerBean.EP_NAME.getExtensions()) {
        myHandler = handlerBean.getHandler(myHandler);
      }
    }
  }

  private static class Handler implements TypedActionHandler {
    @Override
    public void execute(@Nonnull final Editor editor, char charTyped, @Nonnull DataContext dataContext) {
      if (editor.isViewer()) return;

      Document doc = editor.getDocument();
      doc.startGuardedBlockChecking();
      try {
        final String str = String.valueOf(charTyped);
        CommandProcessor.getInstance().setCurrentCommandName(EditorBundle.message("typing.in.editor.command.name"));
        EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(editor, str, true);
      }
      catch (ReadOnlyFragmentModificationException e) {
        EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
      }
      finally {
        doc.stopGuardedBlockChecking();
      }
    }
  }

  /**
   * Gets the current typing handler.
   *
   * @return the current typing handler.
   */
  public TypedActionHandler getHandler() {
    ensureHandlersLoaded();
    return myHandler;
  }

  /**
   * Replaces the typing handler with the specified handler. The handler should pass
   * unprocessed typing to the previously registered handler.
   *
   * @param handler the handler to set.
   * @return the previously registered handler.
   */
  public TypedActionHandler setupHandler(TypedActionHandler handler) {
    ensureHandlersLoaded();
    TypedActionHandler tmp = myHandler;
    myHandler = handler;
    return tmp;
  }

  /**
   * Gets the current 'raw' typing handler.
   *
   * @see #setupRawHandler(TypedActionHandler)
   */
  @Nonnull
  public TypedActionHandler getRawHandler() {
    return myRawHandler;
  }

  /**
   * Replaces current 'raw' typing handler with the specified handler. The handler should pass unprocessed typing to the
   * previously registered 'raw' handler.
   * <p>
   * 'Raw' handler is a handler directly invoked by the code which handles typing in editor. Default 'raw' handler
   * performs some generic logic that has to be done on typing (like checking whether file has write access, creating a command
   * instance for undo subsystem, initiating write action, etc), but delegates to 'normal' handler for actual typing logic.
   *
   * @param handler the handler to set.
   * @return the previously registered handler.
   * @see #getRawHandler()
   * @see #getHandler()
   * @see #setupHandler(TypedActionHandler)
   */
  @Nonnull
  public TypedActionHandler setupRawHandler(@Nonnull TypedActionHandler handler) {
    TypedActionHandler tmp = myRawHandler;
    myRawHandler = handler;
    return tmp;
  }

  public void beforeActionPerformed(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
    if (myRawHandler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)myRawHandler).beforeExecute(editor, c, context, plan);
    }
  }

  public final void actionPerformed(@Nullable final Editor editor, final char charTyped, final DataContext dataContext) {
    if (editor == null) return;
    Project project = dataContext.getData(CommonDataKeys.PROJECT);

    FreezeLogger.getInstance().runUnderPerformanceMonitor(project, () -> myRawHandler.executeAsync(editor, charTyped, dataContext));
  }

  private class DefaultRawHandler implements TypedActionHandlerEx {
    @Override
    public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
      if (editor.isViewer() || !editor.getDocument().isWritable()) return;

      TypedActionHandler handler = getHandler();

      if (handler instanceof TypedActionHandlerEx) {
        ((TypedActionHandlerEx)handler).beforeExecute(editor, c, context, plan);
      }
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<Void> executeAsync(@Nonnull Editor editor, char charTyped, @Nonnull DataContext dataContext) {
      return CommandProcessor.getInstance().executeCommandAsync(dataContext.getData(CommonDataKeys.PROJECT), (commandResult) -> {
        AsyncResult<Void> result = EditorModificationUtil.requestWritingAsync(editor);
        result.doWhenDone(() -> {
          Document doc = editor.getDocument();
          doc.startGuardedBlockChecking();
          try {
            getHandler().executeAsync(editor, charTyped, dataContext).doWhenDone(() -> {
              doc.stopGuardedBlockChecking();

              commandResult.setDone();
            });
          }
          catch (ReadOnlyFragmentModificationException e) {
            EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
          }
        });
      }, "", editor.getDocument(), UndoConfirmationPolicy.DEFAULT, editor.getDocument());
    }
  }
}