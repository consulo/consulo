// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.changes;

import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.impl.internal.autoimport.changes.vfs.VirtualFileChangesListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;

public class AsyncVirtualFilesChangesListener implements VirtualFileChangesListener {
    private final boolean myIgnoreInternalChanges;
    private final AsyncFileChangesListener myListener;

    public AsyncVirtualFilesChangesListener(boolean isIgnoreInternalChanges, AsyncFileChangesListener listener) {
        myIgnoreInternalChanges = isIgnoreInternalChanges;
        myListener = listener;
    }

    @Override
    public void init() {
        myListener.init();
    }

    @Override
    public void apply() {
        myListener.apply();
    }

    @Override
    public boolean isRelevant(VirtualFile file, VFileEvent event) {
        return !myIgnoreInternalChanges || !event.isFromSave();
    }

    @Override
    public void updateFile(VirtualFile file, VFileEvent event) {
        ExternalSystemModificationType modificationType =
            event.isFromRefresh() ? ExternalSystemModificationType.EXTERNAL : ExternalSystemModificationType.INTERNAL;
        myListener.onFileChange(file.getPath(), file.getModificationStamp(), modificationType);
    }
}
