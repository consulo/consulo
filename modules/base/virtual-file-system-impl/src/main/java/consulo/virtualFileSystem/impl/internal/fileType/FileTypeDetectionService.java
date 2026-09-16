// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.fileType;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.collection.ConcurrentPackedBitsArray;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.ByteArraySequence;
import consulo.util.io.ByteSequence;
import consulo.util.io.FileUtil;
import consulo.util.lang.BitUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.FileSystemInterface;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeDetector;
import consulo.virtualFileSystem.fileType.PlainTextLikeFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

/**
 * Content-based file type detection, with its persistent and in-memory caches and its re-detection queue. Detection
 * only ever needs the bytes of a file, so it lives below the language and scheme layers; the file type manager supplies
 * the three pieces that do need them through {@link #getDefaultTextFileType()},
 * {@link #getFileTypeByFileWithoutContent(VirtualFile)} and
 * {@link #onDetectedFileTypesChanged(List, List)}.
 */
public abstract class FileTypeDetectionService {
    private static final Logger LOG = Logger.getInstance(FileTypeDetectionService.class);

    // cached auto-detected file type. If the file was auto-detected as plain text or binary
    // then the value is null and AUTO_DETECTED_* flags stored in packedFlags are used instead.
    private static final Key<FileType> DETECTED_FROM_CONTENT_FILE_TYPE_KEY = Key.create("DETECTED_FROM_CONTENT_FILE_TYPE_KEY");

    // these flags are stored in 'packedFlags' as chunks of four bits
    private static final byte AUTO_DETECTED_AS_TEXT_MASK = 1;        // set if the file was auto-detected as text
    private static final byte AUTO_DETECTED_AS_BINARY_MASK = 1 << 1;   // set if the file was auto-detected as binary

    // set if auto-detection was performed for this file.
    // if some detector returned some custom file type, it's stored in DETECTED_FROM_CONTENT_FILE_TYPE_KEY file key.
    // otherwise if auto-detected as text or binary, the result is stored in AUTO_DETECTED_AS_TEXT_MASK|AUTO_DETECTED_AS_BINARY_MASK bits
    private static final byte AUTO_DETECT_WAS_RUN_MASK = 1 << 2;
    // set if AUTO_* bits above were loaded from the file persistent attributes and saved to packedFlags
    private static final byte ATTRIBUTES_WERE_LOADED_MASK = 1 << 3;

    private static final int CHUNK_SIZE = 10;

    private final ConcurrentPackedBitsArray packedFlags = new ConcurrentPackedBitsArray(4);

    private final AtomicInteger counterAutoDetect = new AtomicInteger();
    private final AtomicLong elapsedAutoDetect = new AtomicLong();

    private final HashSetQueue<VirtualFile> filesToRedetect = new HashSetQueue<>();
    private final ExecutorService reDetectExecutor;
    private final boolean myReDetectAsync;

    private volatile FileAttribute autoDetectedAttribute;

    boolean toLog;

    protected FileTypeDetectionService(Application application, int fileTypeChangedCounter, Disposable parentDisposable) {
        autoDetectedAttribute =
            new FileAttribute("AUTO_DETECTION_CACHE_ATTRIBUTE", fileTypeChangedCounter + getVersionFromDetectors(), true);
        myReDetectAsync = !application.isUnitTestMode();
        reDetectExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "FileTypeManager Redetect Pool",
            PooledThreadExecutor.getInstance(),
            1,
            parentDisposable
        );
    }

    /**
     * The type a file whose content turned out to be readable text gets when no detector claimed it.
     */
    protected abstract FileType getDefaultTextFileType();

    /**
     * The file type resolved from everything but the content, or null when only the content can tell.
     */
    protected abstract @Nullable FileType getFileTypeByFileWithoutContent(VirtualFile file);

    /**
     * Files whose detected type changed since it was last cached, and files whose content could not be read.
     */
    protected abstract void onDetectedFileTypesChanged(List<VirtualFile> changed, List<VirtualFile> crashed);

    public static boolean mightBeReplacedByDetectedFileType(FileType fileType) {
        return fileType instanceof PlainTextLikeFileType && fileType.isReadOnly();
    }

    public static int getVersionFromDetectors() {
        int version = 0;
        for (FileTypeDetector detector : FileTypeDetector.EP_NAME.getExtensionList()) {
            version += detector.getVersion();
        }
        return version;
    }

    public Executor getReDetectExecutor() {
        return reDetectExecutor;
    }

    public void setFileTypeChangedCounter(int counter) {
        autoDetectedAttribute = autoDetectedAttribute.newVersion(counter);
        if (toLog()) {
            log("F: setFileTypeChangedCounter(" + counter + ")");
        }
    }

    public void clearCaches() {
        packedFlags.clear();
        if (toLog()) {
            log("F: clearCaches()");
        }
    }

    public void logStatistics() {
        LOG.info(String.format("%s auto-detected files. Detection took %s ms", counterAutoDetect, elapsedAutoDetect));
    }

    public void queueForReDetect(List<? extends VFileEvent> events) {
        Collection<VirtualFile> files = ContainerUtil.map2Set(events, event -> {
            VirtualFile file = event instanceof VFileCreateEvent ? /* avoid expensive find child here */ null : event.getFile();
            VirtualFile filtered = file != null && wasAutoDetectedBefore(file) && isDetectable(file) ? file : null;
            if (toLog()) {
                log("F: after() VFS event " +
                    event +
                    "; filtered file: " +
                    filtered +
                    " (file: " +
                    file +
                    "; wasAutoDetectedBefore(file): " +
                    (file == null ? null : wasAutoDetectedBefore(file)) +
                    "; isDetectable(file): " +
                    (file == null ? null : isDetectable(file)) +
                    "; file.getLength(): " +
                    (file == null ? null : file.getLength()) +
                    "; file.isValid(): " +
                    (file == null ? null : file.isValid()) +
                    "; file.is(VFileProperty.SPECIAL): " +
                    (file == null ? null : file.is(VFileProperty.SPECIAL)) +
                    "; packedFlags.get(id): " +
                    (file instanceof VirtualFileWithId virtualFileWithId
                        ? readableFlags(packedFlags.get(virtualFileWithId.getId())) : null) +
                    "; file.getFileSystem():" +
                    (file == null ? null : file.getFileSystem()) +
                    ")");
            }
            return filtered;
        });
        files.remove(null);
        if (toLog()) {
            log("F: after() VFS events: " + events + "; files: " + files);
        }
        if (!files.isEmpty() && myReDetectAsync) {
            if (toLog()) {
                log("F: after() queued to redetect: " + files);
            }

            synchronized (filesToRedetect) {
                if (filesToRedetect.addAll(files)) {
                    awakeReDetectExecutor();
                }
            }
        }
    }

    private void awakeReDetectExecutor() {
        reDetectExecutor.execute(() -> {
            List<VirtualFile> files = new ArrayList<>(CHUNK_SIZE);
            synchronized (filesToRedetect) {
                for (int i = 0; i < CHUNK_SIZE; i++) {
                    VirtualFile file = filesToRedetect.poll();
                    if (file == null) {
                        break;
                    }
                    files.add(file);
                }
            }
            if (files.size() == CHUNK_SIZE) {
                awakeReDetectExecutor();
            }
            reDetect(files);
        });
    }

    private void reDetect(Collection<? extends VirtualFile> files) {
        List<VirtualFile> changed = new ArrayList<>();
        List<VirtualFile> crashed = new ArrayList<>();
        for (VirtualFile file : files) {
            boolean shouldRedetect = wasAutoDetectedBefore(file) && isDetectable(file);
            if (toLog()) {
                log("F: reDetect(" + file.getName() + ") " + file.getName() + "; shouldRedetect: " + shouldRedetect);
            }
            if (shouldRedetect) {
                int id = ((VirtualFileWithId)file).getId();
                long flags = packedFlags.get(id);
                FileType before = ObjectUtil.notNull(
                    textOrBinaryFromCachedFlags(flags),
                    ObjectUtil.notNull(file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY), getDefaultTextFileType())
                );
                FileType after = getFileTypeByFileWithoutContent(file);

                if (toLog()) {
                    log("F: reDetect(" +
                        file.getName() +
                        ") prepare to redetect. flags: " +
                        readableFlags(flags) +
                        "; beforeType: " +
                        before.getId() +
                        "; afterByFileType: " +
                        (after == null ? null : after.getId()));
                }

                if (after == null || mightBeReplacedByDetectedFileType(after)) {
                    try {
                        after = detectFromContentAndCache(file, null);
                    }
                    catch (IOException e) {
                        crashed.add(file);
                        if (toLog()) {
                            log("F: reDetect(" +
                                file.getName() +
                                ") " +
                                "before: " +
                                before.getId() +
                                "; after: crashed with " +
                                e.getMessage() +
                                "; now getFileType()=" +
                                file.getFileType().getId() +
                                "; getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY): " +
                                file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY));
                        }
                        continue;
                    }
                }
                else {
                    // back to standard file type
                    // detected by conventional methods, no need to run detect-from-content
                    file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, null);
                    flags = 0;
                    packedFlags.set(id, flags);
                }
                if (toLog()) {
                    log("F: reDetect(" +
                        file.getName() +
                        ") " +
                        "before: " +
                        before.getId() +
                        "; after: " +
                        after.getId() +
                        "; now getFileType()=" +
                        file.getFileType().getId() +
                        "; getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY): " +
                        file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY));
                }

                if (before != after) {
                    changed.add(file);
                }
            }
        }

        if (!changed.isEmpty() || !crashed.isEmpty()) {
            onDetectedFileTypesChanged(changed, crashed);
        }
    }

    private boolean wasAutoDetectedBefore(VirtualFile file) {
        if (file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY) != null) {
            return true;
        }
        if (file instanceof VirtualFileWithId virtualFileWithId) {
            int id = virtualFileWithId.getId();
            // do not re-detect binary files
            return (packedFlags.get(id) & (AUTO_DETECT_WAS_RUN_MASK | AUTO_DETECTED_AS_BINARY_MASK)) == AUTO_DETECT_WAS_RUN_MASK;
        }
        return false;
    }

    public FileType getOrDetectFromContent(VirtualFile file, byte @Nullable [] content) {
        if (!isDetectable(file)) {
            return UnknownFileType.INSTANCE;
        }
        if (file instanceof VirtualFileWithId virtualFileWithId) {
            int id = virtualFileWithId.getId();

            long flags = packedFlags.get(id);
            if (!BitUtil.isSet(flags, ATTRIBUTES_WERE_LOADED_MASK)) {
                flags = readFlagsFromCache(file);
                flags = BitUtil.set(flags, ATTRIBUTES_WERE_LOADED_MASK, true);

                packedFlags.set(id, flags);
                if (toLog()) {
                    log("F: getOrDetectFromContent(" + file.getName() + "): readFlagsFromCache() = " + readableFlags(flags));
                }
            }
            boolean autoDetectWasRun = BitUtil.isSet(flags, AUTO_DETECT_WAS_RUN_MASK);
            if (autoDetectWasRun) {
                FileType type = textOrBinaryFromCachedFlags(flags);
                if (toLog()) {
                    log("F: getOrDetectFromContent(" + file.getName() + "):" +
                        " cached type = " + (type == null ? null : type.getId()) +
                        "; packedFlags.get(id):" + readableFlags(flags) +
                        "; getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY): " +
                        file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY));
                }
                if (type != null) {
                    return type;
                }
            }
        }
        FileType fileType = file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY);
        if (toLog()) {
            log(
                "F: getOrDetectFromContent(" + file.getName() + "): " +
                    "getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY) = " + (fileType == null ? null : fileType.getId())
            );
        }
        if (fileType == null) {
            // run autodetection
            try {
                fileType = detectFromContentAndCache(file, content);
            }
            catch (IOException e) {
                fileType = UnknownFileType.INSTANCE;
            }
        }

        if (toLog()) {
            log("F: getOrDetectFromContent(" + file.getName() + "): getFileType after detect run = " + fileType.getId());
        }

        return fileType;
    }

    // read auto-detection flags from the persistent FS file attributes. If file attributes are absent, return 0 for flags
    // returns three bits value for AUTO_DETECTED_AS_TEXT_MASK, AUTO_DETECTED_AS_BINARY_MASK and AUTO_DETECT_WAS_RUN_MASK bits
    protected byte readFlagsFromCache(VirtualFile file) {
        boolean wasAutoDetectRun = false;
        byte status = 0;
        try (DataInputStream stream = autoDetectedAttribute.readAttribute(file)) {
            status = stream == null ? 0 : stream.readByte();
            wasAutoDetectRun = stream != null;
        }
        catch (IOException ignored) {

        }
        status = BitUtil.set(status, AUTO_DETECT_WAS_RUN_MASK, wasAutoDetectRun);

        return (byte)(status & (AUTO_DETECTED_AS_TEXT_MASK | AUTO_DETECTED_AS_BINARY_MASK | AUTO_DETECT_WAS_RUN_MASK));
    }

    // store auto-detection flags to the persistent FS file attributes
    // writes AUTO_DETECTED_AS_TEXT_MASK, AUTO_DETECTED_AS_BINARY_MASK bits only
    protected void writeFlagsToCache(VirtualFile file, int flags) {
        try (DataOutputStream stream = autoDetectedAttribute.writeAttribute(file)) {
            stream.writeByte(flags & (AUTO_DETECTED_AS_TEXT_MASK | AUTO_DETECTED_AS_BINARY_MASK));
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    private @Nullable FileType textOrBinaryFromCachedFlags(long flags) {
        return BitUtil.isSet(flags, AUTO_DETECTED_AS_TEXT_MASK)
            ? getDefaultTextFileType()
            : BitUtil.isSet(flags, AUTO_DETECTED_AS_BINARY_MASK)
            ? UnknownFileType.INSTANCE
            : null;
    }

    private void cacheAutoDetectedFileType(VirtualFile file, FileType fileType) {
        boolean wasAutodetectedAsText = fileType == getDefaultTextFileType();
        boolean wasAutodetectedAsBinary = fileType == UnknownFileType.INSTANCE;

        int flags = BitUtil.set(0, AUTO_DETECTED_AS_TEXT_MASK, wasAutodetectedAsText);
        flags = BitUtil.set(flags, AUTO_DETECTED_AS_BINARY_MASK, wasAutodetectedAsBinary);
        writeFlagsToCache(file, flags);
        if (file instanceof VirtualFileWithId virtualFileWithId) {
            int id = virtualFileWithId.getId();
            flags = BitUtil.set(flags, AUTO_DETECT_WAS_RUN_MASK, true);
            flags = BitUtil.set(flags, ATTRIBUTES_WERE_LOADED_MASK, true);
            packedFlags.set(id, flags);

            if (wasAutodetectedAsText || wasAutodetectedAsBinary) {
                file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, null);
                if (toLog()) {
                    log(
                        "F: cacheAutoDetectedFileType(" + file.getName() + ") " +
                            "cached to " + fileType.getId() +
                            " flags = " + readableFlags(flags) +
                            "; getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY): " + file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY)
                    );
                }
                return;
            }
        }
        file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, fileType);
        if (toLog()) {
            log(
                "F: cacheAutoDetectedFileType(" + file.getName() + ") " +
                    "cached to " + fileType.getId() +
                    " flags = " + readableFlags(flags) +
                    "; getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY): " + file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY)
            );
        }
    }

    public static boolean isDetectable(VirtualFile file) {
        return !(file.isDirectory() || !file.isValid() || file.is(VFileProperty.SPECIAL)
            || file.getLength() == 0) && file.getFileSystem() instanceof FileSystemInterface;
    }

    private int readSafely(InputStream stream, byte[] buffer, int offset, int length) throws IOException {
        int n = stream.read(buffer, offset, length);
        if (n <= 0) {
            // maybe locked because someone else is writing to it
            // repeat inside read action to guarantee all writes are finished
            if (toLog()) {
                log("F: processFirstBytes(): inputStream.read() returned " + n + "; retrying with read action. stream=" + streamInfo(stream));
            }
            n = ReadAction.compute(() -> stream.read(buffer, offset, length));
            if (toLog()) {
                log("F: processFirstBytes(): under read action inputStream.read() returned " + n + "; stream=" + streamInfo(stream));
            }
        }
        return n;
    }

    private FileType detectFromContentAndCache(VirtualFile file, byte @Nullable [] content) throws IOException {
        long start = System.currentTimeMillis();
        FileType fileType = detectFromContent(file, content, FileTypeDetector.EP_NAME.getExtensionList());

        cacheAutoDetectedFileType(file, fileType);
        counterAutoDetect.incrementAndGet();
        long elapsed = System.currentTimeMillis() - start;
        elapsedAutoDetect.addAndGet(elapsed);

        return fileType;
    }

    private FileType detectFromContent(
        VirtualFile file,
        byte @Nullable [] content,
        Iterable<? extends FileTypeDetector> detectors
    ) throws IOException {
        FileType fileType;
        if (content != null) {
            fileType = detect(file, content, content.length, detectors);
        }
        else {
            try (InputStream inputStream = ((FileSystemInterface)file.getFileSystem()).getInputStream(file)) {
                if (toLog()) {
                    log("F: detectFromContentAndCache(" + file.getName() + "):" + " inputStream=" + streamInfo(inputStream));
                }

                int fileLength = (int)file.getLength();

                int bufferLength = StreamSupport.stream(detectors.spliterator(), false)
                    .map(FileTypeDetector::getDesiredContentPrefixLength)
                    .max(Comparator.naturalOrder())
                    .orElse(RawFileLoader.getInstance().getMaxIntellisenseFileSize());
                byte[] buffer = fileLength <= FileUtil.THREAD_LOCAL_BUFFER_LENGTH
                    ? FileUtil.getThreadLocalBuffer() : new byte[Math.min(fileLength, bufferLength)];

                int n = readSafely(inputStream, buffer, 0, buffer.length);
                fileType = detect(file, buffer, n, detectors);

                if (toLog()) {
                    try (InputStream newStream = ((FileSystemInterface)file.getFileSystem()).getInputStream(file)) {
                        byte[] buffer2 = new byte[50];
                        int n2 = newStream.read(buffer2, 0, buffer2.length);
                        log(
                            "F: detectFromContentAndCache(" + file.getName() +
                                "): result: " + fileType.getId() +
                                "; stream: " + streamInfo(inputStream) +
                                "; newStream: " + streamInfo(newStream) +
                                "; read: " + n2 +
                                "; buffer: " + Arrays.toString(buffer2)
                        );
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(file + "; type=" + fileType.getId() + "; " + counterAutoDetect);
        }
        return fileType;
    }

    private FileType detect(
        VirtualFile file,
        byte[] bytes,
        int length,
        Iterable<? extends FileTypeDetector> detectors
    ) {
        if (length <= 0) {
            return UnknownFileType.INSTANCE;
        }

        // use the default text file type because it doesn't supply its own charset detector
        // help set charset in the process to avoid double charset detection from content
        return LoadTextUtil.processTextFromBinaryPresentationOrNull(
            bytes,
            length,
            file,
            true,
            true,
            getDefaultTextFileType(),
            (@Nullable CharSequence text) -> {
                if (toLog()) {
                    log("F: detectFromContentAndCache.processFirstBytes(" +
                        file.getName() +
                        "): bytes length=" +
                        length +
                        "; isText=" +
                        (text != null) +
                        "; text='" +
                        (text == null ? null : StringUtil.first(text, 100, true)) +
                        "'" +
                        ", detectors=" +
                        detectors);
                }
                FileType detected = null;
                ByteSequence firstBytes = new ByteArraySequence(bytes, 0, length);
                for (FileTypeDetector detector : detectors) {
                    try {
                        detected = detector.detect(file, firstBytes, text);
                    }
                    catch (Exception e) {
                        LOG.error("Detector " + detector + " (" + detector.getClass() + ") exception occurred:", e);
                    }
                    if (detected != null) {
                        if (toLog()) {
                            log(
                                "F: detectFromContentAndCache.processFirstBytes(" + file.getName() + "):" +
                                    " detector " + detector +
                                    " type as " + detected.getId()
                            );
                        }
                        break;
                    }
                }

                if (detected == null) {
                    detected = text == null ? UnknownFileType.INSTANCE : getDefaultTextFileType();
                    if (toLog()) {
                        log(
                            "F: detectFromContentAndCache.processFirstBytes(" + file.getName() + "): " +
                                "no detector was able to detect. assigned " + detected.getId()
                        );
                    }
                }
                return detected;
            }
        );
    }

    protected boolean toLog() {
        return toLog;
    }

    private static void log(String message) {
        LOG.debug(message + " - " + Thread.currentThread());
    }

    private static String readableFlags(long flags) {
        String result = "";
        if (BitUtil.isSet(flags, ATTRIBUTES_WERE_LOADED_MASK)) {
            result += (result.isEmpty() ? "" : " | ") + "ATTRIBUTES_WERE_LOADED_MASK";
        }
        if (BitUtil.isSet(flags, AUTO_DETECT_WAS_RUN_MASK)) {
            result += (result.isEmpty() ? "" : " | ") + "AUTO_DETECT_WAS_RUN_MASK";
        }
        if (BitUtil.isSet(flags, AUTO_DETECTED_AS_BINARY_MASK)) {
            result += (result.isEmpty() ? "" : " | ") + "AUTO_DETECTED_AS_BINARY_MASK";
        }
        if (BitUtil.isSet(flags, AUTO_DETECTED_AS_TEXT_MASK)) {
            result += (result.isEmpty() ? "" : " | ") + "AUTO_DETECTED_AS_TEXT_MASK";
        }
        return result;
    }

    // for diagnostics
    @SuppressWarnings("ConstantConditions")
    private static Object streamInfo(InputStream stream) throws IOException {
        if (stream instanceof BufferedInputStream) {
            InputStream in = ReflectionUtil.getField(stream.getClass(), stream, InputStream.class, "in");
            byte[] buf = ReflectionUtil.getField(stream.getClass(), stream, byte[].class, "buf");
            int count = ReflectionUtil.getField(stream.getClass(), stream, int.class, "count");
            int pos = ReflectionUtil.getField(stream.getClass(), stream, int.class, "pos");
            return "BufferedInputStream(buf=" + (buf == null ? null : Arrays.toString(Arrays.copyOf(buf, count))) +
                ", count=" + count +
                ", pos=" + pos +
                ", in=" + streamInfo(in) + ")";
        }
        if (stream instanceof FileInputStream) {
            String path = ReflectionUtil.getField(stream.getClass(), stream, String.class, "path");
            FileChannel channel = ReflectionUtil.getField(stream.getClass(), stream, FileChannel.class, "channel");
            boolean closed = ReflectionUtil.getField(stream.getClass(), stream, boolean.class, "closed");
            int available = stream.available();
            File file = new File(path);
            return "FileInputStream(path=" + path +
                ", available=" + available +
                ", closed=" + closed +
                ", channel=" + channel +
                ", channel.size=" + (channel == null ? null : channel.size()) +
                ", file.exists=" + file.exists() +
                ", file.content='" + FileUtil.loadFile(file) + "')";
        }
        return stream;
    }
}
