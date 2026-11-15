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

import consulo.annotation.UsedInPlugin;
import consulo.platform.internal.PlatformInternal;
import consulo.ui.UIAccess;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /**
     * TCP of this machine - connections and bound ports.
     */
    PlatformNetwork net();

    default boolean supportsFeature(PlatformFeature feature) {
        return false;
    }

    @UsedInPlugin
    default boolean moveToTrash(Path path) {
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

    default boolean isInBrowser() {
        return false;
    }

    void openInBrowser(URL url);

    /**
     * The name the machine's file manager goes by - "Explorer", "Finder", the file manager of a Linux desktop.
     */
    default String fileManagerName() {
        PlatformOperatingSystem os = os();
        if (os.isWindows()) {
            return "Explorer";
        }
        if (os.isMac()) {
            return "Finder";
        }
        if (os.isHaiku()) {
            return "Tracker";
        }
        // XDG_CURRENT_DESKTOP lists the desktop and what it derives from - "ubuntu:GNOME", "X-Cinnamon"
        String desktop = os.getEnvironmentVariable("XDG_CURRENT_DESKTOP");
        if (desktop != null) {
            for (String name : desktop.toUpperCase(Locale.ROOT).split(":")) {
                String fileManager = switch (name) {
                    case "KDE" -> "Dolphin";
                    case "GNOME", "UNITY", "PANTHEON", "BUDGIE" -> "Files";
                    case "X-CINNAMON", "CINNAMON" -> "Nemo";
                    case "XFCE" -> "Thunar";
                    case "MATE" -> "Caja";
                    case "LXQT" -> "PCManFM-Qt";
                    case "LXDE" -> "PCManFM";
                    case "DEEPIN" -> "Deepin File Manager";
                    default -> null;
                };
                if (fileManager != null) {
                    return fileManager;
                }
            }
        }
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

    default @Nullable Path findExecutableInPath(String name) {
        String pathValue = os().getEnvironmentVariable("PATH");
        if (pathValue == null || pathValue.isEmpty()) {
            return null;
        }

        boolean windows = os().isWindows();
        List<String> names = new ArrayList<>();
        names.add(name);
        if (windows && name.indexOf('.') < 0) {
            String pathExt = os().getEnvironmentVariable("PATHEXT");
            for (String extension : (pathExt == null ? ".COM;.EXE;.BAT;.CMD" : pathExt).split(";")) {
                if (!extension.isEmpty()) {
                    names.add(name + extension.toLowerCase());
                }
            }
        }

        for (String directory : pathValue.split(windows ? ";" : ":")) {
            if (directory.isEmpty()) {
                continue;
            }
            for (String candidateName : names) {
                Path candidate = fs().getPath(directory, candidateName);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * The name of an executable built for the machine - an executable runs on the operating system, so it is built for
     * the system's architecture, which a JVM may not share.
     */
    default String mapExecutableName(String baseName) {
        String archSuffix = os().arch().fileNameSuffix();
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

    /**
     * The file name of a native library of the machine - named the way its operating system names libraries, not the way
     * the machine the IDE runs on does. A library is loaded into a JVM, so it is built for the JVM's architecture.
     */
    default String mapLibraryName(String libName) {
        String baseName = libName + jvm().arch().fileNameSuffix();
        PlatformOperatingSystem os = os();
        if (os.isWindows()) {
            return baseName + ".dll";
        }
        if (os.isMac()) {
            return "lib" + baseName + ".dylib";
        }
        return "lib" + baseName + ".so";
    }
}
