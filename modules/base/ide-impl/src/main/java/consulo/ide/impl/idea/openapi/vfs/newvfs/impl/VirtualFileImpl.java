/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
 * @author max
 */
package consulo.ide.impl.idea.openapi.vfs.newvfs.impl;

import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.openapi.fileTypes.impl.FileTypeManagerImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.LineSeparator;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.internal.keyFMap.KeyFMap;
import consulo.util.io.FileTooBigException;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

public class VirtualFileImpl extends VirtualFileSystemEntry {

  VirtualFileImpl(int id, VfsData.Segment segment, VirtualDirectoryImpl parent) {
    super(id, segment, parent);
  }

  @Override
  @Nullable
  public NewVirtualFile findChild(@Nonnull @NonNls final String name) {
    return null;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getCachedChildren() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Iterable<VirtualFile> iterInDbChildren() {
    return ContainerUtil.emptyIterable();
  }

  @Override
  @Nonnull
  public NewVirtualFileSystem getFileSystem() {
    final VirtualFileSystemEntry parent = getParent();
    assert parent != null;
    return parent.getFileSystem();
  }

  @Override
  @Nullable
  public NewVirtualFile refreshAndFindChild(@Nonnull final String name) {
    return null;
  }

  @Override
  @Nullable
  public NewVirtualFile findChildIfCached(@Nonnull final String name) {
    return null;
  }

  @Override
  public VirtualFile[] getChildren() {
    return EMPTY_ARRAY;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  private static final Key<byte[]> ourPreloadedContentKey = Key.create("preloaded.content.key");

  @Override
  public void setPreloadedContentHint(byte[] preloadedContentHint) {
    putUserData(ourPreloadedContentKey, preloadedContentHint);
  }

  @Override
  @Nonnull
  public InputStream getInputStream() throws IOException {
    final byte[] preloadedContent = getUserData(ourPreloadedContentKey);

    return VfsUtilCore.inputStreamSkippingBOM(preloadedContent == null ? ourPersistence.getInputStream(this) : new DataInputStream(new UnsyncByteArrayInputStream(preloadedContent)), this);
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray() throws IOException {
    return contentsToByteArray(true);
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray(boolean cacheContent) throws IOException {
    checkNotTooLarge(null);
    final byte[] preloadedContent = getUserData(ourPreloadedContentKey);
    if (preloadedContent != null) return preloadedContent;
    byte[] bytes = ourPersistence.contentsToByteArray(this, cacheContent);
    if (!isCharsetSet()) {
      // optimisation: take the opportunity to not load bytes again in getCharset()
      // use getByFile() to not fall into recursive trap from vfile.getFileType() which would try to load contents again to detect charset
      FileType fileType = ObjectUtil.notNull(((FileTypeManagerImpl)FileTypeManager.getInstance()).getByFile(this), UnknownFileType.INSTANCE);
      if (fileType != UnknownFileType.INSTANCE && !fileType.isBinary()) {
        try {
          // execute in impatient mode to not deadlock when the indexing process waits under write action for the queue to load contents in other threads
          // and that other thread asks JspManager for encoding which requires read action for PSI
          ((ApplicationEx)ApplicationManager.getApplication()).executeByImpatientReader(() -> LoadTextUtil.detectCharsetAndSetBOM(this, bytes, fileType));
        }
        catch (ProcessCanceledException ignored) {
        }
      }
    }
    return bytes;
  }

  @Nonnull
  @Override
  public CharSequence loadText() {
    return LoadTextUtil.loadText(this);
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(final Object requestor, final long modStamp, final long timeStamp) throws IOException {
    return VfsUtilCore.outputStreamAddingBOM(ourPersistence.getOutputStream(this, requestor, modStamp, timeStamp), this);
  }

  @Override
  public String getDetectedLineSeparator() {
    if (getFlagInt(SYSTEM_LINE_SEPARATOR_DETECTED)) {
      return LineSeparator.getSystemLineSeparator().getSeparatorString();
    }
    return super.getDetectedLineSeparator();
  }

  @Override
  public void setDetectedLineSeparator(String separator) {
    boolean hasSystemSeparator = LineSeparator.getSystemLineSeparator().getSeparatorString().equals(separator);
    setFlagInt(SYSTEM_LINE_SEPARATOR_DETECTED, hasSystemSeparator);
    super.setDetectedLineSeparator(hasSystemSeparator ? null : separator);
  }

  @Override
  protected void setUserMap(@Nonnull KeyFMap map) {
    mySegment.setUserMap(myId, map);
  }

  @Nonnull
  @Override
  protected KeyFMap getUserMap() {
    return mySegment.getUserMap(this, myId);
  }

  @Override
  protected boolean changeUserMap(KeyFMap oldMap, KeyFMap newMap) {
    VirtualDirectoryImpl.checkLeaks(newMap);
    return mySegment.changeUserMap(myId, oldMap, UserDataInterner.internUserData(newMap));
  }

  private void checkNotTooLarge(@Nullable Object requestor) throws FileTooBigException {
    if (!(requestor instanceof LargeFileWriteRequestor) && RawFileLoader.getInstance().isLargeForContentLoading(getLength())) throw new FileTooBigException(getPath());
  }
}
