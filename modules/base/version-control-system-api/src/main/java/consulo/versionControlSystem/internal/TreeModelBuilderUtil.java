/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.internal;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocallyDeletedChange;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
public class TreeModelBuilderUtil {
    @Nonnull
    public static FilePath getPathForObject(@Nonnull Object o) {
        if (o instanceof Change change) {
            return ChangesUtil.getFilePath(change);
        }
        else if (o instanceof VirtualFile virtualFile) {
            return VcsUtil.getFilePath(virtualFile);
        }
        else if (o instanceof FilePath filePath) {
            return filePath;
        }
        else if (o instanceof ChangesBrowserLogicallyLockedFile changesBrowserLogicallyLockedFile) {
            return VcsUtil.getFilePath(changesBrowserLogicallyLockedFile.getUserObject());
        }
        else if (o instanceof LocallyDeletedChange locallyDeletedChange) {
            return locallyDeletedChange.getPath();
        }

        throw new IllegalArgumentException("Unknown type - " + o.getClass());
    }
}
