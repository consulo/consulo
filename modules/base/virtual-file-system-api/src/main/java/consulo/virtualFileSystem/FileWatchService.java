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
package consulo.virtualFileSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Watching files for changes - of this machine, or of other ones (a root is watched by the watcher of the file system of its
 * {@link Path}). A changed file is refreshed in the virtual file system it belongs to.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface FileWatchService {
    static FileWatchService getInstance() {
        return Application.get().getInstance(FileWatchService.class);
    }

    interface WatchRequest {
        Path getRoot();

        boolean isToWatchRecursively();
    }

    default @Nullable WatchRequest addRootToWatch(Path root, boolean watchRecursively) {
        Set<WatchRequest> result = addRootsToWatch(Collections.singleton(root), watchRecursively);
        return result.size() == 1 ? result.iterator().next() : null;
    }

    default Set<WatchRequest> addRootsToWatch(Collection<Path> roots, boolean watchRecursively) {
        if (roots.isEmpty()) {
            return Collections.emptySet();
        }
        return watchRecursively ? replaceWatchedRoots(Collections.emptySet(), roots, null) : replaceWatchedRoots(Collections.emptySet(), null, roots);
    }

    default void removeWatchedRoot(WatchRequest watchRequest) {
        removeWatchedRoots(Collections.singleton(watchRequest));
    }

    default void removeWatchedRoots(Collection<WatchRequest> watchRequests) {
        if (!watchRequests.isEmpty()) {
            replaceWatchedRoots(watchRequests, null, null);
        }
    }

    default @Nullable WatchRequest replaceWatchedRoot(@Nullable WatchRequest watchRequest, Path root, boolean watchRecursively) {
        Set<WatchRequest> requests = watchRequest != null ? Collections.singleton(watchRequest) : Collections.emptySet();
        Set<Path> roots = Collections.singleton(root);
        Set<WatchRequest> result = watchRecursively ? replaceWatchedRoots(requests, roots, null) : replaceWatchedRoots(requests, null, roots);
        return result.size() == 1 ? result.iterator().next() : null;
    }

    /**
     * Stops watching given watch requests and starts watching new roots.
     * May do nothing and return the same set of requests when it contains exactly the same roots.
     */
    Set<WatchRequest> replaceWatchedRoots(Collection<WatchRequest> watchRequests,
                                          @Nullable Collection<Path> recursiveRoots,
                                          @Nullable Collection<Path> flatRoots);
}
