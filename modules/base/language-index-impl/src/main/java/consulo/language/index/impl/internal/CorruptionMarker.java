// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.index.impl.internal;

import consulo.container.boot.ContainerPathManager;
import consulo.index.io.ID;
import consulo.language.internal.SerializationManagerEx;
import consulo.util.io.FileUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CorruptionMarker {
    private static final String CORRUPTION_MARKER_NAME = "corruption.marker";
    private static final String MARKED_AS_DIRTY_REASON = "Indexes marked as dirty (IDE is expected to be work)";
    private static final String EXPLICIT_INVALIDATION_REASON = "Explicit index invalidation";

    private CorruptionMarker() {
    }

    private static Path getCorruptionMarker() {
        return ContainerPathManager.get().getIndexRoot().toPath().resolve(CORRUPTION_MARKER_NAME);
    }

    public static void markIndexesAsDirty() {
        createCorruptionMarker(MARKED_AS_DIRTY_REASON);
    }

    public static void markIndexesAsClosed() {
        Path corruptionMarker = getCorruptionMarker();
        if (Files.exists(corruptionMarker)) {
            try {
                if (MARKED_AS_DIRTY_REASON.equals(Files.readString(corruptionMarker))) {
                    Files.deleteIfExists(corruptionMarker);
                }
            }
            catch (Exception ignored) {
            }
        }
    }

    public static void requestInvalidation() {
        FileBasedIndexImpl.LOG.info("Explicit index invalidation has been requested");
        createCorruptionMarker(EXPLICIT_INVALIDATION_REASON);
    }

    public static boolean requireInvalidation() {
        boolean corruptionMarkerExists = Files.exists(getCorruptionMarker());
        if (corruptionMarkerExists) {
            String message = "Indexes are corrupted and will be rebuilt";
            try {
                String corruptionReason = Files.readString(getCorruptionMarker());
                FileBasedIndexImpl.LOG.info(message + " (reason = " + corruptionReason + ")");
            }
            catch (Exception ignored) {
                FileBasedIndexImpl.LOG.info(message);
            }
        }
        return IndexInfrastructure.hasIndices() && corruptionMarkerExists;
    }

    public static void dropIndexes() {
        FileBasedIndexImpl.LOG.info("Dropping indexes...");

        File indexRoot = ContainerPathManager.get().getIndexRoot();
        FileUtil.deleteWithRenaming(indexRoot);
        indexRoot.mkdirs();

        FileBasedIndexImpl.LOG.info("Indexes are dropped");

        // serialization manager is initialized before and use removed index root so we need to reinitialize it
        SerializationManagerEx.getInstanceEx().reinitializeNameStorage();
        ID.reinitializeDiskStorage();
        PersistentIndicesConfiguration.saveConfiguration();
        FileUtil.delete(getCorruptionMarker().toFile());
    }

    private static void createCorruptionMarker(String reason) {
        try {
            Path corruptionMarker = getCorruptionMarker();
            Files.createDirectories(corruptionMarker.getParent());
            Files.write(corruptionMarker, reason.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e) {
            FileBasedIndexImpl.LOG.warn(e);
        }
    }
}
