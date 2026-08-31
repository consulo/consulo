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
package consulo.it.internal;

import consulo.container.boot.ContainerPathManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * Temp-directory based {@link ContainerPathManager} for headless integration tests: home/config/system
 * live under a fresh temp directory instead of a real installation layout.
 *
 * @author VISTALL
 */
public class HeadlessContainerPathManager extends ContainerPathManager {
    private final File myHome;

    public HeadlessContainerPathManager() {
        try {
            myHome = Files.createTempDirectory("consulo-it").toFile();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getHomePath() {
        return myHome.getPath();
    }

    @Override
    public File getAppHomeDirectory() {
        return myHome;
    }

    @Override
    public String getConfigPath() {
        return new File(myHome, "config").getPath();
    }

    @Override
    public String getSystemPath() {
        return new File(myHome, "system").getPath();
    }

    @Override
    public File getDocumentsDir() {
        return new File(myHome, "documents");
    }
}
