// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.io;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.io.SafeOutputStream;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * <p>Attempts to prevent data loss if OS crash happens during write.</p>
 *
 * <p>The class creates a backup copy before overwriting the target file, and issues {@code fsync()} afterwards. The behavior is based
 * on an assumption that after a crash either a target remains unmodified (i.e. unfinished write doesn't reach the disc),
 * or a backup file exists along with a partially overwritten target file.</p>
 *
 * <p><b>The class is not thread-safe</b>; expected to be used within try-with-resources or an equivalent statement.</p>
 */
public class SafeFileOutputStreamImpl extends SafeOutputStream {
    private static final String DEFAULT_BACKUP_EXT = "~";
    private static final CopyOption[] BACKUP_COPY = {StandardCopyOption.REPLACE_EXISTING};
    private static final OpenOption[] MAIN_WRITE = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC};
    private static final OpenOption[] BACKUP_READ = {StandardOpenOption.DELETE_ON_CLOSE};

    private final Path myTarget;
    private final String myBackupName;
    private final
    @Nullable
    Future<Path> myBackupFuture;
    private final BufferExposingByteArrayOutputStream myBuffer;
    private boolean myClosed = false;

    public SafeFileOutputStreamImpl(@Nonnull ApplicationConcurrency concurrency, @Nonnull Path target) {
        this(concurrency, target, DEFAULT_BACKUP_EXT);
    }

    public SafeFileOutputStreamImpl(@Nonnull ApplicationConcurrency concurrency, @Nonnull Path target, @Nonnull String backupExt) {
        myTarget = target;
        myBackupName = myTarget.getFileName() + backupExt;
        myBackupFuture = !Files.exists(target) ? null : concurrency.getExecutorService().submit(() -> {
            Path parent = myTarget.getParent();
            Path backup = parent != null ? parent.resolve(myBackupName) : Paths.get(myBackupName);
            Files.copy(myTarget, backup, BACKUP_COPY);
            return backup;
        });
        myBuffer = new BufferExposingByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        myBuffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        myBuffer.write(b, off, len);
    }

    public void abort() throws IOException {
        myClosed = true;
        deleteBackup(waitForBackup());
    }

    @Override
    public void close() throws IOException {
        if (myClosed) {
            return;
        }
        myClosed = true;

        @Nullable Path backup = waitForBackup();
        OutputStream sink = openFile();
        try {
            writeData(sink);
            deleteBackup(backup);
        }
        catch (IOException e) {
            restoreFromBackup(backup, e);
        }
    }

    @Nullable
    private Path waitForBackup() throws IOException {
        if (myBackupFuture == null) {
            return null;
        }
        try {
            return myBackupFuture.get();
        }
        catch (InterruptedException | CancellationException e) {
            throw new IllegalStateException(e);
        }
        catch (ExecutionException e) {
            throw new IOException(CommonLocalize.safeWriteBackup(myTarget, myBackupName).get(), e.getCause());
        }
    }

    private OutputStream openFile() throws IOException {
        try {
            return Files.newOutputStream(myTarget, MAIN_WRITE);
        }
        catch (IOException e) {
            throw new IOException(CommonLocalize.safeWriteOpen(myTarget).get(), e);
        }
    }

    private void writeData(OutputStream sink) throws IOException {
        try (OutputStream out = sink) {
            out.write(myBuffer.getInternalBuffer(), 0, myBuffer.size());
        }
    }

    private static void deleteBackup(Path backup) {
        if (backup != null) {
            try {
                Files.delete(backup);
            }
            catch (IOException e) {
                Logger.getInstance(SafeFileOutputStreamImpl.class).warn("cannot delete a backup file " + backup, e);
            }
        }
    }

    private void restoreFromBackup(@Nullable Path backup, IOException e) throws IOException {
        if (backup == null) {
            throw new IOException(CommonLocalize.safeWriteJunk(myTarget).get(), e);
        }

        boolean restored = true;
        try (InputStream in = Files.newInputStream(backup, BACKUP_READ); OutputStream out = Files.newOutputStream(myTarget, MAIN_WRITE)) {
            FileUtil.copy(in, out);
        }
        catch (IOException ex) {
            restored = false;
            e.addSuppressed(ex);
        }
        if (restored) {
            throw new IOException(CommonLocalize.safeWriteRestored(myTarget).get(), e);
        }
        else {
            throw new IOException(CommonLocalize.safeWriteJunkBackup(myTarget, backup.getFileName()).get(), e);
        }
    }
}