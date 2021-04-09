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

package com.intellij.mock;

import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

/**
 * @author nik
 */
public class MockLocalFileSystem extends LocalFileSystem {
  private final MockVirtualFileSystem myDelegate = new MockVirtualFileSystem();

  @Override
  public void refreshIoFiles(@Nonnull Iterable<? extends File> files) {

  }

  @Override
  public void refreshIoFiles(@Nonnull Iterable<? extends File> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {

  }

  @Override
  public void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files) {

  }

  @Override
  public void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {

  }

  @Override
  @Nonnull
  public Set<WatchRequest> addRootsToWatch(@Nonnull final Collection<String> rootPaths, final boolean watchRecursively) {
    throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
  }

  @Override
  public void removeWatchedRoots(@Nonnull final Collection<WatchRequest> rootsToWatch) {
    throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
  }

  @Override
  public Set<WatchRequest> replaceWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests,
                                               @Nullable Collection<String> recursiveRoots,
                                               @Nullable Collection<String> flatRoots) {
    throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
  }

  @Override
  public void registerAuxiliaryFileOperationsHandler(@Nonnull final LocalFileOperationsHandler handler) {
  }

  @Override
  public void unregisterAuxiliaryFileOperationsHandler(@Nonnull final LocalFileOperationsHandler handler) {
  }

  @Override
  @Nonnull
  public String getProtocol() {
    return LocalFileSystem.PROTOCOL;
  }

  @Override
  @Nullable
  public VirtualFile findFileByPath(@Nonnull @NonNls final String path) {
    return myDelegate.findFileByPath(path);
  }

  @Override
  public void refresh(final boolean asynchronous) {
  }

  @Override
  @javax.annotation.Nullable
  public VirtualFile refreshAndFindFileByPath(@Nonnull final String path) {
    return findFileByPath(path);
  }

  @Override
  public void deleteFile(final Object requestor, @Nonnull final VirtualFile vFile) throws IOException {
  }

  @Override
  public void moveFile(final Object requestor, @Nonnull final VirtualFile vFile, @Nonnull final VirtualFile newParent) throws IOException {
  }

  @Override
  public void renameFile(final Object requestor, @Nonnull final VirtualFile vFile, @Nonnull final String newName) throws IOException {
  }

  @Override
  public VirtualFile createChildFile(final Object requestor, @Nonnull final VirtualFile vDir, @Nonnull final String fileName) throws IOException {
    return myDelegate.createChildFile(requestor, vDir, fileName);
  }

  @Override
  @Nonnull
  public VirtualFile createChildDirectory(final Object requestor, @Nonnull final VirtualFile vDir, @Nonnull final String dirName) throws IOException {
    return myDelegate.createChildDirectory(requestor, vDir, dirName);
  }

  @Override
  public VirtualFile copyFile(final Object requestor, @Nonnull final VirtualFile virtualFile, @Nonnull final VirtualFile newParent, @Nonnull final String copyName)
    throws IOException {
    return myDelegate.copyFile(requestor, virtualFile, newParent, copyName);
  }

  @Nonnull
  @Override
  protected String extractRootPath(@Nonnull final String path) {
    return path;
  }

  @Override
  public boolean isCaseSensitive() {
    return false;
  }

  @Override
  public VirtualFile findFileByPathIfCached(@Nonnull @NonNls String path) {
    return findFileByPath(path);
  }

  @Override
  public boolean exists(@Nonnull final VirtualFile fileOrDirectory) {
    return false;
  }

  @Override
  @Nonnull
  public InputStream getInputStream(@Nonnull final VirtualFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray(@Nonnull final VirtualFile file) throws IOException {
    return ArrayUtil.EMPTY_BYTE_ARRAY;
  }

  @Override
  public long getLength(@Nonnull final VirtualFile file) {
    return 0;
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(@Nonnull final VirtualFile file, final Object requestor, final long modStamp, final long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getTimeStamp(@Nonnull final VirtualFile file) {
    return 0;
  }

  @Override
  public boolean isDirectory(@Nonnull final VirtualFile file) {
    return false;
  }

  @Override
  public boolean isWritable(@Nonnull final VirtualFile file) {
    return false;
  }

  @Nonnull
  @Override
  public String[] list(@Nonnull final VirtualFile file) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public void setTimeStamp(@Nonnull final VirtualFile file, final long timeStamp) throws IOException {
  }

  @Override
  public void setWritable(@Nonnull final VirtualFile file, final boolean writableFlag) throws IOException {
  }

  @Override
  public int getRank() {
    return 1;
  }

  @Override
  public FileAttributes getAttributes(@Nonnull VirtualFile file) {
    return null;
  }
}
