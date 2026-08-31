// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

final class NioFileChooserUtil {
    private static final String LAST_OPENED_FILE_PATH = "last_opened_file_path";

    private NioFileChooserUtil() {
    }

    static @Nullable Path getLastOpenedPath(@Nullable Project project) {
        if (project == null) {
            return null;
        }
        String path = ProjectPropertiesComponent.getInstance(project).getValue(LAST_OPENED_FILE_PATH);
        if (path == null) {
            return null;
        }
        try {
            return Path.of(path);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    static void setLastOpenedFile(@Nullable Project project, @Nullable Path path) {
        if (project != null && !project.isDisposed() && path != null) {
            ProjectPropertiesComponent.getInstance(project).setValue(LAST_OPENED_FILE_PATH, path.toString());
        }
    }

    static boolean isHidden(Path path) {
        if (Platform.current().os().isWindows()) {
            DosFileAttributes dosAttrs;
            try {
                dosAttrs = Files.readAttributes(path, DosFileAttributes.class);
            }
            catch (Exception e) {
                dosAttrs = null;
            }
            return isHidden(path, dosAttrs);
        }
        else {
            return isHidden(path, null);
        }
    }

    static boolean isHidden(Path path, @Nullable BasicFileAttributes attrs) {
        if (Platform.current().os().isWindows() && attrs instanceof DosFileAttributes dosAttrs) {
            return dosAttrs.isHidden();
        }
        else {
            Path fileName = path.getFileName();
            return fileName != null && fileName.toString().startsWith(".");
        }
    }

    static @Nullable Path toNioPathSafe(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }

    static List<Path> safeGetChildren(Path directory, boolean showHidden, boolean showFiles, boolean showArchives) {
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> children = new ArrayList<>();
            stream.forEach(child -> {
                boolean accepted;
                try {
                    accepted = (showFiles || Files.isDirectory(child) || (showArchives && isArchiveFile(child)))
                        && (showHidden || !isHidden(child));
                }
                catch (RuntimeException e) {
                    accepted = false;
                }
                if (accepted) {
                    children.add(child);
                }
            });
            children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()));
            return children;
        }
        catch (Exception e) {
            return List.of();
        }
    }

    static boolean isArchiveFile(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        return FileTypeRegistry.getInstance().getFileTypeByFileName(fileName.toString()) instanceof ArchiveFileType;
    }

    static Image getIcon(Path path) {
        return Files.isDirectory(path)
            ? PlatformIconGroup.nodesFolder()
            : FileTypeRegistry.getInstance().getFileTypeByFileName(path.toString()).getIcon();
    }
}
