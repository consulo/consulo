// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.url;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Represents a reference to a file or directory. Code which keeps references to files should store instances of this type
 * instead of paths or URLs as {@link String}, because it consumes less memory and provides an efficient way to locate a
 * {@link VirtualFile}.
 * <p>
 * Use {@link VirtualFileUrlManager#get VirtualFileUrlManager.get}/{@link VirtualFileUrlManager#storeAndGet storeAndGet} to get an
 * instance of this interface, and {@link #getFile()} to locate a {@link VirtualFile} instance by it.
 * <p>
 * <b>Equality:</b> for a given manager the same URL always returns the same instance, so {@code ==} is a valid same-URL
 * check for instances obtained from one manager. Instances obtained from <i>different</i> managers must not be compared with
 * {@code equals}; compare {@link #getUrl()} instead.
 */
public interface VirtualFileUrl {
    /**
     * Returns URL in the Virtual File System format.
     */
    String getUrl();

    String getFileName();

    /**
     * @return the parent of the {@link VirtualFileUrl}, or null if it's a root file. If {@link VirtualFileUrl} for the parent hasn't
     * been created, it'll be automatically instantiated.
     */
    @Nullable VirtualFileUrl getParent();

    /**
     * @return representation of the url without protocol
     */
    String getPresentableUrl();

    /**
     * @return the list of descendants for the current node
     */
    List<VirtualFileUrl> getSubTreeFileUrls();

    /**
     * Resolve the given path against virtual url
     *
     * @param relativePath for resolve
     * @return instance representing the resolved path
     */
    VirtualFileUrl append(String relativePath);

    /**
     * Returns the child for [nameSegment], or `null` if this node has no children yet or has no such child.
     */
    @Nullable VirtualFileUrl findChild(String nameSegment);

    /**
     * Returns instance of {@link VirtualFile} corresponding to this {@link VirtualFileUrl} or {@code null} if there is no a file with
     * such URL in the Virtual File System.
     * <p>
     * Usually it returns an instance cached in a field, so it's cheap. If no value is cached, or the VFS changed after that, it falls
     * back to {@link consulo.virtualFileSystem.VirtualFileManager#findFileByUrl}.
     */
    @Nullable VirtualFile getFile();
}
