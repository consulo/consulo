// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.newvfs;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.component.extension.ExtensionPointName;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.ide.impl.idea.openapi.progress.util.PingProgress;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.ide.impl.idea.openapi.vfs.impl.VirtualFileManagerImpl;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.ide.impl.idea.openapi.vfs.newvfs.persistent.PersistentFS;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

//@ApiStatus.Internal
public final class AsyncEventSupport {
  private static final Logger LOG = Logger.getInstance(AsyncEventSupport.class);

  //@ApiStatus.Internal
  public static final ExtensionPointName<AsyncFileListener> EP_NAME = ExtensionPointName.create(AsyncFileListener.class);
  private static boolean ourSuppressAppliers;

  public static void startListening() {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(BulkFileListener.class, new BulkFileListener() {
      Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> appliersFromBefore;

      @Override
      public void before(@Nonnull List<? extends VFileEvent> events) {
        if (ourSuppressAppliers) return;
        List<AsyncFileListener.ChangeApplier> appliers = runAsyncListeners(events);
        appliersFromBefore = Pair.create(events, appliers);
        beforeVfsChange(appliers);
      }

      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        if (ourSuppressAppliers) return;
        List<AsyncFileListener.ChangeApplier> appliers = appliersFromBefore != null && appliersFromBefore.first.equals(events) ? appliersFromBefore.second : runAsyncListeners(events);
        appliersFromBefore = null;
        afterVfsChange(appliers);
      }

    });
  }

  @Nonnull
  public static List<AsyncFileListener.ChangeApplier> runAsyncListeners(@Nonnull List<? extends VFileEvent> events) {
    if (events.isEmpty()) return Collections.emptyList();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Processing " + events);
    }

    List<AsyncFileListener.ChangeApplier> appliers = new ArrayList<>();
    List<AsyncFileListener> allListeners = ContainerUtil.concat(EP_NAME.getExtensionList(), ((VirtualFileManagerImpl)VirtualFileManager.getInstance()).getAsyncFileListeners());
    for (AsyncFileListener listener : allListeners) {
      ProgressManager.checkCanceled();
      long startNs = System.nanoTime();
      boolean canceled = false;
      try {
        ReadAction.run(() -> ContainerUtil.addIfNotNull(appliers, listener.prepareChange(events)));
      }
      catch (ProcessCanceledException e) {
        canceled = true;
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
      finally {
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        if (elapsedMs > 10_000) {
          LOG.warn(listener + " took too long (" + elapsedMs + "ms) on " + events.size() + " events" + (canceled ? ", canceled" : ""));
        }
      }
    }
    return appliers;
  }

  private static void beforeVfsChange(List<? extends AsyncFileListener.ChangeApplier> appliers) {
    for (AsyncFileListener.ChangeApplier applier : appliers) {
      PingProgress.interactWithEdtProgress();
      try {
        applier.beforeVfsChange();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  private static void afterVfsChange(List<? extends AsyncFileListener.ChangeApplier> appliers) {
    for (AsyncFileListener.ChangeApplier applier : appliers) {
      PingProgress.interactWithEdtProgress();
      try {
        applier.afterVfsChange();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  static void processEvents(List<? extends VFileEvent> events, @Nullable List<? extends AsyncFileListener.ChangeApplier> appliers) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (appliers != null) {
      beforeVfsChange(appliers);
      ourSuppressAppliers = true;
    }
    try {
      PersistentFS.getInstance().processEvents(events);
    }
    finally {
      ourSuppressAppliers = false;
    }
    if (appliers != null) {
      afterVfsChange(appliers);
    }
  }
}
