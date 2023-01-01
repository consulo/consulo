// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io;

import consulo.util.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ReadWriteDirectBufferWrapper extends DirectBufferWrapper {
  private static final Logger LOG = LoggerFactory.getLogger(ReadWriteDirectBufferWrapper.class);
  private static final String RW = "rw";

  protected ReadWriteDirectBufferWrapper(final File file, final long offset, final long length) {
    super(file, offset, length);
    assert length <= Integer.MAX_VALUE : length;
  }

  @Override
  protected ByteBuffer create() throws IOException {
    try (FileContext context = new FileContext(myFile)) {
      RandomAccessFile file = context.file;
      assert file != null;
      FileChannel channel = file.getChannel();
      channel.position(myPosition);
      ByteBuffer buffer = ByteBuffer.allocateDirect((int)myLength);
      channel.read(buffer);
      return buffer;
    }
  }

  static class FileContext implements AutoCloseable {
    final RandomAccessFile file;

    FileContext(File path) throws IOException {
      file = FileUtil.doIOOperation(new FileUtil.RepeatableIOOperation<RandomAccessFile, IOException>() {
        boolean parentWasCreated;

        @Nullable
        @Override
        public RandomAccessFile execute(boolean finalAttempt) throws IOException {
          try {
            return new RandomAccessFile(path, RW);
          }
          catch (FileNotFoundException ex) {
            File parentFile = path.getParentFile();
            if (!parentFile.exists()) {
              if (!parentWasCreated) {
                FileUtil.createDirectory(parentFile);
                parentWasCreated = true;
              }
              else {
                throw new IOException("Parent directory still doesn't exist: " + path);
              }
            }
            if (!finalAttempt) return null;
            throw ex;
          }
        }
      });
    }

    @Override
    public void close() {
      try {
        if (file != null) file.close();
      }
      catch (IOException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  FileContext flushWithContext(@Nullable FileContext fileContext) {
    ByteBuffer buffer = getCachedBuffer();
    if (buffer != null && isDirty()) {
      try {
        if (fileContext == null) {
          fileContext = new FileContext(myFile);
        }
        doFlush(fileContext, buffer);
      }
      catch (IOException e) {
        LOG.error(e.getMessage(), e);
      }
    }
    return fileContext;
  }

  private void doFlush(FileContext fileContext, ByteBuffer buffer) throws IOException {
    RandomAccessFile file = fileContext.file;
    assert file != null;
    FileChannel channel = file.getChannel();
    channel.position(myPosition);
    buffer.rewind();
    channel.write(buffer);
    myDirty = false;
  }

  @Override
  public void flush() {
    ByteBuffer buffer = getCachedBuffer();
    if (buffer != null && isDirty()) {
      try (FileContext context = new FileContext(myFile)) {
        doFlush(context, buffer);
      }
      catch (IOException e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }
}