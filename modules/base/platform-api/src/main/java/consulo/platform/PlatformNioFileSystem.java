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
package consulo.platform;

/**
 * A {@link java.nio.file.FileSystem} of another machine. Its paths stay reachable through the virtual file system
 * by the platform id, which has to be stable while the machine is the same.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public interface PlatformNioFileSystem {
    String getPlatformId();

    /**
     * Starts watching files of the machine - the roots are given by {@link PlatformFileWatch#setRoots}, the changes come to the
     * listener until the watch is closed.
     *
     * @throws UnsupportedOperationException if the machine cannot report changes of its files
     */
    default PlatformFileWatch watch(PlatformFileWatchListener listener) {
        throw new UnsupportedOperationException("Watching files is not supported on " + getPlatformId());
    }
}
