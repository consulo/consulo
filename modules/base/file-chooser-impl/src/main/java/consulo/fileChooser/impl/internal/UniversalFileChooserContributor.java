// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UniversalFileChooserContributor {
    enum MountStatus {
        Permanent,
        Mounted,
        Unmounted
    }

    static @Nullable UniversalFileChooserContributor findOwner(Path path) {
        return Application.get()
            .getExtensionPoint(UniversalFileChooserContributor.class)
            .findFirstSafe(ext -> ext.ownsPath(path));
    }

    static List<Root> getFilteredSystemRoots(Predicate<Path> predicate) {
        List<Root> roots = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            if (predicate.test(root)) {
                roots.add(asDefaultRoot(root));
            }
        }
        return roots;
    }

    static Root asDefaultRoot(Path path) {
        Path fileName = path.getFileName();
        String name = fileName != null ? fileName.toString() : path.toString();
        return new Root(name, new Presentation(LocalizeValue.of(name)), path);
    }

    LocalizeValue getTabTitle();

    CompletableFuture<List<Root>> getRoots();

    default CompletableFuture<List<Root>> getFilteredRoots(Path path) {
        return getRoots();
    }

    boolean ownsPath(Path path);

    default CompletableFuture<MountStatus> getMountStatus(Path path) {
        return CompletableFuture.completedFuture(MountStatus.Permanent);
    }

    default CompletableFuture<?> mount(Path path) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<@Nullable Path> mountVirtualRoot(Root virtualRoot) {
        return CompletableFuture.completedFuture(null);
    }

    default @Nullable FileWatcherAdapter getFileWatcherAdapter() {
        return null;
    }

    record Root(String id, Presentation presentation, @Nullable Path path) {
        public Root(String id, Presentation presentation) {
            this(id, presentation, null);
        }
    }

    record Presentation(LocalizeValue presentableName, @Nullable Image icon, LocalizeValue comment) {
        public Presentation(LocalizeValue presentableName) {
            this(presentableName, null, LocalizeValue.empty());
        }
    }

    default CompletableFuture<@Nullable Presentation> getPresentation(Path path) {
        return CompletableFuture.completedFuture(null);
    }

    default @Nullable String getFileName(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : null;
    }

    default String getPresentablePath(Path path) {
        return path.toString();
    }

    default @Nullable Path parsePresentablePath(String text) {
        try {
            return Path.of(text);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    default @Nullable Path getDesktopPath() {
        return null;
    }

    default LocalizeValue getCustomLoadingText() {
        return LocalizeValue.empty();
    }

    default LocalizeValue getNoEntriesText() {
        return LocalizeValue.empty();
    }
}
