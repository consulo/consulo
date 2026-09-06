// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import jakarta.inject.Singleton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This service collects all the indexing dependencies (e.g. plugins, project and app configuration, whatnot)
 * and builds a fingerprint of those dependencies. Fingerprint is a number. Change of the fingerprint means that
 * some files might be indexed differently now (provided that there are no VFS nor project model changes).
 * <p>
 * In general, indexing depends on:
 * <ol>
 * <li>Extensions. They are provided by plugins. No changes in enabled plugins means no changes in enabled extensions.</li>
 * <li>Configuration. Extensions in plugin may depend on configuration. In the case of configuration change plugins must
 *    request (full) scanning (e.g. change in project language level). Full rescan counter also contributes to the fingerprint.
 *    It is not clear at the moment if partial rescans should contribute or not.</li>
 * <li>VFS. VFS has its own means to track changed files. VFS is not in the scope of the fingerprint. We assume that each time
 *    VFS file changes, its {@code modificationCounter} changes as well. This counter is considered by {@link FileIndexingStamp},
 *    so changed file has modified file indexing stamp even though indexing dependencies fingerprint stays the same. We might
 *    include VFS creation stamp into the fingerprint, but we don't do it at the moment. Invalidation of VFS creation stamp is
 *    currently possible due to VFS re-create. In this case all the {@link FileIndexingStamp} are reset to default
 *    {@code UNINDEXED} state. (see "invalidate cache" notes for {@link ProjectIndexingDependenciesService})</li>
 * <li>Project model. Platform indexes tracks project model state by, and request full or partial scanning if project model changes.</li>
 * <li>Shared indexes. Shared indexes track their state by themself, and should request full scanning if previously indexed files
 *    might become invalid. Specifically, on project open shared indexes tiy to re-attach all the chunks from previous session,
 *    and request full scanning if they could not attach some of them.</li>
 * <li>JVM options and registry. They are kind of settings, but they do not trigger any rescanning - they usually require
 *    APP restart. TODO</li>
 * <li>Did I miss something? Please contact me (e.g. by filing a YouTrack ticket).</li>
 * </ol>
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class IndexingDependenciesFingerprint {
    public record FingerprintImpl(long fingerprint) {
        static FingerprintImpl of(ByteBuffer buffer) {
            return new FingerprintImpl(buffer.rewind().order(ByteOrder.LITTLE_ENDIAN).getLong());
        }

        public ByteBuffer toByteBuffer() {
            // still 32 bytes even though we need only 8 to avoid index storage invalidation
            ByteBuffer buffer = ByteBuffer.allocate(FINGERPRINT_SIZE_IN_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(fingerprint);
            buffer.rewind();
            return buffer;
        }
    }

    public static final int FINGERPRINT_SIZE_IN_BYTES = 32;

    public static final FingerprintImpl NULL_FINGERPRINT = new FingerprintImpl(0);

    public static IndexingDependenciesFingerprint getInstance() {
        return Application.get().getInstance(IndexingDependenciesFingerprint.class);
    }

    private final AtomicReference<FingerprintImpl> myLatestFingerprint = new AtomicReference<>(NULL_FINGERPRINT);

    private int myDebugHelperToken = 0;

    private FingerprintImpl calculateFingerprint() {
        return new FingerprintImpl(IdeFingerprint.ideFingerprint(myDebugHelperToken).asLong());
    }

    public FingerprintImpl getFingerprint() {
        if (myLatestFingerprint.get().equals(NULL_FINGERPRINT)) {
            myLatestFingerprint.compareAndSet(NULL_FINGERPRINT, calculateFingerprint());
        }
        return myLatestFingerprint.get();
    }

    public void resetCache() {
        myLatestFingerprint.set(NULL_FINGERPRINT);
    }

    public void changeFingerprintInTest() {
        myDebugHelperToken++;
        myLatestFingerprint.set(NULL_FINGERPRINT);
    }
}
