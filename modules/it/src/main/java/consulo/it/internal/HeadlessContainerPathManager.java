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
import consulo.util.lang.ShutDownTracker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Temp-directory based {@link ContainerPathManager} for headless integration tests: home/config/system
 * live under a fresh temp directory instead of a real installation layout.
 * <p>
 * The directory is deleted when the JVM exits: with the indexes of a project in it, each run leaves a large directory
 * behind otherwise. Set {@code -Dconsulo.it.keepHome=true} to keep it for a look at a failed run.
 *
 * @author VISTALL
 */
public class HeadlessContainerPathManager extends ContainerPathManager {
    private static final String KEEP_HOME_PROPERTY = "consulo.it.keepHome";

    private final File myHome;

    public HeadlessContainerPathManager() {
        try {
            myHome = Files.createTempDirectory("consulo-it").toFile();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!Boolean.getBoolean(KEEP_HOME_PROPERTY)) {
            // the tasks of the tracker run last registered first: registered before the application starts, this one
            // runs after the indexes and the file system have flushed into the directory on their own shutdown
            ShutDownTracker.getInstance().registerShutdownTask(() -> deleteRecursively(myHome.toPath()));
        }
    }

    private static void deleteRecursively(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                }
                catch (IOException ignored) {
                }
            }
        }
        catch (IOException | UncheckedIOException ignored) {
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
