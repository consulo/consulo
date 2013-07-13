/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.archive.ArchiveHandlerEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class CoreJarHandler extends CoreJarHandlerBase {

  private final CoreJarFileSystem myFileSystem;
  private final VirtualFile myRoot;

  public CoreJarHandler(@NotNull CoreJarFileSystem fileSystem, @NotNull String path) {
    super(path);
    myFileSystem = fileSystem;

    Map<ArchiveHandlerEntry, CoreJarVirtualFile> entries = new HashMap<ArchiveHandlerEntry, CoreJarVirtualFile>();

    final Map<String, ArchiveHandlerEntry> entriesMap = getEntriesMap();
    for (ArchiveHandlerEntry info : entriesMap.values()) {
      getOrCreateFile(info, entries);
    }

    ArchiveHandlerEntry rootInfo = getEntryInfo("");
    myRoot = rootInfo != null ? getOrCreateFile(rootInfo, entries) : null;
  }

  @NotNull
  private CoreJarVirtualFile getOrCreateFile(@NotNull ArchiveHandlerEntry info, @NotNull Map<ArchiveHandlerEntry, CoreJarVirtualFile> entries) {
    CoreJarVirtualFile answer = entries.get(info);
    if (answer == null) {
      ArchiveHandlerEntry parentEntry = info.getParent();
      answer = new CoreJarVirtualFile(this, info, parentEntry != null ? getOrCreateFile(parentEntry, entries) : null);
      entries.put(info, answer);
    }

    return answer;
  }

  @Nullable
  public VirtualFile findFileByPath(@NotNull String pathInJar) {
    return myRoot != null ? myRoot.findFileByRelativePath(pathInJar) : null;
  }

  @NotNull
  public CoreJarFileSystem getFileSystem() {
    return myFileSystem;
  }
}
