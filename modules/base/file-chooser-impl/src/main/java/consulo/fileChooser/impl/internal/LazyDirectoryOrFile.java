// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.core.local.CoreLocalFileSystem;
import consulo.virtualFileSystem.internal.core.local.CoreLocalVirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class LazyDirectoryOrFile extends CoreLocalVirtualFile {
    private static final Logger LOG = Logger.getInstance(LazyDirectoryOrFile.class);

    private final CoreLocalFileSystem myFileSystem;
    private final @Nullable VirtualFile myParent;
    private final @Nullable Map<String, Optional<LazyDirectoryOrFile>> myChildren;

    LazyDirectoryOrFile(CoreLocalFileSystem fileSystem, @Nullable VirtualFile parent, Path file, BasicFileAttributes attrs) {
        super(fileSystem, file, attrs);
        myFileSystem = fileSystem;
        myParent = parent;
        myChildren = attrs.isDirectory() ? new HashMap<>() : null;
        if (parent instanceof PreloadedDirectory preloaded) {
            preloaded.addChild(this);
        }
    }

    @Override
    public @Nullable VirtualFile getParent() {
        return myParent;
    }

    @Override
    public @Nullable VirtualFile findChild(String name) {
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(name);
        }
        return myChildren == null ? null : myChildren.computeIfAbsent(name, k -> {
            try {
                Path childFile = getFile().resolve(name);
                BasicFileAttributes attrs = Files.readAttributes(childFile, BasicFileAttributes.class);
                return Optional.of(new LazyDirectoryOrFile(myFileSystem, this, childFile, attrs));
            }
            catch (Exception e) {
                LOG.trace(e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Override
    public VirtualFile[] getChildren() {
        return myChildren == null
            ? EMPTY_ARRAY
            : myChildren.values().stream().map(o -> o.orElse(null)).filter(Objects::nonNull).toArray(VirtualFile[]::new);
    }
}
