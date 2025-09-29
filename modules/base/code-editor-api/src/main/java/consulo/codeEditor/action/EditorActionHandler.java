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
package consulo.codeEditor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.dataContext.DataContext;
import consulo.document.DocCommandGroupId;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface for actions activated by keystrokes in the editor.
 * Implementations should override
 * {@link #execute(Editor, Caret, DataContext)}
 * .
 * <p>
 * Two types of handlers are supported: the ones which are executed once, and the ones which are executed for each caret. The latter can be
 * created using {@link EditorActionHandler#EditorActionHandler(boolean)} constructor.
 *
 * @see EditorActionManager#setActionHandler(String, EditorActionHandler)
 */
public abstract class EditorActionHandler {
    private final boolean myRunForEachCaret;
    private boolean myWorksInInjected;
    private boolean inExecution;
    private boolean inCheck;

    protected EditorActionHandler() {
        this(false);
    }

    protected EditorActionHandler(boolean runForEachCaret) {
        myRunForEachCaret = runForEachCaret;
    }

    /**
     * @deprecated Implementations should override
     * {@link #isEnabledForCaret(Editor, Caret, DataContext)}
     * instead,
     * client code should invoke
     * {@link #isEnabled(Editor, Caret, DataContext)}
     * instead.
     */
    @Deprecated
    public boolean isEnabled(Editor editor, DataContext dataContext) {
        if (inCheck) {
            return true;
        }
        inCheck = true;
        try {
            if (editor == null) {
                return false;
            }
            Editor hostEditor = dataContext == null ? null : dataContext.getData(EditorKeys.HOST_EDITOR);
            if (hostEditor == null) {
                hostEditor = editor;
            }
            boolean[] result = new boolean[1];
            CaretTask check = (___, __) -> result[0] = true;
            if (myRunForEachCaret) {
                hostEditor.getCaretModel().runForEachCaret(caret -> doIfEnabled(caret, dataContext, check));
            }
            else {
                doIfEnabled(hostEditor.getCaretModel().getCurrentCaret(), dataContext, check);
            }
            return result[0];
        }
        finally {
            inCheck = false;
        }
    }

    private void doIfEnabled(@Nonnull Caret hostCaret, @Nullable DataContext context, @Nonnull CaretTask task) {
        DataContext caretContext =
            context == null ? null : CodeEditorInternalHelper.getInstance().createCaretDataContext(context, hostCaret);
        Editor editor = hostCaret.getEditor();
        if (myWorksInInjected && caretContext != null) {
            DataContext injectedCaretContext = AnActionEvent.getInjectedDataContext(caretContext);
            Caret injectedCaret = injectedCaretContext.getData(Caret.KEY);
            if (injectedCaret != null && injectedCaret != hostCaret
                && isEnabledForCaret(injectedCaret.getEditor(), injectedCaret, injectedCaretContext)) {
                task.perform(injectedCaret, injectedCaretContext);
                return;
            }
        }
        if (isEnabledForCaret(editor, hostCaret, caretContext)) {
            task.perform(hostCaret, caretContext);
        }
    }

    static boolean ensureInjectionUpToDate(@Nonnull Caret hostCaret) {
        return CodeEditorInternalHelper.getInstance().ensureInjectionUpToDate(hostCaret);
    }

    /**
     * Implementations can override this method to define whether handler is enabled for a specific caret in a given editor.
     */
    protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        if (inCheck) {
            return true;
        }
        inCheck = true;
        try {
            //noinspection deprecation
            return isEnabled(editor, dataContext);
        }
        finally {
            inCheck = false;
        }
    }

    /**
     * If {@code caret} is {@code null}, checks whether handler is enabled in general (i.e. enabled for at least one caret in editor),
     * if {@code caret} is not {@code null}, checks whether it's enabled for specified caret.
     */
    public final boolean isEnabled(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        //noinspection deprecation
        return caret == null ? isEnabled(editor, dataContext) : isEnabledForCaret(editor, caret, dataContext);
    }

    /**
     * @deprecated To implement action logic, override
     * {@link #doExecute(Editor, Caret, DataContext)},
     * to invoke the handler, call
     * {@link #execute(Editor, Caret, DataContext)}.
     */
    @Deprecated
    public void execute(@Nonnull Editor editor, @Nullable DataContext dataContext) {
        if (inExecution) {
            return;
        }
        try {
            inExecution = true;
            execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
        }
        finally {
            inExecution = false;
        }
    }

    /**
     * Executes the action in the context of given caret. Subclasses should override this method.
     *
     * @param editor      the editor in which the action is invoked.
     * @param caret       the caret for which the action is performed at the moment, or {@code null} if it's a 'one-off' action executed
     *                    without current context
     * @param dataContext the data context for the action.
     */
    protected void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (inExecution) {
            return;
        }
        try {
            inExecution = true;
            //noinspection deprecation
            execute(editor, dataContext);
        }
        finally {
            inExecution = false;
        }
    }

    public boolean executeInCommand(@Nonnull Editor editor, DataContext dataContext) {
        return true;
    }

    public boolean runForAllCarets() {
        return myRunForEachCaret;
    }

    /**
     * Executes the action in the context of given caret. If the caret is {@code null}, and the handler is a 'per-caret' handler,
     * it's executed for all carets.
     *
     * @param editor      the editor in which the action is invoked.
     * @param dataContext the data context for the action.
     */
    public final void execute(@Nonnull Editor editor, @Nullable Caret contextCaret, DataContext dataContext) {
        Editor hostEditor = dataContext == null ? null : dataContext.getData(EditorKeys.HOST_EDITOR);
        if (hostEditor == null) {
            hostEditor = editor;
        }
        if (contextCaret == null && runForAllCarets()) {
            hostEditor.getCaretModel().runForEachCaret(caret -> {
                if (myWorksInInjected) {
                    ensureInjectionUpToDate(caret);
                }
                doIfEnabled(caret, dataContext, (caret1, dc) -> doExecute(caret1.getEditor(), caret1, dc));
            });
        }
        else if (contextCaret == null) {
            if (myWorksInInjected) {
                ensureInjectionUpToDate(hostEditor.getCaretModel().getCurrentCaret());
            }
            doIfEnabled(
                hostEditor.getCaretModel().getCurrentCaret(),
                dataContext,
                (caret, dc) -> doExecute(caret.getEditor(), null, dc)
            );
        }
        else {
            doExecute(editor, contextCaret, dataContext);
        }
    }

    public void setWorksInInjected(boolean worksInInjected) {
        myWorksInInjected = worksInInjected;
    }

    public DocCommandGroupId getCommandGroupId(@Nonnull Editor editor) {
        // by default avoid merging two consequential commands, and, in the same time, pass along the Document
        return DocCommandGroupId.noneGroupId(editor.getDocument());
    }

    public abstract static class ForEachCaret extends EditorActionHandler {
        protected ForEachCaret() {
            super(true);
        }

        @Override
        protected abstract void doExecute(
            @Nonnull Editor editor,
            @SuppressWarnings("NullableProblems") @Nonnull Caret caret,
            DataContext dataContext
        );
    }

    @FunctionalInterface
    private interface CaretTask {
        void perform(@Nonnull Caret caret, @Nullable DataContext dataContext);
    }
}
