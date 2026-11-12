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
package consulo.content.internal;

import org.jspecify.annotations.Nullable;

/**
 * Names of the bundles of remote platform targets.
 * <p>
 * The bundle table knows such a bundle as {@code $remote:<target id>$-<name>} - it never clashes with a local bundle or one of
 * another target of the same name. Project files store the plain {@code <name>}, they stay the same whatever machine the project
 * is opened from - a remote project resolves it to the bundle of its own target. The UI shows {@code <name> (remote)}.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public final class RemoteSdkNames {
    private static final String PREFIX = "$remote:";
    private static final String SEPARATOR = "$-";

    private RemoteSdkNames() {
    }

    /**
     * The name in the bundle table of the bundle {@code name} of the target {@code platformTargetId}.
     */
    public static String toTableName(String platformTargetId, String name) {
        if (isRemote(name)) {
            return name;
        }
        return PREFIX + platformTargetId + SEPARATOR + name;
    }

    public static boolean isRemote(String name) {
        return name.startsWith(PREFIX) && name.indexOf(SEPARATOR, PREFIX.length()) > 0;
    }

    /**
     * The target of a bundle of the table, null for a local one.
     */
    public static @Nullable String getPlatformTargetId(String name) {
        if (!isRemote(name)) {
            return null;
        }
        return name.substring(PREFIX.length(), name.indexOf(SEPARATOR, PREFIX.length()));
    }

    /**
     * The name as project files store it - without the target.
     */
    public static String toStoredName(String name) {
        if (!isRemote(name)) {
            return name;
        }
        return name.substring(name.indexOf(SEPARATOR, PREFIX.length()) + SEPARATOR.length());
    }

    /**
     * The name the UI shows.
     */
    public static String toPresentableName(String name) {
        if (!isRemote(name)) {
            return name;
        }
        return toStoredName(name) + " (remote)";
    }
}
