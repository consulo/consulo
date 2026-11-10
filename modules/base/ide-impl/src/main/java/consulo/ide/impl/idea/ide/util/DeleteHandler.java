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

package consulo.ide.impl.idea.ide.util;

import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.fileEditor.impl.internal.NonProjectFileWritingAccessProvider;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.util.ReadOnlyAttributeUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteDialogDescriptor;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteProcessor;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.DeleteUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.Iteration;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeleteHandler {
    private DeleteHandler() {
    }

    public static class DefaultDeleteProvider implements DeleteProvider {
        public static final DefaultDeleteProvider INSTANCE = new DefaultDeleteProvider();

        @Override
        public boolean canDeleteElement(DataContext dataContext) {
            if (!dataContext.hasData(Project.KEY)) {
                return false;
            }
            PsiElement[] elements = getPsiElements(dataContext);
            return elements != null && shouldEnableDeleteAction(elements);
        }

        private static @Nullable PsiElement[] getPsiElements(DataContext dataContext) {
            PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
            if (elements == null) {
                Object data = dataContext.getData(PsiElement.KEY);
                if (data != null) {
                    elements = new PsiElement[]{(PsiElement)data};
                }
                else {
                    Object data1 = dataContext.getData(PsiFile.KEY);
                    if (data1 != null) {
                        elements = new PsiElement[]{(PsiFile)data1};
                    }
                }
            }
            return elements;
        }

        @Override
        @RequiredUIAccess
        public void deleteElement(DataContext dataContext) {
            PsiElement[] elements = getPsiElements(dataContext);
            if (elements == null) {
                return;
            }
            Project project = dataContext.getData(Project.KEY);
            if (project == null) {
                return;
            }
            LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeLocalize.progressDeleting());
            try {
                deletePsiElement(elements, project).whenComplete((result, error) -> a.finish());
            }
            catch (RuntimeException | Error e) {
                a.finish();
                throw e;
            }
        }
    }

    @RequiredUIAccess
    public static CompletableFuture<?> deletePsiElement(PsiElement[] elementsToDelete, Project project) {
        return deletePsiElement(elementsToDelete, project, true);
    }

    @RequiredUIAccess
    public static CompletableFuture<?> deletePsiElement(PsiElement[] elementsToDelete, Project project, boolean needConfirmation) {
        if (elementsToDelete == null || elementsToDelete.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        PsiElement[] elements = PsiTreeUtil.filterAncestors(elementsToDelete);

        boolean safeDeleteApplicable = true;
        for (int i = 0; i < elements.length && safeDeleteApplicable; i++) {
            PsiElement element = elements[i];
            safeDeleteApplicable = SafeDeleteProcessor.validElement(element);
        }

        boolean dumb = DumbService.getInstance(project).isDumb();
        CompletableFuture<Boolean> confirmed;
        if (!needConfirmation) {
            confirmed = CompletableFuture.completedFuture(Boolean.TRUE);
        }
        else if (safeDeleteApplicable && !dumb) {
            SafeDeleteDialogDescriptor dialog = new SafeDeleteDialogDescriptor(
                project,
                elements,
                true,
                descriptor -> {
                    if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), true)) {
                        return;
                    }
                    Coroutine.first(UIAction.<Object, SafeDeleteProcessor>await(ignored -> SafeDeleteProcessor.createInstanceAsync(
                            project,
                            null,
                            elements,
                            descriptor.isSearchInComments(),
                            descriptor.isSearchForTextOccurences(),
                            true
                        )))
                        .then(UIAction.apply(processor -> {
                            descriptor.invokeSafeDelete(processor, null);
                            return null;
                        }))
                        .runAsync(CoroutineScope.of(project.coroutineContext()), null);
                }
            );
            confirmed = dialog.showAsync().handle((value, error) -> error == null && value != null && !dialog.isSafeDelete());
        }
        else {
            String warningMessage = DeleteUtil.generateWarningMessage(IdeLocalize::promptDeleteElements, elements).get();

            boolean anyDirectories = false;
            String directoryName = null;
            for (PsiElement psiElement : elementsToDelete) {
                if (psiElement instanceof PsiDirectory psiDirectory && !PsiUtilBase.isSymLink(psiDirectory)) {
                    anyDirectories = true;
                    directoryName = psiDirectory.getName();
                    break;
                }
            }
            if (anyDirectories) {
                if (elements.length == 1) {
                    warningMessage += IdeLocalize.warningDeleteAllFilesAndSubdirectories(directoryName);
                }
                else {
                    warningMessage += IdeLocalize.warningDeleteAllFilesAndSubdirectoriesInTheSelectedDirectory();
                }
            }

            if (safeDeleteApplicable && dumb) {
                warningMessage +=
                    "\n\nWarning:\n" +
                        "  Safe delete is not available while " + Application.get().getName() + " updates indices,\n" +
                        "  no usages will be checked.";
            }

            confirmed = MessageBoxBuilder.<Boolean>create()
                .asQuestion()
                .title(IdeLocalize.titleDelete())
                .text(LocalizeValue.of(warningMessage))
                .button(MessageButtonRole.OK, ApplicationLocalize.buttonDelete(), Boolean.TRUE)
                .asDefaultButton()
                .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), Boolean.FALSE)
                .asExitButton()
                .showAsync(project);
        }

        Set<PsiElement> skipped = new HashSet<>();
        AtomicBoolean delete = new AtomicBoolean();
        return Coroutine.first(UIAction.<List<PsiElement>, List<PsiElement>>await(all -> confirmed.thenApply(answer -> {
                delete.set(answer);
                return answer ? all : List.of();
            })))
            .then(Iteration.<PsiElement, List<PsiElement>>forEach(
                UIAction.<PsiElement, @Nullable Void>await(element -> confirmReadOnly(element, project, skipped))
            ))
            .then(UIAction.apply(ignored -> {
                if (delete.get()) {
                    deleteElements(elements, skipped, project);
                }
                return null;
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), Arrays.asList(elements))
            .toFuture();
    }

    @RequiredUIAccess
    private static CompletableFuture<@Nullable Void> confirmReadOnly(PsiElement element, Project project, Set<PsiElement> skipped) {
        if (element instanceof PsiDirectory directory) {
            VirtualFile virtualFile = directory.getVirtualFile();
            if (!virtualFile.isInLocalFileSystem() || virtualFile.is(VFileProperty.SYMLINK)) {
                return CompletableFuture.completedFuture(null);
            }
            List<VirtualFile> readOnlyFiles = new ArrayList<>();
            CommonRefactoringUtil.collectReadOnlyFiles(virtualFile, readOnlyFiles);
            if (readOnlyFiles.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return MessageBoxes.yesNo()
                .title(IdeLocalize.titleDelete())
                .text(IdeLocalize.promptDirectoryContainsReadOnlyFiles(virtualFile.getPresentableUrl()))
                .showAsync(project)
                .thenAccept(answer -> {
                    if (!answer || !clearReadOnlyFlags(readOnlyFiles, project)) {
                        skipped.add(element);
                    }
                });
        }
        if (element.isWritable() || element instanceof PsiFileSystemItem item && PsiUtilBase.isSymLink(item)) {
            return CompletableFuture.completedFuture(null);
        }
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return CompletableFuture.completedFuture(null);
        }
        return MessageBoxes.yesNo()
            .asWarning()
            .title(IdeLocalize.titleDelete())
            .text(LocalizeValue.join(
                " ",
                UILocalize.fileIsReadOnlyMessageText(virtualFile.getPresentableUrl()),
                IdeLocalize.promptDeleteItAnyway()
            ))
            .showAsync(project)
            .thenAccept(answer -> {
                if (!answer || !clearReadOnlyFlag(virtualFile, project)) {
                    skipped.add(element);
                }
            });
    }

    @RequiredUIAccess
    private static void deleteElements(PsiElement[] elements, Set<PsiElement> skipped, Project project) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(RefactoringLocalize.safeDeleteCommand(RefactoringUIUtil.calculatePsiElementDescriptionList(elements)))
            .run(() -> NonProjectFileWritingAccessProvider.disableChecksDuring(() -> {
                Collection<PsiElement> directories = new SmartList<>();
                for (PsiElement e : elements) {
                    if (e instanceof PsiFileSystemItem && e.getParent() != null) {
                        directories.add(e.getParent());
                    }
                }

                if (!CommonRefactoringUtil.checkReadOnlyStatus(project, Arrays.asList(elements), directories, false)) {
                    return;
                }

                if (!DataManager.getInstance().getDataContext().hasData(Editor.KEY)) {
                    CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
                }

                for (PsiElement elementToDelete : elements) {
                    if (!elementToDelete.isValid() || skipped.contains(elementToDelete)) {
                        continue;
                    }

                    try {
                        elementToDelete.checkDelete();
                    }
                    catch (IncorrectOperationException ex) {
                        showError(project, ex.getMessage());
                        continue;
                    }

                    project.getApplication().runWriteAction(() -> {
                        try {
                            elementToDelete.delete();
                        }
                        catch (IncorrectOperationException ex) {
                            project.getApplication().invokeLater(() -> showError(project, ex.getMessage()));
                        }
                    });
                }
            }));
    }

    @RequiredUIAccess
    private static boolean clearReadOnlyFlags(List<VirtualFile> files, Project project) {
        for (VirtualFile file : files) {
            if (!clearReadOnlyFlag(file, project)) {
                return false;
            }
        }
        return true;
    }

    @RequiredUIAccess
    private static boolean clearReadOnlyFlag(VirtualFile virtualFile, Project project) {
        SimpleReference<String> error = SimpleReference.create();
        boolean cleared = CommandProcessor.getInstance().<Boolean>newCommand()
            .project(project)
            .inWriteAction()
            .compute(() -> {
                try {
                    ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualFile, false);
                    return true;
                }
                catch (IOException e) {
                    error.set(e.getMessage());
                    return false;
                }
            });
        if (!cleared) {
            showError(project, error.get());
        }
        return cleared;
    }

    @RequiredUIAccess
    private static void showError(Project project, @Nullable String message) {
        MessageBoxes.okError(LocalizeValue.ofNullable(message))
            .title(CommonLocalize.titleError())
            .showAsync(project);
    }

    public static boolean shouldEnableDeleteAction(PsiElement[] elements) {
        if (elements == null || elements.length == 0) {
            return false;
        }
        for (PsiElement element : elements) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
            if (virtualFile == null) {
                return false;
            }
            if (!WritingAccessProvider.isPotentiallyWritable(virtualFile, element.getProject())) {
                return false;
            }
        }
        return true;
    }
}
