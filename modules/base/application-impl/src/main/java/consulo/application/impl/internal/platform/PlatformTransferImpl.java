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
package consulo.application.impl.internal.platform;

import consulo.annotation.component.ServiceImpl;
import consulo.application.internal.PlatformTransferDirectoryProvider;
import consulo.application.platform.PlatformTransfer;
import consulo.logging.Logger;
import consulo.platform.Platform;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Copies with plain {@link Files} calls across file systems - the platform file system takes the content as a stream.
 * A copy lives in {@code <transfer directory>/<hash>/<name>} - the IDE directory of the platform (the workspace of a remote
 * agent), or {@code ~/.cache/consulo/transfer} for a platform without one. The hash is taken over the relative
 * names, sizes and modification times, and a marker file tells a complete copy from an interrupted one.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@Singleton
@ServiceImpl
public class PlatformTransferImpl implements PlatformTransfer {
    private static final Logger LOG = Logger.getInstance(PlatformTransferImpl.class);

    private static final String COMPLETE_MARKER = ".consulo-transfer-complete";

    // platform id + content hash -> the copy, so a session does not even look at the remote side twice
    private final Map<String, Path> myTransferred = new ConcurrentHashMap<>();

    @Override
    public Path transfer(Path localPath, Platform platform) throws IOException {
        if (platform == Platform.current()) {
            return localPath;
        }

        String hash = hash(localPath);
        String key = platform.getId() + "/" + hash;
        Path known = myTransferred.get(key);
        if (known != null) {
            return known;
        }

        Path directory = getTransferDirectory(platform).resolve(hash);
        Path target = directory.resolve(localPath.getFileName().toString());
        Path marker = directory.resolve(COMPLETE_MARKER);
        if (!Files.exists(marker)) {
            LOG.info("Transferring " + localPath + " to " + target + " of " + platform.getName());
            Files.createDirectories(directory);
            copy(localPath, target, !platform.os().isWindows());
            Files.writeString(marker, localPath.toString(), StandardCharsets.UTF_8);
        }
        myTransferred.put(key, target);
        return target;
    }

    private static Path getTransferDirectory(Platform platform) {
        if (platform instanceof PlatformTransferDirectoryProvider provider) {
            return provider.getTransferDirectory();
        }
        return platform.user().homePath().resolve(".cache").resolve("consulo").resolve("transfer");
    }

    private static void copy(Path source, Path target, boolean posix) throws IOException {
        if (!Files.isDirectory(source)) {
            copyFile(source, target, posix);
            return;
        }
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(resolve(target, source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                copyFile(file, resolve(target, source.relativize(file)), posix);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * An executable stays executable - a tool started on that machine.
     */
    private static void copyFile(Path source, Path target, boolean posix) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        if (posix && Files.isExecutable(source)) {
            Files.setAttribute(target, "posix:permissions", PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    /**
     * A relative path of the local file system resolved on the target one - name by name, the separators may differ.
     */
    private static Path resolve(Path target, Path relative) {
        Path result = target;
        for (Path name : relative) {
            result = result.resolve(name.toString());
        }
        return result;
    }

    private static String hash(Path localPath) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(localPath)) {
            try (Stream<Path> walk = Files.walk(localPath)) {
                walk.filter(Files::isRegularFile).sorted().forEach(files::add);
            }
        }
        else {
            files.add(localPath);
        }
        digest.update(localPath.getFileName().toString().getBytes(StandardCharsets.UTF_8));
        for (Path file : files) {
            String entry = localPath.relativize(file) + ":" + Files.size(file) + ":" + Files.getLastModifiedTime(file).toMillis();
            digest.update(entry.getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    }
}
