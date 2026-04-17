/*
 * Copyright 2013-2025 consulo.io
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
package consulo.graphql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link GraphQLQueryLoader} that loads queries from the classpath,
 * resolves {@code #include} directives for fragment files,
 * and caches the results.
 */
public class CachingGraphQLQueryLoader implements GraphQLQueryLoader {
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include \"(.+)\"");

    private final ClassLoader classLoader;
    private final String basePath;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * @param classLoader the classloader to load resources from
     * @param basePath    the base resource path prefix (e.g., "graphql/")
     */
    public CachingGraphQLQueryLoader(ClassLoader classLoader, String basePath) {
        this.classLoader = classLoader;
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
    }

    @Override
    public String loadQuery(String queryPath) throws IOException {
        String cached = cache.get(queryPath);
        if (cached != null) {
            return cached;
        }

        String resolved = loadAndResolve(queryPath);
        cache.put(queryPath, resolved);
        return resolved;
    }

    private String loadAndResolve(String queryPath) throws IOException {
        String fullPath = basePath + queryPath;
        String rawContent = loadResource(fullPath);
        return resolveIncludes(rawContent, queryPath);
    }

    private String resolveIncludes(String content, String relativeTo) throws IOException {
        StringBuilder result = new StringBuilder();
        String parentDir = getParentDir(relativeTo);

        for (String line : content.split("\n")) {
            Matcher matcher = INCLUDE_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                String includePath = parentDir + matcher.group(1);
                String included = loadQuery(includePath);
                result.append(included);
            }
            else {
                result.append(line);
            }
            result.append('\n');
        }

        return result.toString().trim();
    }

    private String getParentDir(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(0, lastSlash + 1) : "";
    }

    private String loadResource(String resourcePath) throws IOException {
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("GraphQL query resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString().trim();
            }
        }
    }
}
