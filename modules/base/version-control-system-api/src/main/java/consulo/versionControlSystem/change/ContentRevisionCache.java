// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import consulo.application.util.function.Throwable2Computable;
import consulo.project.Project;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ContentRevisionCache {
    private final Object myLock = new Object();
    private final Cache<Key, byte[]> myCache = CacheBuilder.newBuilder().maximumSize(100).softValues().build();

    private final Map<Key, byte[]> myConstantCache = new HashMap<>();

    private void put(@Nonnull FilePath path,
                     @Nonnull VcsRevisionNumber number,
                     @Nonnull VcsKey vcsKey,
                     @Nonnull UniqueType type,
                     @Nullable byte[] bytes) {
        if (bytes != null) {
            myCache.put(new Key(path, number, vcsKey, type), bytes);
        }
    }

    @Contract("!null, _, _ -> !null")
    public static @Nullable String getAsString(@Nullable byte[] bytes, @Nonnull FilePath file, @Nullable Charset charset) {
        if (bytes == null) {
            return null;
        }
        if (charset == null) {
            return bytesToString(file, bytes);
        }
        else {
            return CharsetToolkit.bytesToString(bytes, charset);
        }
    }

    private static @Nonnull String bytesToString(FilePath path, @Nonnull byte [] bytes) {
        Charset charset = null;
        if (path.getVirtualFile() != null) {
            charset = path.getVirtualFile().getCharset();
        }

        if (charset != null) {
            int bomLength = CharsetToolkit.getBOMLength(bytes, charset);
            final CharBuffer charBuffer = charset.decode(ByteBuffer.wrap(bytes, bomLength, bytes.length - bomLength));
            return charBuffer.toString();
        }

        return CharsetToolkit.bytesToString(bytes, EncodingRegistry.getInstance().getDefaultCharset());
    }

    @Nullable
    public byte[] getBytes(FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey, @Nonnull UniqueType type) {
        return myCache.getIfPresent(new Key(path, number, vcsKey, type));
    }

    @Nonnull
    public static byte[] loadAsBytes(@Nonnull FilePath path,
                                     Throwable2Computable<byte[], ? extends VcsException, ? extends IOException> loader)
        throws VcsException, IOException {
        checkLocalFileSize(path);
        return loader.compute();
    }

    @Nonnull
    public static byte[] getOrLoadAsBytes(@Nonnull Project project,
                                          @Nonnull FilePath path,
                                          @Nonnull VcsRevisionNumber number,
                                          @Nonnull VcsKey vcsKey,
                                          @Nonnull UniqueType type,
                                          @Nonnull Throwable2Computable<byte [], ? extends VcsException, ? extends IOException> loader)
        throws VcsException, IOException {
        ContentRevisionCache cache = ProjectLevelVcsManager.getInstance(project).getContentRevisionCache();
        byte[] bytes = cache.getBytes(path, number, vcsKey, type);
        if (bytes != null) {
            return bytes;
        }
        bytes = cache.getFromConstantCache(path, number, vcsKey, type);
        if (bytes != null) {
            return bytes;
        }

        checkLocalFileSize(path);
        bytes = loader.compute();
        cache.put(path, number, vcsKey, type, bytes);
        return bytes;
    }

    private static void checkLocalFileSize(@Nonnull FilePath path) throws VcsException {
        File ioFile = path.getIOFile();
        if (ioFile.exists()) {
            checkContentsSize(ioFile.getPath(), ioFile.length());
        }
    }

    public static void checkContentsSize(final String path, final long size) throws VcsException {
        if (size > VcsUtil.getMaxVcsLoadedFileSize()) {
            throw new VcsException(VcsBundle.message("file.content.too.big.to.load.increase.property.suggestion", path,
                StringUtil.formatFileSize(VcsUtil.getMaxVcsLoadedFileSize()),
                VcsUtil.MAX_VCS_LOADED_SIZE_KB));
        }
    }

    public void putIntoConstantCache(@Nonnull FilePath path,
                                     @Nonnull VcsRevisionNumber revisionNumber,
                                     @Nonnull VcsKey vcsKey,
                                     byte[] content) {
        synchronized (myConstantCache) {
            myConstantCache.put(new Key(path, revisionNumber, vcsKey, UniqueType.REPOSITORY_CONTENT), content);
        }
    }

    public byte[] getFromConstantCache(@Nonnull FilePath path,
                                       @Nonnull VcsRevisionNumber revisionNumber,
                                       @Nonnull VcsKey vcsKey,
                                       @Nonnull UniqueType type) {
        synchronized (myConstantCache) {
            return myConstantCache.get(new Key(path, revisionNumber, vcsKey, type));
        }
    }

    public void clearConstantCache() {
        myConstantCache.clear();
    }

    public static Pair<VcsRevisionNumber, byte[]> getOrLoadCurrentAsBytes(@Nonnull Project project,
                                                                          @Nonnull FilePath path,
                                                                          @Nonnull VcsKey vcsKey,
                                                                          @Nonnull CurrentRevisionProvider loader)
        throws VcsException, IOException {
        ContentRevisionCache cache = ProjectLevelVcsManager.getInstance(project).getContentRevisionCache();

        while (true) {
            VcsRevisionNumber currentRevision = loader.getCurrentRevision();

            final byte[] cachedCurrent = cache.getBytes(path, currentRevision, vcsKey, UniqueType.REPOSITORY_CONTENT);
            if (cachedCurrent != null) {
                return Pair.create(currentRevision, cachedCurrent);
            }

            checkLocalFileSize(path);
            Pair<VcsRevisionNumber, byte[]> loaded = loader.get();

            if (loaded.getFirst().equals(currentRevision)) {
                cache.put(path, currentRevision, vcsKey, UniqueType.REPOSITORY_CONTENT, loaded.getSecond());
                return loaded;
            }
        }
    }

    private static class CurrentKey {
        protected final FilePath myPath;
        protected final VcsKey myVcsKey;

        private CurrentKey(FilePath path, VcsKey vcsKey) {
            myPath = path;
            myVcsKey = vcsKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CurrentKey that = (CurrentKey) o;
            return Objects.equals(myPath, that.myPath) && Objects.equals(myVcsKey, that.myVcsKey);
        }

        @Override
        public int hashCode() {
            int result = myPath != null ? myPath.hashCode() : 0;
            result = 31 * result + (myVcsKey != null ? myVcsKey.hashCode() : 0);
            return result;
        }
    }

    private static final class Key extends CurrentKey {
        private final VcsRevisionNumber myNumber;
        private final UniqueType myType;

        private Key(FilePath path, VcsRevisionNumber number, VcsKey vcsKey, UniqueType type) {
            super(path, vcsKey);
            myNumber = number;
            myType = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Key key = (Key) o;
            return Objects.equals(myNumber, key.myNumber) && myPath.equals(key.myPath) && myType == key.myType && myVcsKey.equals(key.myVcsKey);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + myPath.hashCode();
            result = 31 * result + (myNumber == null ? 0 : myNumber.hashCode());
            result = 31 * result + myVcsKey.hashCode();
            result = 31 * result + myType.hashCode();
            return result;
        }
    }

    public enum UniqueType {
        REPOSITORY_CONTENT,
        REMOTE_CONTENT
    }

    public void clearAll() {
        myCache.invalidateAll();
        synchronized (myLock) {
            myConstantCache.clear();
        }
    }
}
