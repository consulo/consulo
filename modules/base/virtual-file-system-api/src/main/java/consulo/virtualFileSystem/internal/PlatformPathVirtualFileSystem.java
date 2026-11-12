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

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * The file system of the {@link consulo.virtualFileSystem.StandardFileSystems#PLATFORM_PROTOCOL platform} protocol - files
 * of other machines, reached through the {@link consulo.platform.PlatformNioFileSystem} of each.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface PlatformPathVirtualFileSystem {
    /**
     * The virtual file of a path of some platform file system - which becomes known to this file system by it.
     */
    @Nullable VirtualFile findFileByNioPath(Path path, boolean refresh);

    /**
     * The path of a file of a platform file system, as {@link VirtualFile#getPath()} answers it: {@code <platform id><path>}.
     * The platform becomes known to this file system by it.
     */
    String toVfsPath(Path path);
}
