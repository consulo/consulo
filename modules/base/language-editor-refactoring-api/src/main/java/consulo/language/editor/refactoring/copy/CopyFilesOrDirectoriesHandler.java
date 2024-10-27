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
package consulo.language.editor.refactoring.copy;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.PlatformPackageUtil;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.NewFileModuleResolver;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl(id = "copyFilesOrDirectories")
public class CopyFilesOrDirectoriesHandler extends CopyHandlerDelegateBase {
    private static Logger LOG = Logger.getInstance(CopyFilesOrDirectoriesHandler.class);

    @Override
    @RequiredReadAction
    public boolean canCopy(PsiElement[] elements, boolean fromUpdate) {
        Set<String> names = new HashSet<>();
        for (PsiElement element : elements) {
            if (!(element instanceof PsiDirectory || element instanceof PsiFile)) {
                return false;
            }
            if (!element.isValid()) {
                return false;
            }
            if (element instanceof PsiCompiledFile) {
                return false;
            }

            String name = ((PsiFileSystemItem)element).getName();
            if (names.contains(name)) {
                return false;
            }
            names.add(name);
        }

        PsiElement[] filteredElements = PsiTreeUtil.filterAncestors(elements);
        return filteredElements.length == elements.length;
    }

    @Override
    @RequiredReadAction
    public void doCopy(final PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
        if (defaultTargetDirectory == null) {
            defaultTargetDirectory = getCommonParentDirectory(elements);
        }
        Project project = defaultTargetDirectory != null ? defaultTargetDirectory.getProject() : elements[0].getProject();
        if (defaultTargetDirectory != null) {
            defaultTargetDirectory = resolveDirectory(defaultTargetDirectory);
            if (defaultTargetDirectory == null) {
                return;
            }
        }

        defaultTargetDirectory = tryNotNullizeDirectory(project, defaultTargetDirectory);

        copyAsFiles(elements, defaultTargetDirectory, project);
    }

    @Nullable
    @RequiredReadAction
    private static PsiDirectory tryNotNullizeDirectory(@Nonnull Project project, @Nullable PsiDirectory defaultTargetDirectory) {
        if (defaultTargetDirectory == null) {
            VirtualFile root = ArrayUtil.getFirstElement(ProjectRootManager.getInstance(project).getContentRoots());
            if (root == null) {
                root = project.getBaseDir();
            }
            if (root == null) {
                root = VirtualFileUtil.getUserHomeDir();
            }
            defaultTargetDirectory = root != null ? PsiManager.getInstance(project).findDirectory(root) : null;

            if (defaultTargetDirectory == null) {
                LOG.warn("No directory found for project: " + project.getName() + ", root: " + root);
            }
        }
        return defaultTargetDirectory;
    }

    @RequiredUIAccess
    public static void copyAsFiles(PsiElement[] elements, @Nullable PsiDirectory defaultTargetDirectory, Project project) {
        doCopyAsFiles(elements, defaultTargetDirectory, project);
    }

    @RequiredUIAccess
    private static void doCopyAsFiles(PsiElement[] elements, @Nullable PsiDirectory defaultTargetDirectory, Project project) {
        PsiDirectory targetDirectory;
        String newName;
        boolean openInEditor;
        VirtualFile[] files = Arrays.stream(elements).map(el -> ((PsiFileSystemItem)el).getVirtualFile()).toArray(VirtualFile[]::new);
        if (Application.get().isUnitTestMode()) {
            targetDirectory = defaultTargetDirectory;
            newName = null;
            openInEditor = true;
        }
        else {
            CopyFilesOrDirectoriesDialog dialog = new CopyFilesOrDirectoriesDialog(elements, defaultTargetDirectory, project, false);
            if (dialog.showAndGet()) {
                newName = elements.length == 1 ? dialog.getNewName() : null;
                targetDirectory = dialog.getTargetDirectory();
                openInEditor = dialog.openInEditor();
            }
            else {
                return;
            }
        }


        if (targetDirectory != null) {
            PsiManager manager = PsiManager.getInstance(project);
            try {
                for (VirtualFile file : files) {
                    if (file.isDirectory()) {
                        PsiFileSystemItem psiElement = manager.findDirectory(file);
                        MoveFilesOrDirectoriesUtil.checkIfMoveIntoSelf(psiElement, targetDirectory);
                    }
                }
            }
            catch (IncorrectOperationException e) {
                CommonRefactoringUtil.showErrorHint(project, null, e.getMessage(), CommonLocalize.titleError().get(), null);
                return;
            }

            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(RefactoringLocalize.copyHandlerCopyFilesDirectories())
                .run(() -> copyImpl(files, newName, targetDirectory, false, openInEditor));
        }
    }

    @Override
    @RequiredReadAction
    public void doClone(final PsiElement element) {
        doCloneFile(element);
    }

    @RequiredReadAction
    public static void doCloneFile(PsiElement element) {
        PsiDirectory targetDirectory;
        if (element instanceof PsiDirectory directory) {
            targetDirectory = directory.getParentDirectory();
        }
        else {
            targetDirectory = PlatformPackageUtil.getDirectory(element);
        }
        targetDirectory = tryNotNullizeDirectory(element.getProject(), targetDirectory);
        if (targetDirectory == null) {
            return;
        }

        PsiElement[] elements = {element};
        VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
        CopyFilesOrDirectoriesDialog dialog = new CopyFilesOrDirectoriesDialog(elements, null, element.getProject(), true);
        if (dialog.showAndGet()) {
            String newName = dialog.getNewName();
            copyImpl(new VirtualFile[]{file}, newName, targetDirectory, true, true);
        }
    }

    @Nullable
    private static PsiDirectory getCommonParentDirectory(PsiElement[] elements) {
        PsiDirectory result = null;

        for (PsiElement element : elements) {
            PsiDirectory directory;

            if (element instanceof PsiDirectory psiDirectory) {
                directory = psiDirectory.getParentDirectory();
            }
            else if (element instanceof PsiFile) {
                directory = PlatformPackageUtil.getDirectory(element);
            }
            else {
                throw new IllegalArgumentException("unexpected element " + element);
            }

            if (directory == null) {
                continue;
            }

            if (result == null) {
                result = directory;
            }
            else {
                if (PsiTreeUtil.isAncestor(directory, result, true)) {
                    result = directory;
                }
            }
        }

        return result;
    }

    /**
     * @param newName can be not null only if elements.length == 1
     */
    @RequiredUIAccess
    private static void copyImpl(
        @Nonnull final VirtualFile[] files,
        @Nullable final String newName,
        @Nonnull final PsiDirectory targetDirectory,
        final boolean doClone,
        final boolean openInEditor
    ) {
        if (doClone && files.length != 1) {
            throw new IllegalArgumentException("invalid number of elements to clone:" + files.length);
        }

        if (newName != null && files.length != 1) {
            throw new IllegalArgumentException("no new name should be set; number of elements is: " + files.length);
        }

        final Project project = targetDirectory.getProject();
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, Collections.singleton(targetDirectory), false)) {
            return;
        }

        LocalizeValue title = doClone
            ? RefactoringLocalize.copyHandlerCloneFilesDirectories()
            : RefactoringLocalize.copyHandlerCopyFilesDirectories();
        try {
            PsiFile firstFile = null;
            final int[] choice = files.length > 1 || files[0].isDirectory() ? new int[]{-1} : null;
            PsiManager manager = PsiManager.getInstance(project);
            for (VirtualFile file : files) {
                PsiElement psiElement = file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
                if (psiElement == null) {
                    LOG.info("invalid file: " + file.getExtension());
                    continue;
                }
                PsiFile f = copyToDirectory((PsiFileSystemItem)psiElement, newName, targetDirectory, choice, title.get());
                if (firstFile == null) {
                    firstFile = f;
                }
            }

            if (firstFile != null && openInEditor) {
                CopyHandler.updateSelectionInActiveProjectView(firstFile, project, doClone);
                if (!(firstFile instanceof PsiBinaryFile)) {
                    EditorHelper.openInEditor(firstFile);
                    ToolWindowManager.getInstance(project).activateEditorComponent();
                }
            }
        }
        catch (final IncorrectOperationException | IOException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), RefactoringLocalize.errorTitle().get());
        }
    }

    /**
     * @param elementToCopy PsiFile or PsiDirectory
     * @param newName       can be not null only if elements.length == 1
     * @return first copied PsiFile (recursively); null if no PsiFiles copied
     */
    @Nullable
    @RequiredReadAction
    public static PsiFile copyToDirectory(
        @Nonnull PsiFileSystemItem elementToCopy,
        @Nullable String newName,
        @Nonnull PsiDirectory targetDirectory
    ) throws IncorrectOperationException, IOException {
        return copyToDirectory(elementToCopy, newName, targetDirectory, null, null);
    }

    /**
     * @param elementToCopy PsiFile or PsiDirectory
     * @param newName       can be not null only if elements.length == 1
     * @param choice        a horrible way to pass/keep user preference
     * @return first copied PsiFile (recursively); null if no PsiFiles copied
     */
    @Nullable
    @RequiredReadAction
    public static PsiFile copyToDirectory(
        @Nonnull PsiFileSystemItem elementToCopy,
        @Nullable String newName,
        @Nonnull PsiDirectory targetDirectory,
        @Nullable int[] choice,
        @Nullable String title
    ) throws IncorrectOperationException, IOException {
        if (elementToCopy instanceof PsiFile file) {
            String name = newName == null ? file.getName() : newName;
            if (checkFileExist(targetDirectory, choice, file, name, "Copy")) {
                return null;
            }
            return new WriteCommandAction<PsiFile>(targetDirectory.getProject(), title) {
                @Override
                protected void run(@Nonnull Result<PsiFile> result) throws Throwable {
                    PsiFile returnFile = targetDirectory.copyFileFrom(name, file);

                    Module module = NewFileModuleResolver.resolveModule(
                        targetDirectory.getProject(),
                        targetDirectory.getVirtualFile(),
                        file.getFileType()
                    );
                    if (module != null) {
                        ModuleRootManager manager = ModuleRootManager.getInstance(module);
                        ModifiableRootModel modifiableModel = manager.getModifiableModel();
                        modifiableModel.addContentEntry(returnFile.getVirtualFile());
                        modifiableModel.commit();
                    }
                    result.setResult(returnFile);
                }
            }.execute().getResultObject();
        }
        else if (elementToCopy instanceof PsiDirectory directory) {
            if (directory.equals(targetDirectory)) {
                return null;
            }
            if (newName == null) {
                newName = directory.getName();
            }
            final PsiDirectory existing = targetDirectory.findSubdirectory(newName);
            final PsiDirectory subdirectory;
            if (existing == null) {
                String finalNewName = newName;
                subdirectory = new WriteCommandAction<PsiDirectory>(targetDirectory.getProject(), title) {
                    @Override
                    protected void run(@Nonnull Result<PsiDirectory> result) throws Throwable {
                        result.setResult(targetDirectory.createSubdirectory(finalNewName));
                    }
                }.execute().getResultObject();
            }
            else {
                subdirectory = existing;
            }
            EncodingRegistry.doActionAndRestoreEncoding(directory.getVirtualFile(), subdirectory::getVirtualFile);

            PsiFile firstFile = null;
            Project project = directory.getProject();
            PsiManager manager = PsiManager.getInstance(project);
            VirtualFile[] children = directory.getVirtualFile().getChildren();
            for (VirtualFile file : children) {
                PsiFileSystemItem item = file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
                if (item == null) {
                    LOG.info("Invalidated item: " + file.getExtension());
                    continue;
                }
                PsiFile f = copyToDirectory(item, item.getName(), subdirectory, choice, title);
                if (firstFile == null) {
                    firstFile = f;
                }
            }
            return firstFile;
        }
        else {
            throw new IllegalArgumentException("unexpected elementToCopy: " + elementToCopy);
        }
    }

    @Deprecated
    public static boolean checkFileExist(@Nullable PsiDirectory targetDirectory, int[] choice, PsiFile file, String name, String title) {
        return CommonRefactoringUtil.checkFileExist(targetDirectory, choice, file, name, title);
    }

    @Nullable
    public static PsiDirectory resolveDirectory(@Nonnull PsiDirectory defaultTargetDirectory) {
        final Project project = defaultTargetDirectory.getProject();
        final Boolean showDirsChooser =
            defaultTargetDirectory.getCopyableUserData(RefactoringInternalHelper.COPY_PASTE_DELEGATE_SHOW_CHOOSER_KEY);
        if (showDirsChooser != null && showDirsChooser.booleanValue()) {
            final PsiDirectoryContainer directoryContainer =
                PsiPackageHelper.getInstance(project).getDirectoryContainer(defaultTargetDirectory);
            if (directoryContainer == null) {
                return defaultTargetDirectory;
            }
            return MoveFilesOrDirectoriesUtil.resolveToDirectory(project, directoryContainer);
        }
        return defaultTargetDirectory;
    }
}
