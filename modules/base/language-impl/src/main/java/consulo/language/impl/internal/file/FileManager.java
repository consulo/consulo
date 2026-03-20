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

package consulo.language.impl.internal.file;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public interface FileManager extends Disposable {
    @RequiredReadAction
    @Nullable PsiFile findFile(VirtualFile vFile);

    @RequiredReadAction
    @Nullable PsiDirectory findDirectory(VirtualFile vFile);

    @RequiredWriteAction
    default void reloadFromDisk(PsiFile file) {
        reloadFromDisk(file, false);
    }

    default void reloadFromDisk(PsiFile file, boolean ignoreDocument) {
    }

    @RequiredReadAction
    @Nullable PsiFile getCachedPsiFile(VirtualFile vFile);

    @TestOnly
    void cleanupForNextTest();

    @RequiredReadAction
    FileViewProvider findViewProvider(VirtualFile file);

    @RequiredReadAction
    FileViewProvider findCachedViewProvider(VirtualFile file);

    @RequiredReadAction
    void setViewProvider(VirtualFile virtualFile, FileViewProvider fileViewProvider);

    
    List<PsiFile> getAllCachedFiles();

    
    FileViewProvider createFileViewProvider(VirtualFile file, boolean physical);

    default void processFileTypesChanged() {
    }

    default void markInitialized() {
    }

    default PsiDirectory getCachedDirectory(VirtualFile vFile) {
        return null;
    }

    default void removeInvalidFilesAndDirs(boolean useFind) {
    }

    @RequiredReadAction
    default PsiFile getCachedPsiFileInner(VirtualFile file) {
        return null;
    }

    @RequiredWriteAction
    default void forceReload(VirtualFile vFile) {
    }

    default void removeFilesAndDirsRecursively(VirtualFile vFile) {
    }

    default boolean isInitialized() {
        return true;
    }

    default void dispatchPendingEvents() {
    }

    @RequiredWriteAction
    default void firePropertyChangedForUnloadedPsi() {
    }
}
