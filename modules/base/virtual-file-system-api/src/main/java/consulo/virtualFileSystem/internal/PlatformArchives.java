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
package consulo.virtualFileSystem.internal;

import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import org.jspecify.annotations.Nullable;

/**
 * The archives of other platforms: {@code platform-<archive protocol>://<platform id>/<path>!/<entry>}.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public final class PlatformArchives {
    private static final String PLATFORM_PROTOCOL_PREFIX = StandardFileSystems.PLATFORM_PROTOCOL + "-";

    /**
     * The protocol of the archives of other platforms: {@code jar} -> {@code platform-jar}.
     */
    public static String getPlatformProtocol(String archiveProtocol) {
        return PLATFORM_PROTOCOL_PREFIX + archiveProtocol;
    }

    /**
     * The archive protocol of the protocol of the archives of other platforms: {@code platform-jar} -> {@code jar}.
     */
    public static @Nullable String getArchiveProtocolOfPlatform(String protocol) {
        return protocol.startsWith(PLATFORM_PROTOCOL_PREFIX) ? protocol.substring(PLATFORM_PROTOCOL_PREFIX.length()) : null;
    }

    /**
     * The archive file system for the archives hosted by the given file system - the archive file system itself for the local
     * files, its platform one for the files of other platforms.
     */
    public static @Nullable VirtualFileSystem getArchiveFileSystem(ArchiveFileSystem archiveFileSystem, VirtualFileSystem host) {
        if (!StandardFileSystems.PLATFORM_PROTOCOL.equals(host.getProtocol())) {
          return archiveFileSystem;
        }
        return VirtualFileManager.getInstance().getFileSystem(getPlatformProtocol(archiveFileSystem.getProtocol()));
    }

    private PlatformArchives() {
    }
}
