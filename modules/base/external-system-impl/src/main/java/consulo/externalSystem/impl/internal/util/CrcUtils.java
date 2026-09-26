// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.util;

import consulo.application.Application;
import consulo.document.Document;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.util.ExternalSystemCrcCalculator;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.zip.CRC32;

public final class CrcUtils {
    private static final Logger LOG = Logger.getInstance(CrcUtils.class);

    private static final Key<CrcCache> CRC_CACHE = Key.create("consulo.externalSystem.impl.internal.util.CRC_CACHE");

    private CrcUtils() {
    }

    public static long calculateCrc(Document document, Project project, VirtualFile file) {
        return calculateCrc(document, project, null, file);
    }

    public static long calculateCrc(Document document, Project project, @Nullable ProjectSystemId systemId, VirtualFile file) {
        long modificationStamp = document.getModificationStamp();
        Long cachedCrc = getCachedCrc(file, modificationStamp);
        if (cachedCrc != null) {
            return cachedCrc;
        }
        return findOrCalculateCrc(project.getApplication(), document, modificationStamp, () -> {
            Long crc = doCalculateCrc(document, project, systemId, file);
            return crc != null ? crc : calculateCrc(file);
        });
    }

    public static long calculateCrc(VirtualFile file, Project project) {
        return calculateCrc(file, project, null);
    }

    public static long calculateCrc(VirtualFile file, Project project, @Nullable ProjectSystemId systemId) {
        return findOrCalculateCrc(project.getApplication(), file, file.getModificationStamp(), () -> {
            Long crc = doCalculateCrc(file, project, systemId);
            return crc != null ? crc : calculateCrc(file);
        });
    }

    public static long calculateCrc(VirtualFile file) throws IOException {
        CRC32 crc32 = new CRC32();
        crc32.update(file.contentsToByteArray());
        return crc32.getValue();
    }

    private static long findOrCalculateCrc(
        Application application,
        UserDataHolder holder,
        long modificationStamp,
        ThrowableSupplier<Long, IOException> calculate
    ) {
        application.assertReadAccessAllowed();
        Long cachedCrc = getCachedCrc(holder, modificationStamp);
        if (cachedCrc != null) {
            return cachedCrc;
        }
        long crc;
        try {
            crc = calculate.get();
        }
        catch (IOException e) {
            LOG.warn(e);
            crc = 0;
        }
        setCachedCrc(holder, crc, modificationStamp);
        return crc;
    }

    private static @Nullable Long calculateCrc(
        Project project,
        CharSequence charSequence,
        @Nullable ProjectSystemId systemId,
        VirtualFile file
    ) {
        ExternalSystemCrcCalculator crcCalculator = getCrcCalculator(systemId, file);
        return crcCalculator.calculateCrc(project, file, charSequence);
    }

    private static ExternalSystemCrcCalculator getCrcCalculator(@Nullable ProjectSystemId systemId, VirtualFile file) {
        if (systemId != null) {
            ExternalSystemCrcCalculator crcCalculator = ExternalSystemCrcCalculator.getInstance(systemId, file);
            return crcCalculator != null ? crcCalculator : DefaultCrcCalculator.INSTANCE;
        }
        return DefaultCrcCalculator.INSTANCE;
    }

    private static @Nullable Long doCalculateCrc(
        Document document,
        Project project,
        @Nullable ProjectSystemId systemId,
        VirtualFile file
    ) {
        if (file.getFileType().isBinary()) {
            return null;
        }
        return calculateCrc(project, document.getImmutableCharSequence(), systemId, file);
    }

    private static @Nullable Long doCalculateCrc(VirtualFile file, Project project, @Nullable ProjectSystemId systemId) throws IOException {
        if (file.isDirectory()) {
            return null;
        }
        if (file.getFileType().isBinary()) {
            return null;
        }
        return calculateCrc(project, VirtualFileUtil.loadText(file), systemId, file);
    }

    private static @Nullable Long getCachedCrc(UserDataHolder holder, long modificationStamp) {
        CrcCache crcCache = holder.getUserData(CRC_CACHE);
        if (crcCache == null) {
            return null;
        }
        if (crcCache.modificationStamp() == modificationStamp) {
            return crcCache.value();
        }
        return null;
    }

    private static void setCachedCrc(UserDataHolder holder, long value, long modificationStamp) {
        holder.putUserData(CRC_CACHE, new CrcCache(value, modificationStamp));
    }

    private record CrcCache(long value, long modificationStamp) {
    }
}
