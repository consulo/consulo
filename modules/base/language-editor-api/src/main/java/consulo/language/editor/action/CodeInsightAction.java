// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.action;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataContext;
import consulo.document.DocCommandGroupId;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class CodeInsightAction extends AnAction {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            Editor hostEditor = e.getData(EditorKeys.HOST_EDITOR);
            if (hostEditor != null) {
                PsiFile file = PsiDocumentManager.getInstance(project).getCachedPsiFile(hostEditor.getDocument());
                if (file != null) {
                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                }
            }

            Editor editor = getEditor(e.getDataContext(), project, false);

            actionPerformedImpl(project, editor);
        }
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Nullable
    protected Editor getEditor(@Nonnull DataContext dataContext, @Nonnull Project project, boolean forUpdate) {
        return dataContext.getData(Editor.KEY);
    }

    @RequiredUIAccess
    public void actionPerformedImpl(@Nonnull Project project, Editor editor) {
        if (editor == null) {
            return;
        }
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) {
            return;
        }
        CodeInsightActionHandler handler = getHandler();
        PsiElement elementToMakeWritable = handler.getElementToMakeWritable(psiFile);
        if (elementToMakeWritable != null && !(LanguageEditorUtil.checkModificationAllowed(editor)
            && FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable))) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(editor.getDocument())
            .name(getTemplatePresentation().getTextValue())
            .groupId(DocCommandGroupId.noneGroupId(editor.getDocument()))
            .inWriteActionIf(handler.startInWriteAction())
            .run(() -> {
                if (Application.get().isHeadlessEnvironment() || editor.getContentComponent().isShowing()) {
                    handler.invoke(project, editor, psiFile);
                }
            });
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

        DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext, project, true);
        if (editor == null) {
            presentation.setEnabled(false);
            return;
        }

        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
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
