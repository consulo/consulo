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
package consulo.it.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.impl.internal.local.FileWatcherNotificationSink;
import consulo.virtualFileSystem.impl.internal.local.PluggableFileWatcher;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An in-process stand-in for the native file watcher. Off by default, so the other tests keep running without a watcher;
 * a test which enables it gets an operational watcher whose notifications it sends itself, the way the native process
 * reports them, straight into the platform's notification sink.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class HeadlessFileWatcher extends PluggableFileWatcher {
    private static volatile boolean ourEnabled;
    private static volatile @Nullable FileWatcherNotificationSink ourSink;
    private static volatile List<String> ourRecursiveRoots = List.of();
    private static volatile List<String> ourFlatRoots = List.of();

    /**
     * Makes the watcher operational. The roots registered after this call are watched.
     */
    public static void enable() {
        ourEnabled = true;
    }

    public static void disable() {
        ourEnabled = false;
        ourRecursiveRoots = List.of();
        ourFlatRoots = List.of();
    }

    public static List<String> getRecursiveRoots() {
        return ourRecursiveRoots;
    }

    public static List<String> getFlatRoots() {
        return ourFlatRoots;
    }

    /**
     * The content or the attributes of the file at the path changed.
     */
    public static void notifyDirtyPath(String path) {
        sink().notifyDirtyPath(path);
    }

    /**
     * A file was created or deleted at the path.
     */
    public static void notifyPathCreatedOrDeleted(String path) {
        sink().notifyPathCreatedOrDeleted(path);
    }

    public static void notifyDirtyDirectory(String path) {
        sink().notifyDirtyDirectory(path);
    }

    public static void notifyDirtyPathRecursive(String path) {
        sink().notifyDirtyPathRecursive(path);
    }

    /**
     * The watcher lost events (e.g. the event queue overflowed) and everything under the path, or every root, may be stale.
     */
    public static void notifyReset(@Nullable String path) {
        sink().notifyReset(path);
    }

    private static FileWatcherNotificationSink sink() {
        FileWatcherNotificationSink sink = ourSink;
        if (sink == null || !ourEnabled) {
            throw new IllegalStateException("The headless file watcher is not enabled");
        }
        return sink;
    }

    @Override
    public void initialize(ManagingFS managingFS, FileWatcherNotificationSink notificationSink) {
        ourSink = notificationSink;
    }

    @Override
    public void dispose() {
        ourSink = null;
    }

    @Override
    public boolean isOperational() {
        return ourEnabled && ourSink != null;
    }

    @Override
    public boolean isSettingRoots() {
        return false;
    }

    @Override
    public void setWatchRoots(List<String> recursive, List<String> flat) {
        if (!isOperational()) {
            return;
        }
        ourRecursiveRoots = List.copyOf(recursive);
        ourFlatRoots = List.copyOf(flat);
        // every root is watchable
        sink().notifyManualWatchRoots(List.of());
    }

    @Override
    public void startup() {
    }

    @Override
    public void shutdown() {
    }
}
