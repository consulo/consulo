/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.openapi.vfs.newvfs.VfsImplUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * <p>{@link VcsUtil} extension that needs access to the <code>vcs-impl</code> module.</p>
 *
 * @author Kirill Likhodedov
 */
public class VcsImplUtil {
  @Nullable
  public static VirtualFile findValidParentAccurately(@NotNull FilePath filePath) {
    VirtualFile result = filePath.getVirtualFile();
    if (result != null) return result;

    String path = filePath.getPath();
    if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
      result = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
      if (result != null) return result;
    }

    Pair<NewVirtualFile, NewVirtualFile> pair = VfsImplUtil.findCachedFileByPath(LocalFileSystem.getInstance(), path);
    return pair.first != null ? pair.first : pair.second;
  }
}
