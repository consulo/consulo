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
package consulo.language.editor.refactoring.rename;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.TitledHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author yole
 */
public abstract class DirectoryAsPackageRenameHandlerBase<T extends PsiDirectoryContainer> implements RenameHandler, TitledHandler {
    private static final Logger LOG = Logger.getInstance(DirectoryAsPackageRenameHandlerBase.class);

    protected abstract VirtualFile[] occursInPackagePrefixes(T aPackage);

    protected abstract boolean isIdentifier(String name, Project project);

    protected abstract String getQualifiedName(T aPackage);

    protected abstract @Nullable T getPackage(PsiDirectory psiDirectory);

    protected abstract BaseRefactoringProcessor createProcessor(
        String newQName,
        Project project,
        PsiDirectory[] dirsToRename,
        boolean searchInComments,
        boolean searchInNonJavaFiles
    );

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        PsiElement element = adjustForRename(dataContext, PsiElementRenameHandler.getElement(dataContext));
        if (element instanceof PsiDirectory directory) {
            VirtualFile virtualFile = directory.getVirtualFile();
            Project project = element.getProject();
            if (Comparing.equal(project.getBaseDir(), virtualFile)) {
                return false;
            }
            if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(virtualFile)) {
                return true;
            }
        }
        return false;
    }

    private static PsiElement adjustForRename(DataContext dataContext, PsiElement element) {
        if (element instanceof PsiDirectoryContainer directoryContainer) {
            Module module = dataContext.getData(Module.KEY);
            if (module != null) {
                PsiDirectory[] directories = directoryContainer.getDirectories(GlobalSearchScope.moduleScope(module));
                if (directories.length >= 1) {
                    element = directories[0];
                }
            }
        }
        return element;
    }

    @Override
    public boolean isRenaming(DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file, DataContext dataContext) {
        PsiElement element = adjustForRename(dataContext, PsiElementRenameHandler.getElement(dataContext));
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
        doRename(element, project, nameSuggestionContext, editor);
    }

    @Override
    public void invoke(Project project, PsiElement[] elements, DataContext dataContext) {
        PsiElement element = elements.length == 1 ? elements[0] : null;
        if (element == null) {
            element = PsiElementRenameHandler.getElement(dataContext);
        }
        PsiElement nameSuggestionContext = element;
        element = adjustForRename(dataContext, element);
        LOG.assertTrue(element != null);
        Editor editor = dataContext.getData(Editor.KEY);
        doRename(element, project, nameSuggestionContext, editor);
    }

    
    private void doRename(PsiElement element, Project project, PsiElement nameSuggestionContext, Editor editor) {
        PsiDirectory psiDirectory = (PsiDirectory) element;
        T aPackage = getPackage(psiDirectory);
        String qualifiedName = aPackage != null ? getQualifiedName(aPackage) : "";
        if (aPackage == null || qualifiedName.length() == 0/*default package*/ ||
            !isIdentifier(psiDirectory.getName(), project)) {
            PsiElementRenameHandler.rename(element, project, nameSuggestionContext, editor);
        }
        else {
            PsiDirectory[] directories = aPackage.getDirectories();
            VirtualFile[] virtualFiles = occursInPackagePrefixes(aPackage);
            if (virtualFiles.length == 0 && directories.length == 1) {
                PsiElementRenameHandler.rename(aPackage, project, nameSuggestionContext, editor);
            }
            else { // the directory corresponds to a package that has multiple associated directories
                ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                boolean inLib = false;
                for (PsiDirectory directory : directories) {
                    inLib |= !projectFileIndex.isInContent(directory.getVirtualFile());
                }

                PsiDirectory[] projectDirectories = aPackage.getDirectories(GlobalSearchScope.projectScope(project));
                if (inLib) {
                    Module module = ModuleUtilCore.findModuleForPsiElement(psiDirectory);
                    LOG.assertTrue(module != null);
                    PsiDirectory[] moduleDirs = null;
                    if (nameSuggestionContext instanceof PsiPackage) {
                        moduleDirs = aPackage.getDirectories(GlobalSearchScope.moduleScope(module));
                        if (moduleDirs.length <= 1) {
                            moduleDirs = null;
                        }
                    }
                    String promptMessage = "Package \'" + aPackage.getName() +
                        "\' contains directories in libraries which cannot be renamed. Do you want to rename " +
                        (moduleDirs == null ? "current directory" : "current module directories");
                    if (projectDirectories.length > 0) {
                        PsiDirectory[] currentDirs = moduleDirs == null ? new PsiDirectory[]{psiDirectory} : moduleDirs;
                        askThreeWay(
                            project,
                            LocalizeValue.of(promptMessage + " or all directories in project?"),
                            RefactoringLocalize.renameCurrentDirectory(),
                            RefactoringLocalize.renameDirectories()
                        ).thenAcceptAsync(answer -> {
                            if (answer != ThreeState.UNSURE) {
                                renameDirs(project, nameSuggestionContext, editor, psiDirectory, aPackage,
                                    answer == ThreeState.YES ? currentDirs : projectDirectories);
                            }
                        }, UIAccess.current());
                    }
                    else {
                        MessageBoxes.okCancel()
                            .asWarning()
                            .title(RefactoringLocalize.warningTitle())
                            .text(LocalizeValue.of(promptMessage + "?"))
                            .showAsync(project)
                            .thenAcceptAsync(ok -> {
                                if (Boolean.TRUE.equals(ok)) {
                                    renameDirs(project, nameSuggestionContext, editor, psiDirectory, aPackage, psiDirectory);
                                }
                            }, UIAccess.current());
                    }
                }
                else {
                    StringBuffer message = new StringBuffer();
                    RenameUtil.buildPackagePrefixChangedMessage(virtualFiles, message, qualifiedName);
                    buildMultipleDirectoriesInPackageMessage(message, getQualifiedName(aPackage), directories);
                    message.append(
                        RefactoringLocalize.directoriesAndAllReferencesToPackageWillBeRenamed(psiDirectory.getVirtualFile().getPresentableUrl()).get()
                    );
                    askThreeWay(
                        project,
                        LocalizeValue.of(message.toString()),
                        RefactoringLocalize.renamePackageButtonText(),
                        RefactoringLocalize.renameDirectoryButtonText()
                    ).thenAcceptAsync(answer -> {
                        if (answer == ThreeState.YES) {
                            PsiElementRenameHandler.rename(aPackage, project, nameSuggestionContext, editor);
                        }
                        else if (answer == ThreeState.NO) {
                            renameDirs(project, nameSuggestionContext, editor, psiDirectory, aPackage, psiDirectory);
                        }
                    }, UIAccess.current());
                }
            }
        }
    }

    @RequiredUIAccess
    private static CompletableFuture<ThreeState> askThreeWay(
        Project project,
        LocalizeValue text,
        LocalizeValue yesText,
        LocalizeValue noText
    ) {
        return MessageBoxBuilder.<ThreeState>create()
            .asWarning()
            .title(RefactoringLocalize.warningTitle())
            .text(text)
            .button(MessageButtonRole.YES, yesText, ThreeState.YES)
            .asDefaultButton()
            .button(MessageButtonRole.NO, noText, ThreeState.NO)
            .button(MessageButtonRole.CANCEL, ThreeState.UNSURE)
            .asExitButton()
            .showAsync(project);
    }

    @RequiredUIAccess
    private void renameDirs(
        final Project project,
        final PsiElement nameSuggestionContext,
        final Editor editor,
        final PsiDirectory contextDirectory,
        final T aPackage,
        final PsiDirectory... dirsToRename
    ) {
        RenameDialog dialog = new RenameDialog(project, contextDirectory, nameSuggestionContext, editor) {
            @Override
            @RequiredUIAccess
            protected void doAction() {
                String newQName = StringUtil.getQualifiedName(StringUtil.getPackageName(getQualifiedName(aPackage)), getNewName());
                BaseRefactoringProcessor moveProcessor = createProcessor(newQName, project, dirsToRename, isSearchInComments(),
                    isSearchInNonJavaFiles());
                invokeRefactoring(moveProcessor);
            }
        };
        dialog.showAsync();
    }

    public static void buildMultipleDirectoriesInPackageMessage(
        StringBuffer message,
        String packageQname,
        PsiDirectory[] directories
    ) {
        message.append(RefactoringLocalize.multipleDirectoriesCorrespondToPackage().get());
        message.append(packageQname);
        message.append(":\n\n");
        for (int i = 0; i < directories.length; i++) {
            PsiDirectory directory = directories[i];
            if (i > 0) {
                message.append("\n");
            }
            message.append(directory.getVirtualFile().getPresentableUrl());
        }
    }

    
    @Override
    public LocalizeValue getActionTitleValue() {
        return RefactoringLocalize.renameDirectoryTitle();
    }
}
