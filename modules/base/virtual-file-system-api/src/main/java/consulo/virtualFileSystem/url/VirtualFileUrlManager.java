// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.url;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import org.jspecify.annotations.Nullable;

/**
 * Storage for URLs (in the Virtual File System format) of files that are referenced from the project model.
 * <p>
 * Creating a {@link VirtualFileUrl} never touches the Virtual File System - only {@link VirtualFileUrl#getFile()} does.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface VirtualFileUrlManager {
    /**
     * Returns an existing, or creates and stores a new instance of {@link VirtualFileUrl} instance for the given URL in the Virtual
     * File System format. This function <b>should be used only to</b> obtain an instance which will be stored. It must not be used
     * for other purposes (e.g., to convert between different URL formats or to find
     * {@link consulo.virtualFileSystem.VirtualFile VirtualFile}), because all created URLs are stored in the shared data structures.
     */
    VirtualFileUrl storeAndGet(String url);

    /**
     * @return an existing instance of {@link VirtualFileUrl} for the given URL or {@code null} if no instance was registered.
     */
    @Nullable VirtualFileUrl get(String url);

    /**
     * Returns an existing or creates a new instance of {@link VirtualFileUrl} instance for the given path to a file in the local
     * filesystems. It's better to use {@link #storeAndGet} wherever possible, because it works for files in other filesystems as
     * well, e.g. inside JAR files and for remote filesystems.
     */
    VirtualFileUrl fromPath(String path);
}
