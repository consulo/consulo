/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.Iconable;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.BaseVirtualFileManager;
import consulo.virtualFileSystem.internal.VirtualFileSystemInternalHelper;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class PlatformVirtualFileManager extends BaseVirtualFileManager {
    
    private final ManagingFS myManagingFS;

    @Inject
    public PlatformVirtualFileManager(Application application, ManagingFS managingFS) {
        super(application);
        myManagingFS = managingFS;
    }

    @Override
    public @Nullable Image getFileIcon(VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VirtualFileSystemInternalHelper.getInstance().getFileIcon(file, project, flags);
    }

    @RequiredReadAction
    
    @Override
    public Image getFileIconNoDefer(VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VirtualFileSystemInternalHelper.getInstance().getFileIconNoDefer(file, project, flags);
    }

    @Override
    protected long doRefresh(boolean asynchronous, @Nullable Runnable postAction) {
        if (!asynchronous) {
            //noinspection RequiredXAction
            UIAccess.assertIsUIThread();
        }

        // todo: get an idea how to deliver changes from local FS to jar fs before they go refresh
        RefreshSession session = RefreshQueue.getInstance().createSession(asynchronous, true, postAction);
        session.addAllFiles(myManagingFS.getRoots());
        session.launch();

        super.doRefresh(asynchronous, postAction);

        return session.getId();
    }

    @Override
    public long getModificationCount() {
        return myManagingFS.getModificationCount();
    }

    @Override
    public long getStructureModificationCount() {
        return myManagingFS.getStructureModificationCount();
    }

    @Override
    public @Nullable VirtualFile findFileById(int id) {
        return myManagingFS.findFileById(id);
    }

    @Override
    public CharSequence getVFileName(int nameId) {
        return FileNameCache.getVFileName(nameId);
    }

    @Override
    public int storeName(String name) {
        return FileNameCache.storeName(name);
    }
}
