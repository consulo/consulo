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
package consulo.index.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author max
 */
public class RandomAccessDataFile implements Forceable, Closeable {
  protected static final Logger LOG = LoggerFactory.getLogger(RandomAccessDataFile.class);

  private static final ChannelsAccessor ourChannelsAccessor =
    new OpenChannelsCache("RandomAccessDataFile", 150, PageCacheUtils.RESILIENT_CHANNEL_OPENER).asWritable();
  private static int ourFilesCount = 0;

  private final int myCount = ourFilesCount++;
  private final Path myFile;
  private final PagePool myPool;

  private final byte[] myTypedIOBuffer = new byte[8];

  private final OutputStreamWriter log;

  private volatile long mySize;
  private volatile boolean myIsDirty = false;
  private volatile boolean myIsDisposed = false;

  private static final boolean DEBUG = false;

  public RandomAccessDataFile(Path file) throws IOException {
    this(file, PagePool.SHARED);
  }

  public RandomAccessDataFile(Path file, PagePool pool) throws IOException {
    myPool = pool;
    myFile = file;

    mySize = Files.size(file);
    if (DEBUG) {
      log = new OutputStreamWriter(Files.newOutputStream(file.getParent().resolve(file.getFileName() + ".log")), StandardCharsets.UTF_8);
    }
    else {
      log = null;
    }
  }

  public Path getFile() {
    return myFile;
  }

  public void put(long addr, byte[] bytes, int off, int len) {
    assertNotDisposed();

    myIsDirty = true;
    mySize = Math.max(mySize, addr + len);

    while (len > 0) {
      Page page = myPool.alloc(this, addr);
      int written = page.put(addr, bytes, off, len);
      len -= written;
      addr += written;
      off += written;
    }
  }

  public void get(long addr, byte[] bytes, int off, int len) {
    assertNotDisposed();

    while (len > 0) {
      Page page = myPool.alloc(this, addr);
      int read = page.get(addr, bytes, off, len);
      len -= read;
      addr += read;
      off += read;
    }
  }

  private <T> T useFileChannel(FileChannelOperation<T> channelConsumer) throws IOException {
    return ourChannelsAccessor.executeOp(myFile, channelConsumer);
  }

  public void putInt(long addr, int value) {
    Bits.putInt(myTypedIOBuffer, 0, value);
    put(addr, myTypedIOBuffer, 0, 4);
  }

  public int getInt(long addr) {
    get(addr, myTypedIOBuffer, 0, 4);
    return Bits.getInt(myTypedIOBuffer, 0);
  }

  public void putLong(long addr, long value) {
    Bits.putLong(myTypedIOBuffer, 0, value);
    put(addr, myTypedIOBuffer, 0, 8);
  }

  public void putByte(long addr, byte b) {
    myTypedIOBuffer[0] = b;
    put(addr, myTypedIOBuffer, 0, 1);
  }

  public byte getByte(long addr) {
    get(addr, myTypedIOBuffer, 0, 1);
    return myTypedIOBuffer[0];
  }

  public long getLong(long addr) {
    get(addr, myTypedIOBuffer, 0, 8);
    return Bits.getLong(myTypedIOBuffer, 0);
  }

  public String getUTF(long addr) {
    try {
      int len = getInt(addr);
      byte[] bytes = new byte[len];
      get(addr + 4, bytes, 0, len);
      return new String(bytes, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      // Can't be
      return "";
    }
  }

  public void putUTF(long addr, String value) {
    try {
      byte[] bytes = value.getBytes("UTF-8");
      putInt(addr, bytes.length);
      put(addr + 4, bytes, 0, bytes.length);
    }
    catch (UnsupportedEncodingException e) {
      // Can't be
    }
  }

  public long length() {
    assertNotDisposed();
    return mySize;
  }

  public long physicalLength() {
    assertNotDisposed();

    try {
      return useFileChannel(FileChannel::size);
    }
    catch (IOException e) {
      return 0;
    }
  }

  public void dispose() {
    if (myIsDisposed) return;
    myPool.flushPages(this);
    try {
      ourChannelsAccessor.closeChannel(myFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    myIsDisposed = true;
  }

  public void close() {
    dispose();
  }

  public void force() {
    assertNotDisposed();
    if (isDirty()) {
      myPool.flushPages(this);
      myIsDirty = false;
    }
  }

  public void flushSomePages(int maxPagesToFlush) {
    assertNotDisposed();
    if (isDirty()) {
      myIsDirty = !myPool.flushPages(this, maxPagesToFlush);
    }
  }

  public boolean isDirty() {
    assertNotDisposed();
    return myIsDirty;
  }

  public boolean isDisposed() {
    return myIsDisposed;
  }

  private void assertNotDisposed() {
    if (myIsDisposed) {
      LOG.error("storage file is disposed: " + myFile);
    }
  }

  public static int totalReads = 0;
  public static long totalReadBytes = 0;

  public static int totalWrites = 0;
  public static long totalWriteBytes = 0;

  void loadPage(Page page) {
    assertNotDisposed();
    try {
      ByteBuffer buf = page.getBuf();

      totalReads++;
      totalReadBytes += Page.PAGE_SIZE;

      if (DEBUG) {
        log.write("Read at: \t" + page.getOffset() + "\t len: " + Page.PAGE_SIZE + ", size: " + mySize + "\n");
      }
      useFileChannel(file -> file.read(ByteBuffer.wrap(buf.array(), 0, Page.PAGE_SIZE), page.getOffset()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void flushPage(Page page, int start, int end) {
    assertNotDisposed();
    try {
      flush(page.getBuf(), page.getOffset() + start, start, end - start);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void flush(ByteBuffer buf, long fileOffset, int bufOffset, int length) throws IOException {
    int lengthToWrite = fileOffset + length > mySize ? (int)(mySize - fileOffset) : length;

    useFileChannel(file -> {
      totalWrites++;
      totalWriteBytes += lengthToWrite;

      if (DEBUG) {
        log.write("Write at: \t" + fileOffset + "\t len: " + lengthToWrite + ", size: " + mySize + ", filesize: " + file.size() + "\n");
      }
      return file.write(ByteBuffer.wrap(buf.array(), bufOffset, lengthToWrite), fileOffset);
    });
  }

  public int hashCode() {
    return myCount;
  }

  @Override
  public String toString() {
    return "RandomAccessFile[" + myFile + ", dirty=" + myIsDirty + "]";
  }
}
