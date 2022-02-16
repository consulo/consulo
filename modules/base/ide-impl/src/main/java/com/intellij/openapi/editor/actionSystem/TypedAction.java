// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.actionSystem;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.*;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import com.intellij.reporting.FreezeLogger;
import consulo.document.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides services for registering actions which are activated by typing in the editor.
 */
public abstract class TypedAction {
  public static TypedAction getInstance() {
    return ServiceManager.getService(TypedAction.class);
  }

  private TypedActionHandler myRawHandler;
  private TypedActionHandler myHandler;
  private boolean myHandlersLoaded;

  public TypedAction() {
    myHandler = new Handler();
  }

  private void ensureHandlersLoaded() {
    if (!myHandlersLoaded) {
      myHandlersLoaded = true;
      for (EditorTypedHandlerBean handlerBean : EditorTypedHandlerBean.EP_NAME.getExtensionList()) {
        myHandler = handlerBean.getHandler(myHandler);
      }
    }
  }

  private void loadRawHandlers() {
    for (EditorTypedHandlerBean handlerBean : EditorTypedHandlerBean.RAW_EP_NAME.getExtensionList()) {
      myRawHandler = handlerBean.getHandler(myRawHandler);
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
  public TypedActionHandler setupRawHandler(@Nonnull TypedActionHandler handler) {
    TypedActionHandler tmp = myRawHandler;
    myRawHandler = handler;
    if (tmp == null) {
      loadRawHandlers();
    }
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
    FreezeLogger.getInstance().runUnderPerformanceMonitor(project, () -> myRawHandler.execute(editor, charTyped, dataContext));
  }
}