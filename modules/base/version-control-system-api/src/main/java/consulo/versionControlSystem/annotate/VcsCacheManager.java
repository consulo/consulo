// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.annotate;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.project.Project;
import consulo.versionControlSystem.PluginVcsMappingListener;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.change.ContentRevisionCache;
import consulo.versionControlSystem.history.VcsHistoryCache;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-level service holding the shared {@link VcsHistoryCache} and {@link ContentRevisionCache}.
 * <p>
 * Ported from JetBrains IntelliJ Community {@code VcsCacheManager.java}.
 * Both caches are automatically cleared when the VCS configuration changes.
 * <p>
 * In addition to the JB caches, this service maintains a simple
 * {@link VirtualFile}-keyed in-memory annotation map used by
 * {@link consulo.versionControlSystem.impl.internal.annotate.AnnotationsPreloader}
 * as a fallback when the VCS annotation provider does not yet implement
 * {@link CacheableAnnotationProvider} (e.g. while the git plugin builds against an older snapshot).
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class VcsCacheManager {
    private final VcsHistoryCache myVcsHistoryCache;
    private final ContentRevisionCache myContentRevisionCache;

    /**
     * Fallback in-memory cache keyed by {@link VirtualFile}. Used by the preloader when the VCS
     * annotation provider does not implement {@link CacheableAnnotationProvider} directly.
     * Uses {@link SoftReference} so entries can be reclaimed under memory pressure.
     */
    private final ConcurrentHashMap<VirtualFile, SoftReference<FileAnnotation>> mySimpleAnnotationCache =
        new ConcurrentHashMap<>();

    @Inject
    public VcsCacheManager(Project project) {
        myVcsHistoryCache = new VcsHistoryCache();
        myContentRevisionCache = new ContentRevisionCache();

        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(VcsMappingListener.class, new VcsMappingListener() {
            @Override
            public void directoryMappingChanged() {
                clearAll();
            }
        });
        connection.subscribe(PluginVcsMappingListener.class, new PluginVcsMappingListener() {
            @Override
            public void directoryMappingChanged() {
                clearAll();
            }
        });
    }

    public static VcsCacheManager getInstance(Project project) {
        return project.getInstance(VcsCacheManager.class);
    }

    /** Returns the shared {@link VcsHistoryCache} for this project. */
    public VcsHistoryCache getVcsHistoryCache() {
        return myVcsHistoryCache;
    }

    /** Returns the shared {@link ContentRevisionCache} for this project. */
    public ContentRevisionCache getContentRevisionCache() {
        return myContentRevisionCache;
    }

    // -------------------------------------------------------------------------
    // Simple VirtualFile-based annotation cache (used by AnnotationsPreloader)
    // -------------------------------------------------------------------------

    /** Stores an annotation in the simple per-file cache. */
    public void cacheAnnotation(VirtualFile file, FileAnnotation annotation) {
        mySimpleAnnotationCache.put(file, new SoftReference<>(annotation));
    }

    /**
     * Returns the cached annotation for the given file, or {@code null} if absent
     * or garbage-collected.
     */
    public @Nullable FileAnnotation getCachedAnnotation(VirtualFile file) {
        SoftReference<FileAnnotation> ref = mySimpleAnnotationCache.get(file);
        return ref != null ? ref.get() : null;
    }

    /** Returns {@code true} if a non-null annotation is currently cached for the file. */
    public boolean isCached(VirtualFile file) {
        return getCachedAnnotation(file) != null;
    }

    /** Removes the cached annotation for the given file. */
    public void clearCachedAnnotation(VirtualFile file) {
        mySimpleAnnotationCache.remove(file);
    }

    // -------------------------------------------------------------------------
    // Cache clearing
    // -------------------------------------------------------------------------

    private void clearAll() {
        myVcsHistoryCache.clearAll();
        myContentRevisionCache.clearAll();
        mySimpleAnnotationCache.clear();
    }
}
