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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2017-05-16
 */
public interface Platform extends UserDataHolder {
    static String LOCAL = "local";

    static Platform current() {
        return PlatformInternal.current();
    }

    String getId();

    String getName();

    PlatformFileSystem fs();

    PlatformOperatingSystem os();

    PlatformJvm jvm();

    PlatformUser user();

    default boolean supportsFeature(PlatformFeature feature) {
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

    void openInBrowser(URL url);

    default String fileManagerName() {
        return "File Manager";
    }

    default void openFileInFileManager(Path path, UIAccess uiAccess) {
        openFileInFileManager(path.toFile(), uiAccess);
    }

    void openFileInFileManager(File file, UIAccess uiAccess);

    default void openDirectoryInFileManager(Path path, UIAccess uiAccess) {
        openFileInFileManager(path.toFile(), uiAccess);
    }

    void openDirectoryInFileManager(File file, UIAccess uiAccess);

    default String mapExecutableName(String baseName) {
        String archSuffix = jvm().arch().fileNameSuffix();
        return !archSuffix.isEmpty() ? baseName + archSuffix : baseName;
    }

    default String mapAnyExecutableName(String baseName) {
        if (os().isWindows()) {
            return mapWindowsExecutable(baseName, "exe");
        }

        return mapExecutableName(baseName);
    }

    default String mapWindowsExecutable(String baseName, String extension) {
        if (!os().isWindows()) {
            throw new IllegalArgumentException("Must be Windows");
        }

        return mapExecutableName(baseName) + "." + extension;
    }

    default String mapLibraryName(String libName) {
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
