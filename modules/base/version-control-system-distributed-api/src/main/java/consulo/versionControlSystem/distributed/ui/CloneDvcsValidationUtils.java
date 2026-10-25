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
package consulo.versionControlSystem.distributed.ui;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class CloneDvcsValidationUtils {
    /**
     * The pattern for SSH URL-s in form [user@]host:path
     */
    private static final Pattern SSH_URL_PATTERN;

    static {
        // TODO make real URL pattern
        String ch = "[\\p{ASCII}&&[\\p{Graph}]&&[^@:/]]";
        String ch2 = "[\\p{ASCII}&&[\\p{Graph}]&&[^/]]";
        String host = ch + "+(?:\\." + ch + "+)*";
        String path = "/?" + ch2 + "+(?:/" + ch2 + "+)*/?";
        String all = "(?:" + ch + "+@)?" + host + ":" + path;
        SSH_URL_PATTERN = Pattern.compile(all);
    }

    /**
     * @return empty if the destination directory exists or was created
     */
    public static LocalizeValue createDestination(String path) {
        try {
            Path directoryPath = Path.of(path);
            if (!directoryPath.toFile().exists()) {
                Files.createDirectories(directoryPath);
            }
            else if (!directoryPath.toFile().isDirectory()) {
                return DistributedVcsLocalize.cloneDestinationDirectoryErrorAccess();
            }
            return LocalizeValue.empty();
        }
        catch (InvalidPathException e) {
            return DistributedVcsLocalize.cloneDestinationDirectoryErrorInvalid();
        }
        catch (Exception e) {
            return DistributedVcsLocalize.cloneDestinationDirectoryErrorAccess();
        }
    }

    /**
     * Check destination directory and return appropriate error text if there are problems
     *
     * @return empty if destination directory is OK.
     */
    public static LocalizeValue checkDirectory(String directoryPath) {
        if (directoryPath.isEmpty()) {
            return LocalizeValue.empty();
        }

        try {
            Path path = Path.of(directoryPath);
            if (!path.toFile().exists()) {
                return LocalizeValue.empty();
            }
            else if (!path.toFile().isDirectory()) {
                return DistributedVcsLocalize.cloneDestinationDirectoryErrorNotDirectory();
            }
            else if (!isDirectoryEmpty(path)) {
                return DistributedVcsLocalize.cloneDestinationDirectoryErrorExists();
            }
        }
        catch (InvalidPathException | IOException e) {
            return DistributedVcsLocalize.cloneDestinationDirectoryErrorInvalid();
        }
        return LocalizeValue.empty();
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            return !directoryStream.iterator().hasNext();
        }
    }

    /**
     * Check repository URL and return appropriate error text if there are problems
     *
     * @return empty if repository URL is OK.
     */
    public static LocalizeValue checkRepositoryURL(String repositoryUrl) {
        if (repositoryUrl.isEmpty()) {
            return DistributedVcsLocalize.cloneRepositoryUrlErrorEmpty();
        }

        String repository = sanitizeCloneUrl(repositoryUrl);

        // Is it a proper URL?
        if (isRepositoryUrlValid(repository)) {
            return LocalizeValue.empty();
        }

        // Is it FS URL?
        try {
            Path path = Path.of(repository);

            if (path.toFile().exists()) {
                if (!path.toFile().isDirectory()) {
                    return DistributedVcsLocalize.cloneUrlIsNotDirectoryError();
                }
                return LocalizeValue.empty();
            }
        }
        catch (Exception fileExp) {
            // do nothing
        }

        return DistributedVcsLocalize.cloneInvalidUrl();
    }

    public static boolean isRepositoryUrlValid(String repository) {
        if (repository.isEmpty()) {
            return false;
        }
        try {
            if (new URI(repository).isAbsolute()) {
                return true;
            }
        }
        catch (URISyntaxException urlExp) {
            // do nothing
        }

        // Is it SSH URL?
        return SSH_URL_PATTERN.matcher(repository).matches();
    }

    public static String sanitizeCloneUrl(String urlText) {
        return StringUtil.trimStart(StringUtil.trimStart(urlText.trim(), "git clone"), "hg clone").trim();
    }

    private CloneDvcsValidationUtils() {
    }
}
