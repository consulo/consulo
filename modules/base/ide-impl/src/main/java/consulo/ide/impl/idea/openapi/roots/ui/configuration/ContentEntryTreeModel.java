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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.fileChooser.util.FileChooserUtil;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextAttribute;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ContentEntryTreeModel implements TreeModel<VirtualFile> {
    private volatile @Nullable ContentEntrySnapshot mySnapshot;

    public void setSnapshot(@Nullable ContentEntrySnapshot snapshot) {
        mySnapshot = snapshot;
    }

    @Override
    @RequiredReadAction
    public void buildChildren(Function<VirtualFile, TreeNode<VirtualFile>> nodeFactory, @Nullable VirtualFile parentValue) {
        ContentEntrySnapshot snapshot = mySnapshot;
        if (snapshot == null) {
            return;
        }

        if (parentValue == null) {
            VirtualFile root = VirtualFileManager.getInstance().findFileByUrl(snapshot.url());
            if (root != null && root.isValid() && root.isDirectory()) {
                VirtualFile parent = root.getParent();
                createNode(nodeFactory, snapshot, root, parent == null ? null : parent.getPresentableUrl());
            }
            return;
        }

        if (!parentValue.isValid()) {
            return;
        }

        List<VirtualFile> children = new ArrayList<>();
        for (VirtualFile child : parentValue.getChildren()) {
            if (isVisible(child)) {
                children.add(child);
            }
        }
        children.sort((o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName()));

        for (VirtualFile child : children) {
            createNode(nodeFactory, snapshot, child, null);
        }
    }

    @RequiredReadAction
    private static void createNode(
        Function<VirtualFile, TreeNode<VirtualFile>> nodeFactory,
        ContentEntrySnapshot snapshot,
        VirtualFile file,
        @Nullable String location
    ) {
        String name = file.getName().isEmpty() ? file.getPresentableUrl() : file.getName();
        String fileUrl = file.getUrl();
        ContentFolderSnapshot marked = null;
        ContentFolderSnapshot parentFolder = null;
        for (ContentFolderSnapshot folder : snapshot.folders()) {
            if (folder.url().equals(fileUrl)) {
                marked = folder;
            }
            else if (VirtualFileUtil.isEqualOrAncestor(folder.url(), fileUrl)
                && (parentFolder == null || folder.url().length() > parentFolder.url().length())) {
                parentFolder = folder;
            }
        }

        Image icon;
        TextAttribute attribute = TextAttribute.REGULAR;
        boolean hidden = FileChooserUtil.isFileHidden(file);
        if (marked != null) {
            icon = ContentFoldersSupportUtil.getContentFolderIcon(marked.type(), marked.properties());
            attribute = new TextAttribute(Font.PLAIN, marked.type().getGroupColor());
        }
        else if (parentFolder != null) {
            boolean excluded = parentFolder.type() instanceof ExcludedContentFolderTypeProvider;
            icon = excluded || snapshot.packageSupport()
                ? parentFolder.type().getChildDirectoryIcon(null, null)
                : PlatformIconGroup.nodesTreeopen();
        }
        else {
            icon = PlatformIconGroup.nodesFolder();
        }

        if (hidden && marked == null) {
            icon = ImageEffects.transparent(icon);
            attribute = TextAttribute.GRAYED;
        }

        Image nodeIcon = icon;
        TextAttribute nodeAttribute = attribute;
        TreeNode<VirtualFile> node = nodeFactory.apply(file);
        node.setLeaf(!hasVisibleChild(file));
        node.setRenderer((value, presentation) -> {
            presentation.withIcon(nodeIcon);
            presentation.append(name, nodeAttribute);
            if (location != null) {
                presentation.append("  " + location, TextAttribute.GRAYED);
            }
        });
    }

    private static boolean hasVisibleChild(VirtualFile file) {
        for (VirtualFile child : file.getChildren()) {
            if (isVisible(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVisible(VirtualFile file) {
        if (!file.isDirectory()) {
            return false;
        }
        if (file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null) {
            return false;
        }
        return !FileTypeRegistry.getInstance().isFileIgnored(file);
    }
}
