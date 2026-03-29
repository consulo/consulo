/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.virtualFileSystem;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

@ExtensionImpl
public class VcsFileSystem extends BaseVirtualFileSystem {
    private static final String PROTOCOL = "vcs";

    public static VcsFileSystem getInstance() {
        return (VcsFileSystem) VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public VirtualFile findFileByPath(String path) {
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        return null;
    }

    @Override
    @RequiredWriteAction
    public void fireContentsChanged(@Nullable Object requestor, VirtualFile file, long oldModificationStamp) {
        super.fireContentsChanged(requestor, file, oldModificationStamp);
    }

    @Override
    @RequiredWriteAction
    protected void fireBeforeFileDeletion(@Nullable Object requestor, VirtualFile file) {
        super.fireBeforeFileDeletion(requestor, file);
    }

    @Override
    @RequiredWriteAction
    protected void fireFileDeleted(@Nullable Object requestor, VirtualFile file, String fileName, VirtualFile parent) {
        super.fireFileDeleted(requestor, file, fileName, parent);
    }

    @Override
    @RequiredWriteAction
    protected void fireBeforeContentsChange(Object requestor, VirtualFile file) {
        super.fireBeforeContentsChange(requestor, file);
    }

    @Override
    @RequiredWriteAction
    public void deleteFile(@Nullable Object requestor, VirtualFile vFile) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }

    @Override
    @RequiredWriteAction
    public void moveFile(@Nullable Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }

    @Override
    @RequiredWriteAction
    public VirtualFile copyFile(@Nullable Object requestor, VirtualFile vFile, VirtualFile newParent, String copyName) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }

    @Override
    @RequiredWriteAction
    public void renameFile(@Nullable Object requestor, VirtualFile vFile, String newName) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }

    @Override
    @RequiredWriteAction
    public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile vDir, String fileName) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }

    @Override
    @RequiredWriteAction
    public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile vDir, String dirName) throws IOException {
        throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
    }
}
