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
package consulo.compiler.scope;

import consulo.compiler.CompileStatusNotification;
import consulo.compiler.CompilerManager;
import consulo.compiler.util.ExportableUserDataHolder;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface describing the current compilation scope.
 * Only sources that belong to the scope are compiled.
 *
 * @see CompilerManager#compile(CompileScope, CompileStatusNotification)
 */
public interface CompileScope extends ExportableUserDataHolder {
  CompileScope[] EMPTY_ARRAY = new CompileScope[0];
  /**
   * Returns the list of files within the scope.
   *
   * @param fileType     the type of the files. Null should be passed if all available files are needed.
   * @return a list of files of given type that belong to this scope.
   */
  @Nonnull
  VirtualFile[] getFiles(@Nullable FileType fileType);

  /**
   * Checks if the file with the specified URL belongs to the scope.
   *
   * @param url an VFS url. Note that actual file may not exist on the disk.
   * @return true if the url specified belongs to the scope, false otherwise.
   *         Note: the method may be time-consuming.
   */
  boolean belongs(String url);

  /**
   * Returns the list of modules files in which belong to the scope.
   *
   * @return a list of modules this scope affects.
   */
  @Nonnull
  Module[] getAffectedModules();

  /**
   * This scope include test sources or not
   */
  default boolean includeTestScope() {
    return true;
  }
}
