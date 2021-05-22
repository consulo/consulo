/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl;

import consulo.logging.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.FileAccessorCache;
import com.intellij.util.text.ByteArrayCharSequence;
import consulo.vfs.impl.archive.ArchiveEntry;
import consulo.vfs.impl.archive.ArchiveFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public abstract class ZipHandler extends ArchiveHandler {
  private static final FileAccessorCache<ZipHandler, ArchiveFile> ourZipFileFileAccessorCache = new FileAccessorCache<ZipHandler, ArchiveFile>(20, 10) {
    @Override
    protected ArchiveFile createAccessor(ZipHandler key) throws IOException {
      final String canonicalPathToZip = key.getCanonicalPathToZip();
      FileAttributes attributes = FileSystemUtil.getAttributes(canonicalPathToZip);
      key.myFileStamp = attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
      key.myFileLength = attributes != null ? attributes.length : DEFAULT_LENGTH;

      return key.createArchiveFile(canonicalPathToZip);
    }

    @Override
    protected void disposeAccessor(final ArchiveFile fileAccessor) throws IOException {
      // todo: ZipFile isn't disposable for Java6, replace the code below with 'disposeCloseable(fileAccessor);'
      fileAccessor.close();
    }

    @Override
    public boolean equals(ZipHandler val1, ZipHandler val2) {
      return val1 == val2; // reference equality to handle different jars for different ZipHandlers on the same path
    }
  };

  private volatile String myCanonicalPathToZip;
  private volatile long myFileStamp;

  private volatile long myFileLength;

  public ZipHandler(@Nonnull String path) {
    super(path);
  }

  public abstract ArchiveFile createArchiveFile(@Nonnull String path) throws IOException;

  @Nonnull
  private String getCanonicalPathToZip() throws IOException {
    String value = myCanonicalPathToZip;
    if (value == null) {
      myCanonicalPathToZip = value = getFileToUse().getCanonicalPath();
    }
    return value;
  }

  @Nonnull
  @Override
  protected Map<String, EntryInfo> createEntriesMap() throws IOException {
    FileAccessorCache.Handle<ArchiveFile> zipRef = getZipFileHandle();
    try {
      ArchiveFile zip = zipRef.get();

      Map<String, EntryInfo> map = new ZipEntryMap(zip.getSize());
      map.put("", createRootEntry());

      Iterator<? extends ArchiveEntry> entries = zip.entries();
      while (entries.hasNext()) {
        getOrCreate(entries.next(), map, zip);
      }

      return map;
    }
    finally {
      zipRef.release();
    }
  }

  @Nonnull
  private FileAccessorCache.Handle<ArchiveFile> getZipFileHandle() throws IOException {
    FileAccessorCache.Handle<ArchiveFile> handle = ourZipFileFileAccessorCache.get(this);

    if (getFile() == getFileToUse()) { // files are canonicalized
      // IDEA-148458, http://bugs.java.com/view_bug.do?bug_id=4425695, JVM crashes on use of opened ZipFile after it was updated
      // Reopen file if the file has been changed
      FileAttributes attributes = FileSystemUtil.getAttributes(getCanonicalPathToZip());
      if (attributes == null) {
        throw new FileNotFoundException(getCanonicalPathToZip());
      }

      if (attributes.lastModified == myFileStamp && attributes.length == myFileLength) return handle;

      // Note that zip_util.c#ZIP_Get_From_Cache will allow us to have duplicated ZipFile instances without a problem
      removeZipHandlerFromCache();
      handle.release();
      handle = ourZipFileFileAccessorCache.get(this);
    }

    return handle;
  }

  private void removeZipHandlerFromCache() {
    ourZipFileFileAccessorCache.remove(this);
  }

  @Nonnull
  protected File getFileToUse() {
    return getFile();
  }

  @Override
  public void dispose() {
    super.dispose();
    removeZipHandlerFromCache();
  }

  @Nonnull
  private EntryInfo getOrCreate(@Nonnull ArchiveEntry entry, @Nonnull Map<String, EntryInfo> map, @Nonnull ArchiveFile zip) {
    boolean isDirectory = entry.isDirectory();
    String entryName = entry.getName();
    if (StringUtil.endsWithChar(entryName, '/')) {
      entryName = entryName.substring(0, entryName.length() - 1);
      isDirectory = true;
    }

    EntryInfo info = map.get(entryName);
    if (info != null) return info;

    Pair<String, String> path = splitPath(entryName);
    EntryInfo parentInfo = getOrCreate(path.first, map, zip);
    if (".".equals(path.second)) {
      return parentInfo;
    }
    info = store(map, parentInfo, path.second, isDirectory, entry.getSize(), myFileStamp, entryName);
    return info;
  }

  @Nonnull
  private static EntryInfo store(@Nonnull Map<String, EntryInfo> map,
                                 @Nullable EntryInfo parentInfo,
                                 @Nonnull CharSequence shortName,
                                 boolean isDirectory,
                                 long size,
                                 long time,
                                 @Nonnull String entryName) {
    CharSequence sequence = shortName instanceof ByteArrayCharSequence ? shortName : ByteArrayCharSequence.convertToBytesIfPossible(shortName);
    EntryInfo info = new EntryInfo(sequence, isDirectory, size, time, parentInfo);
    map.put(entryName, info);
    return info;
  }

  @Nonnull
  private EntryInfo getOrCreate(@Nonnull String entryName, Map<String, EntryInfo> map, @Nonnull ArchiveFile zip) {
    EntryInfo info = map.get(entryName);

    if (info == null) {
      ArchiveEntry entry = zip.getEntry(entryName + "/");
      if (entry != null) {
        return getOrCreate(entry, map, zip);
      }

      Pair<String, String> path = splitPath(entryName);
      EntryInfo parentInfo = getOrCreate(path.first, map, zip);
      info = store(map, parentInfo, path.second, true, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, entryName);
    }

    if (!info.isDirectory) {
      Logger.getInstance(getClass()).info(zip.getName() + ": " + entryName + " should be a directory");
      info = store(map, info.parent, info.shortName, true, info.length, info.timestamp, entryName);
    }

    return info;
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray(@Nonnull String relativePath) throws IOException {
    FileAccessorCache.Handle<ArchiveFile> zipRef;

    try {
      zipRef = getZipFileHandle();
    }
    catch (RuntimeException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof IOException) throw (IOException)cause;
      throw ex;
    }

    try {
      ArchiveFile zip = zipRef.get();
      ArchiveEntry entry = zip.getEntry(relativePath);
      if (entry != null) {
        InputStream stream = zip.getInputStream(entry);
        if (stream != null) {
          // ZipFile.c#Java_java_util_zip_ZipFile_read reads data in 8K (stack allocated) blocks - no sense to create BufferedInputStream
          try {
            return FileUtil.loadBytes(stream, (int)entry.getSize());
          }
          finally {
            stream.close();
          }
        }
      }
    }
    finally {
      zipRef.release();
    }

    throw new FileNotFoundException(getFile() + "!/" + relativePath);
  }

  // also used in Kotlin
  public static void clearFileAccessorCache() {
    ourZipFileFileAccessorCache.clear();
  }
}