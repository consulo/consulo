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
package consulo.ide.impl.idea.util.indexing;

import consulo.index.io.ContentHashesUtil;
import consulo.index.io.data.IOUtil;
import consulo.util.lang.ShutDownTracker;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.FlushingDaemon;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * @author Maxim.Mossienko
 * @since 4/10/2014.
 */
class ContentHashesSupport {
    private static volatile ContentHashesUtil.HashEnumerator ourHashesWithFileType;

    static void initContentHashesEnumerator() throws IOException {
        if (ourHashesWithFileType != null) {
            return;
        }
        synchronized (ContentHashesSupport.class) {
            if (ourHashesWithFileType != null) {
                return;
            }
            File hashEnumeratorFile = new File(IndexInfrastructure.getPersistentIndexRoot(), "hashesWithFileType");
            try {
                ContentHashesUtil.HashEnumerator hashEnumerator = new ContentHashesUtil.HashEnumerator(hashEnumeratorFile, null);
                FlushingDaemon.everyFiveSeconds(ContentHashesSupport::flushContentHashes);
                ShutDownTracker.getInstance().registerShutdownTask(ContentHashesSupport::flushContentHashes);
                ourHashesWithFileType = hashEnumerator;
            }
            catch (IOException ex) {
                IOUtil.deleteAllFilesStartingWith(hashEnumeratorFile);
                throw ex;
            }
        }
    }

    static void flushContentHashes() {
        if (ourHashesWithFileType != null && ourHashesWithFileType.isDirty()) {
            ourHashesWithFileType.force();
        }
    }

    static byte[] calcContentHash(@Nonnull byte[] bytes, @Nonnull FileType fileType) {
        MessageDigest messageDigest = ContentHashesUtil.HASHER_CACHE.getValue();

        Charset defaultCharset = Charset.defaultCharset();
        messageDigest.update(fileType.getId().getBytes(defaultCharset));
        messageDigest.update((byte) 0);
        messageDigest.update(String.valueOf(bytes.length).getBytes(defaultCharset));
        messageDigest.update((byte) 0);
        messageDigest.update(bytes, 0, bytes.length);
        return messageDigest.digest();
    }

    static int calcContentHashIdWithFileType(@Nonnull byte[] bytes, @Nullable Charset charset, @Nonnull FileType fileType) throws IOException {
        return enumerateHash(calcContentHashWithFileType(bytes, charset, fileType));
    }

    static int calcContentHashId(@Nonnull byte[] bytes, @Nonnull FileType fileType) throws IOException {
        return enumerateHash(calcContentHash(bytes, fileType));
    }

    static int enumerateHash(@Nonnull byte[] digest) throws IOException {
        return ourHashesWithFileType.enumerate(digest);
    }

    static byte[] calcContentHashWithFileType(@Nonnull byte[] bytes, @Nullable Charset charset, @Nonnull FileType fileType) {
        MessageDigest messageDigest = ContentHashesUtil.HASHER_CACHE.getValue();

        Charset defaultCharset = Charset.defaultCharset();
        messageDigest.update(fileType.getName().getBytes(defaultCharset));
        messageDigest.update((byte) 0);
        messageDigest.update(String.valueOf(bytes.length).getBytes(defaultCharset));
        messageDigest.update((byte) 0);
        messageDigest.update((charset != null ? charset.name() : "null_charset").getBytes(defaultCharset));
        messageDigest.update((byte) 0);

        messageDigest.update(bytes, 0, bytes.length);
        return messageDigest.digest();
    }
}