// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport.changes.vfs;

import consulo.application.progress.ProgressManager;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class VirtualFileChangesSeparator {
    private final List<Runnable> myBeforeAppliers = new ArrayList<>();
    private final List<Runnable> myAfterAppliers = new ArrayList<>();

    VirtualFileChangesSeparator(VirtualFileChangesListener listener, List<? extends VFileEvent> events) {
        for (VFileEvent each : events) {
            ProgressManager.checkCanceled();

            if (each instanceof VFilePropertyChangeEvent propertyChangeEvent) {
                if (propertyChangeEvent.isRename()) {
                    before(() -> process(listener, propertyChangeEvent.getFile(), propertyChangeEvent));
                    after(() -> process(listener, propertyChangeEvent.getFile(), propertyChangeEvent));
                }
            }
            else if (each instanceof VFileMoveEvent moveEvent) {
                before(() -> process(listener, moveEvent.getFile(), moveEvent));
                after(() -> process(listener, moveEvent.getFile(), moveEvent));
            }
            else if (each instanceof VFileCopyEvent copyEvent) {
                after(() -> {
                    VirtualFile newFile = copyEvent.getNewParent().findChild(copyEvent.getNewChildName());
                    if (newFile != null) {
                        process(listener, newFile, copyEvent);
                    }
                });
            }
            else if (each instanceof VFileCreateEvent createEvent) {
                after(() -> {
                    VirtualFile file = createEvent.getFile();
                    if (file != null) {
                        process(listener, file, createEvent);
                    }
                });
            }
            else if (each instanceof VFileDeleteEvent || each instanceof VFileContentChangeEvent) {
                before(() -> {
                    VirtualFile file = each.getFile();
                    if (file != null) {
                        process(listener, file, each);
                    }
                });
            }
        }
    }

    private void before(Runnable action) {
        myBeforeAppliers.add(action);
    }

    private void after(Runnable action) {
        myAfterAppliers.add(action);
    }

    void processBeforeEvents() {
        myBeforeAppliers.forEach(Runnable::run);
    }

    void processAfterEvents() {
        myAfterAppliers.forEach(Runnable::run);
    }

    private static void process(VirtualFileChangesListener listener, VirtualFile file, VFileEvent event) {
        if (listener.isProcessRecursively()) {
            processRecursively(listener, file, event);
        }
        else {
            processFile(listener, file, event);
        }
    }

    private static void processFile(VirtualFileChangesListener listener, VirtualFile file, VFileEvent event) {
        if (listener.isRelevant(file, event)) {
            listener.updateFile(file, event);
        }
    }

    private static void processRecursively(VirtualFileChangesListener listener, VirtualFile file, VFileEvent event) {
        VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(VirtualFile child) {
                processFile(listener, child, event);
                return true;
            }

            @Override
            public @Nullable Iterable<VirtualFile> getChildrenIterable(VirtualFile child) {
                return child.isDirectory() && child instanceof NewVirtualFile newVirtualFile ? newVirtualFile.iterInDbChildren() : null;
            }
        });
    }
}
