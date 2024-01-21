/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.util;

import consulo.application.concurrent.DataLock;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.fileEditor.EditorHistoryManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.NavigationItem;
import consulo.navigation.NavigationUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandInfo;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.INativeFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 05-Apr-22
 */
public class LanguageEditorNavigationUtil {
  // region Async methods
  @Nonnull
  @RequiredUIAccess
  public static CompletableFuture<Boolean> activateFileWithPsiElementAsync(@Nonnull PsiElement elt) {
    return activateFileWithPsiElementAsync(elt, true);
  }

  @Nonnull
  @RequiredUIAccess
  public static CompletableFuture<Boolean> activateFileWithPsiElementAsync(@Nonnull PsiElement elt, boolean searchForOpen) {
    return activateFileWithPsiElementAsync(elt, searchForOpen, true);
  }

  @Nonnull
  @RequiredUIAccess
  public static CompletableFuture<Boolean> activateFileWithPsiElementAsync(PsiElement element,
                                                                           boolean searchForOpen,
                                                                           boolean requestFocus) {
    boolean openAsNative = false;
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        openAsNative = virtualFile.getFileType() instanceof INativeFileType || virtualFile.getFileType() == UnknownFileType.INSTANCE;
      }
    }

    if (searchForOpen) {
      element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
    }
    else {
      element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, true);
    }

    UIAccess uiAccess = UIAccess.current();
    boolean openAsNativeFinal = openAsNative;
    CommandInfo commandInfo = CommandInfo.newBuilder().withProject(element.getProject()).build();
    CompletableFuture<Boolean> future = CommandProcessor.getInstance().executeCommandAsync(commandInfo, e -> {
      if (openAsNativeFinal) {
        final NavigationItem navigationItem = (NavigationItem)element;
        if (navigationItem.canNavigate()) {
          return navigationItem.navigateAsync(requestFocus).handle((o, throwable) -> true);
        }
      }

      return activatePsiElementIfOpenAsync(element, searchForOpen, requestFocus, uiAccess).thenCompose(opened -> {
        if (opened) {
          return CompletableFuture.completedFuture(true);
        }
        
        final NavigationItem navigationItem = (NavigationItem)element;
        if (navigationItem.canNavigate()) {
          return navigationItem.navigateAsync(requestFocus).handle((o, throwable) -> true);
        } else {
          return CompletableFuture.completedFuture(false);
        }
      });
    });
    future.whenComplete((aBoolean, throwable) -> element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null));
    return future;
  }

  // endregion

  public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt) {
    return activateFileWithPsiElement(elt, true);
  }

  public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt, boolean searchForOpen) {
    return openFileWithPsiElement(elt, searchForOpen, true);
  }

  public static boolean openFileWithPsiElement(PsiElement element, boolean searchForOpen, boolean requestFocus) {
    boolean openAsNative = false;
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        openAsNative = virtualFile.getFileType() instanceof INativeFileType || virtualFile.getFileType() == UnknownFileType.INSTANCE;
      }
    }

    if (searchForOpen) {
      element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
    }
    else {
      element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, true);
    }

    SimpleReference<Boolean> resultRef = new SimpleReference<>();
    boolean openAsNativeFinal = openAsNative;
    // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
    CommandProcessor.getInstance().executeCommand(element.getProject(), () -> {
      if (openAsNativeFinal || !activatePsiElementIfOpen(element, searchForOpen, requestFocus)) {
        final NavigationItem navigationItem = (NavigationItem)element;
        if (navigationItem.canNavigate()) {
          navigationItem.navigate(requestFocus);
          resultRef.set(Boolean.TRUE);
        }
        else {
          resultRef.set(Boolean.FALSE);
        }
      }
    }, "", null);
    if (!resultRef.isNull()) return resultRef.get();

    element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
    return false;
  }

  private static boolean activatePsiElementIfOpen(@Nonnull PsiElement elt, boolean searchForOpen, boolean requestFocus) {
    if (!elt.isValid()) return false;
    elt = elt.getNavigationElement();
    final PsiFile file = elt.getContainingFile();
    if (file == null || !file.isValid()) return false;

    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) return false;

    if (!EditorHistoryManager.getInstance(elt.getProject()).hasBeenOpen(vFile)) return false;

    final FileEditorManager fem = FileEditorManager.getInstance(elt.getProject());
    if (!fem.isFileOpen(vFile)) {
      fem.openFile(vFile, requestFocus, searchForOpen);
    }

    final TextRange range = elt.getTextRange();
    if (range == TextRange.EMPTY_RANGE) return false;

    final FileEditor[] editors = fem.getEditors(vFile);
    for (FileEditor editor : editors) {
      if (editor instanceof TextEditor) {
        final Editor text = ((TextEditor)editor).getEditor();
        final int offset = text.getCaretModel().getOffset();

        if (range.containsOffset(offset)) {
          // select the file
          fem.openFile(vFile, requestFocus, searchForOpen);
          return true;
        }
      }
    }

    return false;
  }

  private static CompletableFuture<Boolean> activatePsiElementIfOpenAsync(@Nonnull PsiElement elt,
                                                                          boolean searchForOpen,
                                                                          boolean requestFocus,
                                                                          @Nonnull UIAccess uiAccess) {
    if (!elt.isValid()) {
      return CompletableFuture.completedFuture(false);
    }

    elt = elt.getNavigationElement();
    final PsiFile file = elt.getContainingFile();
    if (file == null || !file.isValid()) {
      return CompletableFuture.completedFuture(false);
    }

    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) {
      return CompletableFuture.completedFuture(false);
    }

    if (!EditorHistoryManager.getInstance(elt.getProject()).hasBeenOpen(vFile)) {
      return CompletableFuture.completedFuture(false);
    }

    final FileEditorManager fem = FileEditorManager.getInstance(elt.getProject());
    CompletableFuture<List<FileEditor>> fileEditorsFuture;
    if (!fem.isFileOpen(vFile)) {
      fileEditorsFuture = fem.openFileAsync(vFile, requestFocus, searchForOpen, uiAccess);
    }
    else {
      fileEditorsFuture = uiAccess.giveAsync(() -> List.of(fem.getEditors(vFile)));
    }

    DataLock dataLock = DataLock.getInstance();

    final PsiElement finalElt = elt;

    return fileEditorsFuture.thenCompose((fileEditors) -> dataLock.readAsync(() -> Pair.create(finalElt.getTextRange(), fileEditors)))
                            .thenComposeAsync(pair -> {
                              List<FileEditor> editors = pair.getValue();
                              TextRange range = pair.getKey();

                              if (range == TextRange.EMPTY_RANGE) {
                                return CompletableFuture.completedFuture(false);
                              }
                              else {
                                for (FileEditor editor : editors) {
                                  if (editor instanceof TextEditor) {
                                    final Editor text = ((TextEditor)editor).getEditor();
                                    final int offset = text.getCaretModel().getOffset();

                                    if (range.containsOffset(offset)) {
                                      // select the file
                                      return fem.openFileAsync(vFile, requestFocus, searchForOpen, uiAccess).handle((f, t) -> true);
                                    }
                                  }
                                }

                                return CompletableFuture.completedFuture(false);
                              }
                            }, uiAccess);
  }
}
