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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public interface FileManager extends Disposable {
    @Nullable
    @RequiredReadAction
    PsiFile findFile(@Nonnull VirtualFile vFile);

    @Nullable
    @RequiredReadAction
    PsiDirectory findDirectory(@Nonnull VirtualFile vFile);

    @RequiredWriteAction
    default void reloadFromDisk(@Nonnull PsiFile file) {
        reloadFromDisk(file, false);
    }

    default void reloadFromDisk(@Nonnull PsiFile file, boolean ignoreDocument) {
    }

    @Nullable
    @RequiredReadAction
    PsiFile getCachedPsiFile(@Nonnull VirtualFile vFile);

    @TestOnly
    void cleanupForNextTest();

    @RequiredReadAction
    FileViewProvider findViewProvider(@Nonnull VirtualFile file);

    @RequiredReadAction
    FileViewProvider findCachedViewProvider(@Nonnull VirtualFile file);

    @RequiredReadAction
    void setViewProvider(@Nonnull VirtualFile virtualFile, FileViewProvider fileViewProvider);

    @Nonnull
    List<PsiFile> getAllCachedFiles();

    @Nonnull
    FileViewProvider createFileViewProvider(@Nonnull VirtualFile file, boolean physical);

    default void processFileTypesChanged() {
    }

    default void markInitialized() {
    }

    default PsiDirectory getCachedDirectory(@Nonnull VirtualFile vFile) {
        return null;
    }

    default void removeInvalidFilesAndDirs(boolean useFind) {
    }

    @RequiredReadAction
    default PsiFile getCachedPsiFileInner(@Nonnull VirtualFile file) {
        return null;
    }

    @RequiredWriteAction
    default void forceReload(@Nonnull VirtualFile vFile) {
    }

    default void removeFilesAndDirsRecursively(@Nonnull VirtualFile vFile) {
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
