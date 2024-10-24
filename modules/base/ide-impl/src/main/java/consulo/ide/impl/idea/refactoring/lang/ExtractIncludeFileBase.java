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

package consulo.ide.impl.idea.refactoring.lang;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.find.FindManager;
import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.ide.impl.idea.ui.ReplacePromptDialog;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.PsiEquivalenceUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.RefactoringBundle;
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
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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


    protected abstract void doReplaceRange(final String includePath, final T first, final T last);

    @Nonnull
    protected String doExtract(
        final PsiDirectory targetDirectory,
        final String targetfileName,
        final T first,
        final T last,
        final Language includingLanguage
    ) throws IncorrectOperationException {
        final PsiFile file = targetDirectory.createFile(targetfileName);
        Project project = targetDirectory.getProject();
        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        final Document document = documentManager.getDocument(file);
        document.replaceString(0, document.getTextLength(), first.getText().trim());
        documentManager.commitDocument(document);
        CodeStyleManager.getInstance(PsiManager.getInstance(project).getProject()).reformat(file);  //TODO: adjustLineIndent

        final String relativePath = PsiFileSystemItemUtil.getRelativePath(first.getContainingFile(), file);
        if (relativePath == null) {
            throw new IncorrectOperationException("Cannot extract!");
        }
        return relativePath;
    }

    protected abstract boolean verifyChildRange(final T first, final T last);

    @RequiredUIAccess
    private void replaceDuplicates(
        final String includePath,
        final List<IncludeDuplicate<T>> duplicates,
        final Editor editor,
        final Project project
    ) {
        if (duplicates.size() > 0) {
            final LocalizeValue message =
                RefactoringLocalize.ideaHasFoundFragmentsThatCanBeReplacedWithIncludeDirective(Application.get().getName());
            final int exitCode = Messages.showYesNoDialog(project, message.get(), getRefactoringName(), UIUtil.getInformationIcon());
            if (exitCode == Messages.YES) {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> {
                        boolean replaceAll = false;
                        for (IncludeDuplicate<T> pair : duplicates) {
                            if (!replaceAll) {

                                highlightInEditor(project, pair, editor);

                                ReplacePromptDialog promptDialog =
                                    new ReplacePromptDialog(false, RefactoringLocalize.replaceFragment().get(), project);
                                promptDialog.show();
                                final int promptResult = promptDialog.getExitCode();
                                if (promptResult == FindManager.PromptResult.SKIP) {
                                    continue;
                                }
                                if (promptResult == FindManager.PromptResult.CANCEL) {
                                    break;
                                }

                                if (promptResult == FindManager.PromptResult.OK) {
                                    doReplaceRange(includePath, pair.getStart(), pair.getEnd());
                                }
                                else if (promptResult == FindManager.PromptResult.ALL) {
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
                    },
                    RefactoringLocalize.removeDuplicatesCommand().get(),
                    null
                );
            }
        }
    }

    private static void highlightInEditor(final Project project, final IncludeDuplicate pair, final Editor editor) {
        final HighlightManager highlightManager = HighlightManager.getInstance(project);
        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        final int startOffset = pair.getStart().getTextRange().getStartOffset();
        final int endOffset = pair.getEnd().getTextRange().getEndOffset();
        highlightManager.addRangeHighlight(editor, startOffset, endOffset, attributes, true, null);
        final LogicalPosition logicalPosition = editor.offsetToLogicalPosition(startOffset);
        editor.getScrollingModel().scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    }

    @Nonnull
    protected Language getLanguageForExtract(PsiElement firstExtracted) {
        return firstExtracted.getLanguage();
    }

    @Nullable
    private static FileType getFileType(final Language language) {
        final FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
        for (FileType fileType : fileTypes) {
            if (fileType instanceof LanguageFileType && language.equals(((LanguageFileType)fileType).getLanguage())) {
                return fileType;
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file, DataContext dataContext) {
        myIncludingFile = file;
        if (!editor.getSelectionModel().hasSelection()) {
            String message = RefactoringBundle.getCannotRefactorMessage(RefactoringLocalize.noSelection().get());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }
        final int start = editor.getSelectionModel().getSelectionStart();
        final int end = editor.getSelectionModel().getSelectionEnd();

        final Pair<T, T> children = findPairToExtract(start, end);
        if (children == null) {
            String message =
                RefactoringBundle.getCannotRefactorMessage(RefactoringLocalize.selectionDoesNotFormAFragmentForExtraction().get());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        if (!verifyChildRange(children.getFirst(), children.getSecond())) {
            String message =
                RefactoringBundle.getCannotRefactorMessage(RefactoringLocalize.cannotExtractSelectedElementsIntoIncludeFile().get());
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        final FileType fileType = getFileType(getLanguageForExtract(children.getFirst()));
        if (!(fileType instanceof LanguageFileType)) {
            String message = RefactoringLocalize.theLanguageForSelectedElementsHasNoAssociatedFileType().get();
            CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
            return;
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) {
            return;
        }

        ExtractIncludeDialog dialog = createDialog(file.getContainingDirectory(), getExtractExtension(fileType, children.first));
        dialog.show();
        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            final PsiDirectory targetDirectory = dialog.getTargetDirectory();
            LOG.assertTrue(targetDirectory != null);
            final String targetfileName = dialog.getTargetFileName();
            CommandProcessor.getInstance().executeCommand(
                project,
                () -> project.getApplication().runWriteAction(() -> {
                    try {
                        final List<IncludeDuplicate<T>> duplicates = new ArrayList<>();
                        final T first = children.getFirst();
                        final T second = children.getSecond();
                        PsiEquivalenceUtil.findChildRangeDuplicates(
                            first,
                            second,
                            file,
                            (start1, end1) -> duplicates.add(new IncludeDuplicate<>((T)start1, (T)end1))
                        );
                        final String includePath = processPrimaryFragment(first, second, targetDirectory, targetfileName, file);
                        editor.getCaretModel().moveToOffset(first.getTextRange().getStartOffset());

                        project.getApplication().invokeLater(() -> replaceDuplicates(includePath, duplicates, editor, project));
                    }
                    catch (IncorrectOperationException e) {
                        CommonRefactoringUtil.showErrorMessage(getRefactoringName(), e.getMessage(), null, project);
                    }

                    editor.getSelectionModel().removeSelection();
                }),
                getRefactoringName(),
                null
            );
        }
    }

    protected ExtractIncludeDialog createDialog(final PsiDirectory containingDirectory, final String extractExtension) {
        return new ExtractIncludeDialog(containingDirectory, extractExtension);
    }

    @Nullable
    protected abstract Pair<T, T> findPairToExtract(int start, int end);

    @NonNls
    protected String getExtractExtension(final FileType extractFileType, final T first) {
        return extractFileType.getDefaultExtension();
    }

    public boolean isValidRange(final T firstToExtract, final T lastToExtract) {
        return verifyChildRange(firstToExtract, lastToExtract);
    }

    public String processPrimaryFragment(
        final T firstToExtract,
        final T lastToExtract,
        final PsiDirectory targetDirectory,
        final String targetfileName,
        final PsiFile srcFile
    ) throws IncorrectOperationException {
        final String includePath = doExtract(targetDirectory, targetfileName, firstToExtract, lastToExtract, srcFile.getLanguage());

        doReplaceRange(includePath, firstToExtract, lastToExtract);
        return includePath;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionTitleValue() {
        return LocalizeValue.localizeTODO("Extract Include File...");
    }

    protected String getRefactoringName() {
        return REFACTORING_NAME.get();
    }
}
