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
package consulo.virtualFileSystem.http.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.proxy.EventDispatcher;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.*;
import consulo.virtualFileSystem.http.event.FileDownloadingAdapter;
import consulo.virtualFileSystem.http.event.HttpVirtualFileListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class RemoteFileManagerImpl extends RemoteFileManager implements Disposable {
    private final LocalFileStorage myStorage;
    private final Map<Pair<Boolean, String>, HttpXVirtualFileImpl> myRemoteFiles;
    private final EventDispatcher<HttpVirtualFileListener> myDispatcher = EventDispatcher.create(HttpVirtualFileListener.class);
    private final List<RemoteContentProvider> myProviders = new ArrayList<>();
    private final DefaultRemoteContentProvider myDefaultRemoteContentProvider;

    public RemoteFileManagerImpl() {
        myStorage = new LocalFileStorage();
        myRemoteFiles = new HashMap<>();
        myDefaultRemoteContentProvider = new DefaultRemoteContentProvider();
    }

    @Nonnull
    public RemoteContentProvider findContentProvider(@Nonnull String url) {
        for (RemoteContentProvider provider : myProviders) {
            if (provider.canProvideContent(url)) {
                return provider;
            }
        }
        return myDefaultRemoteContentProvider;
    }

    public synchronized HttpXVirtualFileImpl getOrCreateFile(@Nonnull String url, @Nonnull String path, boolean directory) throws IOException {
        Pair<Boolean, String> key = Pair.create(directory, url);
        HttpXVirtualFileImpl file = myRemoteFiles.get(key);

        if (file == null) {
            if (!directory) {
                RemoteFileInfoImpl fileInfo = new RemoteFileInfoImpl(url, this);
                file = new HttpXVirtualFileImpl(getHttpFileSystem(url), path, fileInfo);
                fileInfo.addDownloadingListener(new MyDownloadingListener(file));
            }
            else {
                file = new HttpXVirtualFileImpl(getHttpFileSystem(url), path, null);
            }
            myRemoteFiles.put(key, file);
        }
        return file;
    }

    private static BaseHttpFileSystem getHttpFileSystem(@Nonnull String url) {
        return url.startsWith(HttpsFileSystem.HTTPS_PROTOCOL) ? HttpsFileSystem.getInstance() : HttpFileSystem.getInstance();
    }

    @Override
    public void addRemoteContentProvider(@Nonnull final RemoteContentProvider provider, @Nonnull Disposable parentDisposable) {
        addRemoteContentProvider(provider);
        Disposer.register(parentDisposable, () -> removeRemoteContentProvider(provider));
    }

    @Override
    public void addRemoteContentProvider(@Nonnull RemoteContentProvider provider) {
        myProviders.add(provider);
    }

    @Override
    public void removeRemoteContentProvider(@Nonnull RemoteContentProvider provider) {
        myProviders.remove(provider);
    }

    @Override
    public void addFileListener(@Nonnull HttpVirtualFileListener listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public void addFileListener(@Nonnull HttpVirtualFileListener listener, @Nonnull Disposable parentDisposable) {
        addFileListener(listener);
        Disposer.register(parentDisposable, () -> removeFileListener(listener));
    }

    @Override
    public void removeFileListener(@Nonnull HttpVirtualFileListener listener) {
        myDispatcher.removeListener(listener);
    }

    public void fireFileDownloaded(@Nonnull VirtualFile file) {
        myDispatcher.getMulticaster().fileDownloaded(file);
    }

    public LocalFileStorage getStorage() {
        return myStorage;
    }

    @Override
    public void dispose() {
        myStorage.deleteDownloadedFiles();
    }

    private class MyDownloadingListener extends FileDownloadingAdapter {
        private final HttpXVirtualFileImpl myFile;

        public MyDownloadingListener(HttpXVirtualFileImpl file) {
            myFile = file;
        }

        @Override
        public void fileDownloaded(VirtualFile localFile) {
            fireFileDownloaded(myFile);
        }
    }
}
