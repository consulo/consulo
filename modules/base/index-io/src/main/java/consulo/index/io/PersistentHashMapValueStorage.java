// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.*;
import consulo.util.lang.Comparing;
import consulo.util.lang.SystemProperties;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author max
 */
public class PersistentHashMapValueStorage {
  @Nullable
  private RAReader myCompactionModeReader;
  private volatile long mySize;
  private final File myFile;
  private final String myPath;

  private final CreationTimeOptions myOptions;

  private boolean myCompactionMode;

  private static final int CACHE_PROTECTED_QUEUE_SIZE = 10;
  private static final int CACHE_PROBATIONAL_QUEUE_SIZE = 20;
  private static final long MAX_RETAINED_LIMIT_WHEN_COMPACTING = 100 * 1024 * 1024;

  static final long SOFT_MAX_RETAINED_LIMIT = 10 * 1024 * 1024;
  static final int BLOCK_SIZE_TO_WRITE_WHEN_SOFT_MAX_RETAINED_LIMIT_IS_HIT = 1024;

  public static class CreationTimeOptions {
    public static final ThreadLocal<ExceptionalIOCancellationCallback> EXCEPTIONAL_IO_CANCELLATION = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> READONLY = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> COMPACT_CHUNKS_WITH_VALUE_DESERIALIZATION = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> HAS_NO_CHUNKS = new ThreadLocal<>();

    static final ThreadLocal<Boolean> DO_COMPRESSION = new ThreadLocal<Boolean>() {
      @Override
      protected Boolean initialValue() {
        return COMPRESSION_ENABLED;
      }
    };

    private final ExceptionalIOCancellationCallback myExceptionalIOCancellationCallback;
    private final boolean myReadOnly;
    private final boolean myCompactChunksWithValueDeserialization;
    private final boolean myHasNoChunks;
    private final boolean myDoCompression;

    private CreationTimeOptions(ExceptionalIOCancellationCallback callback, boolean readOnly, boolean compactChunksWithValueDeserialization, boolean hasNoChunks, boolean doCompression) {
      myExceptionalIOCancellationCallback = callback;
      myReadOnly = readOnly;
      myCompactChunksWithValueDeserialization = compactChunksWithValueDeserialization;
      myHasNoChunks = hasNoChunks;
      myDoCompression = doCompression;
    }

    int getVersion() {
      return (myHasNoChunks ? 10 : 0) * 31 + (myDoCompression ? 0x13 : 0);
    }

    @Nonnull
    CreationTimeOptions setReadOnly() {
      return new CreationTimeOptions(myExceptionalIOCancellationCallback, true, myCompactChunksWithValueDeserialization, myHasNoChunks, myDoCompression);
    }

    @Nonnull
    static CreationTimeOptions threadLocalOptions() {
      return new CreationTimeOptions(EXCEPTIONAL_IO_CANCELLATION.get(), READONLY.get() == Boolean.TRUE, COMPACT_CHUNKS_WITH_VALUE_DESERIALIZATION.get() == Boolean.TRUE,
                                     HAS_NO_CHUNKS.get() == Boolean.TRUE, DO_COMPRESSION.get() == Boolean.TRUE);
    }
  }

  public interface ExceptionalIOCancellationCallback {
    void checkCancellation();
  }

  @Nonnull
  CreationTimeOptions getOptions() {
    return myOptions;
  }

  // cache size is twice larger than constants because (when used) it replaces two caches
  private static final FileAccessorCache<String, RandomAccessFileWithLengthAndSizeTracking> ourRandomAccessFileCache =
          new FileAccessorCache<String, RandomAccessFileWithLengthAndSizeTracking>(2 * CACHE_PROTECTED_QUEUE_SIZE, 2 * CACHE_PROBATIONAL_QUEUE_SIZE) {
            @Nonnull
            @Override
            protected RandomAccessFileWithLengthAndSizeTracking createAccessor(String path) throws IOException {
              return new RandomAccessFileWithLengthAndSizeTracking(path);
            }

            @Override
            protected void disposeAccessor(@Nonnull RandomAccessFileWithLengthAndSizeTracking fileAccessor) throws IOException {
              fileAccessor.close();
            }
          };

  private static final boolean useSingleFileDescriptor = SystemProperties.getBooleanProperty("idea.use.single.file.descriptor.for.persistent.hash.map", true);

  private static final FileAccessorCache<String, DataOutputStream> ourAppendersCache = new FileAccessorCache<String, DataOutputStream>(CACHE_PROTECTED_QUEUE_SIZE, CACHE_PROBATIONAL_QUEUE_SIZE) {
    @Nonnull
    @Override
    protected DataOutputStream createAccessor(String path) throws IOException {
      OutputStream out = useSingleFileDescriptor ? new OutputStreamOverRandomAccessFileCache(path) : new FileOutputStream(path, true);
      return new DataOutputStream(new BufferedOutputStream(out));
    }

    @Override
    protected void disposeAccessor(@Nonnull DataOutputStream fileAccessor) throws IOException {
      if (!useSingleFileDescriptor) IOUtil.syncStream(fileAccessor);
      fileAccessor.close();
    }
  };

  private static final FileAccessorCache<String, RAReader> ourReadersCache = new FileAccessorCache<String, RAReader>(CACHE_PROTECTED_QUEUE_SIZE, CACHE_PROBATIONAL_QUEUE_SIZE) {
    @Nonnull
    @Override
    protected RAReader createAccessor(String path) {
      return useSingleFileDescriptor ? new ReaderOverRandomAccessFileCache(path) : new FileReader(new File(path));
    }

    @Override
    protected void disposeAccessor(@Nonnull RAReader fileAccessor) {
      fileAccessor.dispose();
    }
  };

  private final CompressedAppendableFile myCompressedAppendableFile;

  public static final boolean COMPRESSION_ENABLED = SystemProperties.getBooleanProperty("idea.compression.enabled", true);

  private PersistentHashMapValueStorage(@Nonnull String path) {
    this(path, CreationTimeOptions.threadLocalOptions());
  }

  private PersistentHashMapValueStorage(@Nonnull String path, @Nonnull CreationTimeOptions options) {
    myPath = path;
    myFile = new File(path);
    myOptions = options;

    myCompressedAppendableFile = myOptions.myDoCompression ? new MyCompressedAppendableFile() : null;
    // volatile write
    mySize = myCompressedAppendableFile == null ? myFile.length() : myCompressedAppendableFile.length();
  }

  public long appendBytes(ByteArraySequence data, long prevChunkAddress) throws IOException {
    return appendBytes(data.getBytes(), data.getOffset(), data.getLength(), prevChunkAddress);
  }

  public long appendBytes(byte[] data, int offset, int dataLength, long prevChunkAddress) throws IOException {
    if (mySize == 0) {
      byte[] bytes = "Header Record For PersistentHashMapValueStorage".getBytes(StandardCharsets.UTF_8);
      doAppendBytes(bytes, 0, bytes.length, 0);

      // avoid corruption issue when disk fails to write first record synchronously or unexpected first write file increase (IDEA-106306),
      // code depends on correct value of mySize
      FileAccessorCache.Handle<DataOutputStream> streamCacheValue = ourAppendersCache.getIfCached(myPath);
      if (streamCacheValue != null) {
        try {
          IOUtil.syncStream(streamCacheValue.get());
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
        finally {
          streamCacheValue.release();
        }
      }

      long currentLength = myFile.length();
      if (currentLength > mySize) {  // if real file length (unexpectedly) increases
        LoggerFactory.getLogger(getClass()).info("Avoided PSHM corruption due to write failure:" + myPath);
        mySize = currentLength;  // volatile write
      }
    }

    return doAppendBytes(data, offset, dataLength, prevChunkAddress);
  }

  void checkAppendsAllowed(int previouslyAccumulatedChunkSize) {
    if (previouslyAccumulatedChunkSize != 0 && myOptions.myHasNoChunks) throw new AssertionError();
  }

  private long doAppendBytes(byte[] data, int offset, int dataLength, long prevChunkAddress) throws IOException {
    if (!allowedToCompactChunks()) throw new AssertionError();
    if (prevChunkAddress != 0 && myOptions.myHasNoChunks) throw new AssertionError();
    long result = mySize; // volatile read
    final FileAccessorCache.Handle<DataOutputStream> appender = myCompressedAppendableFile != null ? null : ourAppendersCache.get(myPath);

    try {
      if (myCompressedAppendableFile != null) {
        BufferExposingByteArrayOutputStream stream = new BufferExposingByteArrayOutputStream(15);
        DataOutputStream testStream = new DataOutputStream(stream);
        saveHeader(dataLength, prevChunkAddress, result, testStream);
        myCompressedAppendableFile.append(stream.getInternalBuffer(), stream.size());
        myCompressedAppendableFile.append(data, offset, dataLength);
        mySize += stream.size() + dataLength;  // volatile write
      }
      else {
        DataOutputStream dataOutputStream = appender.get();
        dataOutputStream.resetWrittenBytesCount();

        saveHeader(dataLength, prevChunkAddress, result, dataOutputStream);
        dataOutputStream.write(data, offset, dataLength);
        mySize += dataOutputStream.resetWrittenBytesCount();  // volatile write
      }
    }
    finally {
      if (appender != null) appender.release();
    }

    return result;
  }

  private void saveHeader(int dataLength, long prevChunkAddress, long result, @Nonnull DataOutputStream dataOutputStream) throws IOException {
    DataInputOutputUtil.writeINT(dataOutputStream, dataLength);
    if (!myOptions.myHasNoChunks) {
      if (result < prevChunkAddress) {
        throw new IOException("writePrevChunkAddress:" + result + "," + prevChunkAddress + "," + myFile);
      }
      long diff = result - prevChunkAddress;
      DataInputOutputUtil.writeLONG(dataOutputStream, prevChunkAddress == 0 ? 0 : diff);
    }
  }

  private static final ThreadLocalCachedByteArray myBuffer = new ThreadLocalCachedByteArray();
  private final UnsyncByteArrayInputStream myBufferStreamWrapper = new UnsyncByteArrayInputStream(ArrayUtil.EMPTY_BYTE_ARRAY);
  private final DataInputStream myBufferDataStreamWrapper = new DataInputStream(myBufferStreamWrapper);
  private static final int ourBufferLength = 1024;

  private long compactValuesWithoutChunks(@Nonnull List<? extends PersistentHashMap.CompactionRecordInfo> infos, @Nonnull PersistentHashMapValueStorage storage) throws IOException {
    //infos = new ArrayList<PersistentHashMap.CompactionRecordInfo>(infos);
    infos.sort((Comparator<PersistentHashMap.CompactionRecordInfo>)(info, info2) -> Comparing.compare(info.valueAddress, info2.valueAddress));

    final int fileBufferLength = 256 * 1024;
    final byte[] buffer = new byte[fileBufferLength];

    int fragments = 0;
    int newFragments = 0;

    byte[] outputBuffer = new byte[4096];

    long readStartOffset = -1;
    int bytesRead = -1;

    for (PersistentHashMap.CompactionRecordInfo info : infos) {
      int recordStartInBuffer = (int)(info.valueAddress - readStartOffset);

      if (recordStartInBuffer + 5 > fileBufferLength || readStartOffset == -1) {
        readStartOffset = info.valueAddress;
        long remainingBytes = readStartOffset != -1 ? mySize - readStartOffset : mySize;
        bytesRead = remainingBytes < fileBufferLength ? (int)remainingBytes : fileBufferLength;

        myCompactionModeReader.get(readStartOffset, buffer, 0, bytesRead); // buffer contains [readStartOffset, readStartOffset + bytesRead)
        recordStartInBuffer = (int)(info.valueAddress - readStartOffset);
      }

      myBufferStreamWrapper.init(buffer, recordStartInBuffer, buffer.length);
      int available = myBufferStreamWrapper.available();
      int chunkSize = DataInputOutputUtil.readINT(myBufferDataStreamWrapper);
      long prevChunkAddress = readPrevChunkAddress(info.valueAddress);
      assert prevChunkAddress == 0;
      int dataOffset = available - myBufferStreamWrapper.available() + recordStartInBuffer;

      if (chunkSize >= outputBuffer.length) {
        outputBuffer = new byte[(chunkSize / 4096 + 1) * 4096];
      }

      // dataOffset .. dataOffset + chunkSize
      int bytesFitInBuffer = Math.min(chunkSize, fileBufferLength - dataOffset);
      System.arraycopy(buffer, dataOffset, outputBuffer, 0, bytesFitInBuffer);

      while (bytesFitInBuffer != chunkSize) {
        readStartOffset += bytesRead;

        long remainingBytes = mySize - readStartOffset;
        bytesRead = remainingBytes < fileBufferLength ? (int)remainingBytes : fileBufferLength;

        myCompactionModeReader.get(readStartOffset, buffer, 0, bytesRead); // buffer contains [readStartOffset, readStartOffset + bytesRead)
        int newBytesFitInBuffer = Math.min(chunkSize - bytesFitInBuffer, fileBufferLength);
        System.arraycopy(buffer, 0, outputBuffer, bytesFitInBuffer, newBytesFitInBuffer);
        bytesFitInBuffer += newBytesFitInBuffer;
      }

      info.newValueAddress = storage.appendBytes(outputBuffer, 0, chunkSize, 0);

      ++fragments;
      ++newFragments;
    }

    return fragments | ((long)newFragments << 32);
  }

  long compactValues(@Nonnull List<? extends PersistentHashMap.CompactionRecordInfo> infos, @Nonnull PersistentHashMapValueStorage storage) throws IOException {
    if (myOptions.myHasNoChunks) {
      return compactValuesWithoutChunks(infos, storage);
    }

    PriorityQueue<PersistentHashMap.CompactionRecordInfo> records = new PriorityQueue<>(infos.size(), (info, info2) -> Comparing.compare(info2.valueAddress, info.valueAddress));

    records.addAll(infos);

    final int fileBufferLength = 256 * 1024;
    final int maxRecordHeader = 5 /* max length - variable int */ + 10 /* max long offset*/;
    final byte[] buffer = new byte[fileBufferLength + maxRecordHeader];
    byte[] reusedAccumulatedChunksBuffer = {};

    long lastReadOffset = mySize;
    long lastConsumedOffset = lastReadOffset;
    long allRecordsStart = 0;
    int fragments = 0;
    int newFragments = 0;
    int allRecordsLength = 0;

    byte[] stuffFromPreviousRecord = null;
    int bytesRead = (int)(mySize - (mySize / fileBufferLength) * fileBufferLength);
    long retained = 0;

    while (lastReadOffset != 0) {
      final long readStartOffset = lastReadOffset - bytesRead;
      myCompactionModeReader.get(readStartOffset, buffer, 0, bytesRead); // buffer contains [readStartOffset, readStartOffset + bytesRead)

      while (!records.isEmpty()) {
        final PersistentHashMap.CompactionRecordInfo info = records.peek();
        if (info.valueAddress >= readStartOffset) {
          if (info.valueAddress >= lastReadOffset) {
            throw new IOException("Value storage is corrupted: value file size:" + mySize + ", readStartOffset:" + readStartOffset + ", record address:" + info.valueAddress + "; file: " + myPath);
          }
          // record start is inside our buffer

          final int recordStartInBuffer = (int)(info.valueAddress - readStartOffset);
          myBufferStreamWrapper.init(buffer, recordStartInBuffer, buffer.length);

          if (stuffFromPreviousRecord != null && fileBufferLength - recordStartInBuffer < maxRecordHeader) {
            // add additional bytes to read offset / size
            if (allRecordsStart != 0) {
              myCompactionModeReader.get(allRecordsStart, buffer, bytesRead, maxRecordHeader);
            }
            else {
              final int maxAdditionalBytes = Math.min(stuffFromPreviousRecord.length, maxRecordHeader);
              for (int i = 0; i < maxAdditionalBytes; ++i) {
                buffer[bytesRead + i] = stuffFromPreviousRecord[i];
              }
            }
          }

          int available = myBufferStreamWrapper.available();
          int chunkSize = DataInputOutputUtil.readINT(myBufferDataStreamWrapper);
          final long prevChunkAddress = readPrevChunkAddress(info.valueAddress);
          final int dataOffset = available - myBufferStreamWrapper.available();

          byte[] accumulatedChunksBuffer;
          if (info.value != null) {
            int defragmentedChunkSize = info.value.length + chunkSize;
            if (prevChunkAddress == 0) {
              if (defragmentedChunkSize >= reusedAccumulatedChunksBuffer.length) {
                reusedAccumulatedChunksBuffer = new byte[defragmentedChunkSize];
              }
              accumulatedChunksBuffer = reusedAccumulatedChunksBuffer;
            }
            else {
              accumulatedChunksBuffer = new byte[defragmentedChunkSize];
              retained += defragmentedChunkSize;
            }
            System.arraycopy(info.value, 0, accumulatedChunksBuffer, chunkSize, info.value.length);
          }
          else {
            if (prevChunkAddress == 0) {
              if (chunkSize >= reusedAccumulatedChunksBuffer.length) reusedAccumulatedChunksBuffer = new byte[chunkSize];
              accumulatedChunksBuffer = reusedAccumulatedChunksBuffer;
            }
            else {
              accumulatedChunksBuffer = new byte[chunkSize];
              retained += chunkSize;
            }
          }

          final int chunkSizeOutOfBuffer = Math.min(chunkSize, Math.max((int)(info.valueAddress + dataOffset + chunkSize - lastReadOffset), 0));
          if (chunkSizeOutOfBuffer > 0) {
            if (allRecordsStart != 0) {
              myCompactionModeReader.get(allRecordsStart, accumulatedChunksBuffer, chunkSize - chunkSizeOutOfBuffer, chunkSizeOutOfBuffer);
            }
            else {
              int offsetInStuffFromPreviousRecord = Math.max((int)(info.valueAddress + dataOffset - lastReadOffset), 0);
              // stuffFromPreviousRecord starts from lastReadOffset
              System.arraycopy(stuffFromPreviousRecord, offsetInStuffFromPreviousRecord, accumulatedChunksBuffer, chunkSize - chunkSizeOutOfBuffer, chunkSizeOutOfBuffer);
            }
          }

          stuffFromPreviousRecord = null;
          allRecordsStart = allRecordsLength = 0;

          lastConsumedOffset = info.valueAddress;
          checkPreconditions(accumulatedChunksBuffer, chunkSize, 0);

          System.arraycopy(buffer, recordStartInBuffer + dataOffset, accumulatedChunksBuffer, 0, chunkSize - chunkSizeOutOfBuffer);

          ++fragments;
          records.remove(info);
          if (info.value != null) {
            chunkSize += info.value.length;
            retained -= info.value.length;
            info.value = null;
          }

          if (prevChunkAddress == 0) {
            info.newValueAddress = storage.appendBytes(accumulatedChunksBuffer, 0, chunkSize, info.newValueAddress);
            ++newFragments;
          }
          else {
            if (retained > SOFT_MAX_RETAINED_LIMIT && accumulatedChunksBuffer.length > BLOCK_SIZE_TO_WRITE_WHEN_SOFT_MAX_RETAINED_LIMIT_IS_HIT || retained > MAX_RETAINED_LIMIT_WHEN_COMPACTING) {
              // to avoid OOME we need to save bytes in accumulatedChunksBuffer
              newFragments += saveAccumulatedDataOnDiskPreservingWriteOrder(storage, info, prevChunkAddress, accumulatedChunksBuffer, chunkSize);
              retained -= accumulatedChunksBuffer.length;
              continue;
            }
            info.value = accumulatedChunksBuffer;
            info.valueAddress = prevChunkAddress;
            records.add(info);
          }
        }
        else {
          // [readStartOffset,lastConsumedOffset) is from previous segment
          if (stuffFromPreviousRecord == null) {
            stuffFromPreviousRecord = new byte[(int)(lastConsumedOffset - readStartOffset)];
            System.arraycopy(buffer, 0, stuffFromPreviousRecord, 0, stuffFromPreviousRecord.length);
          }
          else {
            allRecordsStart = readStartOffset;
            allRecordsLength += buffer.length;
          }
          break; // request next read
        }
      }

      lastReadOffset -= bytesRead;
      bytesRead = fileBufferLength;
    }

    return fragments | ((long)newFragments << 32);
  }

  private int saveAccumulatedDataOnDiskPreservingWriteOrder(PersistentHashMapValueStorage storage,
                                                            PersistentHashMap.CompactionRecordInfo info,
                                                            long prevChunkAddress,
                                                            byte[] accumulatedChunksData,
                                                            int accumulatedChunkDataLength) throws IOException {
    ReadResult result = readBytes(prevChunkAddress);
    // to avoid possible OOME result.bytes and accumulatedChunksData are not combined in one chunk, instead they are
    // placed one after another, such near placement should be fine because of disk caching
    info.newValueAddress = storage.appendBytes(result.buffer, 0, result.buffer.length, info.newValueAddress);
    info.newValueAddress = storage.appendBytes(accumulatedChunksData, 0, accumulatedChunkDataLength, info.newValueAddress);

    info.value = null;
    info.valueAddress = 0;
    return 2; // number of chunks produced = number of appendBytes called
  }

  static class ReadResult {
    final byte[] buffer;
    final int chunksCount;

    ReadResult(byte[] buffer, int chunksCount) {
      this.buffer = buffer;
      this.chunksCount = chunksCount;
    }
  }

  private long myChunksRemovalTime;
  private long myChunksReadingTime;
  private int myChunks;
  private long myChunksOriginalBytes;
  private long myChunksBytesAfterRemoval;
  private int myLastReportedChunksCount;

  /**
   * Reads bytes pointed by tailChunkAddress into result passed, returns new address if linked list compactification have been performed
   */
  public ReadResult readBytes(long tailChunkAddress) throws IOException {
    forceAppender(myPath);

    checkCancellation();
    long startedTime = ourDumpChunkRemovalTime ? System.nanoTime() : 0;

    RAReader reader = myCompactionModeReader;
    FileAccessorCache.Handle<RAReader> readerHandle = null;
    if (reader == null) {
      readerHandle = myCompressedAppendableFile != null ? null : ourReadersCache.get(myPath);
      reader = myCompressedAppendableFile != null ? null : readerHandle.get();
    }

    int chunkCount = 0;
    byte[] result = null;
    try {
      long chunk = tailChunkAddress;
      while (chunk != 0) {
        if (chunk < 0 || chunk > mySize) throw new PersistentEnumeratorBase.CorruptedException(myFile);

        byte[] buffer = myBuffer.getBuffer(ourBufferLength);
        int len = (int)Math.min(ourBufferLength, mySize - chunk);

        if (myCompressedAppendableFile != null) {
          DataInputStream stream = myCompressedAppendableFile.getStream(chunk);
          stream.readFully(buffer, 0, len);
          stream.close();
        }
        else {
          reader.get(chunk, buffer, 0, len);
        }
        myBufferStreamWrapper.init(buffer, 0, len);

        final int chunkSize = DataInputOutputUtil.readINT(myBufferDataStreamWrapper);
        if (chunkSize < 0) {
          throw new IOException("Value storage corrupted: negative chunk size: " + chunkSize);
        }
        final long prevChunkAddress = readPrevChunkAddress(chunk);
        final int headerOffset = len - myBufferStreamWrapper.available();

        byte[] b = new byte[(result != null ? result.length : 0) + chunkSize];
        if (result != null) System.arraycopy(result, 0, b, b.length - result.length, result.length);
        result = b;

        checkPreconditions(result, chunkSize, 0);
        if (chunkSize < ourBufferLength - headerOffset) {
          System.arraycopy(buffer, headerOffset, result, 0, chunkSize);
        }
        else {
          if (myCompressedAppendableFile != null) {
            DataInputStream stream = myCompressedAppendableFile.getStream(chunk + headerOffset);
            stream.readFully(result, 0, chunkSize);
            stream.close();
          }
          else {
            reader.get(chunk + headerOffset, result, 0, chunkSize);
          }
        }

        if (prevChunkAddress >= chunk) throw new PersistentEnumeratorBase.CorruptedException(myFile);

        chunk = prevChunkAddress;
        chunkCount++;

        if (prevChunkAddress != 0) {
          checkCancellation();
          assert !myOptions.myHasNoChunks;
        }
        if (result.length > mySize && myCompressedAppendableFile == null) {
          throw new PersistentEnumeratorBase.CorruptedException(myFile);
        }
      }
    }
    catch (OutOfMemoryError error) {
      throw new PersistentEnumeratorBase.CorruptedException(myFile);
    }
    finally {
      if (readerHandle != null) {
        readerHandle.release();
      }
    }

    if (chunkCount > 1) {
      checkCancellation();

      myChunksReadingTime += (ourDumpChunkRemovalTime ? System.nanoTime() : 0) - startedTime;
      myChunks += chunkCount;
      myChunksOriginalBytes += result.length;
    }

    return new ReadResult(result, chunkCount);
  }

  private boolean allowedToCompactChunks() {
    return !myCompactionMode && !myOptions.myReadOnly;
  }

  boolean performChunksCompaction(int chunksCount, int chunksBytesSize) {
    return chunksCount > 1 && allowedToCompactChunks();
  }

  long compactChunks(PersistentHashMap.ValueDataAppender appender, ReadResult result) throws IOException {
    checkCancellation();
    long startedTime = ourDumpChunkRemovalTime ? System.nanoTime() : 0;
    long newValueOffset;

    if (myOptions.myCompactChunksWithValueDeserialization) {
      final BufferExposingByteArrayOutputStream stream = new BufferExposingByteArrayOutputStream(result.buffer.length);
      DataOutputStream testStream = new DataOutputStream(stream);
      appender.append(testStream);
      newValueOffset = appendBytes(stream.toByteArraySequence(), 0);
      myChunksBytesAfterRemoval += stream.size();
    }
    else {
      newValueOffset = appendBytes(new ByteArraySequence(result.buffer), 0);
      myChunksBytesAfterRemoval += result.buffer.length;
    }

    if (ourDumpChunkRemovalTime) {
      myChunksRemovalTime += System.nanoTime() - startedTime;

      if (myChunks - myLastReportedChunksCount > 1000) {
        myLastReportedChunksCount = myChunks;

        System.out.println(myChunks +
                           " chunks were read " +
                           (myChunksReadingTime / 1000000) +
                           "ms, bytes: " +
                           myChunksOriginalBytes +
                           (myChunksOriginalBytes != myChunksBytesAfterRemoval ? "->" + myChunksBytesAfterRemoval : "") +
                           " compaction:" +
                           (myChunksRemovalTime / 1000000) +
                           "ms in " +
                           myPath);
      }
    }

    return newValueOffset;
  }

  private static final boolean ourDumpChunkRemovalTime = SystemProperties.getBooleanProperty("idea.phmp.dump.chunk.removal.time", false);

  // hook for exceptional termination of long io operation
  private void checkCancellation() {
    if (myOptions.myExceptionalIOCancellationCallback != null) myOptions.myExceptionalIOCancellationCallback.checkCancellation();
  }

  private long readPrevChunkAddress(long chunk) throws IOException {
    if (myOptions.myHasNoChunks) return 0;
    final long prevOffsetDiff = DataInputOutputUtil.readLONG(myBufferDataStreamWrapper);
    if (prevOffsetDiff >= chunk) {
      throw new IOException("readPrevChunkAddress:" + chunk + "," + prevOffsetDiff + "," + mySize + "," + myFile);
    }
    return prevOffsetDiff != 0 ? chunk - prevOffsetDiff : 0;
  }

  public long getSize() {
    return mySize;
  }

  private static void checkPreconditions(final byte[] result, final int chunkSize, final int off) throws IOException {
    if (chunkSize < 0) {
      throw new IOException("Value storage corrupted: negative chunk size");
    }
    if (off < 0) {
      throw new IOException("Value storage corrupted: negative offset");
    }
    if (chunkSize > result.length - off) {
      throw new IOException("Value storage corrupted");
    }
  }

  public void force() {
    if (myOptions.myReadOnly) return;
    if (myCompressedAppendableFile != null) {
      myCompressedAppendableFile.force();
    }
    if (mySize < 0) assert false;  // volatile read
    forceAppender(myPath);
  }

  private static void forceAppender(String path) {
    final FileAccessorCache.Handle<DataOutputStream> cached = ourAppendersCache.getIfCached(path);
    if (cached != null) {
      try {
        cached.get().flush();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        cached.release();
      }
    }
  }

  public void dispose() {
    try {
      if (myCompressedAppendableFile != null) myCompressedAppendableFile.dispose();
    }
    finally {
      if (mySize < 0) assert false; // volatile read
      ourReadersCache.remove(myPath);
      ourAppendersCache.remove(myPath);

      ourRandomAccessFileCache.remove(myPath);

      if (myCompactionModeReader != null) {
        myCompactionModeReader.dispose();
        myCompactionModeReader = null;
      }
    }
  }

  void switchToCompactionMode() {
    ourReadersCache.remove(myPath);

    ourRandomAccessFileCache.remove(myPath);
    // in compaction mode use faster reader
    if (myCompressedAppendableFile != null) {
      myCompactionModeReader = new RAReader() {
        @Override
        public void get(long addr, byte[] dst, int off, int len) throws IOException {
          DataInputStream stream = myCompressedAppendableFile.getStream(addr);
          stream.readFully(dst, off, len);
          stream.close();
        }

        @Override
        public void dispose() {
        }
      };
    }
    else {
      myCompactionModeReader = new FileReader(myFile);
    }

    myCompactionMode = true;
  }

  public static PersistentHashMapValueStorage create(final String path, boolean readOnly) {
    if (readOnly) CreationTimeOptions.READONLY.set(Boolean.TRUE);
    try {
      return new PersistentHashMapValueStorage(path);
    }
    finally {
      if (readOnly) CreationTimeOptions.READONLY.set(null);
    }
  }

  public static PersistentHashMapValueStorage create(@Nonnull String path, @Nonnull CreationTimeOptions options) {
    return new PersistentHashMapValueStorage(path, options);
  }

  private interface RAReader {
    void get(long addr, byte[] dst, int off, int len) throws IOException;

    void dispose();
  }

  private static class ReaderOverRandomAccessFileCache implements RAReader {
    private final String myPath;

    private ReaderOverRandomAccessFileCache(@Nonnull String path) {
      myPath = path;
    }

    @Override
    public void get(final long addr, final byte[] dst, final int off, final int len) throws IOException {
      FileAccessorCache.Handle<RandomAccessFileWithLengthAndSizeTracking> fileAccessor = ourRandomAccessFileCache.get(myPath);

      try {
        RandomAccessFileWithLengthAndSizeTracking file = fileAccessor.get();
        file.seek(addr);
        file.read(dst, off, len);
      }
      finally {
        fileAccessor.release();
      }
    }

    @Override
    public void dispose() {
    }
  }

  private static class FileReader implements RAReader {
    private final RandomAccessFile myFile;

    private FileReader(File file) {
      try {
        myFile = new RandomAccessFile(file, "r");
      }
      catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void get(final long addr, final byte[] dst, final int off, final int len) throws IOException {
      myFile.seek(addr);
      myFile.read(dst, off, len);
    }

    @Override
    public void dispose() {
      try {
        myFile.close();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class OutputStreamOverRandomAccessFileCache extends OutputStream {
    private final String myPath;

    OutputStreamOverRandomAccessFileCache(String path) {
      myPath = path;
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
      FileAccessorCache.Handle<RandomAccessFileWithLengthAndSizeTracking> fileAccessor = ourRandomAccessFileCache.get(myPath);
      RandomAccessFileWithLengthAndSizeTracking file = fileAccessor.get();

      try {
        file.seek(file.length());
        file.write(b, off, len);
      }
      finally {
        fileAccessor.release();
      }
    }

    @Override
    public void write(int b) throws IOException {
      byte[] r = {(byte)(b & 0xFF)};
      write(r);
    }
  }

  private class MyCompressedAppendableFile extends CompressedAppendableFile {
    MyCompressedAppendableFile() {
      super(myFile);
    }

    @Nonnull
    @Override
    protected InputStream getChunkInputStream(File appendFile, long offset, int pageSize) throws IOException {
      forceAppender(myPath);
      FileAccessorCache.Handle<RAReader> fileAccessor = ourReadersCache.get(myPath);

      try {
        byte[] bytes = new byte[pageSize];
        fileAccessor.get().get(offset, bytes, 0, pageSize);
        return new ByteArrayInputStream(bytes);
      }
      finally {
        fileAccessor.release();
      }
    }

    @Override
    protected void saveChunk(BufferExposingByteArrayOutputStream compressedChunk, long endOfFileOffset) throws IOException {
      FileAccessorCache.Handle<DataOutputStream> streamCacheValue = ourAppendersCache.get(myPath);
      try {
        streamCacheValue.get().write(compressedChunk.getInternalBuffer(), 0, compressedChunk.size());
      }
      finally {
        streamCacheValue.release();
      }

      streamCacheValue = ourAppendersCache.get(myPath + CompressedAppendableFile.INCOMPLETE_CHUNK_LENGTH_FILE_EXTENSION);
      try {
        DataInputOutputUtil.writeINT(streamCacheValue.get(), compressedChunk.size());
      }
      finally {
        streamCacheValue.release();
      }
    }

    @Nonnull
    @Override
    protected File getChunksFile() {
      return myFile;
    }

    @Override
    protected File getChunkLengthFile() {
      return new File(myFile.getPath() + CompressedAppendableFile.INCOMPLETE_CHUNK_LENGTH_FILE_EXTENSION);
    }

    @Override
    public synchronized void force() {
      super.force();
      forceAppender(myPath + CompressedAppendableFile.INCOMPLETE_CHUNK_LENGTH_FILE_EXTENSION);
    }

    @Override
    public synchronized void dispose() {
      super.dispose();

      ourAppendersCache.remove(myPath + CompressedAppendableFile.INCOMPLETE_CHUNK_LENGTH_FILE_EXTENSION);
      ourRandomAccessFileCache.remove(myPath + CompressedAppendableFile.INCOMPLETE_CHUNK_LENGTH_FILE_EXTENSION);
    }
  }

  @TestOnly
  public boolean isReadOnly() {
    return myOptions.myReadOnly;
  }
}
