// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.refactoring.ui;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.language.editor.FilePasteProvider;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.editor.refactoring.copy.CopyHandler;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.move.MoveHandler;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.CopyPasteSupport;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.PasteProvider;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.LocalFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class CopyPasteDelegator implements CopyPasteSupport {
    public static final Key<Boolean> SHOW_CHOOSER_KEY = RefactoringInternalHelper.COPY_PASTE_DELEGATE_SHOW_CHOOSER_KEY;

    private final Project myProject;
    private final JComponent myKeyReceiver;
    private final MyEditable myEditable;

    public CopyPasteDelegator(@Nonnull Project project, @Nonnull JComponent keyReceiver) {
        myProject = project;
        myKeyReceiver = keyReceiver;
        myEditable = new MyEditable();
    }

    @Nonnull
    protected PsiElement[] getSelectedElements() {
        DataContext dataContext = DataManager.getInstance().getDataContext(myKeyReceiver);
        return ObjectUtil.notNull(dataContext.getData(PsiElement.KEY_OF_ARRAY), PsiElement.EMPTY_ARRAY);
    }

    @Nonnull
    private PsiElement[] getValidSelectedElements() {
        PsiElement[] selectedElements = getSelectedElements();
        for (PsiElement element : selectedElements) {
            if (element == null || !element.isValid()) {
                return PsiElement.EMPTY_ARRAY;
            }
        }
        return selectedElements;
    }

    private void updateView() {
        myKeyReceiver.repaint();
    }

    @Override
    public CopyProvider getCopyProvider() {
        return myEditable;
    }

    @Override
    public CutProvider getCutProvider() {
        return myEditable;
    }

    @Override
    public PasteProvider getPasteProvider() {
        return myEditable;
    }

    private class MyEditable implements CutProvider, CopyProvider, PasteProvider {
        @Override
        public void performCopy(@Nonnull DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            PsiCopyPasteManager.getInstance().setElements(elements, true);
            updateView();
        }

        @Override
        public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            return CopyHandler.canCopy(elements)
                || JBIterable.of(elements).filter(Predicates.instanceOf(PsiNamedElement.class)).isNotEmpty();
        }

        @Override
        public boolean isCopyVisible(@Nonnull DataContext dataContext) {
            return true;
        }

        @Override
        public void performCut(@Nonnull DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            if (MoveHandler.adjustForMove(myProject, elements, null) == null) {
                return;
            }
            // 'elements' passed instead of result of 'adjustForMove' because otherwise ProjectView would
            // not recognize adjusted elements when graying them
            PsiCopyPasteManager.getInstance().setElements(elements, false);
            updateView();
        }

        @Override
        public boolean isCutEnabled(@Nonnull DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            return elements.length != 0 && MoveHandler.canMove(elements, null);
        }

        @Override
        public boolean isCutVisible(@Nonnull DataContext dataContext) {
            return true;
        }

        @Override
        public void performPaste(@Nonnull DataContext dataContext) {
            if (!performDefaultPaste(dataContext)) {
                for (PasteProvider provider : Application.get().getExtensionList(FilePasteProvider.class)) {
                    if (provider.isPasteEnabled(dataContext)) {
                        provider.performPaste(dataContext);
                        break;
                    }
                }
            }
        }

        private boolean performDefaultPaste(DataContext dataContext) {
            boolean[] isCopied = new boolean[1];
            PsiElement[] elements = PsiCopyPasteManager.getInstance().getElements(isCopied);
            if (elements == null) {
                return false;
            }

            DumbService.getInstance(myProject).setAlternativeResolveEnabled(true);
            try {
                Module module = dataContext.getData(Module.KEY);
                PsiElement target = getPasteTarget(dataContext, module);
                if (isCopied[0]) {
                    pasteAfterCopy(elements, module, target, true);
                }
                else if (MoveHandler.canMove(elements, target)) {
                    pasteAfterCut(dataContext, elements, target);
                }
                else {
                    return false;
                }
            }
            finally {
                DumbService.getInstance(myProject).setAlternativeResolveEnabled(false);
                updateView();
            }
            return true;
        }

        private PsiElement getPasteTarget(@Nonnull DataContext dataContext, @Nullable Module module) {
            PsiElement target = dataContext.getData(LangDataKeys.PASTE_TARGET_PSI_ELEMENT);
            if (module != null && target instanceof PsiDirectoryContainer directoryContainer) {
                PsiDirectory[] directories = directoryContainer.getDirectories(GlobalSearchScope.moduleScope(module));
                if (directories.length == 1) {
                    return directories[0];
                }
            }
            return target;
        }

        @Nullable
        private PsiDirectory getTargetDirectory(@Nullable Module module, @Nullable PsiElement target) {
            PsiDirectory targetDirectory = target instanceof PsiDirectory ? (PsiDirectory)target : null;
            if (targetDirectory == null && target instanceof PsiDirectoryContainer directoryContainer) {
                PsiDirectory[] directories = module == null
                    ? directoryContainer.getDirectories()
                    : directoryContainer.getDirectories(GlobalSearchScope.moduleScope(module));
                if (directories.length > 0) {
                    targetDirectory = directories[0];
                    targetDirectory.putCopyableUserData(SHOW_CHOOSER_KEY, directories.length > 1);
                }
            }
            if (targetDirectory == null && target != null) {
                PsiFile containingFile = target.getContainingFile();
                if (containingFile != null) {
                    targetDirectory = containingFile.getContainingDirectory();
                }
            }
            return targetDirectory;
        }

        private void pasteAfterCopy(PsiElement[] elements, Module module, PsiElement target, boolean tryFromFiles) {
            PsiDirectory targetDirectory = elements.length == 1 && elements[0] == target ? null : getTargetDirectory(module, target);
            try {
                if (CopyHandler.canCopy(elements)) {
                    CopyHandler.doCopy(elements, targetDirectory);
                }
                else if (tryFromFiles) {
                    List<File> files = PsiCopyPasteManager.asFileList(elements);
                    if (files != null) {
                        PsiManager manager = elements[0].getManager();
                        PsiFileSystemItem[] items = files.stream()
                            .map(file -> LocalFileSystem.getInstance().findFileByIoFile(file))
                            .map(file -> {
                                if (file != null) {
                                    return file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
                                }
                                return null;
                            })
                            .filter(Predicates.notNull())
                            .toArray(PsiFileSystemItem[]::new);
                        pasteAfterCopy(items, module, target, false);
                    }
                }
            }
            finally {
                if (targetDirectory != null) {
                    targetDirectory.putCopyableUserData(SHOW_CHOOSER_KEY, null);
                }
            }
        }

        private void pasteAfterCut(DataContext dataContext, PsiElement[] elements, PsiElement target) {
            MoveHandler.doMove(myProject, elements, target, dataContext, () -> PsiCopyPasteManager.getInstance().clear());
        }

        @Override
        public boolean isPastePossible(@Nonnull DataContext dataContext) {
            return true;
        }

        @Override
        public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
            if (isDefaultPasteEnabled(dataContext)) {
                return true;
            }
            for (PasteProvider provider : Application.get().getExtensionList(FilePasteProvider.class)) {
                if (provider.isPasteEnabled(dataContext)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isDefaultPasteEnabled(DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            if (project == null) {
                return false;
            }

            if (DumbService.isDumb(project)) {
                return false;
            }

            Object target = dataContext.getData(LangDataKeys.PASTE_TARGET_PSI_ELEMENT);
            if (target == null) {
                return false;
            }
            PsiElement[] elements = PsiCopyPasteManager.getInstance().getElements(new boolean[]{false});
            if (elements == null) {
                return false;
            }

            // disable cross-project paste
            for (PsiElement element : elements) {
                PsiManager manager = element.getManager();
                if (manager == null || manager.getProject() != project) {
                    return false;
                }
            }

            return true;
        }
    }
}
