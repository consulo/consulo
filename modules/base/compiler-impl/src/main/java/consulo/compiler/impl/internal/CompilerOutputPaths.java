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
package consulo.compiler.impl.internal;

import consulo.project.Project;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;

import java.nio.file.Path;

/**
 * An output url of the compiler as a path of the machine the project lives on.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
final class CompilerOutputPaths {
    private CompilerOutputPaths() {
    }

    static Path toPath(Project project, String url) {
        String protocol = VirtualFileManager.extractProtocol(url);
        String path = VirtualFileManager.extractPath(url);
        if (StandardFileSystems.PLATFORM_PROTOCOL.equals(protocol)) {
            // <platform id>/<path> - the path as that machine writes it
            VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
            if (fileSystem != null) {
                path = fileSystem.extractPresentableUrl(path);
            }
        }
        return project.getPlatform().fs().getPath(path);
    }
}
