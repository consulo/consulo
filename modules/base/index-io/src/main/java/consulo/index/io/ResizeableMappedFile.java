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
package consulo.index.io;

import consulo.index.io.data.DataOutputStream;
import consulo.util.io.FileUtil;
import consulo.util.lang.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;

/**
 * @author max
 */
public class ResizeableMappedFile implements Forceable {
  private static final Logger LOG = LoggerFactory.getLogger(ResizeableMappedFile.class);

  private static final boolean truncateOnClose = SystemProperties.getBooleanProperty("idea.resizeable.file.truncate.on.close", false);
  private long myLogicalSize;
  private long myLastWrittenLogicalSize;
  private final PagedFileStorage myStorage;
  private final int myInitialSize;

  static final int DEFAULT_ALLOCATION_ROUND_FACTOR = 4096;
  private int myRoundFactor = DEFAULT_ALLOCATION_ROUND_FACTOR;

  public ResizeableMappedFile(@Nonnull File file, int initialSize, @Nullable PagedFileStorage.StorageLockContext lockContext, int pageSize, boolean valuesAreBufferAligned) throws IOException {
    this(file, initialSize, lockContext, pageSize, valuesAreBufferAligned, false);
  }

  public ResizeableMappedFile(@Nonnull File file, int initialSize, @Nullable PagedFileStorage.StorageLockContext lockContext, int pageSize, boolean valuesAreBufferAligned, boolean nativeBytesOrder)
          throws IOException {
    myStorage = new PagedFileStorage(file, lockContext, pageSize, valuesAreBufferAligned, nativeBytesOrder);
    myInitialSize = initialSize;
    myLastWrittenLogicalSize = myLogicalSize = readLength();
  }

  public ResizeableMappedFile(final File file, int initialSize, PagedFileStorage.StorageLock lock, int pageSize, boolean valuesAreBufferAligned) throws IOException {
    this(file, initialSize, lock.myDefaultStorageLockContext, pageSize, valuesAreBufferAligned);
  }

  public ResizeableMappedFile(final File file, int initialSize, PagedFileStorage.StorageLock lock) throws IOException {
    this(file, initialSize, lock, -1, false);
  }

  public long length() {
    return myLogicalSize;
  }

  private long realSize() {
    return myStorage.length();
  }

  void ensureSize(final long pos) {
    myLogicalSize = Math.max(pos, myLogicalSize);
    expand(pos);
  }

  public void setRoundFactor(int roundFactor) {
    myRoundFactor = roundFactor;
  }

  private void expand(final long max) {
    long realSize = realSize();
    if (max <= realSize) return;
    long suggestedSize;

    if (realSize == 0) {
      suggestedSize = doRoundToFactor(Math.max(myInitialSize, max));
    }
    else {
      suggestedSize = Math.max(realSize + 1, 2); // suggestedSize should increase with int multiplication on 1.625 factor

      while (max > suggestedSize) {
        long newSuggestedSize = suggestedSize * 13 >> 3;
        if (newSuggestedSize >= Integer.MAX_VALUE) {
          suggestedSize += suggestedSize / 5;
        }
        else {
          suggestedSize = newSuggestedSize;
        }
      }

      suggestedSize = doRoundToFactor(suggestedSize);
    }

    try {
      myStorage.resize(suggestedSize);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private long doRoundToFactor(long suggestedSize) {
    int roundFactor = myRoundFactor;
    if (suggestedSize % roundFactor != 0) {
      suggestedSize = (suggestedSize / roundFactor + 1) * roundFactor;
    }
    return suggestedSize;
  }

  private File getLengthFile() {
    return new File(myStorage.getFile().getPath() + ".len");
  }

  private void writeLength(final long len) {
    final File lengthFile = getLengthFile();
    DataOutputStream stream = null;
    try {
      stream = FileUtil.doIOOperation(new FileUtil.RepeatableIOOperation<DataOutputStream, IOException>() {
        boolean parentWasCreated;

        @Nullable
        @Override
        public DataOutputStream execute(boolean lastAttempt) throws IOException {
          try {
            return new DataOutputStream(new FileOutputStream(lengthFile));
          }
          catch (FileNotFoundException ex) {
            final File parentFile = lengthFile.getParentFile();

            if (!parentFile.exists()) {
              if (!parentWasCreated) {
                parentFile.mkdirs();
                parentWasCreated = true;
              }
              else {
                throw new IOException("Parent file still doesn't exist:" + lengthFile);
              }
            }
            if (!lastAttempt) return null;
            throw ex;
          }
        }
      });
      if (stream != null) stream.writeLong(len);
    }
    catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    finally {
      if (stream != null) {
        try {
          stream.close();
        }
        catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public boolean isDirty() {
    return myStorage.isDirty();
  }

  @Override
  public void force() {
    if (isDirty()) {
      if (myLastWrittenLogicalSize != myLogicalSize) {
        writeLength(myLogicalSize);
        myLastWrittenLogicalSize = myLogicalSize;
      }
    }
    myStorage.force();
  }

  private long readLength() {
    File lengthFile = getLengthFile();
    DataInputStream stream = null;
    try {
      stream = new DataInputStream(new FileInputStream(lengthFile));
      return stream.readLong();
    }
    catch (FileNotFoundException ignore) {
      return 0;
    }
    catch (IOException e) {
      long realSize = realSize();
      writeLength(realSize);
      return realSize;
    }
    finally {
      if (stream != null) {
        try {
          stream.close();
        }
        catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
  }

  public int getInt(long index) {
    return myStorage.getInt(index);
  }

  public void putInt(long index, int value) {
    ensureSize(index + 4);
    myStorage.putInt(index, value);
  }

  public short getShort(long index) {
    return myStorage.getShort(index);
  }

  public void putShort(long index, short value) {
    ensureSize(index + 2);
    myStorage.putShort(index, value);
  }

  public long getLong(long index) {
    return myStorage.getLong(index);
  }

  public void putLong(long index, long value) {
    ensureSize(index + 8);
    myStorage.putLong(index, value);
  }

  public byte get(long index) {
    return myStorage.get(index);
  }

  public void put(long index, byte value) {
    ensureSize(index + 1);
    myStorage.put(index, value);
  }

  public void get(long index, byte[] dst, int offset, int length) {
    myStorage.get(index, dst, offset, length);
  }

  public void put(long index, byte[] src, int offset, int length) {
    ensureSize(index + length);
    myStorage.put(index, src, offset, length);
  }

  public void close() {
    try {
      force();
      if (truncateOnClose && myLogicalSize < myStorage.length()) {
        myStorage.resize(myLogicalSize);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      myStorage.close();
    }
  }

  @Nonnull
  public PagedFileStorage getPagedFileStorage() {
    return myStorage;
  }
}
