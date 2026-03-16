/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.virtualFileSystem.http.impl.internal;

import consulo.disposer.Disposable;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.http.BaseHttpFileSystem;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.http.RemoteFileManager;
import consulo.virtualFileSystem.http.RemoteFileState;
import consulo.virtualFileSystem.http.event.HttpVirtualFileListener;

import java.io.IOException;

/**
 * @author nik
 */
public abstract class HttpXFileSystemImpl extends BaseVirtualFileSystem implements BaseHttpFileSystem {
    private final String myProtocol;

    public HttpXFileSystemImpl(String protocol) {
        myProtocol = protocol;
    }

    @Override
    public VirtualFile findFileByPath(String path) {
        return findFileByPath(path, false);
    }

    @Override
    public VirtualFile findFileByPath(String path, boolean isDirectory) {
        try {
            String url = VirtualFileManager.constructUrl(myProtocol, path);
            return getRemoteFileManager().getOrCreateFile(url, path, isDirectory);
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    public void addFileListener(HttpVirtualFileListener listener) {
        getRemoteFileManager().addFileListener(listener);
    }

    @Override
    public void addFileListener(HttpVirtualFileListener listener, Disposable parentDisposable) {
        getRemoteFileManager().addFileListener(listener, parentDisposable);
    }

    @Override
    public void removeFileListener(HttpVirtualFileListener listener) {
        getRemoteFileManager().removeFileListener(listener);
    }

    @Override
    public boolean isFileDownloaded(VirtualFile file) {
        return file instanceof HttpVirtualFile && ((HttpVirtualFile) file).getFileInfo().getState() == RemoteFileState.DOWNLOADED;
    }

    @Override
    
    public VirtualFile createChildDirectory(Object requestor, VirtualFile vDir, String dirName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile createChildFile(Object requestor, VirtualFile vDir, String fileName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile(Object requestor, VirtualFile vFile) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveFile(Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile copyFile(Object requestor, VirtualFile vFile, VirtualFile newParent, String copyName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameFile(Object requestor, VirtualFile vFile, String newName) throws IOException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public String extractPresentableUrl(String path) {
        return VirtualFileManager.constructUrl(myProtocol, path);
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    
    @Override
    public String getProtocol() {
        return myProtocol;
    }

    private static RemoteFileManagerImpl getRemoteFileManager() {
        return (RemoteFileManagerImpl) RemoteFileManager.getInstance();
    }
}
