/*
 * Copyright 2013-2026 consulo.io
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
package consulo.virtualFileSystem.http;

import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.http.event.HttpVirtualFileListener;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2026-02-07
 */
public interface BaseHttpFileSystem extends VirtualFileSystem {
    VirtualFile findFileByPath(@Nonnull String path, boolean isDirectory);

    boolean isFileDownloaded(@Nonnull VirtualFile file);

    void addFileListener(@Nonnull HttpVirtualFileListener listener);

    void addFileListener(@Nonnull HttpVirtualFileListener listener, @Nonnull Disposable parentDisposable);

    void removeFileListener(@Nonnull HttpVirtualFileListener listener);
}