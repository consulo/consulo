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
package consulo.fileEditor.impl.internal;

import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Local copies of the unsaved documents whose files are on a machine which is not connected - an IDE crash, or a close
 * while the machine is away, does not lose their changes. A copy is the url of the file on its first line, then the text.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class OfflineDocumentBackups {
    private static final Logger LOG = Logger.getInstance(OfflineDocumentBackups.class);

    record Backup(Path path, String url, String text) {
    }

    private final Path myDirectory = Path.of(ContainerPathManager.get().getSystemPath(), "remote-unsaved");
    // the urls with a copy - most saves have none, they do not touch the disk
    private final Set<String> myUrls = ConcurrentHashMap.newKeySet();

    OfflineDocumentBackups() {
        for (Backup backup : list()) {
            myUrls.add(backup.url());
        }
    }

    void save(VirtualFile file, CharSequence text) {
        String url = file.getUrl();
        try {
            Files.createDirectories(myDirectory);
            Files.writeString(pathOf(url), url + "\n" + text, StandardCharsets.UTF_8);
            myUrls.add(url);
        }
        catch (IOException e) {
            LOG.warn("Cannot keep a local copy of " + url, e);
        }
    }

    void remove(VirtualFile file) {
        remove(file.getUrl());
    }

    void remove(String url) {
        if (!myUrls.remove(url)) {
            return;
        }
        try {
            Files.deleteIfExists(pathOf(url));
        }
        catch (IOException e) {
            LOG.warn("Cannot delete the local copy of " + url, e);
        }
    }

    List<Backup> list() {
        List<Backup> backups = new ArrayList<>();
        try (Stream<Path> children = Files.list(myDirectory)) {
            for (Path child : children.toList()) {
                String content = Files.readString(child, StandardCharsets.UTF_8);
                int lineEnd = content.indexOf('\n');
                if (lineEnd > 0) {
                    backups.add(new Backup(child, content.substring(0, lineEnd), content.substring(lineEnd + 1)));
                }
            }
        }
        catch (NoSuchFileException ignored) {
        }
        catch (IOException e) {
            LOG.warn("Cannot read the local copies of unsaved files", e);
        }
        return backups;
    }

    private Path pathOf(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            return myDirectory.resolve(HexFormat.of().formatHex(digest, 0, 16) + ".txt");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
