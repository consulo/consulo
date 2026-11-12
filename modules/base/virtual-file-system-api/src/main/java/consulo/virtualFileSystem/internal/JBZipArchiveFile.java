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
package consulo.virtualFileSystem.internal;

import consulo.util.io.zip.JBZipEntry;
import consulo.util.io.zip.JBZipFile;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.archive.ArchiveEntry;
import consulo.virtualFileSystem.archive.ArchiveFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * A zip of another platform, read through its {@link Path}: the central directory once, an entry by its range.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public final class JBZipArchiveFile implements ArchiveFile {
    private record Entry(JBZipEntry entry) implements ArchiveEntry {
        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public long getSize() {
            return entry.getSize();
        }

        @Override
        public long getTime() {
            return entry.getTime();
        }

        @Override
        public boolean isDirectory() {
            return entry.isDirectory();
        }
    }

    private final Path myPath;
    private final JBZipFile myZipFile;

    public JBZipArchiveFile(Path path) throws IOException {
        myPath = path;
        // remote reads come in larger buffers - every read is a round trip
        boolean remote = path.getFileSystem() != FileSystems.getDefault();
        myZipFile = new JBZipFile(path, true, remote, ThreeState.UNSURE);
    }

    @Override
    public String getName() {
        return myPath.toString();
    }

    @Override
    public @Nullable ArchiveEntry getEntry(String name) {
        JBZipEntry entry = myZipFile.getEntry(name);
        return entry == null ? null : new Entry(entry);
    }

    @Override
    public InputStream getInputStream(ArchiveEntry entry) throws IOException {
        return ((Entry) entry).entry().getInputStream();
    }

    @Override
    public Iterator<? extends ArchiveEntry> entries() {
        return myZipFile.getEntries().stream().map(Entry::new).iterator();
    }

    @Override
    public int getSize() {
        return myZipFile.getEntries().size();
    }

    @Override
    public void close() throws IOException {
        myZipFile.close();
    }
}
