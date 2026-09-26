// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport.changes.vfs;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;

import java.util.List;

public interface VirtualFileChangesListener {
    default boolean isProcessRecursively() {
        return false;
    }

    default void init() {
    }

    default void apply() {
    }

    default boolean isRelevant(VirtualFile file, VFileEvent event) {
        return false;
    }

    default void updateFile(VirtualFile file, VFileEvent event) {
    }

    static void installBulkVirtualFileListener(VirtualFileChangesListener listener, Disposable parentDisposable) {
        BulkFileListener bulkListener = new BulkFileListener() {
            @Override
            public void before(List<? extends VFileEvent> events) {
                VirtualFileChangesSeparator separator = new VirtualFileChangesSeparator(listener, events);
                listener.init();
                separator.processBeforeEvents();
                listener.apply();
            }

            @Override
            public void after(List<? extends VFileEvent> events) {
                VirtualFileChangesSeparator separator = new VirtualFileChangesSeparator(listener, events);
                listener.init();
                separator.processAfterEvents();
                listener.apply();
            }
        };
        Application.get().getMessageBus().connect(parentDisposable).subscribe(BulkFileListener.class, bulkListener);
    }

    static void installAsyncVirtualFileListener(VirtualFileChangesListener listener, Disposable parentDisposable) {
        AsyncFileListener asyncListener = events -> {
            VirtualFileChangesSeparator separator = new VirtualFileChangesSeparator(listener, events);
            return new AsyncFileListener.ChangeApplier() {
                @Override
                public void beforeVfsChange() {
                    listener.init();
                    separator.processBeforeEvents();
                }

                @Override
                public void afterVfsChange() {
                    separator.processAfterEvents();
                    listener.apply();
                }
            };
        };
        VirtualFileManager.getInstance().addAsyncFileListener(asyncListener, parentDisposable);
    }
}
