/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.document.DocCommandGroupId;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.refactoring.ContextAwareActionHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.UpdateInBackground;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public abstract class BaseRefactoringAction extends AnAction implements UpdateInBackground {
    private final Predicate<Language> myLanguageCondition = this::isAvailableForLanguage;

    protected abstract boolean isAvailableInEditorOnly();

    protected abstract boolean isEnabledOnElements(@Nonnull PsiElement[] elements);

    protected boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        return true;
    }

    public boolean hasAvailableHandler(@Nonnull DataContext dataContext) {
        final RefactoringActionHandler handler = getHandler(dataContext);
        if (handler != null) {
            if (handler instanceof ContextAwareActionHandler contextAwareActionHandler) {
                final Editor editor = dataContext.getData(Editor.KEY);
                final PsiFile file = dataContext.getData(PsiFile.KEY);
                if (editor != null && file != null && !contextAwareActionHandler.isAvailableForQuickList(editor, file, dataContext)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Nullable
    protected abstract RefactoringActionHandler getHandler(@Nonnull DataContext dataContext);

    @RequiredUIAccess
    @Override
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        final Editor editor = e.getData(Editor.KEY);
        final PsiElement[] elements = getPsiElementArray(dataContext);

        Runnable markEventCount = UIAccess.current().markEventCount();
        RefactoringActionHandler handler;
        try {
            handler = getHandler(dataContext);
        }
        catch (ProcessCanceledException ignored) {
            return;
        }
        if (handler == null) {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.errorWrongCaretPositionSymbolToRefactor()).get(),
                RefactoringLocalize.cannotPerformRefactoring().get(),
                null
            );
            return;
        }

        if (!InplaceRefactoring.canStartAnotherRefactoring(editor, project, handler, elements)) {
            InplaceRefactoring.unableToStartWarning(project, editor);
            return;
        }

        if (InplaceRefactoring.getActiveInplaceRenamer(editor) == null) {
            final LookupEx lookup = LookupManager.getActiveLookup(editor);
            if (lookup != null) {
                assert editor != null;
                Document doc = editor.getDocument();
                DocCommandGroupId group = DocCommandGroupId.noneGroupId(doc);
                CommandProcessor.getInstance().newCommand()
                    .project(editor.getProject())
                    .document(doc)
                    .name(ApplicationLocalize.titleCodeCompletion())
                    .groupId(group)
                    .run(() -> lookup.finishLookup(Lookup.NORMAL_SELECT_CHAR));
            }
        }

        markEventCount.run();

        if (editor != null) {
            final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }
            DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, file);
            handler.invoke(project, editor, file, dataContext);
        }
        else {
            handler.invoke(project, elements, dataContext);
        }
    }

    protected boolean isEnabledOnDataContext(DataContext dataContext) {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setVisible(true);
        presentation.setEnabled(true);
        DataContext dataContext = e.getDataContext();
        Project project = e.getData(Project.KEY);
        if (project == null || isHidden()) {
            hideAction(e);
            return;
        }

        Editor editor = e.getData(Editor.KEY);
        PsiFile file = e.getData(PsiFile.KEY);
        if (file != null) {
            if (file instanceof PsiCompiledElement || !isAvailableForFile(file)) {
                hideAction(e);
                return;
            }
        }

        if (editor == null) {
            if (isAvailableInEditorOnly()) {
                hideAction(e);
                return;
            }
            final PsiElement[] elements = getPsiElementArray(dataContext);
            final boolean isEnabled = isEnabledOnDataContext(dataContext) || elements.length != 0 && isEnabledOnElements(elements);
            if (!isEnabled) {
                disableAction(e);
            }
        }
        else {
            PsiElement element = e.getData(PsiElement.KEY);
            Language[] languages = e.getData(LangDataKeys.CONTEXT_LANGUAGES);
            if (element == null || !isAvailableForLanguage(element.getLanguage())) {
                if (file == null) {
                    hideAction(e);
                    return;
                }
                element = getElementAtCaret(editor, file);
            }

            if (element == null || element instanceof SyntheticElement || languages == null) {
                hideAction(e);
                return;
            }

            boolean isVisible = ContainerUtil.find(languages, myLanguageCondition) != null;
            if (isVisible) {
                boolean isEnabled = isAvailableOnElementInEditorAndFile(element, editor, file, dataContext);
                if (!isEnabled) {
                    disableAction(e);
                }
            }
            else {
                hideAction(e);
            }
        }
    }

    private static void hideAction(AnActionEvent e) {
        e.getPresentation().setVisible(false);
        disableAction(e);
    }

    protected boolean isHidden() {
        return false;
    }

    @RequiredReadAction
    public static PsiElement getElementAtCaret(final Editor editor, final PsiFile file) {
        final int offset = fixCaretOffset(editor);
        PsiElement element = file.findElementAt(offset);
        if (element == null && offset == file.getTextLength()) {
            element = file.findElementAt(offset - 1);
        }

        if (element instanceof PsiWhiteSpace) {
            element = file.findElementAt(element.getTextRange().getStartOffset() - 1);
        }
        return element;
    }

    private static int fixCaretOffset(final Editor editor) {
        final int caret = editor.getCaretModel().getOffset();
        if (editor.getSelectionModel().hasSelection() && caret == editor.getSelectionModel().getSelectionEnd()) {
            return Math.max(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd() - 1);
        }

        return caret;
    }

    private static void disableAction(final AnActionEvent e) {
        e.getPresentation().setEnabled(false);
    }

    protected boolean isAvailableForLanguage(Language language) {
        return true;
    }

    protected boolean isAvailableForFile(PsiFile file) {
        return true;
    }

    @Nonnull
    public static PsiElement[] getPsiElementArray(DataContext dataContext) {
        PsiElement[] psiElements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        if (psiElements == null || psiElements.length == 0) {
            PsiElement element = dataContext.getData(PsiElement.KEY);
            if (element != null) {
                psiElements = new PsiElement[]{element};
            }
        }

        if (psiElements == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        List<PsiElement> filtered = null;
        for (PsiElement element : psiElements) {
            if (element instanceof SyntheticElement) {
                if (filtered == null) {
                    filtered = new ArrayList<>(Arrays.asList(element));
                }
                filtered.remove(element);
            }
        }
        return filtered == null ? psiElements : PsiUtilCore.toPsiElementArray(filtered);
    }
}
