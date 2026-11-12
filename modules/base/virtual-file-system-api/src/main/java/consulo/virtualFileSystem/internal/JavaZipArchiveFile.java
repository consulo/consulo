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

import consulo.virtualFileSystem.archive.ArchiveEntry;
import consulo.virtualFileSystem.archive.ArchiveFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A zip of the local machine, read by {@link ZipFile} - natively.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public final class JavaZipArchiveFile implements ArchiveFile {
    private record Entry(ZipEntry entry) implements ArchiveEntry {
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

    private final ZipFile myZipFile;

    /**
     * @param path a file of the local machine
     */
    public JavaZipArchiveFile(Path path) throws IOException {
        myZipFile = new ZipFile(path.toFile());
    }

    @Override
    public String getName() {
        return myZipFile.getName();
    }

    @Override
    public @Nullable ArchiveEntry getEntry(String name) {
        ZipEntry entry = myZipFile.getEntry(name);
        return entry == null ? null : new Entry(entry);
    }

    @Override
    public InputStream getInputStream(ArchiveEntry entry) throws IOException {
        return myZipFile.getInputStream(((Entry) entry).entry());
    }

    @Override
    public Iterator<? extends ArchiveEntry> entries() {
        return myZipFile.stream().map(Entry::new).iterator();
    }

    @Override
    public int getSize() {
        return myZipFile.size();
    }

    @Override
    public void close() throws IOException {
        myZipFile.close();
    }
}
