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
package consulo.virtualFileSystem.impl.internal;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.disposer.TraceableDisposable;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

class VirtualFilePointerImpl implements VirtualFilePointer {
  private static final Logger LOG = Logger.getInstance(VirtualFilePointerImpl.class);

  private static final boolean TRACE_CREATION = LOG.isDebugEnabled() || ApplicationManager.getApplication().isUnitTestMode();

  volatile FilePointerPartNode myNode; // null means disposed
  boolean recursive; // true if the validityChanged() event should be fired for any change under this directory. Used for library jar directories.

  private final TraceableDisposable myTraceableDisposable;

  VirtualFilePointerImpl() {
    myTraceableDisposable = TraceableDisposable.newTraceDisposable(TRACE_CREATION);
  }

  @Override
  @Nonnull
  public String getFileName() {
    FilePointerPartNode node = checkDisposed(myNode);
    if (node == null) return "";
    Pair<VirtualFile, String> result = node.update();
    if (result == null) return "";
    VirtualFile file = result.first;
    if (file != null) {
      return file.getName();
    }
    String url = result.second;
    int index = url.lastIndexOf('/');
    return index >= 0 ? url.substring(index + 1) : url;
  }

  @Override
  public VirtualFile getFile() {
    FilePointerPartNode node = checkDisposed(myNode);
    if (node == null) return null;
    Pair<VirtualFile, String> result = node.update();
    return result == null ? null : result.first;
  }

  @Override
  @Nonnull
  public String getUrl() {
    FilePointerPartNode node = myNode;
    if (node == null) return "";
    // optimization: when file is null we shouldn't try to do expensive findFileByUrl() just to return the url
    Pair<VirtualFile, String> fileAndUrl = node.myFileAndUrl;
    if (fileAndUrl != null && fileAndUrl.getFirst() == null) {
      return fileAndUrl.getSecond();
    }
    Pair<VirtualFile, String> result = node.update();
    return result == null ? "" : result.second;
  }

  @Override
  @Nonnull
  public String getPresentableUrl() {
    FilePointerPartNode node = checkDisposed(myNode);
    if (node == null) return "";
    Pair<VirtualFile, String> result = node.update();
    return result == null ? "" : VirtualFilePathUtil.toPresentableUrl(result.second);
  }

  private FilePointerPartNode checkDisposed(FilePointerPartNode node) {
    if (node == null) {
      ProgressManager.checkCanceled();
      LOG.error("Already disposed: URL='" + this + "'");
    }
    return node;
  }

  public TraceableDisposable getTraceableDisposable() {
    return myTraceableDisposable;
  }

  @Override
  public boolean isValid() {
    FilePointerPartNode node = myNode;
    Pair<VirtualFile, String> result = node == null ? null : node.update();
    return result != null && result.first != null;
  }

  @Override
  public String toString() {
    FilePointerPartNode node = myNode;
    Pair<VirtualFile, String> fileAndUrl;
    return node == null ? "(disposed)" : (fileAndUrl = node.myFileAndUrl) == null ? "?" : fileAndUrl.second;
  }

  public void dispose() {
    FilePointerPartNode node = checkDisposed(myNode);
    if (node.incrementUsageCount(-1) == 0) {
      myTraceableDisposable.kill("URL when die: " + this);
      VirtualFilePointerManager pointerManager = VirtualFilePointerManager.getInstance();
      if (pointerManager instanceof VirtualFilePointerManagerImpl) {
        ((VirtualFilePointerManagerImpl)pointerManager).removeNodeFrom(this);
      }
    }
  }

  int incrementUsageCount(int delta) {
    FilePointerPartNode node = checkDisposed(myNode);
    if (node == null) return 1;
    return node.incrementUsageCount(delta);
  }

  @Override
  public boolean isRecursive() {
    return recursive;
  }
}
