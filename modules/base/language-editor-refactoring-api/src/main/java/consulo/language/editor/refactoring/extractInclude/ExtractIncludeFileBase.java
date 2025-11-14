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
package consulo.language.editor.refactoring.extractInclude;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.PsiEquivalenceUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.TitledHandler;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.*;
import consulo.language.psi.path.PsiFileSystemItemUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ReplacePromptDialog;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ven
 */
public abstract class ExtractIncludeFileBase<T extends PsiElement> implements RefactoringActionHandler, TitledHandler {
    private static final Logger LOG = Logger.getInstance(ExtractIncludeFileBase.class);
    private static final LocalizeValue REFACTORING_NAME = RefactoringLocalize.extractIncludeFileTitle();
    protected PsiFile myIncludingFile;
    public static final String HELP_ID = "refactoring.extractInclude";

    private static class IncludeDuplicate<E extends PsiElement> {
        private final SmartPsiElementPointer<E> myStart;
        private final SmartPsiElementPointer<E> myEnd;

        private IncludeDuplicate(E start, E end) {
            myStart = SmartPointerManager.getInstance(start.getProject()).createSmartPsiElementPointer(start);
            myEnd = SmartPointerManager.getInstance(start.getProject()).createSmartPsiElementPointer(end);
        }

        @RequiredReadAction
        E getStart() {
            return myStart.getElement();
        }

        @RequiredReadAction
        E getEnd() {
            return myEnd.getElement();
        }
    }

    protected abstract void doReplaceRange(String includePath, T first, T last);

    @Nonnull
    @RequiredReadAction
    protected String doExtract(PsiDirectory targetDirectory, String targetFileName, T first, T last, Language includingLanguage)
        throws IncorrectOperationException {
        PsiFile file = targetDirectory.createFile(targetFileName);
        Project project = targetDirectory.getProject();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = documentManager.getDocument(file);
        document.replaceString(0, document.getTextLength(), first.getText().trim());
        documentManager.commitDocument(document);
        CodeStyleManager.getInstance(PsiManager.getInstance(project).getProject()).reformat(file);  //TODO: adjustLineIndent

        String relativePath = PsiFileSystemItemUtil.getRelativePath(first.getContainingFile(), file);
        if (relativePath == null) {
            throw new IncorrectOperationException("Cannot extract!");
        }
        return relativePath;
    }

    protected abstract boolean verifyChildRange(T first, T last);

    @RequiredUIAccess
    private void replaceDuplicates(
        String includePath,
        List<IncludeDuplicate<T>> duplicates,
        Editor editor,
        @Nonnull Project project
    ) {
        if (duplicates.isEmpty()) {
            return;
        }
        LocalizeValue message =
            RefactoringLocalize.ideaHasFoundFragmentsThatCanBeReplacedWithIncludeDirective(project.getApplication().getName());
        int exitCode = Messages.showYesNoDialog(project, message.get(), getRefactoringName().get(), UIUtil.getInformationIcon());
        if (exitCode != Messages.YES) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(RefactoringLocalize.removeDuplicatesCommand())
            .run(() -> {
                boolean replaceAll = false;
                for (IncludeDuplicate<T> pair : duplicates) {
                    if (!replaceAll) {
                        highlightInEditor(project, pair, editor);

                        ReplacePromptDialog promptDialog = new ReplacePromptDialog(false, RefactoringLocalize.replaceFragment(), project);
                        promptDialog.show();
                        ReplacePromptDialog.PromptResult promptResult = promptDialog.getPromptResult();
                        if (promptResult == ReplacePromptDialog.PromptResult.SKIP) {
                            continue;
                        }
                        if (promptResult == ReplacePromptDialog.PromptResult.CANCEL) {
                            break;
                        }

                        if (promptResult == ReplacePromptDialog.PromptResult.OK) {
                            doReplaceRange(includePath, pair.getStart(), pair.getEnd());
                        }
                        else if (promptResult == ReplacePromptDialog.PromptResult.ALL) {
                            doReplaceRange(includePath, pair.getStart(), pair.getEnd());
                            replaceAll = true;
                        }
                        else {
                            LOG.error("Unknown return status");
                        }
                    }
                    else {
                        doReplaceRange(includePath, pair.getStart(), pair.getEnd());
                    }
                }
            });
    }

    @RequiredReadAction
    private static void highlightInEditor(Project project, IncludeDuplicate pair, Editor editor) {
        HighlightManager highlightManager = HighlightManager.getInstance(project);
        int startOffset = pair.getStart().getTextRange().getStartOffset();
        int endOffset = pair.getEnd().getTextRange().getEndOffset();
        highlightManager.addRangeHighlight(editor, startOffset, endOffset, EditorColors.SEARCH_RESULT_ATTRIBUTES, true, null);
        LogicalPosition logicalPosition = editor.offsetToLogicalPosition(startOffset);
        editor.getScrollingModel().scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    }

    @Nonnull
    @RequiredReadAction
    protected Language getLanguageForExtract(PsiElement firstExtracted) {
        return firstExtracted.getLanguage();
    }

    @Nullable
    private static FileType getFileType(Language language) {
        FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
        for (FileType fileType : fileTypes) {
            if (fileType instanceof LanguageFileType languageFileType && language.equals(languageFileType.getLanguage())) {
                return fileType;
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        myIncludingFile = file;
        if (!editor.getSelectionModel().hasSelection()) {
            LocalizeValue message = RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.noSelection());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();

        Pair<T, T> children = findPairToExtract(start, end);
        if (children == null) {
            LocalizeValue message =
                RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.selectionDoesNotFormAFragmentForExtraction());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        if (!verifyChildRange(children.getFirst(), children.getSecond())) {
            LocalizeValue message =
                RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.cannotExtractSelectedElementsIntoIncludeFile());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        FileType fileType = getFileType(getLanguageForExtract(children.getFirst()));
        if (!(fileType instanceof LanguageFileType)) {
            LocalizeValue message = RefactoringLocalize.theLanguageForSelectedElementsHasNoAssociatedFileType();
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) {
            return;
        }

        ExtractIncludeDialog dialog = createDialog(file.getContainingDirectory(), getExtractExtension(fileType, children.first));
        dialog.show();
        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            PsiDirectory targetDirectory = dialog.getTargetDirectory();
            LOG.assertTrue(targetDirectory != null);
            String targetFileName = dialog.getTargetFileName();
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(getRefactoringName())
                .inWriteAction()
                .run(() -> {
                    try {
                        List<IncludeDuplicate<T>> duplicates = new ArrayList<>();
                        T first = children.getFirst();
                        T second = children.getSecond();
                        PsiEquivalenceUtil.findChildRangeDuplicates(
                            first,
                            second,
                            file,
                            (start1, end1) -> duplicates.add(new IncludeDuplicate<>((T) start1, (T) end1))
                        );
                        String includePath = processPrimaryFragment(first, second, targetDirectory, targetFileName, file);
                        editor.getCaretModel().moveToOffset(first.getTextRange().getStartOffset());

                        project.getApplication().invokeLater(() -> replaceDuplicates(includePath, duplicates, editor, project));
                    }
                    catch (IncorrectOperationException e) {
                        CommonRefactoringUtil.showErrorMessage(
                            getRefactoringName(),
                            LocalizeValue.ofNullable(e.getMessage()),
                            null,
                            project
                        );
                    }

                    editor.getSelectionModel().removeSelection();
                });
        }
    }

    protected ExtractIncludeDialog createDialog(PsiDirectory containingDirectory, String extractExtension) {
        return new ExtractIncludeDialog(containingDirectory, extractExtension);
    }

    @Nullable
    protected abstract Pair<T, T> findPairToExtract(int start, int end);

    protected String getExtractExtension(FileType extractFileType, T first) {
        return extractFileType.getDefaultExtension();
    }

    public boolean isValidRange(T firstToExtract, T lastToExtract) {
        return verifyChildRange(firstToExtract, lastToExtract);
    }

    @RequiredReadAction
    public String processPrimaryFragment(
        T firstToExtract,
        T lastToExtract,
        PsiDirectory targetDirectory,
        String targetFileName,
        PsiFile srcFile
    ) throws IncorrectOperationException {
        String includePath = doExtract(targetDirectory, targetFileName, firstToExtract, lastToExtract, srcFile.getLanguage());

        doReplaceRange(includePath, firstToExtract, lastToExtract);
        return includePath;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionTitleValue() {
        return RefactoringLocalize.extractIncludeFileActionTitle();
    }

    @Nonnull
    protected LocalizeValue getRefactoringName() {
        return REFACTORING_NAME;
    }
}
