// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.icon;

import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @param <T> icon key type
 */
public class CachingIconsProvider<T> implements IconsProvider<T> {
    private record CachedIcon(Image icon, long lastAccess) {
    }

    private final IconsProvider<T> myDelegate;
    private final @Nullable Integer myMaxSize;
    private final long myExpiresAfterMillis;

    private final Map<Pair<@Nullable T, Integer>, CachedIcon> myIconsCache = new LinkedHashMap<>(16, 0.75f, true);

    public CachingIconsProvider(IconsProvider<T> delegate) {
        this(delegate, customizer -> {
        });
    }

    public CachingIconsProvider(IconsProvider<T> delegate, Consumer<CacheCustomizer> customizeCache) {
        myDelegate = delegate;

        CacheCustomizer customizer = new CacheCustomizer();
        customizeCache.accept(customizer);
        myMaxSize = customizer.getMaxSize();
        Integer expiresAfterMinutes = customizer.getExpiresAfterMinutes();
        myExpiresAfterMillis = expiresAfterMinutes == null ? -1 : TimeUnit.MINUTES.toMillis(expiresAfterMinutes);
    }

    @Override
    public Image getIcon(@Nullable T key, int iconSize) {
        Pair<@Nullable T, Integer> cacheKey = Pair.create(key, iconSize);
        long now = System.currentTimeMillis();
        synchronized (myIconsCache) {
            removeExpired(now);

            CachedIcon cached = myIconsCache.get(cacheKey);
            if (cached != null) {
                myIconsCache.put(cacheKey, new CachedIcon(cached.icon(), now));
                return cached.icon();
            }
        }

        Image icon = myDelegate.getIcon(key, iconSize);
        synchronized (myIconsCache) {
            CachedIcon cached = myIconsCache.get(cacheKey);
            if (cached != null) {
                return cached.icon();
            }
            myIconsCache.put(cacheKey, new CachedIcon(icon, now));
            if (myMaxSize != null) {
                Iterator<Pair<@Nullable T, Integer>> iterator = myIconsCache.keySet().iterator();
                while (myIconsCache.size() > myMaxSize && iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
        return icon;
    }

    public void invalidateAll() {
        synchronized (myIconsCache) {
            myIconsCache.clear();
        }
    }

    public void cleanUp() {
        synchronized (myIconsCache) {
            removeExpired(System.currentTimeMillis());
        }
    }

    private void removeExpired(long now) {
        if (myExpiresAfterMillis < 0) {
            return;
        }
        myIconsCache.values().removeIf(cached -> now - cached.lastAccess() > myExpiresAfterMillis);
    }
}
