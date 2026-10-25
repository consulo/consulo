// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ide.impl.idea.openapi.fileChooser.impl;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchivePath;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileChooserUtil {
    private static final String LAST_OPENED_FILE = "last_opened_file_path";
    public static final String RECENT_FILES = "file.chooser.recent.files";
    private static final int RECENT_FILES_LIMIT = 30;

    public static boolean isLastOpenedPathSet(@Nullable Project project) {
        String last = getLastOpenedFilePath(project);
        return last != null;
    }

    public static @Nullable Path getLastOpenedFile(@Nullable Project project) {
        String last = getLastOpenedFilePath(project);
        return last != null ? NioFiles.toPath(FileUtil.toSystemDependentName(last)) : null;
    }

    public static @Nullable String getLastOpenedFilePath(@Nullable Project project) {
        return (project != null ? PropertiesComponent.getInstance(project) : PropertiesComponent.getInstance()).getValue(LAST_OPENED_FILE);
    }

    public static void setLastOpenedFile(@Nullable Project project, Path file) {
        setLastOpenedPath(project, FileUtil.toSystemIndependentName(toLocalPath(file).toString()));
    }

    private static void setLastOpenedPath(@Nullable Project project, String path) {
        if (project == null) {
            PropertiesComponent.getInstance().setValue(LAST_OPENED_FILE, path);
        }
        else if (!project.isDisposed()) {
            PropertiesComponent.getInstance(project).setValue(LAST_OPENED_FILE, path);
        }
    }

    public static List<String> getRecentPaths() {
        return getRecentPaths(RECENT_FILES);
    }

    public static List<String> getRecentPaths(String propertyName) {
        String[] values = PropertiesComponent.getInstance().getValues(propertyName);
        return values != null ? List.of(values) : List.of();
    }

    public static void updateRecentPaths(@Nullable Project project, Path file) {
        updateRecentPaths(project, file, RECENT_FILES, null);
    }

    public static void updateRecentPaths(
        @Nullable Project project,
        Path file,
        String propertyName,
        @Nullable Function<Path, String> pathProvider
    ) {
        Path localFile = toLocalPath(file);

        String path = pathProvider != null ? pathProvider.apply(localFile) : FileUtil.toSystemIndependentName(localFile.toString());
        setLastOpenedPath(project, path);
        List<String> recent = Stream.concat(Stream.of(path), getRecentPaths(propertyName).stream())
            .distinct()
            .limit(RECENT_FILES_LIMIT)
            .collect(Collectors.toList());
        PropertiesComponent.getInstance().setValues(propertyName, ArrayUtil.toStringArray(recent));
    }

    /**
     * An entry inside an archive is remembered as its local archive file.
     */
    private static Path toLocalPath(Path file) {
        return file instanceof ArchivePath archivePath ? archivePath.getArchiveFile() : file;
    }

    public static @Nullable VirtualFile getFileToSelect(FileChooserDescriptor descriptor, @Nullable Project project, @Nullable VirtualFile toSelect) {
        Path path = pathToSelect(project, toSelect == null ? null : toSelect.getFileSystem().getNioPath(toSelect), false);
        VirtualFile result = path == null ? null : VirtualFileManager.getInstance().findFileByNioPath(path);
        return result != null && descriptor instanceof FileSaverDescriptor && !result.isDirectory() ? result.getParent() : result;
    }

    public static @Nullable Path getInitialPath(
        FileChooserDescriptor descriptor,
        @Nullable Project project,
        @Nullable Path toSelect,
        boolean preferLastOverExplicit
    ) {
        Path result = pathToSelect(project, toSelect, preferLastOverExplicit);
        return result != null && descriptor instanceof FileSaverDescriptor && !Files.isDirectory(result) ? result.getParent() : result;
    }

    private static @Nullable Path pathToSelect(@Nullable Project project, @Nullable Path toSelect, boolean preferLastOverExplicit) {
        String last = getLastOpenedFilePath(project);
        if (last != null && (toSelect == null || preferLastOverExplicit)) {
            return NioFiles.toPath(FileUtil.toSystemDependentName(last));
        }

        if (toSelect != null) {
            return toSelect;
        }

        if (project != null && !project.isDefault()) {
            String basePath = project.getBasePath();
            if (basePath != null) {
                return NioFiles.toPath(FileUtil.toSystemDependentName(basePath));
            }
        }

        return Platform.current().user().homePath();
    }

    public static List<VirtualFile> getChosenFiles(FileChooserDescriptor descriptor, Collection<VirtualFile> selectedFiles) {
        return ContainerUtil.mapNotNull(selectedFiles, file -> file != null && file.isValid() ? descriptor.getFileToSelect(file) : null);
    }
}
