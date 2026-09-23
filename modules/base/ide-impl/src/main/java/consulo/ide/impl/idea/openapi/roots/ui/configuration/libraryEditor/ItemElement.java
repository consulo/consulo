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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.ide.impl.idea.ide.IconUtilEx;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.http.HttpFileSystem;
import org.jspecify.annotations.Nullable;

import java.io.File;

class ItemElement extends LibraryTableTreeContentElement<ItemElement> {
    protected final String myUrl;
    private final String myRootType;

    public ItemElement(OrderRootTypeElement parent, String url, String rootType, boolean isJarDirectory, boolean isValid) {
        super(parent);
        myUrl = url;
        myName = getPresentablePath(url).replace('/', File.separatorChar);
        myColor = getForegroundColor(isValid);
        setIcon(getIconForUrl(url, isValid, isJarDirectory));
        myRootType = rootType;
    }

    private static Image getIconForUrl(String url, boolean isValid, boolean isJarDirectory) {
        if (!isValid) {
            return PlatformIconGroup.nodesPpinvalid();
        }

        VirtualFile presentableFile = isArchiveFileRoot(url)
            ? LocalFileSystem.getInstance().findFileByPath(getPresentablePath(url))
            : VirtualFileManager.getInstance().findFileByUrl(url);

        if (presentableFile == null || !presentableFile.isValid()) {
            return PlatformIconGroup.nodesPpinvalid();
        }
        else if (presentableFile.getFileSystem() instanceof HttpFileSystem) {
            return PlatformIconGroup.nodesPpweb();
        }
        else if (!presentableFile.isDirectory()) {
            return IconUtilEx.getIcon(presentableFile, 0, null);
        }
        else if (isJarDirectory) {
            return PlatformIconGroup.nodesJardirectory();
        }
        else {
            return PlatformIconGroup.nodesTreeclosed();
        }
    }

    public static String getPresentablePath(String url) {
        String presentablePath = VirtualFileManager.extractPath(url);
        if (isArchiveFileRoot(url)) {
            presentablePath = presentablePath.substring(0, presentablePath.length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
        }
        return presentablePath;
    }

    private static boolean isArchiveFileRoot(String url) {
        return VirtualFileManager.extractPath(url).endsWith(ArchiveFileSystem.ARCHIVE_SEPARATOR);
    }

    public OrderRootTypeElement getParent() {
        return (OrderRootTypeElement) getParentDescriptor();
    }

    public String getRootType() {
        return myRootType;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ItemElement that
            && getParent().equals(that.getParent())
            && myRootType.equals(that.myRootType)
            && myUrl.equals(that.myUrl);
    }

    public String getUrl() {
        return myUrl;
    }

    @Override
    public int hashCode() {
        int result = 29 * getParent().hashCode() + myUrl.hashCode();
        return 29 * result + myRootType.hashCode();
    }
}
