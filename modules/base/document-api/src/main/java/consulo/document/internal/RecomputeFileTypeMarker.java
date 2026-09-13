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
package consulo.document.internal;

import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

/**
 * The document manager marks a file whose type may have changed after a document reload; the PSI
 * VFS listener consumes the mark before deciding how to rebuild the file's view provider. Shared
 * here because the writer (file-editor-impl) and the reader (language-impl) live in different
 * modules.
 *
 * @author VISTALL
 */
public final class RecomputeFileTypeMarker {
    public static final Key<Boolean> MUST_RECOMPUTE_FILE_TYPE = Key.create("Must recompute file type");

    private RecomputeFileTypeMarker() {
    }

    public static boolean recomputeFileTypeIfNecessary(VirtualFile virtualFile) {
        if (virtualFile.getUserData(MUST_RECOMPUTE_FILE_TYPE) != null) {
            virtualFile.getFileType();
            virtualFile.putUserData(MUST_RECOMPUTE_FILE_TYPE, null);
            return true;
        }
        return false;
    }
}
