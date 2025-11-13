/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.util;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.UsageInfo;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author ven
 */
public class CommonRefactoringUtil {
    private CommonRefactoringUtil() {
    }

    @RequiredUIAccess
    public static void showErrorMessage(
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue message,
        @Nullable String helpId,
        @Nonnull Project project
    ) {
        if (project.getApplication().isUnitTestMode()) {
            throw new RuntimeException(message.get());
        }
        RefactoringMessageDialog dialog =
            new RefactoringMessageDialog(title, message, helpId, UIUtil.getErrorIcon(), false, project);
        dialog.show();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    public static void showErrorMessage(String title, String message, @Nullable String helpId, @Nonnull Project project) {
        showErrorMessage(LocalizeValue.ofNullable(title), LocalizeValue.ofNullable(message), helpId, project);
    }

    // order of usages across different files is irrelevant
    @RequiredReadAction
    public static void sortDepthFirstRightLeftOrder(UsageInfo[] usages) {
        Arrays.sort(usages, (usage1, usage2) -> {
            PsiElement element1 = usage1.getElement(), element2 = usage2.getElement();
            if (element1 == element2) {
                return 0;
            }
            if (element1 == null) {
                return 1;
            }
            if (element2 == null) {
                return -1;
            }
            return element2.getTextRange().getStartOffset() - element1.getTextRange().getStartOffset();
        });
    }

    /**
     * Fatal refactoring problem during unit test run. Corresponds to message of modal dialog shown during user driven refactoring.
     */
    public static class RefactoringErrorHintException extends RuntimeException {
        public RefactoringErrorHintException(@Nonnull LocalizeValue message) {
            super(message.get());
        }

        public RefactoringErrorHintException(String message) {
            super(message);
        }
    }

    @RequiredUIAccess
    public static void showErrorHint(
        @Nonnull Project project,
        @Nullable Editor editor,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue title,
        @Nullable String helpId
    ) {
        if (project.getApplication().isUnitTestMode()) {
            throw new RefactoringErrorHintException(message.get());
        }

        project.getApplication().invokeLater(() -> {
            if (editor == null || editor.getComponent().getRootPane() == null) {
                showErrorMessage(title, message, helpId, project);
            }
            else {
                HintManager.getInstance().showErrorHint(editor, message);
            }
        });
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    public static void showErrorHint(
        @Nonnull Project project,
        @Nullable Editor editor,
        @Nonnull String message,
        @Nonnull String title,
        @Nullable String helpId
    ) {
        showErrorHint(project, editor, LocalizeValue.ofNullable(message), LocalizeValue.ofNullable(title), helpId);
    }

    public static String htmlEmphasize(@Nonnull String text) {
        return StringUtil.htmlEmphasize(text);
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(@Nonnull PsiElement element) {
        VirtualFile file = element.getContainingFile().getVirtualFile();
        return file != null && !ReadonlyStatusHandler.getInstance(element.getProject()).ensureFilesWritable(file).hasReadonlyFiles();
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(@Nonnull Project project, @Nonnull PsiElement element) {
        return checkReadOnlyStatus(element, project, RefactoringLocalize.refactoringCannotBePerformed());
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(@Nonnull Project project, @Nonnull PsiElement... elements) {
        return checkReadOnlyStatus(
            project,
            Collections.<PsiElement>emptySet(),
            Arrays.asList(elements),
            RefactoringLocalize.refactoringCannotBePerformed(),
            true
        );
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(
        @Nonnull Project project,
        @Nonnull Collection<? extends PsiElement> elements,
        boolean notifyOnFail
    ) {
        return checkReadOnlyStatus(
            project,
            Collections.<PsiElement>emptySet(),
            elements,
            RefactoringLocalize.refactoringCannotBePerformed(),
            notifyOnFail
        );
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(@Nonnull PsiElement element, @Nonnull Project project, @Nonnull LocalizeValue messagePrefix) {
        return element.isWritable()
            || checkReadOnlyStatus(project, Collections.<PsiElement>emptySet(), Collections.singleton(element), messagePrefix, true);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(@Nonnull PsiElement element, @Nonnull Project project, @Nonnull String messagePrefix) {
        return checkReadOnlyStatus(element, project, LocalizeValue.of(messagePrefix));
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatusRecursively(@Nonnull Project project, @Nonnull Collection<? extends PsiElement> elements) {
        return checkReadOnlyStatus(
            project,
            elements,
            Collections.<PsiElement>emptySet(),
            RefactoringLocalize.refactoringCannotBePerformed(),
            false
        );
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatusRecursively(
        @Nonnull Project project,
        @Nonnull Collection<? extends PsiElement> elements,
        boolean notifyOnFail
    ) {
        return checkReadOnlyStatus(
            project,
            elements,
            Collections.<PsiElement>emptySet(),
            RefactoringLocalize.refactoringCannotBePerformed(),
            notifyOnFail
        );
    }

    @RequiredUIAccess
    public static boolean checkReadOnlyStatus(
        @Nonnull Project project,
        @Nonnull Collection<? extends PsiElement> recursive,
        @Nonnull Collection<? extends PsiElement> flat,
        boolean notifyOnFail
    ) {
        return checkReadOnlyStatus(project, recursive, flat, RefactoringLocalize.refactoringCannotBePerformed(), notifyOnFail);
    }

    @RequiredUIAccess
    private static boolean checkReadOnlyStatus(
        @Nonnull Project project,
        @Nonnull Collection<? extends PsiElement> recursive,
        @Nonnull Collection<? extends PsiElement> flat,
        @Nonnull LocalizeValue messagePrefix,
        boolean notifyOnFail
    ) {
        Collection<VirtualFile> readonly = new HashSet<>();  // not writable, but could be checked out
        Collection<VirtualFile> failed = new HashSet<>();  // those located in read-only filesystem

        boolean seenNonWritablePsiFilesWithoutVirtualFile =
            checkReadOnlyStatus(flat, false, readonly, failed) || checkReadOnlyStatus(recursive, true, readonly, failed);

        ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(readonly);
        ContainerUtil.addAll(failed, status.getReadonlyFiles());

        if (notifyOnFail && (!failed.isEmpty() || seenNonWritablePsiFilesWithoutVirtualFile && readonly.isEmpty())) {
            StringBuilder message = new StringBuilder(messagePrefix.get()).append('\n');
            int i = 0;
            for (VirtualFile virtualFile : failed) {
                LocalizeValue subj = virtualFile.isDirectory()
                    ? RefactoringLocalize.directoryDescription(virtualFile.getPresentableUrl())
                    : RefactoringLocalize.fileDescription(virtualFile.getPresentableUrl());
                if (virtualFile.getFileSystem().isReadOnly()) {
                    message.append(RefactoringLocalize.zeroIsLocatedInAArchiveFile(subj).get()).append('\n');
                }
                else {
                    message.append(RefactoringLocalize.zeroIsReadOnly(subj).get()).append('\n');
                }
                if (i++ > 20) {
                    message.append("...\n");
                    break;
                }
            }
            showErrorMessage(RefactoringLocalize.errorTitle(), LocalizeValue.localizeTODO(message.toString()), null, project);
            return false;
        }

        return failed.isEmpty();
    }

    private static boolean checkReadOnlyStatus(
        Collection<? extends PsiElement> elements,
        boolean recursively,
        Collection<VirtualFile> readonly,
        Collection<VirtualFile> failed
    ) {
        boolean seenNonWritablePsiFilesWithoutVirtualFile = false;

        for (PsiElement element : elements) {
            if (element instanceof PsiDirectory dir) {
                VirtualFile vFile = dir.getVirtualFile();
                if (vFile.getFileSystem().isReadOnly()) {
                    failed.add(vFile);
                }
                else if (recursively) {
                    collectReadOnlyFiles(vFile, readonly);
                }
                else {
                    readonly.add(vFile);
                }
            }
            else if (element instanceof PsiDirectoryContainer dirContainer) {
                for (PsiDirectory directory : dirContainer.getDirectories()) {
                    VirtualFile virtualFile = directory.getVirtualFile();
                    if (recursively) {
                        if (virtualFile.getFileSystem().isReadOnly()) {
                            failed.add(virtualFile);
                        }
                        else {
                            collectReadOnlyFiles(virtualFile, readonly);
                        }
                    }
                    else if (virtualFile.getFileSystem().isReadOnly()) {
                        failed.add(virtualFile);
                    }
                    else {
                        readonly.add(virtualFile);
                    }
                }
            }
            else {
                PsiFile file = element.getContainingFile();
                if (file == null) {
                    if (!element.isWritable()) {
                        seenNonWritablePsiFilesWithoutVirtualFile = true;
                    }
                }
                else {
                    VirtualFile vFile = file.getVirtualFile();
                    if (vFile != null) {
                        readonly.add(vFile);
                    }
                    else if (!element.isWritable()) {
                        seenNonWritablePsiFilesWithoutVirtualFile = true;
                    }
                }
            }
        }

        return seenNonWritablePsiFilesWithoutVirtualFile;
    }

    public static void collectReadOnlyFiles(@Nonnull VirtualFile vFile, @Nonnull final Collection<VirtualFile> list) {
        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();

        VirtualFileUtil.visitChildrenRecursively(vFile, new VirtualFileVisitor(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                boolean ignored = fileTypeManager.isFileIgnored(file);
                if (!file.isWritable() && !ignored) {
                    list.add(file);
                }
                return !ignored;
            }
        });
    }

    public static String capitalize(@Nonnull String text) {
        return StringUtil.capitalize(text);
    }

    public static boolean isAncestor(@Nonnull PsiElement resolved, @Nonnull Collection<? extends PsiElement> scopes) {
        for (PsiElement scope : scopes) {
            if (PsiTreeUtil.isAncestor(scope, resolved, false)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public static boolean checkFileExist(
        @Nullable PsiDirectory targetDirectory,
        int[] choice,
        PsiFile file,
        String name,
        @Nonnull LocalizeValue title
    ) {
        if (targetDirectory == null) {
            return false;
        }
        PsiFile existing = targetDirectory.findFile(name);
        if (existing != null && !existing.equals(file)) {
            int selection;
            if (choice == null || choice[0] == -1) {
                LocalizeValue message = RefactoringLocalize.dialogMessageFileAlreadyExistsInDirectory(
                    name,
                    targetDirectory.getVirtualFile().getPresentableUrl()
                );
                String[] options = choice == null
                    ? new String[]{RefactoringLocalize.copyOverwriteButton().get(), RefactoringLocalize.copySkipButton().get()}
                    : new String[]{RefactoringLocalize.copyOverwriteButton().get(), RefactoringLocalize.copySkipButton().get(), RefactoringLocalize.copyOverwriteForAllButton().get(), RefactoringLocalize.copySkipForAllButton().get()};
                selection = Messages.showDialog(message.get(), title.get(), options, 0, UIUtil.getQuestionIcon());
            }
            else {
                selection = choice[0];
            }

            if (choice != null && selection > 1) {
                choice[0] = selection % 2;
                selection = choice[0];
            }

            if (selection == 0 && file != existing) {
                CommandProcessor.getInstance().newCommand()
                    .project(targetDirectory.getProject())
                    .name(title)
                    .inWriteAction()
                    .run(existing::delete);
            }
            else {
                return true;
            }
        }

        return false;
    }
}