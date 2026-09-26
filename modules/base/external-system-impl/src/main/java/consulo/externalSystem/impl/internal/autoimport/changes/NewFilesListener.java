// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.changes;

import consulo.disposer.Disposable;
import consulo.externalSystem.impl.internal.autoimport.changes.vfs.VirtualFileChangesListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public abstract class NewFilesListener implements VirtualFileChangesListener {
    private final Queue<VirtualFile> myModifiedFiles = new ConcurrentLinkedQueue<>();

    protected abstract void fireNewFilesCreated(Collection<VirtualFile> files);

    @Override
    public void init() {
        myModifiedFiles.clear();
    }

    @Override
    public void apply() {
        List<VirtualFile> set = new ArrayList<>(myModifiedFiles);
        if (!set.isEmpty()) {
            fireNewFilesCreated(set);
        }
    }

    @Override
    public boolean isRelevant(VirtualFile file, VFileEvent event) {
        return event instanceof VFileCopyEvent
            || event instanceof VFileCreateEvent
            || event instanceof VFileMoveEvent
            || (event instanceof VFilePropertyChangeEvent propertyChangeEvent && propertyChangeEvent.isRename());
    }

    @Override
    public void updateFile(VirtualFile file, VFileEvent event) {
        myModifiedFiles.add(file);
    }

    public static void whenNewFilesCreated(Consumer<Collection<VirtualFile>> action, Disposable parentDisposable) {
        NewFilesListener listener = new NewFilesListener() {
            @Override
            protected void fireNewFilesCreated(Collection<VirtualFile> files) {
                action.accept(files);
            }
        };
        VirtualFileChangesListener.installAsyncVirtualFileListener(listener, parentDisposable);
    }
}
