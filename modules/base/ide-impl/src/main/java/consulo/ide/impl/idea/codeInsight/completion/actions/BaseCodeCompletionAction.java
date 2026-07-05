// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion.actions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionHandlerBase;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;

import java.awt.event.InputEvent;

/**
 * @author peter
 */
public abstract class BaseCodeCompletionAction extends DumbAwareAction
    implements HintManagerImpl.ActionToIgnore, AnActionWithAsyncUpdate {
    protected BaseCodeCompletionAction() {
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    protected BaseCodeCompletionAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    @RequiredUIAccess
    protected void invokeCompletion(AnActionEvent e, CompletionType type, int time) {
        Editor editor = e.getRequiredData(Editor.KEY);
        Project project = editor.getProject();
        assert project != null;
        InputEvent inputEvent = e.getInputEvent();
        createHandler(type, true, false, true)
            .invokeCompletion(project, editor, time, inputEvent != null && inputEvent.getModifiers() != 0);
    }

    
    public CodeCompletionHandlerBase createHandler(
        CompletionType completionType,
        boolean invokedExplicitly,
        boolean autopopup,
        boolean synchronous
    ) {
        return new CodeCompletionHandlerBase(completionType, invokedExplicitly, autopopup, synchronous);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            presentation.setEnabled(false);

            Editor editor = e.getData(EditorKeys.EDITOR_SNAPSHOT);
            if (editor == null) {
                return;
            }

            Project project = editor.getProject();
            PsiFile psiFile = project == null ? null : PsiUtilBase.getPsiFileInEditor(editor, project);
            if (psiFile == null) {
                return;
            }

            if (!project.getApplication().isHeadlessEnvironment() && !editor.getContentComponent().isShowing()) {
                return;
            }
            presentation.setEnabled(true);
        }).toCoroutine();
    }
}
