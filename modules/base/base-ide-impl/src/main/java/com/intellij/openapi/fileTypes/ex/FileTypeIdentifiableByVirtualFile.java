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

/*
 * Created by IntelliJ IDEA.
 * User: valentin
 * Date: 29.01.2004
 * Time: 21:10:56
 */
package com.intellij.openapi.fileTypes.ex;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayFactory;

public interface FileTypeIdentifiableByVirtualFile extends FileType {
  public static final FileTypeIdentifiableByVirtualFile[] EMPTY_ARRAY = new FileTypeIdentifiableByVirtualFile[0];

  public static ArrayFactory<FileTypeIdentifiableByVirtualFile> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new FileTypeIdentifiableByVirtualFile[count];

  boolean isMyFileType(VirtualFile file);
}