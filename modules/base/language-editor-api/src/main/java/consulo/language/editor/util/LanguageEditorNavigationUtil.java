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

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.fileEditor.EditorHistoryManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.navigation.NavigationUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.INativeFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2022-04-05
 */
public class LanguageEditorNavigationUtil {
    @RequiredUIAccess
    public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt) {
        return activateFileWithPsiElement(elt, true);
    }

    @RequiredUIAccess
    public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt, boolean searchForOpen) {
        return openFileWithPsiElement(elt, searchForOpen, true);
    }

    @RequiredUIAccess
    public static boolean openFileWithPsiElement(PsiElement element, boolean searchForOpen, boolean requestFocus) {
        boolean openAsNative = false;
        if (element instanceof PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                openAsNative =
                    virtualFile.getFileType() instanceof INativeFileType || virtualFile.getFileType() == UnknownFileType.INSTANCE;
            }
        }

        if (searchForOpen) {
            element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
        }
        else {
            element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, true);
        }

        boolean openAsNativeFinal = openAsNative;
        // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
        Boolean result = CommandProcessor.getInstance().<Boolean>newCommand()
            .project(element.getProject())
            .compute(() -> {
                if (openAsNativeFinal || !activatePsiElementIfOpen(element, searchForOpen, requestFocus)) {
                    NavigationItem navigationItem = (NavigationItem)element;
                    if (navigationItem.getNavigateOptions().canNavigate()) {
                        navigationItem.navigate(requestFocus);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                return null;
            });
        if (result != null) {
            return result;
        }

        element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
        return false;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static CompletableFuture<?> openFileWithPsiElementAsync(
        @Nonnull UIAccess uiAccess,
        @Nonnull PsiElement element,
        boolean searchForOpen,
        boolean requestFocus
    ) {
        element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, searchForOpen ? null : Boolean.TRUE);

        Navigatable navigatable = (Navigatable) element;
        if (!navigatable.getNavigateOptions().canNavigate()) {
            element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
            return CompletableFuture.completedFuture(null);
        }

        return CoroutineScope.launchAsync(element.getProject().coroutineContext(), () -> {
            return Coroutine
                .first(CompletableFutureStep.<Void, Object>await(v ->
                    (CompletableFuture<Object>) navigatable.navigateAsync(uiAccess, requestFocus)))
                .then(CodeExecution.<Object, Void>apply(result -> {
                    element.putUserData(NavigationUtil.USE_CURRENT_WINDOW, null);
                    return null;
                }));
        }).toFuture();
    }

    @RequiredReadAction
    private static boolean activatePsiElementIfOpen(@Nonnull PsiElement elt, boolean searchForOpen, boolean requestFocus) {
        if (!elt.isValid()) {
            return false;
        }
        elt = elt.getNavigationElement();
        PsiFile file = elt.getContainingFile();
        if (file == null || !file.isValid()) {
            return false;
        }

        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null || !EditorHistoryManager.getInstance(elt.getProject()).hasBeenOpen(vFile)) {
            return false;
        }

        FileEditorManager fem = FileEditorManager.getInstance(elt.getProject());
        if (!fem.isFileOpen(vFile)) {
            fem.openFile(vFile, requestFocus, searchForOpen);
        }

        TextRange range = elt.getTextRange();
        if (range == TextRange.EMPTY_RANGE) {
            return false;
        }

        FileEditor[] editors = fem.getEditors(vFile);
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor textEditor) {
                Editor text = textEditor.getEditor();
                int offset = text.getCaretModel().getOffset();

                if (range.containsOffset(offset)) {
                    // select the file
                    fem.openFile(vFile, requestFocus, searchForOpen);
                    return true;
                }
            }
        }

        return false;
    }
}
