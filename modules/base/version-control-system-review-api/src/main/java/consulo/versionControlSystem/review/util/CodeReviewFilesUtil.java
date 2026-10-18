// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.review.util;

import consulo.fileEditor.FileEditorManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;

public final class CodeReviewFilesUtil {
    private CodeReviewFilesUtil() {
    }

    /**
     * A utility function to close a set of files programmatically
     * Without this {@link FileEditorManager#closeFile} will throw an exception
     */
    @RequiredUIAccess
    public static void closeFilesSafely(FileEditorManager manager, Collection<VirtualFile> files) {
        for (VirtualFile file : files) {
            manager.closeFile(file);
        }
    }
}
