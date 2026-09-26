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
package consulo.builtinWebServer.impl;

import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.project.Project;
import consulo.util.io.NetUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\built-in-server\src\org\jetbrains\builtInWebServer\BuiltInWebServer.kt
 */
public class BuiltInWebServerKt {
    public static final String TOKEN_PARAM_NAME = "_ijt";
    public static final String TOKEN_HEADER_NAME = "x-ijt";

    private static final String[] INDEX_NAME_PREFIXES = {"index.", "default."};
    private static final String LOCAL_HOST_SUFFIX = ".local";

    public static boolean compareNameAndProjectBasePath(String projectName, Project project) {
        String basePath = project.getBasePath();
        return basePath != null && endsWithName(basePath, projectName);
    }

    public static @Nullable VirtualFile findIndexFile(VirtualFile basedir) {
        VirtualFile[] children = basedir.getChildren();
        if (children == null || children.length == 0) {
            return null;
        }

        for (String indexNamePrefix : INDEX_NAME_PREFIXES) {
            VirtualFile index = null;
            String preferredName = indexNamePrefix + "html";
            for (VirtualFile child : children) {
                if (!child.isDirectory()) {
                    String name = child.getName();
                    if (name.equals(preferredName)) {
                        return child;
                    }
                    else if (index == null && name.startsWith(indexNamePrefix)) {
                        index = child;
                    }
                }
            }
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    public static @Nullable Path findIndexFile(Path basedir) throws IOException {
        List<Path> children = new ArrayList<>();
        DirectoryStream.Filter<Path> filter = path -> {
            String name = path.getFileName().toString();
            for (String indexNamePrefix : INDEX_NAME_PREFIXES) {
                if (name.startsWith(indexNamePrefix)) {
                    return true;
                }
            }
            return false;
        };
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(basedir, filter)) {
            for (Path child : stream) {
                children.add(child);
            }
        }
        catch (NoSuchFileException ignored) {
            return null;
        }

        for (String indexNamePrefix : INDEX_NAME_PREFIXES) {
            Path index = null;
            String preferredName = indexNamePrefix + "html";
            for (Path child : children) {
                if (!Files.isDirectory(child)) {
                    String name = child.getFileName().toString();
                    if (name.equals(preferredName)) {
                        return child;
                    }
                    else if (index == null && name.startsWith(indexNamePrefix)) {
                        index = child;
                    }
                }
            }
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    public static boolean isOwnHostName(String host) {
        if (NetUtil.isLocalhost(host)) {
            return true;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (host.equals(address.getHostAddress()) || host.equalsIgnoreCase(address.getCanonicalHostName())) {
                return true;
            }

            String localHostName = InetAddress.getLocalHost().getHostName();
            return localHostName.equalsIgnoreCase(host)
                || (host.endsWith(LOCAL_HOST_SUFFIX)
                && localHostName.regionMatches(true, 0, host, 0, host.length() - LOCAL_HOST_SUFFIX.length()));
        }
        catch (IOException ignored) {
            return false;
        }
    }

    public static boolean endsWithName(String path, String name) {
        return path.endsWith(name) && (path.length() == name.length() || path.charAt(path.length() - name.length() - 1) == '/');
    }

    public static HttpResponse redirectToDirectory(HttpRequest request) {
        String uri = request.uri();
        int end = uri.length();
        int queryIndex = uri.indexOf('?');
        if (queryIndex >= 0) {
            end = queryIndex;
        }
        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex >= 0 && fragmentIndex < end) {
            end = fragmentIndex;
        }

        String rawPath = uri.substring(0, end);
        String lastSegment = rawPath.substring(rawPath.lastIndexOf('/') + 1);
        return HttpResponse.create(HttpURLConnection.HTTP_MOVED_PERM, null, null).withHeader("Location", "./" + lastSegment + "/");
    }
}
