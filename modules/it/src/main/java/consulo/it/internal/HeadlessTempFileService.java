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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.TempFileService;
import consulo.container.boot.ContainerPathManager;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessTempFileService implements TempFileService {
    private static final int MAX_ATTEMPTS = 1000;

    private final Queue<Path> myDirectoriesToDelete = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean myShutdownHookRegistered = new AtomicBoolean();

    @Override
    public Path createTempDirectory(Path dir, String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
        Path directory = create(dir, prefix, suffix, true);
        if (deleteOnExit) {
            myDirectoriesToDelete.add(directory);
            if (myShutdownHookRegistered.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::deleteDirectories, "HeadlessTempFileService deleteOnExit"));
            }
        }
        return directory;
    }

    @Override
    public Path createTempFile(Path dir, String prefix, @Nullable String suffix, boolean create, boolean deleteOnExit) throws IOException {
        Path file = create(dir, prefix, suffix, false);
        if (deleteOnExit) {
            file.toFile().deleteOnExit();
        }
        if (!create) {
            Files.deleteIfExists(file);
        }
        return file;
    }

    @Override
    public Path getTempDirectory() {
        return Path.of(ContainerPathManager.get().getTempPath()).toAbsolutePath();
    }

    private static Path create(Path dir, String prefix, @Nullable String suffix, boolean directory) throws IOException {
        Files.createDirectories(dir);

        String head = new File(prefix.length() < 3 ? (prefix + "___").substring(0, 3) : prefix).getName();
        String tail = suffix == null ? "" : suffix;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String name = i == 0 ? head : head + i;
            if (name.endsWith(".") && tail.startsWith(".")) {
                name = name.substring(0, name.length() - 1);
            }
            Path path = dir.resolve(name + tail);
            try {
                return directory ? Files.createDirectory(path) : Files.createFile(path);
            }
            catch (FileAlreadyExistsException ignored) {
            }
        }
        throw new IOException("Unable to create a temporary " + (directory ? "directory" : "file") + " '" + head + tail + "' in " + dir);
    }

    private void deleteDirectories() {
        Path directory;
        while ((directory = myDirectoriesToDelete.poll()) != null) {
            try (Stream<Path> walk = Files.walk(directory)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
            catch (IOException ignored) {
            }
        }
    }
}
