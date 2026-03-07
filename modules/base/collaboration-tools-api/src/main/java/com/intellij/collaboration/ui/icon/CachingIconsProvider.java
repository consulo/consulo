// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.icon;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import kotlin.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class CachingIconsProvider<T> implements IconsProvider<T> {
    private static final int DEFAULT_MAX_SIZE = 500;

    private final IconsProvider<T> myDelegate;
    private final LoadingCache<Pair<T, Integer>, Icon> myIconsCache;

    public CachingIconsProvider(@Nonnull IconsProvider<T> delegate) {
        this(delegate, c -> {
        });
    }

    public CachingIconsProvider(@Nonnull IconsProvider<T> delegate, @Nonnull Consumer<CacheCustomizer> customizeCache) {
        myDelegate = delegate;
        CacheCustomizer customizer = new CacheCustomizer();
        customizeCache.accept(customizer);

        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (customizer.maxSize != null) {
            builder.maximumSize(customizer.maxSize.longValue());
        }
        if (customizer.expiresAfterMinutes != null) {
            builder.expireAfterAccess(customizer.expiresAfterMinutes.longValue(), TimeUnit.MINUTES);
        }
        @SuppressWarnings("unchecked")
        LoadingCache<Pair<T, Integer>, Icon> cache = (LoadingCache<Pair<T, Integer>, Icon>) (LoadingCache<?, ?>) builder.build(
            key -> {
                @SuppressWarnings("unchecked")
                Pair<T, Integer> typedKey = (Pair<T, Integer>) key;
                return myDelegate.getIcon(typedKey.getFirst(), typedKey.getSecond());
            }
        );
        myIconsCache = cache;
    }

    @Override
    public @Nonnull Icon getIcon(@Nullable T key, int iconSize) {
        return myIconsCache.get(new Pair<>(key, iconSize));
    }

    public void invalidateAll() {
        myIconsCache.invalidateAll();
    }

    public void cleanUp() {
        myIconsCache.cleanUp();
    }

    public static final class CacheCustomizer {
        public @Nullable Integer maxSize = DEFAULT_MAX_SIZE;
        public @Nullable Integer expiresAfterMinutes = 5;
    }
}
