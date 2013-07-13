/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.archive;

import com.intellij.openapi.util.io.BufferExposingByteArrayInputStream;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ArchiveEntry;
import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.reference.SoftReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.TimedReference;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class CoreArchiveHandler implements ArchiveHandler {

  private static final long DEFAULT_LENGTH = 0L;
  private static final long DEFAULT_TIMESTAMP = -1L;

  private final TimedReference<ArchiveFile> myArchiveFile = new TimedReference<ArchiveFile>(null);
  private Reference<Map<String, ArchiveHandlerEntry>> myRelPathsToEntries = new SoftReference<Map<String, ArchiveHandlerEntry>>(null);
  private final Object lock = new Object();

  protected final String myBasePath;

  public CoreArchiveHandler(@NotNull String path) {
    myBasePath = path;
  }

  protected void clear() {
    synchronized (lock) {
      myRelPathsToEntries = null;
      myArchiveFile.set(null);
    }
  }

  @NotNull
  protected Map<String, ArchiveHandlerEntry> initEntries() {
    synchronized (lock) {
      Map<String, ArchiveHandlerEntry> map = myRelPathsToEntries != null ? myRelPathsToEntries.get() : null;
      if (map == null) {
        final ArchiveFile zip = getArchiveFile();

        map = new THashMap<String, ArchiveHandlerEntry>();
        if (zip != null) {
          map.put("", new ArchiveHandlerEntry("", null, true));
          final Iterator<? extends ArchiveEntry> entries = zip.entries();
          while (entries.hasNext()) {
            ArchiveEntry entry = entries.next();
            final String name = entry.getName();
            final boolean isDirectory = StringUtil.endsWithChar(name, '/');
            getOrCreate(isDirectory ? name.substring(0, name.length() - 1) : name, isDirectory, map);
          }

          myRelPathsToEntries = new SoftReference<Map<String, ArchiveHandlerEntry>>(map);
        }
      }
      return map;
    }
  }

  public String getBasePath() {
    return myBasePath;
  }

  public File getMirrorFile(@NotNull File originalFile) {
    return originalFile;
  }

  @Nullable
  public ArchiveFile getArchiveFile() {
    ArchiveFile jar = myArchiveFile.get();
    if (jar == null) {
      synchronized (lock) {
        jar = myArchiveFile.get();
        if (jar == null) {
          jar = createArchiveFile();
          if (jar != null) {
            myArchiveFile.set(jar);
          }
        }
      }
    }
    return jar;
  }

  @Nullable
  protected abstract ArchiveFile createArchiveFile();

  @Override
  @NotNull
  public File getOriginalFile() {
    return new File(myBasePath);
  }

  @NotNull
  private static ArchiveHandlerEntry getOrCreate(@NotNull String entryName, boolean isDirectory, @NotNull Map<String, ArchiveHandlerEntry> map) {
    ArchiveHandlerEntry info = map.get(entryName);
    if (info == null) {
      int idx = entryName.lastIndexOf('/');
      final String parentEntryName = idx > 0 ? entryName.substring(0, idx) : "";
      String shortName = idx > 0 ? entryName.substring(idx + 1) : entryName;
      if (".".equals(shortName)) return getOrCreate(parentEntryName, true, map);

      info = new ArchiveHandlerEntry(shortName, getOrCreate(parentEntryName, true, map), isDirectory);
      map.put(entryName, info);
    }

    return info;
  }

  @Override
  @NotNull
  public String[] list(@NotNull final VirtualFile file) {
    synchronized (lock) {
      ArchiveHandlerEntry parentEntry = getEntryInfo(file);

      Set<String> names = new HashSet<String>();
      for (ArchiveHandlerEntry info : getEntriesMap().values()) {
        if (info.getParent() == parentEntry) {
          names.add(info.getShortName());
        }
      }

      return ArrayUtil.toStringArray(names);
    }
  }

  protected ArchiveHandlerEntry getEntryInfo(@NotNull VirtualFile file) {
    synchronized (lock) {
      String parentPath = getRelativePath(file);
      return getEntryInfo(parentPath);
    }
  }

  public ArchiveHandlerEntry getEntryInfo(@NotNull String parentPath) {
    return getEntriesMap().get(parentPath);
  }

  @NotNull
  protected Map<String, ArchiveHandlerEntry> getEntriesMap() {
    return initEntries();
  }

  @NotNull
  private String getRelativePath(@NotNull VirtualFile file) {
    final String path = file.getPath().substring(myBasePath.length() + 1);
    return StringUtil.startsWithChar(path, '/') ? path.substring(1) : path;
  }

  @Nullable
  private ArchiveEntry convertToEntry(@NotNull VirtualFile file) {
    String path = getRelativePath(file);
    final ArchiveFile jar = getArchiveFile();
    return jar == null ? null : jar.getEntry(path);
  }

  public long getLength(@NotNull final VirtualFile file) {
    final ArchiveEntry entry = convertToEntry(file);
    synchronized (lock) {
      return entry == null ? DEFAULT_LENGTH : entry.getSize();
    }
  }

  @NotNull
  public InputStream getInputStream(@NotNull final VirtualFile file) throws IOException {
    return new BufferExposingByteArrayInputStream(contentsToByteArray(file));
  }

  @NotNull
  public byte[] contentsToByteArray(@NotNull final VirtualFile file) throws IOException {
    final ArchiveEntry entry = convertToEntry(file);
    if (entry == null) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
    synchronized (lock) {
      final ArchiveFile jar = getArchiveFile();
      assert jar != null : file;

      final InputStream stream = jar.getInputStream(entry);
      assert stream != null : file;

      try {
        return FileUtil.loadBytes(stream, (int)entry.getSize());
      }
      finally {
        stream.close();
      }
    }
  }

  @Override
  public long getTimeStamp(@NotNull final VirtualFile file) {
    if (file.getParent() == null) return getOriginalFile().lastModified(); // Optimization
    final ArchiveEntry entry = convertToEntry(file);
    synchronized (lock) {
      return entry == null ? DEFAULT_TIMESTAMP : entry.getTime();
    }
  }

  @Override
  public boolean isDirectory(@NotNull final VirtualFile file) {
    if (file.getParent() == null) return true; // Optimization
    synchronized (lock) {
      final String path = getRelativePath(file);
      final ArchiveHandlerEntry info = getEntryInfo(path);
      return info == null || info.isDirectory();
    }
  }

  public boolean exists(@NotNull final VirtualFile fileOrDirectory) {
    if (fileOrDirectory.getParent() == null) {
      // Optimization. Do not build entries if asked for jar root existence.
      return myArchiveFile.get() != null || getOriginalFile().exists();
    }

    return getEntryInfo(fileOrDirectory) != null;
  }

  @Override
  @Nullable
  public FileAttributes getAttributes(@NotNull final VirtualFile file) {
    final ArchiveEntry entry = convertToEntry(file);
    synchronized (lock) {
      final ArchiveHandlerEntry entryInfo = getEntryInfo(getRelativePath(file));
      if (entryInfo == null) return null;
      final long length = entry != null ? entry.getSize() : DEFAULT_LENGTH;
      final long timeStamp = entry != null ? entry.getTime() : DEFAULT_TIMESTAMP;
      return new FileAttributes(entryInfo.isDirectory(), false, false, false, length, timeStamp, false);
    }
  }
}
