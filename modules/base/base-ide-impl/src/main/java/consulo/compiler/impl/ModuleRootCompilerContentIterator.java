/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.impl;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 17.08.14
 */
public class ModuleRootCompilerContentIterator implements ContentIterator {
  private final FileType myFileType;
  private final Collection<VirtualFile> myFiles;

  public ModuleRootCompilerContentIterator(FileType fileType, Collection<VirtualFile> files) {
    myFileType = fileType;
    myFiles = files;
  }

  @Override
  public boolean processFile(VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) {
      return true;
    }
    if (!fileOrDir.isInLocalFileSystem()) {
      return true;
    }

    if (myFileType == null || myFileType == fileOrDir.getFileType()) {
      myFiles.add(fileOrDir);
    }
    return true;
  }
}
