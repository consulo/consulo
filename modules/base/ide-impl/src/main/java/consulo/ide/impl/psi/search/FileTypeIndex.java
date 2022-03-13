/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.psi.search;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.index.io.ID;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class FileTypeIndex {
  /**
   * @deprecated Use {@link #getFiles(FileType, GlobalSearchScope)},
   * {@link #containsFileOfType(FileType, GlobalSearchScope)} or
   * {@link #processFiles(FileType, Processor, GlobalSearchScope)} instead
   */
  @Deprecated
  public static final ID<FileType, Void> NAME = ID.create("filetypes");

  @Nonnull
  public static Collection<VirtualFile> getFiles(@Nonnull FileType fileType, @Nonnull GlobalSearchScope scope) {
    return FilenameIndex.getService().getFilesWithFileType(fileType, scope);
  }

  public static boolean containsFileOfType(@Nonnull FileType type, @Nonnull GlobalSearchScope scope) {
    return !processFiles(type, CommonProcessors.alwaysFalse(), scope);
  }

  public static boolean processFiles(@Nonnull FileType fileType, @Nonnull Processor<? super VirtualFile> processor, @Nonnull GlobalSearchScope scope) {
    return FilenameIndex.getService().processFilesWithFileType(fileType, processor, scope);
  }
}
