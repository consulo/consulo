// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.fileEditor.FileEditorManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public final class CodeReviewFilesUtil {
    private CodeReviewFilesUtil() {
    }

    /**
     * A utility function to close a set of files programmatically.
     * Without this {@link FileEditorManager#closeFile} will throw an exception.
     */
    @ApiStatus.Experimental
    @RequiresEdt
    @RequiresWriteLock
    public static void closeFilesSafely(@Nonnull FileEditorManager manager, @Nonnull Collection<? extends VirtualFile> files) {
        // otherwise the exception is thrown when removing an editor tab
        ((TransactionGuardImpl) TransactionGuard.getInstance()).performUserActivity(() -> {
            for (VirtualFile file : files) {
                manager.closeFile(file);
            }
        });
    }
}
