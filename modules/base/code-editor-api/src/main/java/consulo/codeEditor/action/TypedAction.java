// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.util.FreezeLogger;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.ExtensionTypedActionHandler;
import consulo.codeEditor.internal.RawTypedActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Provides services for registering actions which are activated by typing in the editor.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class TypedAction {
    public static TypedAction getInstance() {
        return Application.get().getInstance(TypedAction.class);
    }

    private TypedActionHandler myRawHandler;
    private TypedActionHandler myHandler;
    private boolean myHandlersLoaded;

    private final Application myApplication;

    public TypedAction(Application application) {
        myApplication = application;
        myHandler = new Handler();
    }

    private void ensureHandlersLoaded() {
        if (!myHandlersLoaded) {
            myHandlersLoaded = true;

            List<ExtensionTypedActionHandler> extensionList =
                myApplication.getExtensionPoint(ExtensionTypedActionHandler.class).getExtensionList();
            for (ExtensionTypedActionHandler handler : extensionList) {
                handler.init(myHandler);

                myHandler = handler;
            }
        }
    }

    private void loadRawHandlers() {
        for (RawTypedActionHandler handler : myApplication.getExtensionPoint(RawTypedActionHandler.class).getExtensionList()) {
            handler.init(myRawHandler);

            myRawHandler = handler;
        }
    }

    private static class Handler implements TypedActionHandler {
        @Override
        public void execute(@Nonnull Editor editor, char charTyped, @Nonnull DataContext dataContext) {
            if (editor.isViewer()) {
                return;
            }

            Document doc = editor.getDocument();
            doc.startGuardedBlockChecking();
            try {
                String str = String.valueOf(charTyped);
                CommandProcessor.getInstance().setCurrentCommandName(CodeEditorLocalize.typingInEditorCommandName());
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

    public final void actionPerformed(@Nullable Editor editor, char charTyped, DataContext dataContext) {
        if (editor == null) {
            return;
        }
        Project project = dataContext.getData(Project.KEY);
        FreezeLogger.getInstance().runUnderPerformanceMonitor(project, () -> myRawHandler.execute(editor, charTyped, dataContext));
    }
}