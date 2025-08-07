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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.document.DocCommandGroupId;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

/**
 * Base class for PSI-aware editor actions that need to support multiple carets.
 * Recognizes multi-root PSI and injected fragments, so different carets might be processed in context of different
 * {@link Editor} and {@link PsiFile} instances.
 * <p>
 * Implementations should implement {@link #getHandler()} method, and might override {@link
 * #isValidFor(Project, Editor, Caret, PsiFile)} method.
 *
 * @see MultiCaretCodeInsightActionHandler
 */
public abstract class MultiCaretCodeInsightAction extends AnAction {
    protected MultiCaretCodeInsightAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor hostEditor = e.getRequiredData(Editor.KEY);
        actionPerformedImpl(project, hostEditor);
    }

    @RequiredUIAccess
    public void actionPerformedImpl(Project project, Editor hostEditor) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(getTemplatePresentation().getTextValue())
            .groupId(DocCommandGroupId.noneGroupId(hostEditor.getDocument()))
            .inWriteAction()
            .run(() -> {
                MultiCaretCodeInsightActionHandler handler = getHandler();
                try {
                    iterateOverCarets(project, hostEditor, handler);
                }
                finally {
                    handler.postInvoke();
                }
            });

        hostEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor hostEditor = e.getData(Editor.KEY);
        if (project == null || hostEditor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final SimpleReference<Boolean> enabled = new SimpleReference<>(false);
        iterateOverCarets(
            project,
            hostEditor,
            new MultiCaretCodeInsightActionHandler() {
                @Override
                public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Caret caret, @Nonnull PsiFile file) {
                    if (isValidFor(project, editor, caret, file)) {
                        enabled.set(true);
                    }
                }
            }
        );
        e.getPresentation().setEnabled(enabled.get());
    }

    private static void iterateOverCarets(
        @Nonnull Project project,
        @Nonnull Editor hostEditor,
        @Nonnull MultiCaretCodeInsightActionHandler handler
    ) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = documentManager.getCachedPsiFile(hostEditor.getDocument());
        documentManager.commitAllDocuments();

        hostEditor.getCaretModel().runForEachCaret(caret -> {
            Editor editor = hostEditor;
            if (psiFile != null) {
                Caret injectedCaret = InjectedLanguageUtil.getCaretForInjectedLanguageNoCommit(caret, psiFile);
                if (injectedCaret != null) {
                    caret = injectedCaret;
                    editor = caret.getEditor();
                }
            }
            PsiFile file = PsiUtilBase.getPsiFileInEditor(caret, project);
            if (file != null) {
                handler.invoke(project, editor, caret, file);
            }
        });
    }

    /**
     * During action status update this method is invoked for each caret in editor. If at least for a single caret it returns
     * <code>true</code>, action is considered enabled.
     */
    protected boolean isValidFor(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Caret caret, @Nonnull PsiFile file) {
        return true;
    }

    @Nonnull
    protected abstract MultiCaretCodeInsightActionHandler getHandler();
}
