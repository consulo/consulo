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
package consulo.ide.impl.idea.openapi.vfs;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.Iconable;
import consulo.ide.impl.VfsIconUtil;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.FileNameCache;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.BaseVirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class PlatformVirtualFileManager extends BaseVirtualFileManager {
    @Nonnull
    private final ManagingFS myManagingFS;

    @Inject
    public PlatformVirtualFileManager(@Nonnull Application application, @Nonnull ManagingFS managingFS) {
        super(application);
        myManagingFS = managingFS;
    }

    @Override
    public Image getFileIcon(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VfsIconUtil.getIcon(file, flags, (Project) project);
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Image getFileIconNoDefer(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VfsIconUtil.getIconNoDefer(file, flags, (Project) project);
    }

    @Override
    protected long doRefresh(boolean asynchronous, @Nullable Runnable postAction) {
        if (!asynchronous) {
            myApplication.assertIsDispatchThread();
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
    public VirtualFile findFileById(int id) {
        return myManagingFS.findFileById(id);
    }

    @Nonnull
    @Override
    public CharSequence getVFileName(int nameId) {
        return FileNameCache.getVFileName(nameId);
    }

    @Override
    public int storeName(@Nonnull String name) {
        return FileNameCache.storeName(name);
    }
}
