// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.ignore.impl.internal.cache;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.language.ignore.cache.PatternCache;
import consulo.language.ignore.util.RegexUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListenerBackgroundable;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Cache that retrieves matching files using given {@link Pattern}.
 * Cache population happened on demand in the background.
 * The cache eviction happen in the following cases:
 * <ul>
 * <li>by using {@link BulkFileListenerBackgroundable} to handle filesystem changes and clean cache if needed for the specific pattern parts.</li>
 * <li>after entries have been expired: entries becomes expired if no read/write operations happened with the corresponding key during some amount of time (10 minutes).</li>
 * <li>after project dispose</li>
 * </ul>
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class IgnorePatternsMatchedFilesCache implements Disposable {
    private static final long EXPIRE_AFTER_ACCESS = TimeUnit.MINUTES.toMillis(10);

    private static final long UPDATE_DELAY = 500;

    private record CacheEntry(Collection<VirtualFile> files, long accessTime) {
    }

    private final Project myProject;

    private final ProjectFileIndex myProjectFileIndex;

    private final ConcurrentMap<String, CacheEntry> myCache = new ConcurrentHashMap<>();

    private final Object myUpdateLock = new Object();

    private @Nullable ScheduledFuture<?> myScheduledUpdate;

    private @Nullable String myPendingKey;

    private @Nullable Pattern myPendingPattern;

    @Inject
    public IgnorePatternsMatchedFilesCache(Project project, ProjectFileIndex projectFileIndex) {
        myProject = project;
        myProjectFileIndex = projectFileIndex;

        project.getApplication().getMessageBus().connect(this).subscribe(
            BulkFileListenerBackgroundable.class,
            new BulkFileListenerBackgroundable() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                if (myCache.isEmpty()) {
                    return;
                }

                for (VFileEvent event : events) {
                    if (event instanceof VFileCreateEvent ||
                        event instanceof VFileDeleteEvent ||
                        event instanceof VFileCopyEvent) {
                        cleanupCache(event.getPath());
                    }
                    else if (event instanceof VFilePropertyChangeEvent propertyChangeEvent && propertyChangeEvent.isRename()) {
                        cleanupCache(propertyChangeEvent.getOldPath());
                        cleanupCache(event.getPath());
                    }
                    else if (event instanceof VFileMoveEvent moveEvent) {
                        cleanupCache(moveEvent.getOldPath());
                        cleanupCache(event.getPath());
                    }
                }
            }

            private void cleanupCache(String path) {
                PatternCache globCache = PatternCache.getInstance(myProject);
                for (String key : myCache.keySet()) {
                    Pattern pattern = globCache.getPattern(key);
                    if (pattern == null) {
                        continue;
                    }
                    String[] parts = RegexUtil.getParts(pattern);
                    if (RegexUtil.matchAnyPart(parts, path)) {
                        myCache.remove(key);
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        synchronized (myUpdateLock) {
            if (myScheduledUpdate != null) {
                myScheduledUpdate.cancel(false);
                myScheduledUpdate = null;
            }
        }
        myCache.clear();
    }

    /**
     * Finds {@link VirtualFile} instances in project for the specific {@link Pattern} and caches them.
     *
     * @param pattern to match
     * @return matched files list
     */
    public Collection<VirtualFile> getFilesForPattern(Pattern pattern) {
        String key = pattern.toString();
        Collection<VirtualFile> files = getIfPresent(key);
        if (files.isEmpty()) {
            runSearchRequest(key, pattern);
        }
        return files;
    }

    private Collection<VirtualFile> getIfPresent(String key) {
        long now = System.currentTimeMillis();
        expireEntries(now);

        CacheEntry entry = myCache.computeIfPresent(key, (k, value) -> new CacheEntry(value.files(), now));
        return entry != null ? entry.files() : List.of();
    }

    private void expireEntries(long now) {
        for (Map.Entry<String, CacheEntry> entry : myCache.entrySet()) {
            if (now - entry.getValue().accessTime() > EXPIRE_AFTER_ACCESS) {
                myCache.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private void runSearchRequest(String key, Pattern pattern) {
        synchronized (myUpdateLock) {
            myPendingKey = key;
            myPendingPattern = pattern;

            if (myScheduledUpdate != null) {
                myScheduledUpdate.cancel(false);
            }
            myScheduledUpdate = AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(this::runLatest, UPDATE_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    private void runLatest() {
        String key;
        Pattern pattern;
        synchronized (myUpdateLock) {
            key = myPendingKey;
            pattern = myPendingPattern;
            myPendingKey = null;
            myPendingPattern = null;
            myScheduledUpdate = null;
        }

        if (key == null || pattern == null || myProject.isDisposed()) {
            return;
        }

        myCache.put(key, new CacheEntry(doSearch(pattern), System.currentTimeMillis()));
    }

    private Set<VirtualFile> doSearch(Pattern pattern) {
        Set<VirtualFile> files = new HashSet<>(1000);
        String[] parts = RegexUtil.getParts(pattern);
        if (parts.length == 0) {
            return files;
        }

        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(myProject);
        myProjectFileIndex.iterateContent(fileOrDir -> {
            ProgressManager.checkCanceled();
            String name = fileOrDir.getName();
            if (RegexUtil.matchAnyPart(parts, name)) {
                Supplier<Collection<VirtualFile>> search = () -> FilenameIndex.getVirtualFilesByName(myProject, name, projectScope);
                Collection<VirtualFile> filesByName = myProject.getApplication().runReadAction(search);
                for (VirtualFile file : filesByName) {
                    if (file.isValid() && RegexUtil.matchAllParts(parts, file.getPath())) {
                        files.add(file);
                    }
                }
            }
            return true;
        });
        return files;
    }

    public static IgnorePatternsMatchedFilesCache getInstance(Project project) {
        return project.getInstance(IgnorePatternsMatchedFilesCache.class);
    }
}
