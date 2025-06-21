// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.PingProgress;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionPointName;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.internal.BaseVirtualFileManager;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AsyncEventSupport {
    private static final Logger LOG = Logger.getInstance(AsyncEventSupport.class);

    public static final ExtensionPointName<AsyncFileListener> EP_NAME = ExtensionPointName.create(AsyncFileListener.class);
    private static boolean ourSuppressAppliers;

    public static void startListening() {
        Application.get().getMessageBus().connect().subscribe(
            BulkFileListener.class,
            new BulkFileListener() {
                Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> appliersFromBefore;

                @Override
                public void before(@Nonnull List<? extends VFileEvent> events) {
                    if (ourSuppressAppliers) {
                        return;
                    }
                    List<AsyncFileListener.ChangeApplier> appliers = runAsyncListeners(events);
                    appliersFromBefore = Pair.create(events, appliers);
                    beforeVfsChange(appliers);
                }

                @Override
                public void after(@Nonnull List<? extends VFileEvent> events) {
                    if (ourSuppressAppliers) {
                        return;
                    }
                    List<AsyncFileListener.ChangeApplier> appliers = appliersFromBefore != null && appliersFromBefore.first.equals(events)
                        ? appliersFromBefore.second
                        : runAsyncListeners(events);
                    appliersFromBefore = null;
                    afterVfsChange(appliers);
                }
            }
        );
    }

    @Nonnull
    public static List<AsyncFileListener.ChangeApplier> runAsyncListeners(@Nonnull List<? extends VFileEvent> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing " + events);
        }

        List<AsyncFileListener.ChangeApplier> appliers = new ArrayList<>();
        List<AsyncFileListener> allListeners = ContainerUtil.concat(
            EP_NAME.getExtensionList(),
            ((BaseVirtualFileManager)VirtualFileManager.getInstance()).getAsyncFileListeners()
        );
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

    @RequiredWriteAction
    public static void processEvents(List<? extends VFileEvent> events, @Nullable List<? extends AsyncFileListener.ChangeApplier> appliers) {
        Application.get().assertWriteAccessAllowed();
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
