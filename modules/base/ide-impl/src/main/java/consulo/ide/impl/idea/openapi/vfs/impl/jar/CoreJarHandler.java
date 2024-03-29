/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.impl.jar;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.impl.ZipHandler;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.ide.impl.virtualFileSystem.archive.zip.ZipArchiveFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
@Deprecated
public class CoreJarHandler extends ZipHandler {
  private final CoreJarFileSystem myFileSystem;
  private final VirtualFile myRoot;

  public CoreJarHandler(@Nonnull CoreJarFileSystem fileSystem, @Nonnull String path) {
    super(path);
    myFileSystem = fileSystem;

    Map<EntryInfo, CoreJarVirtualFile> entries = new HashMap<EntryInfo, CoreJarVirtualFile>();

    final Map<String, EntryInfo> entriesMap = getEntriesMap();
    for (EntryInfo info : entriesMap.values()) {
      getOrCreateFile(info, entries);
    }

    EntryInfo rootInfo = getEntryInfo("");
    myRoot = rootInfo != null ? getOrCreateFile(rootInfo, entries) : null;
  }

  @Nonnull
  private CoreJarVirtualFile getOrCreateFile(@Nonnull EntryInfo info, @Nonnull Map<EntryInfo, CoreJarVirtualFile> entries) {
    CoreJarVirtualFile file = entries.get(info);
    if (file == null) {
      FileAttributes attributes = new FileAttributes(info.isDirectory, false, false, false, info.length, info.timestamp, false);
      EntryInfo parent = info.parent;
      file = new CoreJarVirtualFile(this, info.shortName.toString(), attributes, parent != null ? getOrCreateFile(parent, entries) : null);
      entries.put(info, file);
    }
    return file;
  }

  @Nullable
  public VirtualFile findFileByPath(@Nonnull String pathInJar) {
    return myRoot != null ? myRoot.findFileByRelativePath(pathInJar) : null;
  }

  @Nonnull
  public CoreJarFileSystem getFileSystem() {
    return myFileSystem;
  }

  @Override
  public ArchiveFile createArchiveFile(@Nonnull String path) throws IOException {
    return new ZipArchiveFile(path);
  }
}
