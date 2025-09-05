/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform;

import consulo.platform.internal.PlatformInternal;
import consulo.ui.UIAccess;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public interface Platform extends UserDataHolder {
    static String LOCAL = "local";

    @Nonnull
    static Platform current() {
        return PlatformInternal.current();
    }

    @Nonnull
    String getId();

    @Nonnull
    String getName();

    @Nonnull
    PlatformFileSystem fs();

    @Nonnull
    PlatformOperatingSystem os();

    @Nonnull
    PlatformJvm jvm();

    @Nonnull
    PlatformUser user();

    default boolean supportsFeature(@Nonnull PlatformFeature feature) {
        return false;
    }

    @SuppressWarnings("deprecation")
    default void openInBrowser(String url) {
        try {
            openInBrowser(new URL(url));
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    void openInBrowser(@Nonnull URL url);

    @Nonnull
    default String fileManagerName() {
        return "File Manager";
    }

    default void openFileInFileManager(@Nonnull Path path, @Nonnull UIAccess uiAccess) {
        openFileInFileManager(path.toFile(), uiAccess);
    }

    void openFileInFileManager(@Nonnull File file, @Nonnull UIAccess uiAccess);

    default void openDirectoryInFileManager(@Nonnull Path path, @Nonnull UIAccess uiAccess) {
        openFileInFileManager(path.toFile(), uiAccess);
    }

    void openDirectoryInFileManager(@Nonnull File file, @Nonnull UIAccess uiAccess);

    @Nonnull
    default String mapExecutableName(@Nonnull String baseName) {
        String archSuffix = jvm().arch().fileNameSuffix();
        return !archSuffix.isEmpty() ? baseName + archSuffix : baseName;
    }

    @Nonnull
    default String mapAnyExecutableName(@Nonnull String baseName) {
        if (os().isWindows()) {
            return mapWindowsExecutable(baseName, "exe");
        }

        return mapExecutableName(baseName);
    }

    @Nonnull
    default String mapWindowsExecutable(@Nonnull String baseName, @Nonnull String extension) {
        if (!os().isWindows()) {
            throw new IllegalArgumentException("Must be Windows");
        }

        return mapExecutableName(baseName) + "." + extension;
    }

    @Nonnull
    default String mapLibraryName(@Nonnull String libName) {
        String baseName = libName;
        String archSuffix = jvm().arch().fileNameSuffix();
        if (!archSuffix.isEmpty()) {
            baseName = baseName + archSuffix;
        }

        String fileName = System.mapLibraryName(baseName);
        if (os().isMac()) {
            fileName = fileName.replace(".jnilib", ".dylib");
        }
        return fileName;
    }
}
