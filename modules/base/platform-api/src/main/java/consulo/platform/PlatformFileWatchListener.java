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

import java.nio.file.Path;

/**
 * Changes of files of a machine - see {@link PlatformNioFileSystem#watch}. Called on a thread of the watch, never the UI one.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public interface PlatformFileWatchListener {
    /**
     * The content or the attributes of a file changed.
     */
    void changed(Path path);

    /**
     * A file appeared or went away - its parent has to be listed again.
     */
    void createdOrDeleted(Path path);

    /**
     * Changes under a root may have been lost (the machine dropped them, or the connection was lost) - all of it has to be
     * checked again.
     */
    void reset(Path root);

    /**
     * A root cannot be watched - its changes are not reported.
     */
    default void failed(Path root, String message) {
    }
}
