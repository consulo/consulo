// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.http.graphql;

import com.google.common.cache.CacheBuilder;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built on the assumption that fragments are named the same as the file they're located in.
 * Ex:
 * <p>
 * /graphql/fragment/actor.graphql
 * only publicly exposes the {@code actor} fragment
 */
public class CachingGraphQLQueryLoader implements GraphQLQueryLoader {
    private static final Pattern FRAGMENT_DEFINITION = Pattern.compile("fragment (.*) on .*\\{");

    private final Function<String, @Nullable InputStream> myGetFileStream;
    private final ConcurrentMap<String, GraphQLBlock> myFragmentsCache;
    private final ConcurrentMap<String, String> myQueriesCache;
    private final List<String> myFragmentsDirectories;
    private final String myFragmentsFileExtension;

    public CachingGraphQLQueryLoader(Function<String, @Nullable InputStream> getFileStream) {
        this(withSharedFragments(getFileStream), createFragmentCache(), createQueryCache(), List.of("graphql/fragment"), "graphql");
    }

    private static Function<String, @Nullable InputStream> withSharedFragments(Function<String, @Nullable InputStream> getFileStream) {
        return path -> {
            InputStream stream = getFileStream.apply(path);
            return stream != null ? stream : CachingGraphQLQueryLoader.class.getResourceAsStream("/" + path);
        };
    }

    public CachingGraphQLQueryLoader(Function<String, @Nullable InputStream> getFileStream,
                                     ConcurrentMap<String, GraphQLBlock> fragmentsCache,
                                     ConcurrentMap<String, String> queriesCache,
                                     List<String> fragmentsDirectories,
                                     String fragmentsFileExtension) {
        myGetFileStream = getFileStream;
        myFragmentsCache = fragmentsCache;
        myQueriesCache = queriesCache;
        myFragmentsDirectories = List.copyOf(fragmentsDirectories);
        myFragmentsFileExtension = fragmentsFileExtension;
    }

    @Override
    public String loadQuery(String queryPath) throws IOException {
        InputStream stream = myGetFileStream.apply(queryPath);
        if (stream == null) {
            throw new GraphQLFileNotFoundException("Couldn't find query file at " + queryPath);
        }
        try (stream) {
            return loadQuery(stream, queryPath);
        }
    }

    private String loadQuery(InputStream stream, String key) throws IOException {
        try {
            return myQueriesCache.computeIfAbsent(key, k -> {
                try {
                    return loadQuery(stream);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Doesn't cache the resulting query.
     */
    public String loadQuery(InputStream stream) throws IOException {
        GraphQLBlock block = readBlock(stream);
        StringBuilder builder = new StringBuilder();
        LinkedHashMap<String, GraphQLBlock> fragments = new LinkedHashMap<>();
        readFragmentsInto(block.dependencies(), fragments);
        List<GraphQLBlock> ordered = new ArrayList<>(fragments.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            builder.append(ordered.get(i).body()).append("\n");
        }
        builder.append(block.body());
        return builder.toString();
    }

    private void readFragmentsInto(Set<String> names, Map<String, GraphQLBlock> into) throws IOException {
        for (String fragmentName : names) {
            GraphQLBlock fragment = null;
            for (String directory : myFragmentsDirectories) {
                String path = directory + "/" + fragmentName + "." + myFragmentsFileExtension;
                fragment = myFragmentsCache.get(path);
                if (fragment == null) {
                    InputStream stream = myGetFileStream.apply(path);
                    if (stream != null) {
                        try (stream) {
                            fragment = readBlock(stream);
                        }
                        myFragmentsCache.put(path, fragment);
                    }
                }
                if (fragment != null) {
                    break;
                }
            }
            if (fragment == null) {
                throw new GraphQLFileNotFoundException("Couldn't find file for fragment " + fragmentName);
            }
            into.put(fragmentName, fragment);
            Set<String> nonProcessedDependencies = new LinkedHashSet<>();
            for (String dependency : fragment.dependencies()) {
                if (!into.containsKey(dependency)) {
                    nonProcessedDependencies.add(dependency);
                }
            }
            readFragmentsInto(nonProcessedDependencies, into);
        }
    }

    private GraphQLBlock readBlock(InputStream stream) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        Set<String> fragments = new LinkedHashSet<>();
        Set<String> innerFragments = new LinkedHashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            String line = rawLine.trim();
            bodyBuilder.append(line).append("\n");
            if (line.startsWith("fragment")) {
                Matcher matcher = FRAGMENT_DEFINITION.matcher(line);
                if (matcher.matches()) {
                    innerFragments.add(matcher.group(1).trim());
                }
            }
            if (line.startsWith("...") && line.length() > 3 && !Character.isWhitespace(line.charAt(3))) {
                fragments.add(line.substring(3));
            }
        }
        fragments.removeAll(innerFragments);
        String body = bodyBuilder.toString();
        if (body.endsWith("\n")) {
            body = body.substring(0, body.length() - 1);
        }
        return new GraphQLBlock(body, fragments);
    }

    public static ConcurrentMap<String, GraphQLBlock> createFragmentCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(2)).<String, GraphQLBlock>build().asMap();
    }

    public static ConcurrentMap<String, String> createQueryCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(1)).<String, String>build().asMap();
    }
}
