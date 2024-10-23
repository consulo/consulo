// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.action;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.DocCommandGroupId;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.UpdateInBackground;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class CodeInsightAction extends AnAction implements UpdateInBackground {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            Editor editor = getEditor(e.getDataContext(), project, false);
            actionPerformedImpl(project, editor);
        }
    }

    @Nullable
    protected Editor getEditor(@Nonnull DataContext dataContext, @Nonnull Project project, boolean forUpdate) {
        return dataContext.getData(Editor.KEY);
    }

    @RequiredUIAccess
    public void actionPerformedImpl(@Nonnull final Project project, final Editor editor) {
        if (editor == null) {
            return;
        }
        //final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) {
            return;
        }
        final CodeInsightActionHandler handler = getHandler();
        PsiElement elementToMakeWritable = handler.getElementToMakeWritable(psiFile);
        if (elementToMakeWritable != null && !(LanguageEditorUtil.checkModificationAllowed(editor)
            && FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable))) {
            return;
        }

        CommandProcessor.getInstance().executeCommand(
            project,
            () -> {
                Application application = project.getApplication();
                final Runnable action = () -> {
                    if (!application.isHeadlessEnvironment() && !editor.getContentComponent().isShowing()) {
                        return;
                    }
                    handler.invoke(project, editor, psiFile);
                };
                if (handler.startInWriteAction()) {
                    application.runWriteAction(action);
                }
                else {
                    action.run();
                }
            },
            getCommandName(),
            DocCommandGroupId.noneGroupId(editor.getDocument()),
            editor.getDocument()
        );
    }

    @RequiredUIAccess
    @Override
    public void beforeActionPerformedUpdate(@Nonnull AnActionEvent e) {
        CodeInsightEditorAction.beforeActionPerformedUpdate(e);
        super.beforeActionPerformedUpdate(e);
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Project project = e.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        final DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext, project, true);
        if (editor == null) {
            presentation.setEnabled(false);
            return;
        }

        final PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            presentation.setEnabled(false);
            return;
        }

        update(presentation, project, editor, file, dataContext, e.getPlace());
    }

    protected void update(@Nonnull Presentation presentation, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        presentation.setEnabled(isValidForFile(project, editor, file));
    }

    protected void update(
        @Nonnull Presentation presentation,
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext dataContext,
        @Nullable String actionPlace
    ) {
        update(presentation, project, editor, file);
    }

    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return true;
    }

    @Nonnull
    protected abstract CodeInsightActionHandler getHandler();

    protected String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text == null ? "" : text;
    }
}
