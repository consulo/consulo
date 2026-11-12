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
package consulo.application.platform;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.platform.Platform;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Makes a local file or directory available on a platform - e.g. a script or a helper of a plugin, which a tool running on
 * a remote machine has to read.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PlatformTransfer {
    static PlatformTransfer getInstance() {
        return Application.get().getInstance(PlatformTransfer.class);
    }

    /**
     * @param localPath a file or directory of the local machine
     * @return {@code localPath} itself for the local platform, otherwise its copy on {@code platform} - copied once and kept
     *         in a cache directory of that machine for as long as the content does not change. Blocking - not on the UI thread.
     */
    Path transfer(Path localPath, Platform platform) throws IOException;
}
