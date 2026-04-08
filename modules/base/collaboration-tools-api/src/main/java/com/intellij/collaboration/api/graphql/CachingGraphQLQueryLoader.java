// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.graphql;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
@ApiStatus.Internal
public final class CachingGraphQLQueryLoader implements GraphQLQueryLoader {
    private final @Nonnull Function<String, InputStream> getFileStream;
    private final @Nonnull ConcurrentMap<String, Block> fragmentsCache;
    private final @Nonnull ConcurrentMap<String, String> queriesCache;
    private final @Nonnull List<String> fragmentsDirectories;
    private final @Nonnull String fragmentsFileExtension;

    private final Pattern fragmentDefinitionRegex = Pattern.compile("fragment (.*) on .*\\{");

    public CachingGraphQLQueryLoader(@Nonnull Function<String, InputStream> getFileStream) {
        this(getFileStream, createFragmentCache(), createQueryCache(), List.of("graphql/fragment"), "graphql");
    }

    public CachingGraphQLQueryLoader(
        @Nonnull Function<String, InputStream> getFileStream,
        @Nonnull ConcurrentMap<String, Block> fragmentsCache,
        @Nonnull ConcurrentMap<String, String> queriesCache,
        @Nonnull List<String> fragmentsDirectories,
        @Nonnull String fragmentsFileExtension
    ) {
        this.getFileStream = getFileStream;
        this.fragmentsCache = fragmentsCache;
        this.queriesCache = queriesCache;
        this.fragmentsDirectories = fragmentsDirectories;
        this.fragmentsFileExtension = fragmentsFileExtension;
    }

    @Override
    public @Nonnull String loadQuery(@Nonnull String queryPath) throws IOException {
        InputStream stream = getFileStream.apply(queryPath);
        if (stream == null) {
            throw new GraphQLFileNotFoundException("Couldn't find query file at " + queryPath);
        }
        try (stream) {
            return loadQuery(stream, queryPath);
        }
    }

    private @Nonnull String loadQuery(@Nonnull InputStream stream, @Nonnull String key) throws IOException {
        // Can't use computeIfAbsent with checked exceptions directly
        String cached = queriesCache.get(key);
        if (cached != null) {
            return cached;
        }
        String result = loadQuery(stream);
        queriesCache.putIfAbsent(key, result);
        return queriesCache.get(key);
    }

    /**
     * Doesn't cache the resulting query.
     */
    public @Nonnull String loadQuery(@Nonnull InputStream stream) throws IOException {
        Block block = readBlock(stream);

        StringBuilder builder = new StringBuilder();
        LinkedHashMap<String, Block> fragments = new LinkedHashMap<>();
        readFragmentsInto(block.dependencies(), fragments);
        List<Block> fragmentValues = new ArrayList<>(fragments.values());
        Collections.reverse(fragmentValues);
        for (Block fragment : fragmentValues) {
            builder.append(fragment.body()).append("\n");
        }

        builder.append(block.body());
        return builder.toString();
    }

    private void readFragmentsInto(@Nonnull Set<String> names, @Nonnull Map<String, Block> into) {
        for (String fragmentName : names) {
            Block fragment = null;
            for (String dir : fragmentsDirectories) {
                String path = dir + "/" + fragmentName + "." + fragmentsFileExtension;
                fragment = fragmentsCache.computeIfAbsent(path, p -> {
                    InputStream stream = getFileStream.apply(p);
                    if (stream == null) {
                        return null;
                    }
                    try (stream) {
                        return readBlock(stream);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                if (fragment != null) {
                    break;
                }
            }
            if (fragment == null) {
                throw new RuntimeException(new GraphQLFileNotFoundException("Couldn't find file for fragment " + fragmentName));
            }
            into.put(fragmentName, fragment);

            Set<String> nonProcessedDependencies = new LinkedHashSet<>();
            for (String dep : fragment.dependencies()) {
                if (!into.containsKey(dep)) {
                    nonProcessedDependencies.add(dep);
                }
            }
            readFragmentsInto(nonProcessedDependencies, into);
        }
    }

    private @Nonnull Block readBlock(@Nonnull InputStream stream) {
        StringBuilder bodyBuilder = new StringBuilder();
        Set<String> fragments = new LinkedHashSet<>();
        Set<String> innerFragments = new LinkedHashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        reader.lines().forEach(rawLine -> {
            String line = rawLine.trim();
            bodyBuilder.append(line).append("\n");

            if (line.startsWith("fragment")) {
                Matcher matcher = fragmentDefinitionRegex.matcher(line);
                if (matcher.matches()) {
                    String fragmentName = matcher.group(1).trim();
                    innerFragments.add(fragmentName);
                }
            }

            if (line.startsWith("...") && line.length() > 3 && !Character.isWhitespace(line.charAt(3))) {
                String fragmentName = line.substring(3);
                fragments.add(fragmentName);
            }
        });
        fragments.removeAll(innerFragments);
        String body = bodyBuilder.toString();
        if (body.endsWith("\n")) {
            body = body.substring(0, body.length() - 1);
        }
        return new Block(body, fragments);
    }

    /**
     * @param body         The body of the query to be sent to the server.
     * @param dependencies The set of fragments that the query depends on listed by name.
     */
    public record Block(@Nonnull String body, @Nonnull Set<String> dependencies) {
    }

    public static @Nonnull ConcurrentMap<String, Block> createFragmentCache() {
        return Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(2, ChronoUnit.MINUTES))
            .<String, Block>build()
            .asMap();
    }

    public static @Nonnull ConcurrentMap<String, String> createQueryCache() {
        return Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(1, ChronoUnit.MINUTES))
            .<String, String>build()
            .asMap();
    }
}
