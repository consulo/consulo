// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.copy.CopyHandler;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.JBIterable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class CopyPasteDelegator implements CopyPasteSupport {
  private static final ExtensionPointName<PasteProvider> EP_NAME = ExtensionPointName.create("com.intellij.filePasteProvider");
  public static final Key<Boolean> SHOW_CHOOSER_KEY = Key.create("show.dirs.chooser");

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
    return ObjectUtils.notNull(dataContext.getData(LangDataKeys.PSI_ELEMENT_ARRAY), PsiElement.EMPTY_ARRAY);
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
      return CopyHandler.canCopy(elements) || JBIterable.of(elements).filter(Conditions.instanceOf(PsiNamedElement.class)).isNotEmpty();
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
      final PsiElement[] elements = getValidSelectedElements();
      return elements.length != 0 && MoveHandler.canMove(elements, null);
    }

    @Override
    public boolean isCutVisible(@Nonnull DataContext dataContext) {
      return true;
    }

    @Override
    public void performPaste(@Nonnull DataContext dataContext) {
      if (!performDefaultPaste(dataContext)) {
        for (PasteProvider provider : EP_NAME.getExtensionList()) {
          if (provider.isPasteEnabled(dataContext)) {
            provider.performPaste(dataContext);
            break;
          }
        }
      }
    }

    private boolean performDefaultPaste(final DataContext dataContext) {
      final boolean[] isCopied = new boolean[1];
      final PsiElement[] elements = PsiCopyPasteManager.getInstance().getElements(isCopied);
      if (elements == null) return false;

      DumbService.getInstance(myProject).setAlternativeResolveEnabled(true);
      try {
        final Module module = dataContext.getData(LangDataKeys.MODULE);
        PsiElement target = getPasteTarget(dataContext, module);
        if (isCopied[0]) {
          TransactionGuard.getInstance().submitTransactionAndWait(() -> pasteAfterCopy(elements, module, target, true));
        }
        else if (MoveHandler.canMove(elements, target)) {
          TransactionGuard.getInstance().submitTransactionAndWait(() -> pasteAfterCut(dataContext, elements, target));
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
      if (module != null && target instanceof PsiDirectoryContainer) {
        final PsiDirectory[] directories = ((PsiDirectoryContainer)target).getDirectories(GlobalSearchScope.moduleScope(module));
        if (directories.length == 1) {
          return directories[0];
        }
      }
      return target;
    }

    @Nullable
    private PsiDirectory getTargetDirectory(@Nullable Module module, @Nullable PsiElement target) {
      PsiDirectory targetDirectory = target instanceof PsiDirectory ? (PsiDirectory)target : null;
      if (targetDirectory == null && target instanceof PsiDirectoryContainer) {
        final PsiDirectory[] directories = module == null ? ((PsiDirectoryContainer)target).getDirectories() : ((PsiDirectoryContainer)target).getDirectories(GlobalSearchScope.moduleScope(module));
        if (directories.length > 0) {
          targetDirectory = directories[0];
          targetDirectory.putCopyableUserData(SHOW_CHOOSER_KEY, directories.length > 1);
        }
      }
      if (targetDirectory == null && target != null) {
        final PsiFile containingFile = target.getContainingFile();
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
            PsiFileSystemItem[] items = files.stream().map(file -> LocalFileSystem.getInstance().findFileByIoFile(file)).map(file -> {
              if (file != null) {
                return file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
              }
              return null;
            }).filter(file -> file != null).toArray(PsiFileSystemItem[]::new);
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
      MoveHandler.doMove(myProject, elements, target, dataContext, new MoveCallback() {
        @Override
        public void refactoringCompleted() {
          PsiCopyPasteManager.getInstance().clear();
        }
      });
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
      for (PasteProvider provider : EP_NAME.getExtensionList()) {
        if (provider.isPasteEnabled(dataContext)) {
          return true;
        }
      }
      return false;
    }

    private boolean isDefaultPasteEnabled(final DataContext dataContext) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project == null) {
        return false;
      }

      if (DumbService.isDumb(project)) return false;

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
