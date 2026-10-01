// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.application.ReadAction;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.ui.TextAttribute;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.core.local.CoreLocalFileSystem;
import consulo.virtualFileSystem.internal.core.local.CoreLocalVirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A tree model backed by Java NIO {@link Path} instead of {@link VirtualFile}.
 * Provides the same public interface as the VFS backed file tree model
 * but does not use the Virtual File System (VFS) internally.
 */
public class NioFileTreeModel implements TreeModel<NioFileNode> {
    private static final Logger LOG = Logger.getInstance(NioFileTreeModel.class);

    private static String fileName(Path path) {
        UniversalFileChooserContributor contributor = UniversalFileChooserContributor.findOwner(path);
        String name = contributor != null ? contributor.getFileName(path) : null;
        if (name != null) {
            return name;
        }
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : path.toString();
    }

    private final FileChooserDescriptor myDescriptor;
    private final boolean mySortDirectories;
    private final boolean myVirtualFileRequired;
    private final CoreLocalFileSystem myFileSystem = new CoreLocalFileSystem();
    private final @Nullable List<Path> myDescriptorRoots;

    private volatile List<UniversalFileChooserContributor.Root> myContributorRoots = List.of();

    public NioFileTreeModel(FileChooserDescriptor descriptor) {
        this(descriptor, true);
    }

    public NioFileTreeModel(FileChooserDescriptor descriptor, boolean sortDirectories) {
        myDescriptor = descriptor;
        mySortDirectories = sortDirectories;
        myVirtualFileRequired = NioFileChooserUtil.isVirtualFileRequired(descriptor);
        myDescriptorRoots = getRoots(descriptor);
    }

    public void setContributorRoots(List<UniversalFileChooserContributor.Root> newRoots) {
        myContributorRoots = newRoots;
    }

    @Override
    public void buildChildren(Function<NioFileNode, TreeNode<NioFileNode>> nodeFactory, @Nullable NioFileNode parentValue) {
        if (parentValue == null) {
            buildRootNodes(nodeFactory);
        }
        else {
            buildDirectoryNodes(nodeFactory, parentValue);
        }
    }

    public @Nullable Path matchRoot(Path path) {
        for (UniversalFileChooserContributor.Root root : myContributorRoots) {
            Path rootPath = root.path();
            if (rootPath != null && rootPath.toString().equals(path.toString())) {
                return rootPath;
            }
        }
        return null;
    }

    public UniversalFileChooserContributor.@Nullable Root getVirtualRoot(NioFileNode node) {
        return node instanceof VirtualRootNode virtualRoot ? virtualRoot.getContributorRoot() : null;
    }

    private void buildRootNodes(Function<NioFileNode, TreeNode<NioFileNode>> nodeFactory) {
        if (myDescriptorRoots != null) {
            for (Path root : myDescriptorRoots) {
                NioFileNode node = new NioFileNode(root);
                updateContent(node, null, null);
                updateSelectable(node, createVirtualFile(root));
                createNode(nodeFactory, node, isLeaf(root));
            }
            return;
        }

        for (UniversalFileChooserContributor.Root root : myContributorRoots) {
            Path path = root.path();
            if (path != null) {
                NioFileNode node = new NioFileNode(path);
                updateContent(node, null, null);
                updateSelectable(node, createVirtualFile(path));
                applyPresentation(node, root.presentation());
                createNode(nodeFactory, node, isLeaf(path));
            }
        }

        for (UniversalFileChooserContributor.Root root : myContributorRoots) {
            if (root.path() == null) {
                createNode(nodeFactory, new VirtualRootNode(root), true);
            }
        }
    }

    private void buildDirectoryNodes(Function<NioFileNode, TreeNode<NioFileNode>> nodeFactory, NioFileNode parentValue) {
        Path parentPath = parentValue.getPath();
        if (parentPath == null) {
            return;
        }

        PreloadedDirectory directory = myVirtualFileRequired ? new PreloadedDirectory(myFileSystem, parentPath) : null;
        List<ChildEntry> children = getChildrenWithAttributes(parentPath, directory);
        if (children == null) {
            return;
        }

        List<ChildEntry> visible = new ArrayList<>();
        for (ChildEntry entry : children) {
            if (isVisible(entry)) {
                visible.add(entry);
            }
        }
        visible.sort(this::compare);

        for (ChildEntry entry : visible) {
            NioFileNode node = new NioFileNode(entry.path());
            updateContent(node, entry.attrs(), entry.isDirectory());
            updateSelectable(node, entry.file());
            createNode(nodeFactory, node, !entry.isDirectory());
        }
    }

    private TreeNode<NioFileNode> createNode(
        Function<NioFileNode, TreeNode<NioFileNode>> nodeFactory,
        NioFileNode value,
        boolean leaf
    ) {
        TreeNode<NioFileNode> node = nodeFactory.apply(value);
        node.setLeaf(leaf);
        node.setRenderer((nodeValue, presentation) -> {
            presentation.withIcon(nodeValue.getIcon());
            String name = nodeValue.getName();
            presentation.append(name != null ? name : "");
            String comment = nodeValue.getComment();
            if (comment != null && !comment.isEmpty()) {
                presentation.append(" " + comment, TextAttribute.GRAYED);
            }
        });
        return node;
    }

    private int compare(ChildEntry one, ChildEntry two) {
        if (mySortDirectories) {
            if (one.isDirectory() != two.isDirectory()) {
                return one.isDirectory() ? -1 : 1;
            }
        }
        return StringUtil.naturalCompare(fileName(one.path()), fileName(two.path()));
    }

    private boolean isVisible(ChildEntry entry) {
        boolean showHiddenFiles = myDescriptor.isShowHiddenFiles();
        if (!showHiddenFiles && NioFileChooserUtil.isHidden(entry.path(), entry.attrs())) {
            return false;
        }
        if (!entry.isDirectory() && !matchesExtensionFilter(entry.name())) {
            return false;
        }
        VirtualFile file = entry.file();
        if (file != null) {
            return ReadAction.compute(() -> myDescriptor.isFileVisible(file, showHiddenFiles));
        }
        if (myDescriptor.isHideIgnored() && NioFileChooserUtil.isIgnored(entry.name())) {
            return false;
        }
        return entry.isDirectory()
            || myDescriptor.isChooseFiles()
            || myDescriptor.isChooseJarContents() && NioFileChooserUtil.isArchiveFile(entry.path());
    }

    private boolean matchesExtensionFilter(String name) {
        Pair<LocalizeValue, List<String>> extFilter = myDescriptor.getExtensionFilter();
        if (extFilter == null) {
            return true;
        }
        for (String extension : extFilter.getSecond()) {
            if (StringUtil.endsWithIgnoreCase(name, "." + extension)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable VirtualFile createVirtualFile(Path path) {
        return myVirtualFileRequired ? new CoreLocalVirtualFile(myFileSystem, path) : null;
    }

    private void updateSelectable(NioFileNode node, @Nullable VirtualFile file) {
        node.updateSelectable(file == null ? null : ReadAction.compute(() -> myDescriptor.isFileSelectable(file)));
    }

    private @Nullable List<ChildEntry> getChildrenWithAttributes(Path path, @Nullable PreloadedDirectory directory) {
        if (!isValid(path)) {
            return null;
        }
        if (!Files.isDirectory(path)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            List<ChildEntry> result = new ArrayList<>();
            for (Path childPath : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class);
                    VirtualFile file = directory == null ? null : new LazyDirectoryOrFile(myFileSystem, directory, childPath, attrs);
                    result.add(new ChildEntry(childPath, attrs, isDirectory(childPath, attrs), file));
                }
                catch (IOException | RuntimeException e) {
                    // skip unreadable entries
                }
            }
            return result;
        }
        catch (IOException e) {
            LOG.debug("cannot list directory: " + path, e);
            return null;
        }
        catch (SecurityException e) {
            LOG.debug("cannot list directory: " + path, e);
            return null;
        }
    }

    private static boolean isDirectory(Path path, BasicFileAttributes attrs) {
        return attrs.isSymbolicLink() ? Files.isDirectory(path) : attrs.isDirectory();
    }

    private static void updateContent(NioFileNode node, @Nullable BasicFileAttributes attrs, @Nullable Boolean isDirectory) {
        Path path = node.getPath();
        if (path == null) {
            return;
        }
        node.updateName(fileName(path));
        if (attrs != null) {
            boolean directory = isDirectory != null ? isDirectory : attrs.isDirectory();
            Image icon = getIcon(path, directory);
            boolean symlink = attrs.isSymbolicLink();
            if (symlink) {
                icon = dressIcon(icon);
            }
            node.updateIcon(icon);
            node.updateValid(true);
            node.updateHidden(NioFileChooserUtil.isHidden(path, attrs));
            node.updateSymlink(symlink);
            node.updateWritable(Files.isWritable(path));
        }
        else if (path.getParent() == null) {
            node.updateIcon(PlatformIconGroup.nodesFolder());
            node.updateValid(true);
            node.updateHidden(false);
            node.updateSymlink(false);
            node.updateWritable(false);
        }
        else {
            Image icon = getIcon(path, Files.isDirectory(path));
            boolean symlink = Files.isSymbolicLink(path);
            if (symlink) {
                icon = dressIcon(icon);
            }
            node.updateIcon(icon);
            node.updateValid(Files.exists(path));
            node.updateHidden(NioFileChooserUtil.isHidden(path));
            node.updateSymlink(symlink);
            node.updateWritable(Files.isWritable(path));
        }
    }

    private static Image getIcon(Path path, boolean directory) {
        if (!directory) {
            return NioFileChooserUtil.getIcon(path);
        }
        Image projectIcon = getProjectIcon(path);
        return projectIcon != null ? projectIcon : PlatformIconGroup.nodesFolder();
    }

    /**
     * The processors answer from the path alone, so a directory that holds a project can be named as one
     * without the VFS the chooser is built to stay out of.
     */
    private static @Nullable Image getProjectIcon(Path path) {
        try {
            ProjectOpenProcessor processor = ProjectOpenProcessors.getInstance().findProcessor(path);
            return processor == null ? null : processor.getIcon(path);
        }
        catch (RuntimeException e) {
            LOG.debug("cannot detect project: " + path, e);
            return null;
        }
    }

    private static Image dressIcon(Image baseIcon) {
        return ImageEffects.layered(baseIcon, PlatformIconGroup.nodesSymlink());
    }

    private static void applyPresentation(NioFileNode node, UniversalFileChooserContributor.Presentation presentation) {
        Image icon = presentation.icon();
        if (icon != null) {
            node.updateIcon(icon);
        }
        node.updateName(presentation.presentableName().get());
        node.updateComment(presentation.comment().get());
    }

    private static boolean isValid(@Nullable Path path) {
        return path != null && Files.exists(path);
    }

    private static boolean isLeaf(@Nullable Path path) {
        return path != null && path.getParent() != null && !Files.isDirectory(path);
    }

    private static @Nullable List<Path> getRoots(FileChooserDescriptor descriptor) {
        List<Path> list = new ArrayList<>();
        for (VirtualFile file : descriptor.getRoots()) {
            Path path = NioFileChooserUtil.toNioPathSafe(file);
            if (path != null && isValid(path)) {
                list.add(path);
            }
        }
        return list.isEmpty() && descriptor.isShowFileSystemRoots() ? null : list;
    }

    private record ChildEntry(Path path, BasicFileAttributes attrs, boolean isDirectory, @Nullable VirtualFile file) {
        String name() {
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : path.toString();
        }
    }

    private static class VirtualRootNode extends NioFileNode {
        private final UniversalFileChooserContributor.Root myContributorRoot;

        VirtualRootNode(UniversalFileChooserContributor.Root contributorRoot) {
            super(null);
            myContributorRoot = contributorRoot;
            UniversalFileChooserContributor.Presentation presentation = contributorRoot.presentation();
            updateName(presentation.presentableName().get());
            updateComment(presentation.comment().get());
            updateIcon(presentation.icon());
            updateValid(true);
        }

        UniversalFileChooserContributor.Root getContributorRoot() {
            return myContributorRoot;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o == this || o instanceof VirtualRootNode node && myContributorRoot.id().equals(node.myContributorRoot.id());
        }

        @Override
        public int hashCode() {
            return myContributorRoot.id().hashCode();
        }
    }
}
