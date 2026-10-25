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
package consulo.virtualFileSystem.impl.internal.archive;

import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.archive.ArchivePath;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A path inside an archive: {@code /} separated, absolute when it starts with {@code /}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
final class ArchivePathImpl implements ArchivePath {
    private final ArchiveNioFileSystem myFileSystem;
    private final String myPath;
    private volatile String @Nullable [] myNames;

    ArchivePathImpl(ArchiveNioFileSystem fileSystem, String path) {
        myFileSystem = fileSystem;
        myPath = normalizeSeparators(path);
    }

    private static String normalizeSeparators(String path) {
        String result = path.replace('\\', '/');
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String[] names() {
        String[] names = myNames;
        if (names == null) {
            List<String> list = new ArrayList<>();
            for (String name : myPath.split("/")) {
                if (!name.isEmpty()) {
                    list.add(name);
                }
            }
            myNames = names = list.toArray(String[]::new);
        }
        return names;
    }

    private ArchivePathImpl checkPath(Path path) {
        if (!(path instanceof ArchivePathImpl archivePath) || archivePath.myFileSystem != myFileSystem) {
            throw new ProviderMismatchException();
        }
        return archivePath;
    }

    @Override
    public Path getArchiveFile() {
        return myFileSystem.getArchiveFile();
    }

    @Override
    public String getEntryPath() {
        ArchivePathImpl absolute = (ArchivePathImpl) toAbsolutePath().normalize();
        return absolute.myPath.substring(1);
    }

    @Override
    public String getVirtualFileUrl() {
        return myFileSystem.getVirtualFileUrl(getEntryPath());
    }

    @Override
    public ArchiveNioFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return myPath.startsWith("/");
    }

    @Override
    public @Nullable Path getRoot() {
        return isAbsolute() ? myFileSystem.getRoot() : null;
    }

    @Override
    public @Nullable Path getFileName() {
        String[] names = names();
        if (names.length == 0) {
            return myPath.isEmpty() ? this : null;
        }
        return names.length == 1 && !isAbsolute() ? this : new ArchivePathImpl(myFileSystem, names[names.length - 1]);
    }

    @Override
    public @Nullable Path getParent() {
        String[] names = names();
        if (names.length == 0 || names.length == 1 && !isAbsolute()) {
            return null;
        }
        String parent = String.join("/", List.of(names).subList(0, names.length - 1));
        return new ArchivePathImpl(myFileSystem, isAbsolute() ? "/" + parent : parent);
    }

    @Override
    public int getNameCount() {
        return names().length;
    }

    @Override
    public Path getName(int index) {
        String[] names = names();
        if (index < 0 || index >= names.length) {
            throw new IllegalArgumentException();
        }
        return new ArchivePathImpl(myFileSystem, names[index]);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        String[] names = names();
        if (beginIndex < 0 || beginIndex >= names.length || endIndex > names.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        return new ArchivePathImpl(myFileSystem, String.join("/", List.of(names).subList(beginIndex, endIndex)));
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(other instanceof ArchivePathImpl path) || path.myFileSystem != myFileSystem || path.isAbsolute() != isAbsolute()) {
            return false;
        }
        String[] names = names();
        String[] otherNames = path.names();
        if (otherNames.length > names.length) {
            return false;
        }
        for (int i = 0; i < otherNames.length; i++) {
            if (!names[i].equals(otherNames[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(other instanceof ArchivePathImpl path) || path.myFileSystem != myFileSystem) {
            return false;
        }
        if (path.isAbsolute()) {
            return equals(path);
        }
        String[] names = names();
        String[] otherNames = path.names();
        if (otherNames.length > names.length) {
            return false;
        }
        for (int i = 0; i < otherNames.length; i++) {
            if (!names[names.length - otherNames.length + i].equals(otherNames[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Path normalize() {
        List<String> result = new ArrayList<>();
        for (String name : names()) {
            if (".".equals(name)) {
                continue;
            }
            if ("..".equals(name)) {
                if (!result.isEmpty() && !"..".equals(result.get(result.size() - 1))) {
                    result.remove(result.size() - 1);
                    continue;
                }
                if (isAbsolute()) {
                    continue;
                }
            }
            result.add(name);
        }
        String joined = String.join("/", result);
        return new ArchivePathImpl(myFileSystem, isAbsolute() ? "/" + joined : joined);
    }

    @Override
    public Path resolve(Path other) {
        ArchivePathImpl path = checkPath(other);
        if (path.isAbsolute()) {
            return path;
        }
        if (path.myPath.isEmpty()) {
            return this;
        }
        if (myPath.isEmpty()) {
            return path;
        }
        return new ArchivePathImpl(myFileSystem, myPath + "/" + path.myPath);
    }

    @Override
    public Path relativize(Path other) {
        ArchivePathImpl path = checkPath(other);
        if (path.isAbsolute() != isAbsolute()) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        String[] names = names();
        String[] otherNames = path.names();
        int common = 0;
        while (common < names.length && common < otherNames.length && names[common].equals(otherNames[common])) {
            common++;
        }
        List<String> result = new ArrayList<>();
        for (int i = common; i < names.length; i++) {
            result.add("..");
        }
        result.addAll(List.of(otherNames).subList(common, otherNames.length));
        return new ArchivePathImpl(myFileSystem, String.join("/", result));
    }

    @Override
    public URI toUri() {
        String localPath = FileUtil.toSystemIndependentName(getArchiveFile().toString());
        if (!localPath.startsWith("/")) {
            localPath = "/" + localPath;
        }
        try {
            return new URI(myFileSystem.getProtocol(), "", localPath + "!/" + getEntryPath(), null, null);
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        return isAbsolute() ? this : myFileSystem.getRoot().resolve(this);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        Path real = toAbsolutePath().normalize();
        myFileSystem.provider().checkAccess(real);
        return real;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Path other) {
        return myPath.compareTo(checkPath(other).myPath);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ArchivePathImpl path && path.myFileSystem == myFileSystem && path.myPath.equals(myPath);
    }

    @Override
    public int hashCode() {
        return myPath.hashCode() * 31 + myFileSystem.hashCode();
    }

    @Override
    public String toString() {
        return myPath;
    }
}
