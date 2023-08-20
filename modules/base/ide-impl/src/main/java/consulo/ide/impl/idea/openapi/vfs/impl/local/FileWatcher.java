// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.impl.local;

import consulo.application.Application;
import consulo.application.ApplicationBundle;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.impl.internal.local.FileWatcherNotificationSink;
import consulo.virtualFileSystem.impl.internal.local.PluggableFileWatcher;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author max
 */
public class FileWatcher {
  private static final Logger LOG = Logger.getInstance(FileWatcher.class);

  public static final NotificationGroup NOTIFICATION_GROUP =
    new NotificationGroup("File Watcher Messages", NotificationDisplayType.STICKY_BALLOON, true);

  static class DirtyPaths {
    final Set<String> dirtyPaths = new HashSet<>();
    final Set<String> dirtyPathsRecursive = new HashSet<>();
    final Set<String> dirtyDirectories = new HashSet<>();

    static final DirtyPaths EMPTY = new DirtyPaths();

    boolean isEmpty() {
      return dirtyPaths.isEmpty() && dirtyPathsRecursive.isEmpty() && dirtyDirectories.isEmpty();
    }

    private void addDirtyPath(@Nonnull String path) {
      if (!dirtyPathsRecursive.contains(path)) {
        dirtyPaths.add(path);
      }
    }

    private void addDirtyPathRecursive(@Nonnull String path) {
      dirtyPaths.remove(path);
      dirtyPathsRecursive.add(path);
    }
  }

  private final ManagingFS myManagingFS;
  private final MyFileWatcherNotificationSink myNotificationSink;
  private final PluggableFileWatcher[] myWatchers;
  private final AtomicBoolean myFailureShown = new AtomicBoolean(false);
  private final ExecutorService myFileWatcherExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("File Watcher", 1);
  private final AtomicReference<Future<?>> myLastTask = new AtomicReference<>(null);

  private volatile CanonicalPathMap myPathMap = new CanonicalPathMap();
  private volatile List<Collection<String>> myManualWatchRoots = Collections.emptyList();

  FileWatcher(@Nonnull ManagingFS managingFS) {
    myManagingFS = managingFS;
    myNotificationSink = new MyFileWatcherNotificationSink();
    myWatchers =
      new PluggableFileWatcher[]{new NativeFileWatcherImpl()}; //FIXME [VISTALL] this is dirty hack, due we don't allow change file watcher

    myFileWatcherExecutor.execute(() -> {
      try {
        for (PluggableFileWatcher watcher : myWatchers) {
          watcher.initialize(myManagingFS, myNotificationSink);
        }
      }
      catch (RuntimeException | Error e) {
        LOG.error(e);
      }
    });
  }

  public void dispose() {
    myFileWatcherExecutor.shutdown();

    Future<?> lastTask = myLastTask.get();
    if (lastTask != null) {
      lastTask.cancel(false);
    }

    try {
      myFileWatcherExecutor.awaitTermination(1, TimeUnit.HOURS);
    }
    catch (InterruptedException e) {
      LOG.error(e);
    }

    for (PluggableFileWatcher watcher : myWatchers) {
      watcher.dispose();
    }
  }

  public boolean isOperational() {
    for (PluggableFileWatcher watcher : myWatchers) {
      if (watcher.isOperational()) return true;
    }
    return false;
  }

  public boolean isSettingRoots() {
    Future<?> lastTask = myLastTask.get();  // a new task may come after the read, but this seem to be an acceptable race
    if (lastTask != null && !lastTask.isDone()) {
      return true;
    }
    for (PluggableFileWatcher watcher : myWatchers) {
      if (watcher.isSettingRoots()) return true;
    }
    return false;
  }

  @Nonnull
  DirtyPaths getDirtyPaths() {
    return myNotificationSink.getDirtyPaths();
  }

  @Nonnull
  public Collection<String> getManualWatchRoots() {
    List<Collection<String>> manualWatchRoots = myManualWatchRoots;

    Set<String> result = null;
    for (Collection<String> roots : manualWatchRoots) {
      if (result == null) {
        result = new HashSet<>(roots);
      }
      else {
        result.retainAll(roots);
      }
    }

    return result != null ? result : Collections.emptyList();
  }

  /**
   * Clients should take care of not calling this method in parallel.
   */
  void setWatchRoots(@Nonnull List<String> recursive, @Nonnull List<String> flat) {
    Future<?> prevTask = myLastTask.getAndSet(myFileWatcherExecutor.submit(() -> {
      try {
        CanonicalPathMap pathMap = new CanonicalPathMap(recursive, flat);

        myPathMap = pathMap;
        myManualWatchRoots = Lists.newLockFreeCopyOnWriteList();

        for (PluggableFileWatcher watcher : myWatchers) {
          watcher.setWatchRoots(pathMap.getCanonicalRecursiveWatchRoots(), pathMap.getCanonicalFlatWatchRoots());
        }
      }
      catch (RuntimeException | Error e) {
        LOG.error(e);
      }
    }));
    if (prevTask != null) {
      prevTask.cancel(false);
    }
  }

  public void notifyOnFailure(@Nonnull String cause) {
    LOG.warn(cause);

    if (myFailureShown.compareAndSet(false, true)) {
      String title = ApplicationBundle.message("watcher.slow.sync");
      Application application = Application.get();
      application.invokeLater(() -> {
        Notifications.Bus.notify(NOTIFICATION_GROUP.createNotification(title,
                                                                       cause,
                                                                       NotificationType.WARNING,
                                                                       NotificationListener.URL_OPENING_LISTENER));
      }, application.getNoneModalityState());
    }
  }

  private class MyFileWatcherNotificationSink implements FileWatcherNotificationSink {
    private final Object myLock = new Object();
    private DirtyPaths myDirtyPaths = new DirtyPaths();

    @Nonnull
    DirtyPaths getDirtyPaths() {
      DirtyPaths dirtyPaths = DirtyPaths.EMPTY;

      synchronized (myLock) {
        if (!myDirtyPaths.isEmpty()) {
          dirtyPaths = myDirtyPaths;
          myDirtyPaths = new DirtyPaths();
        }
      }

      for (PluggableFileWatcher watcher : myWatchers) {
        watcher.resetChangedPaths();
      }

      return dirtyPaths;
    }

    @Override
    public void notifyManualWatchRoots(@Nonnull Collection<String> roots) {
      myManualWatchRoots.add(roots.isEmpty() ? Collections.emptySet() : new HashSet<>(roots));
      notifyOnEvent(OTHER);
    }

    @Override
    public void notifyMapping(@Nonnull Collection<? extends Pair<String, String>> mapping) {
      if (!mapping.isEmpty()) {
        myPathMap.addMapping(mapping);
      }
      notifyOnEvent(OTHER);
    }

    @Override
    public void notifyDirtyPath(@Nonnull String path) {
      Collection<String> paths = myPathMap.getWatchedPaths(path, true);
      if (!paths.isEmpty()) {
        synchronized (myLock) {
          for (String eachPath : paths) {
            myDirtyPaths.addDirtyPath(eachPath);
          }
        }
      }
      notifyOnEvent(path);
    }

    @Override
    public void notifyPathCreatedOrDeleted(@Nonnull String path) {
      Collection<String> paths = myPathMap.getWatchedPaths(path, true);
      if (!paths.isEmpty()) {
        synchronized (myLock) {
          for (String p : paths) {
            myDirtyPaths.addDirtyPathRecursive(p);
            String parentPath = new File(p).getParent();
            if (parentPath != null) {
              myDirtyPaths.addDirtyPath(parentPath);
            }
          }
        }
      }
      notifyOnEvent(path);
    }

    @Override
    public void notifyDirtyDirectory(@Nonnull String path) {
      Collection<String> paths = myPathMap.getWatchedPaths(path, false);
      if (!paths.isEmpty()) {
        synchronized (myLock) {
          myDirtyPaths.dirtyDirectories.addAll(paths);
        }
      }
      notifyOnEvent(path);
    }

    @Override
    public void notifyDirtyPathRecursive(@Nonnull String path) {
      Collection<String> paths = myPathMap.getWatchedPaths(path, false);
      if (!paths.isEmpty()) {
        synchronized (myLock) {
          for (String each : paths) {
            myDirtyPaths.addDirtyPathRecursive(each);
          }
        }
      }
      notifyOnEvent(path);
    }

    @Override
    public void notifyReset(@Nullable String path) {
      if (path != null) {
        synchronized (myLock) {
          myDirtyPaths.addDirtyPathRecursive(path);
        }
      }
      else {
        VirtualFile[] roots = myManagingFS.getLocalRoots();
        synchronized (myLock) {
          for (VirtualFile root : roots) {
            myDirtyPaths.addDirtyPathRecursive(root.getPresentableUrl());
          }
        }
      }
      notifyOnEvent(RESET);
    }

    @Override
    public void notifyUserOnFailure(@Nonnull String cause) {
      notifyOnFailure(cause);
    }
  }

  //<editor-fold desc="Test stuff.">
  public static final String RESET = "(reset)";
  public static final String OTHER = "(other)";

  private volatile Consumer<? super String> myTestNotifier;

  private void notifyOnEvent(String path) {
    Consumer<? super String> notifier = myTestNotifier;
    if (notifier != null) notifier.accept(path);
  }

  @TestOnly
  public void startup(@Nullable Consumer<? super String> notifier) throws Exception {
    myTestNotifier = notifier;
    myFileWatcherExecutor.submit(() -> {
      for (PluggableFileWatcher watcher : myWatchers) {
        watcher.startup();
      }
      return null;
    }).get();
  }

  @TestOnly
  public void shutdown() throws Exception {
    myFileWatcherExecutor.submit(() -> {
      for (PluggableFileWatcher watcher : myWatchers) {
        watcher.shutdown();
      }
      myTestNotifier = null;
      return null;
    }).get();
  }
  //</editor-fold>
}