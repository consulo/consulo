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
package consulo.enviroment.remoteAgent.platform;

import consulo.platform.PlatformFileSystem;
import consulo.platform.PlatformOperatingSystem;

import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePlatformFileSystem implements PlatformFileSystem {
    private final PlatformOperatingSystem myOperatingSystem;
    private final FileSystem myNioFileSystem;

    public RemotePlatformFileSystem(PlatformOperatingSystem operatingSystem, FileSystem nioFileSystem) {
        myOperatingSystem = operatingSystem;
        myNioFileSystem = nioFileSystem;
    }

    @Override
    public boolean isCaseSensitive() {
        return myOperatingSystem.isUnix() && !myOperatingSystem.isMac();
    }

    @Override
    public boolean areSymLinksSupported() {
        return myOperatingSystem.isUnix() || myOperatingSystem.isWindows();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return myNioFileSystem.getRootDirectories();
    }

    @Override
    public Path getPath(String path) {
        return myNioFileSystem.getPath(path);
    }

    @Override
    public Path getPath(String path, String... more) {
        return myNioFileSystem.getPath(path, more);
    }
}
