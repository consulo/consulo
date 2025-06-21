/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.virtualFileSystem;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.internal.RootComponentHolder;
import consulo.component.util.Iconable;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;

/**
 * Manages virtual file systems.
 *
 * @see VirtualFileSystem
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class VirtualFileManager implements ModificationTracker {

    public static final ModificationTracker VFS_STRUCTURE_MODIFICATIONS = () -> getInstance().getStructureModificationCount();

    /**
     * Gets the instance of <code>VirtualFileManager</code>.
     *
     * @return <code>VirtualFileManager</code>
     */
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use constructor injection")
    public static VirtualFileManager getInstance() {
        return RootComponentHolder.getRootComponent().getInstance(VirtualFileManager.class);
    }

    /**
     * Gets VirtualFileSystem with the specified protocol.
     *
     * @param protocol String representing the protocol
     * @return {@link VirtualFileSystem}
     * @see VirtualFileSystem#getProtocol
     */
    public abstract VirtualFileSystem getFileSystem(@Nonnull String protocol);

    /**
     * <p>Refreshes the cached file systems information from the physical file systems synchronously.<p/>
     * <p>
     * <p><strong>Note</strong>: this method should be only called within a write-action
     * (see {@linkplain Application#runWriteAction})</p>
     *
     * @return refresh session ID.
     */
    public abstract long syncRefresh();

    /**
     * Refreshes the cached file systems information from the physical file systems asynchronously.
     * Launches specified action when refresh is finished.
     *
     * @return refresh session ID.
     */
    public abstract long asyncRefresh(@Nullable Runnable postAction);

    public abstract void refreshWithoutFileWatcher(boolean asynchronous);

    /**
     * Searches for the file specified by given URL. URL is a string which uniquely identifies file in all
     * file systems.
     *
     * @param url the URL to find file by
     * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
     * @see VirtualFile#getUrl
     * @see VirtualFileSystem#findFileByPath
     * @see #refreshAndFindFileByUrl
     */
    @Nullable
    public abstract VirtualFile findFileByUrl(@Nonnull String url);


    /**
     * Looks for a related {@link VirtualFile} for a given {@link Path}
     *
     * @return <code>{@link VirtualFile}</code> if the file was found, {@code null} otherwise
     * @see VirtualFile#getUrl
     * @see VirtualFileSystem#findFileByPath
     * @see #refreshAndFindFileByUrl
     */
    @Nullable
    public VirtualFile findFileByNioPath(@Nonnull Path path) {
        return null;
    }

    /**
     * Refreshes only the part of the file system needed for searching the file by the given URL and finds file
     * by the given URL.<br>
     * <p>
     * This method is useful when the file was created externally and you need to find <code>{@link VirtualFile}</code>
     * corresponding to it.<p>
     * <p>
     * This method should be only called within write-action.
     * See {@link Application#runWriteAction}.
     *
     * @param url the URL
     * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
     * @see VirtualFileSystem#findFileByPath
     * @see VirtualFileSystem#refreshAndFindFileByPath
     */
    @Nullable
    public abstract VirtualFile refreshAndFindFileByUrl(@Nonnull String url);

    /**
     * <p>Refreshes only the part of the file system needed for searching the file by the given URL and finds file
     * by the given URL.</p>
     *
     * <p>This method is useful when the file was created externally and you need to find <code>{@link VirtualFile}</code>
     * corresponding to it.</p>
     *
     * <p>If this method is invoked not from Swing event dispatch thread, then it must not happen inside a read action.</p>
     *
     * @return <code>{@link VirtualFile}</code> if the file was found, {@code null} otherwise
     * @see VirtualFileSystem#findFileByPath
     * @see VirtualFileSystem#refreshAndFindFileByPath
     **/
    @Nullable
    public VirtualFile refreshAndFindFileByNioPath(@Nonnull Path path) {
        return null;
    }

    /**
     * Adds listener to the file system.
     *
     * @param listener the listener
     * @see VirtualFileListener
     */
    @Deprecated
    @DeprecationInfo("Use MessageBus API")
    public abstract void addVirtualFileListener(@Nonnull VirtualFileListener listener);

    @Deprecated
    @DeprecationInfo("Use MessageBus API")
    public abstract void addVirtualFileListener(@Nonnull VirtualFileListener listener, @Nonnull Disposable parentDisposable);

    /**
     * Removes listener form the file system.
     *
     * @param listener the listener
     */
    @Deprecated
    @DeprecationInfo("Use MessageBus API")
    public abstract void removeVirtualFileListener(@Nonnull VirtualFileListener listener);

    /**
     * Constructs URL by specified protocol and path. URL is a string which uniquely identifies file in all
     * file systems.
     *
     * @param protocol the protocol
     * @param path     the path
     * @return URL
     */
    @Nonnull
    public static String constructUrl(@Nonnull String protocol, @Nonnull String path) {
        return protocol + URLUtil.SCHEME_SEPARATOR + path;
    }

    /**
     * Extracts protocol from the given URL. Protocol is a substring from the beginning of the URL till "://".
     *
     * @param url the URL
     * @return protocol or <code>null</code> if there is no "://" in the URL
     * @see VirtualFileSystem#getProtocol
     */
    @Nullable
    public static String extractProtocol(@Nonnull String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        if (index < 0) {
            return null;
        }
        return url.substring(0, index);
    }

    /**
     * Extracts path from the given URL. Path is a substring from "://" till the end of URL. If there is no "://" URL
     * itself is returned.
     *
     * @param url the URL
     * @return path
     */
    @Nonnull
    public static String extractPath(@Nonnull String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        if (index < 0) {
            return url;
        }
        return url.substring(index + URLUtil.SCHEME_SEPARATOR.length());
    }

    /**
     * @deprecated Use {@link #VFS_CHANGES} message bus topic.
     */
    public abstract void addVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener);

    /**
     * @deprecated Use {@link #VFS_CHANGES} message bus topic.
     */
    public abstract void addVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener, @Nonnull Disposable parentDisposable);

    /**
     * @deprecated Use {@link #VFS_CHANGES} message bus topic.
     */
    public abstract void removeVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener);

    /**
     * Consider using extension point {@code vfs.asyncListener}.
     */
    public abstract void addAsyncFileListener(@Nonnull AsyncFileListener listener, @Nonnull Disposable parentDisposable);

    public abstract void notifyPropertyChanged(@Nonnull VirtualFile virtualFile, @Nonnull String property, Object oldValue, Object newValue);

    /**
     * @return a number that's incremented every time something changes in the VFS, i.e. file hierarchy, names, flags, attributes, contents.
     * This only counts modifications done in current IDE session.
     * @see #getStructureModificationCount()
     */
    @Override
    public abstract long getModificationCount();

    /**
     * @return a number that's incremented every time something changes in the VFS structure, i.e. file hierarchy or names.
     * This only counts modifications done in current IDE session.
     * @see #getModificationCount()
     */
    public abstract long getStructureModificationCount();

    public VirtualFile findFileById(int id) {
        return null;
    }

    public abstract int storeName(@Nonnull String name);

    @Nonnull
    public abstract CharSequence getVFileName(int nameId);

    public abstract Image getBaseFileIcon(@Nonnull VirtualFile file);

    public abstract Image getFileIcon(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags);

    /**
     * Return icon without proxing to lazy icon. Must be called from not UI thread
     */
    @Nonnull
    @RequiredReadAction
    public abstract Image getFileIconNoDefer(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags);
}
