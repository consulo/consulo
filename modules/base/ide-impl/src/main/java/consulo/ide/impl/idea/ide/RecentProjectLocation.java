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
package consulo.ide.impl.idea.ide;

import consulo.project.Project;
import consulo.virtualFileSystem.StandardFileSystems;
import org.jspecify.annotations.Nullable;

/**
 * Where a recent project lives. A local project is stored as its path, a project of a remote platform target as
 * {@code platform://<target id><path>} - the same path may exist both locally and on several targets. A windows path gets
 * a separator of its own: {@code platform://<target id>/C:/a}.
 *
 * @param key            the stored form, the key of every recent projects map
 * @param platformTargetId null for a local project
 * @param path           the path on its machine - shown to the user as it is
 * @author VISTALL
 * @since 2026-11-13
 */
public record RecentProjectLocation(String key, @Nullable String platformTargetId, String path) {
    private static final String PREFIX = StandardFileSystems.PLATFORM_PROTOCOL + "://";

    public static RecentProjectLocation of(@Nullable String platformTargetId, String path) {
        if (platformTargetId == null) {
            return new RecentProjectLocation(path, null, path);
        }
        // a target id may contain a slash - it is encoded, the first slash ends it
        String encodedId = platformTargetId.replace("%", "%25").replace("/", "%2F");
        String separatedPath = path.startsWith("/") ? path : "/" + path;
        return new RecentProjectLocation(PREFIX + encodedId + separatedPath, platformTargetId, path);
    }

    public static RecentProjectLocation of(Project project, String path) {
        return of(project.getPlatformTargetId(), path);
    }

    public static RecentProjectLocation parse(String key) {
        if (!key.startsWith(PREFIX)) {
            return new RecentProjectLocation(key, null, key);
        }
        int pathStart = key.indexOf('/', PREFIX.length());
        if (pathStart < 0) {
            return new RecentProjectLocation(key, null, key);
        }
        String platformTargetId = key.substring(PREFIX.length(), pathStart).replace("%2F", "/").replace("%25", "%");
        String path = key.substring(pathStart);
        if (path.length() > 2 && path.charAt(2) == ':') {
            // /C:/a -> C:/a
            path = path.substring(1);
        }
        return new RecentProjectLocation(key, platformTargetId, path);
    }

    public boolean isRemote() {
        return platformTargetId != null;
    }

    /**
     * The path as its machine writes it - {@code C:\a} for a windows one.
     */
    public static String toPresentablePath(String path) {
        if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
            return path.replace('/', '\\');
        }
        return path;
    }
}
