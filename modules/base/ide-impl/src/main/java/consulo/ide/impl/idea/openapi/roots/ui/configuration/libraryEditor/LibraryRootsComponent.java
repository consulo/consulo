/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.LibraryKind;
import consulo.content.library.LibraryProperties;
import consulo.content.library.LibraryType;
import consulo.content.library.OrderRoot;
import consulo.content.library.ui.AttachRootButtonDescriptor;
import consulo.content.library.ui.DefaultLibraryRootsComponentDescriptor;
import consulo.content.library.ui.LibraryEditor;
import consulo.content.library.ui.LibraryEditorComponent;
import consulo.content.library.ui.LibraryPropertiesEditor;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.ui.OrderRootTypePresentation;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilder;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFilePresentation;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.http.HttpFileSystem;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class LibraryRootsComponent implements Disposable, LibraryEditorComponent {
    private static final Logger LOG = Logger.getInstance(LibraryRootsComponent.class);

    static final UrlComparator ourUrlComparator = new UrlComparator();

    private record RootPresentation(LocalizeValue text, Image icon, boolean valid) {
    }

    private final @Nullable Project myProject;
    private final Supplier<LibraryEditor> myLibraryEditorComputable;
    private final LibraryRootsComponentDescriptor myDescriptor;
    private final Collection<Runnable> myListeners = Lists.newLockFreeCopyOnWriteList();
    private final Map<Object, RootPresentation> myPresentations = new ConcurrentHashMap<>();

    private @Nullable Module myContextModule;
    private @Nullable VirtualFile myLastChosen;
    private @Nullable LibraryPropertiesEditor myPropertiesEditor;
    private @Nullable HtmlLabel myPropertiesLabel;
    private @Nullable Tree<Object> myTree;
    private @Nullable Component myComponent;

    private volatile List<Object> mySelectedValues = List.of();

    public LibraryRootsComponent(@Nullable Project project, LibraryEditor libraryEditor) {
        this(project, () -> libraryEditor);
    }

    public LibraryRootsComponent(@Nullable Project project, Supplier<LibraryEditor> libraryEditorComputable) {
        myProject = project;
        myLibraryEditorComputable = libraryEditorComputable;

        LibraryType type = getLibraryEditor().getType();
        LibraryRootsComponentDescriptor descriptor = type != null ? type.createLibraryRootsComponentDescriptor() : null;
        myDescriptor = descriptor != null ? descriptor : new DefaultLibraryRootsComponentDescriptor();
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public Component createUIComponent(Disposable parentUIDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        DockLayout panel = DockLayout.create();

        if (!(myDescriptor instanceof DefaultLibraryRootsComponentDescriptor)) {
            VerticalLayout propertiesPanel = VerticalLayout.create();

            HtmlLabel propertiesLabel = HtmlLabel.create(LocalizeValue.empty());
            myPropertiesLabel = propertiesLabel;
            propertiesPanel.add(propertiesLabel);

            LibraryType type = getLibraryEditor().getType();
            LibraryPropertiesEditor propertiesEditor = type == null ? null : type.createPropertiesEditor(this);
            myPropertiesEditor = propertiesEditor;
            if (propertiesEditor != null) {
                Component propertiesComponent = ConfigurableUIMigrationUtil.createUIComponent(propertiesEditor, parentUIDisposable);
                if (propertiesComponent != null) {
                    propertiesPanel.add(propertiesComponent);
                }
            }

            panel.top(propertiesPanel);
            updatePropertiesLabel();
        }

        Tree<Object> tree = Tree.create(new RootsTreeModel());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> {
            Object value = node.getValue();
            RootPresentation presentation = value == null ? null : myPresentations.get(value);
            return presentation == null ? "" : presentation.text().get();
        });
        tree.addSelectListener(event -> mySelectedValues = collectSelectedValues(tree));
        Disposer.register(parentUIDisposable, tree.destroyHook());
        myTree = tree;

        ToolbarDecoratorBuilder<Object> builder = ToolbarDecoratorBuilder.newBuilder(tree)
            .addOrReplaceAction(new AttachRootsAction())
            .disableAction(RemoveAction.class)
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class);

        for (AttachRootButtonDescriptor descriptor : myDescriptor.createAttachButtons()) {
            Image icon = descriptor.getToolbarIcon();
            if (icon != null) {
                builder.addExtraAction(new AttachItemAction(descriptor, descriptor.getButtonText(), icon));
            }
        }
        builder.addExtraAction(new ExcludeRootAction());
        builder.addExtraAction(new RemoveRootAction());

        panel.center(builder.build());

        tree.expandAll();

        myComponent = panel;
        return panel;
    }

    @Override
    public LibraryProperties getProperties() {
        return getLibraryEditor().getProperties();
    }

    @Override
    public boolean isNewLibrary() {
        return getLibraryEditor() instanceof NewLibraryEditor;
    }

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    public void setContextModule(Module module) {
        myContextModule = module;
    }

    @Override
    public LibraryEditor getLibraryEditor() {
        return myLibraryEditorComputable.get();
    }

    @Override
    public @Nullable VirtualFile getExistingRootDirectory() {
        for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
            VirtualFile[] existingRoots = getLibraryEditor().getFiles(orderRootType.getId());
            if (existingRoots.length > 0) {
                VirtualFile existingRoot = existingRoots[0];
                if (existingRoot.getFileSystem() instanceof ArchiveFileSystem archiveFileSystem) {
                    existingRoot = archiveFileSystem.getLocalVirtualFileFor(existingRoot);
                }
                if (existingRoot != null) {
                    return existingRoot.isDirectory() ? existingRoot : existingRoot.getParent();
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable VirtualFile getBaseDirectory() {
        return myProject != null ? myProject.getBaseDir() : null;
    }

    @RequiredUIAccess
    public boolean hasChanges() {
        LibraryPropertiesEditor propertiesEditor = myPropertiesEditor;
        return propertiesEditor != null && propertiesEditor.isModified() || getLibraryEditor().hasChanges();
    }

    @Override
    public void renameLibrary(String newName) {
        getLibraryEditor().setName(newName);
        libraryChanged();
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        LibraryPropertiesEditor propertiesEditor = myPropertiesEditor;
        if (propertiesEditor != null) {
            propertiesEditor.disposeUIResources();
        }
        myPropertiesEditor = null;
        myPropertiesLabel = null;
        myTree = null;
        myComponent = null;
    }

    @RequiredUIAccess
    public void resetProperties() {
        LibraryPropertiesEditor propertiesEditor = myPropertiesEditor;
        if (propertiesEditor != null) {
            propertiesEditor.reset();
        }
    }

    @RequiredUIAccess
    public void applyProperties() {
        LibraryPropertiesEditor propertiesEditor = myPropertiesEditor;
        if (propertiesEditor != null && propertiesEditor.isModified()) {
            try {
                propertiesEditor.apply();
            }
            catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @RequiredUIAccess
    public void updatePropertiesLabel() {
        HtmlLabel propertiesLabel = myPropertiesLabel;
        if (propertiesLabel == null) {
            return;
        }

        LibraryType<?> type = getLibraryEditor().getType();
        Set<LibraryKind> excluded = type != null ? Collections.singleton(type.getKind()) : Collections.emptySet();

        List<String> descriptions = new ArrayList<>();
        for (String description : LibraryPresentationManager.getInstance()
            .getDescriptions(getLibraryEditor().getFiles(BinariesOrderRootType.ID), excluded)) {
            descriptions.add(XmlStringUtil.escapeText(description));
        }
        propertiesLabel.setText(LocalizeValue.of(String.join("<br>", descriptions)));
        propertiesLabel.setVisible(!descriptions.isEmpty());
    }

    @RequiredUIAccess
    @Override
    public void updateRootsTree() {
        Tree<Object> tree = myTree;
        if (tree != null) {
            UIAccess uiAccess = UIAccess.current();
            tree.refreshAll().whenCompleteAsync((ignored, error) -> {
                mySelectedValues = collectSelectedValues(tree);
                tree.expandAll();
            }, uiAccess);
        }
    }

    public void addListener(Runnable listener) {
        myListeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        myListeners.remove(listener);
    }

    @RequiredUIAccess
    private void libraryChanged() {
        updatePropertiesLabel();
        updateRootsTree();
        for (Runnable listener : myListeners) {
            listener.run();
        }
    }

    private static List<Object> collectSelectedValues(Tree<Object> tree) {
        List<Object> values = new ArrayList<>();
        for (TreeNode<Object> node : tree.getSelectedNodes()) {
            Object value = node.getValue();
            if (value != null) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private @Nullable VirtualFile getFileToSelect() {
        if (myLastChosen != null) {
            return myLastChosen;
        }

        VirtualFile directory = getExistingRootDirectory();
        if (directory != null) {
            return directory;
        }
        return getBaseDirectory();
    }

    @RequiredUIAccess
    private void attachRoots(CompletableFuture<List<OrderRoot>> rootsFuture) {
        UIAccess uiAccess = UIAccess.current();
        rootsFuture.whenCompleteAsync((roots, error) -> {
            if (error != null || roots.isEmpty()) {
                return;
            }
            List<OrderRoot> rootsToAttach = filterAlreadyAdded(roots);
            if (rootsToAttach.isEmpty()) {
                return;
            }

            Application.get().runWriteAction(() -> getLibraryEditor().addRoots(rootsToAttach));
            myLastChosen = rootsToAttach.get(0).getFile();
            libraryChanged();
         }, uiAccess);
    }

    private List<OrderRoot> filterAlreadyAdded(List<OrderRoot> roots) {
        List<OrderRoot> result = new ArrayList<>();
        for (OrderRoot root : roots) {
            VirtualFile[] libraryFiles = getLibraryEditor().getFiles(root.getType());
            if (!ArrayUtil.contains(root.getFile(), libraryFiles)) {
                result.add(root);
            }
        }
        return result;
    }

    private Set<VirtualFile> getNotExcludedRoots() {
        Set<VirtualFile> roots = new LinkedHashSet<>();
        Set<VirtualFile> excludedRoots = new HashSet<>();
        for (String url : getLibraryEditor().getExcludedRootUrls()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
            if (file != null) {
                excludedRoots.add(file);
            }
        }
        for (OrderRootType type : OrderRootType.getAllTypes()) {
            for (VirtualFile file : getLibraryEditor().getFiles(type.getId())) {
                if (!VirtualFileUtil.isUnder(file, excludedRoots)) {
                    roots.add(VirtualFilePathUtil.getLocalFile(file));
                }
            }
        }
        return roots;
    }

    private static String getPresentablePath(String url) {
        String presentablePath = VirtualFileManager.extractPath(url);
        if (isArchiveFileRoot(url)) {
            presentablePath = presentablePath.substring(0, presentablePath.length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
        }
        return presentablePath;
    }

    private static boolean isArchiveFileRoot(String url) {
        return VirtualFileManager.extractPath(url).endsWith(ArchiveFileSystem.ARCHIVE_SEPARATOR);
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
        if (presentableFile.getFileSystem() instanceof HttpFileSystem) {
            return PlatformIconGroup.nodesPpweb();
        }
        if (!presentableFile.isDirectory()) {
            return VirtualFilePresentation.getIcon(presentableFile);
        }
        if (isJarDirectory) {
            return PlatformIconGroup.nodesJardirectory();
        }
        return PlatformIconGroup.nodesTreeclosed();
    }

    private OrderRootTypePresentation getRootTypePresentation(OrderRootType type) {
        OrderRootTypePresentation presentation = myDescriptor.getRootTypePresentation(type);
        if (presentation != null) {
            return presentation;
        }

        OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.forOrderType(type);
        if (factory == null) {
            LOG.error("No OrderRootTypeUIFactory for " + type.getId());
            return new OrderRootTypePresentation(type.getId(), PlatformIconGroup.nodesFolder());
        }
        return new OrderRootTypePresentation(factory.getNodeText().get(), factory.getIcon());
    }

    private void render(Object value, TextItemPresentation presentation) {
        RootPresentation rootPresentation = myPresentations.get(value);
        if (rootPresentation == null) {
            return;
        }

        presentation.withIcon(rootPresentation.icon());
        presentation.append(rootPresentation.text(), rootPresentation.valid() ? TextAttribute.REGULAR : TextAttribute.ERROR);
    }

    private class RootsTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            LibraryEditor libraryEditor = getLibraryEditor();
            if (parentValue == null) {
                for (OrderRootType type : myDescriptor.getRootTypes()) {
                    if (libraryEditor.getUrls(type.getId()).length == 0) {
                        continue;
                    }

                    OrderRootTypePresentation typePresentation = getRootTypePresentation(type);
                    LibraryRootTypeValue value = new LibraryRootTypeValue(type.getId());
                    myPresentations.put(value, new RootPresentation(LocalizeValue.of(typePresentation.getNodeText()), typePresentation.getIcon(), true));
                    add(nodeFactory, value, false);
                }
            }
            else if (parentValue instanceof LibraryRootTypeValue typeValue) {
                String rootType = typeValue.rootType();
                String[] urls = libraryEditor.getUrls(rootType).clone();
                Arrays.sort(urls, ourUrlComparator);
                String[] excludedUrls = libraryEditor.getExcludedRootUrls();
                for (String url : urls) {
                    boolean valid = libraryEditor.isValid(url, rootType);
                    boolean jarDirectory = libraryEditor.isJarDirectory(url, rootType);
                    LibraryRootValue value = new LibraryRootValue(rootType, url);
                    LocalizeValue text = LocalizeValue.of(getPresentablePath(url).replace('/', File.separatorChar));
                    myPresentations.put(value, new RootPresentation(text, getIconForUrl(url, valid, jarDirectory), valid));

                    boolean hasExcluded = false;
                    for (String excludedUrl : excludedUrls) {
                        if (VirtualFileUtil.isEqualOrAncestor(url, excludedUrl)) {
                            hasExcluded = true;
                            break;
                        }
                    }
                    add(nodeFactory, value, !hasExcluded);
                }
            }
            else if (parentValue instanceof LibraryRootValue rootValue) {
                List<String> excludedUrls = new ArrayList<>();
                for (String excludedUrl : libraryEditor.getExcludedRootUrls()) {
                    if (VirtualFileUtil.isEqualOrAncestor(rootValue.url(), excludedUrl)) {
                        excludedUrls.add(excludedUrl);
                    }
                }
                excludedUrls.sort(ourUrlComparator);

                for (String excludedUrl : excludedUrls) {
                    String text;
                    if (excludedUrl.startsWith(rootValue.url())) {
                        String relativePath = StringUtil.trimStart(excludedUrl.substring(rootValue.url().length()), "/");
                        text = relativePath.isEmpty() ? "<all>" : relativePath;
                    }
                    else {
                        text = getPresentablePath(excludedUrl);
                    }

                    boolean valid = VirtualFileManager.getInstance().findFileByUrl(excludedUrl) != null;
                    LibraryExcludedRootValue value = new LibraryExcludedRootValue(rootValue.url(), excludedUrl);
                    myPresentations.put(value, new RootPresentation(LocalizeValue.of(text), PlatformIconGroup.modulesExcluderoot(), valid));
                    add(nodeFactory, value, true);
                }
            }
        }

        private void add(Function<Object, TreeNode<Object>> nodeFactory, Object value, boolean leaf) {
            TreeNode<Object> node = nodeFactory.apply(value);
            node.setLeaf(leaf);
            node.setRenderer(LibraryRootsComponent.this::render);
        }
    }

    private class AttachRootsAction extends AddAction<Object> {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            List<AttachRootButtonDescriptor> popupItems = new ArrayList<>();
            for (AttachRootButtonDescriptor descriptor : myDescriptor.createAttachButtons()) {
                if (descriptor.getToolbarIcon() == null) {
                    popupItems.add(descriptor);
                }
            }

            AttachFilesAction attachFilesAction = new AttachFilesAction(myDescriptor.getAttachFilesActionName());
            if (popupItems.isEmpty()) {
                attachFilesAction.attach();
                return;
            }

            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            group.add(attachFilesAction);
            for (AttachRootButtonDescriptor descriptor : popupItems) {
                group.add(new AttachItemAction(descriptor, descriptor.getButtonText(), null));
            }

            JBPopupFactory.getInstance()
                .createActionGroupPopup(null, group.build(), e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
                .showUnderneathOf(e);
        }
    }

    private abstract class AttachItemActionBase extends DumbAwareAction {
        protected AttachItemActionBase(LocalizeValue text, @Nullable Image icon) {
            super(text, LocalizeValue.empty(), icon);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            attach();
        }

        @RequiredUIAccess
        void attach() {
            attachRoots(selectRoots(getFileToSelect()));
        }

        @RequiredUIAccess
        protected abstract CompletableFuture<List<OrderRoot>> selectRoots(@Nullable VirtualFile initialSelection);
    }

    private class AttachFilesAction extends AttachItemActionBase {
        private AttachFilesAction(LocalizeValue title) {
            super(title, null);
        }

        @Override
        @RequiredUIAccess
        protected CompletableFuture<List<OrderRoot>> selectRoots(@Nullable VirtualFile initialSelection) {
            FileChooserDescriptor chooserDescriptor = myDescriptor.createAttachFilesChooserDescriptor(getLibraryEditor().getName());
            Module contextModule = myContextModule;
            if (contextModule != null) {
                chooserDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, contextModule);
            }

            UIAccess uiAccess = UIAccess.current();
            Component parent = myTree;
            return FileChooser.chooseFiles(chooserDescriptor, myProject, initialSelection).thenComposeAsync(
                files -> RootDetectionUtil.detectRootsAsync(Arrays.asList(files), parent, myProject, myDescriptor),
                uiAccess
            );
        }
    }

    private class AttachItemAction extends AttachItemActionBase {
        private final AttachRootButtonDescriptor myAttachDescriptor;

        private AttachItemAction(AttachRootButtonDescriptor descriptor, LocalizeValue title, @Nullable Image icon) {
            super(title, icon);
            myAttachDescriptor = descriptor;
        }

        @Override
        @RequiredUIAccess
        protected CompletableFuture<List<OrderRoot>> selectRoots(@Nullable VirtualFile initialSelection) {
            DataContext dataContext = DataContext.builder().add(Module.KEY, myContextModule).build();
            return myAttachDescriptor.selectFiles(myTree, initialSelection, dataContext, getLibraryEditor()).thenApply(files -> {
                List<OrderRoot> roots = new ArrayList<>();
                for (VirtualFile file : myAttachDescriptor.scanForActualRoots(files)) {
                    roots.add(new OrderRoot(file, myAttachDescriptor.getRootType(), myAttachDescriptor.addAsJarDirectories()));
                }
                return roots;
            });
        }
    }

    private class RemoveRootAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveRootAction() {
            super(ProjectLocalize.libraryRemoveAction(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = mySelectedValues;
            if (selected.isEmpty()) {
                return;
            }

            LibraryEditor libraryEditor = getLibraryEditor();
            Application.get().runWriteAction(() -> {
                for (Object value : selected) {
                    if (value instanceof LibraryRootValue rootValue) {
                        libraryEditor.removeRoot(rootValue.url(), rootValue.rootType());
                    }
                    else if (value instanceof LibraryRootTypeValue typeValue) {
                        for (String url : libraryEditor.getUrls(typeValue.rootType())) {
                            libraryEditor.removeRoot(url, typeValue.rootType());
                        }
                    }
                    else if (value instanceof LibraryExcludedRootValue excludedValue) {
                        libraryEditor.removeExcludedRoot(excludedValue.url());
                    }
                }
            });
            libraryChanged();
        }

        @Override
        public void update(AnActionEvent e) {
            List<Object> selected = mySelectedValues;
            boolean allExcluded = !selected.isEmpty();
            for (Object value : selected) {
                if (!(value instanceof LibraryExcludedRootValue)) {
                    allExcluded = false;
                    break;
                }
            }

            e.getPresentation().setEnabled(!selected.isEmpty());
            e.getPresentation().setText(allExcluded ? ProjectLocalize.libraryCancelExclusionAction() : ProjectLocalize.libraryRemoveAction());
        }
    }

    private class ExcludeRootAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ExcludeRootAction() {
            super(ProjectLocalize.libraryExcludeAction(), LocalizeValue.empty(), PlatformIconGroup.modulesAddexcludedroot());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Set<VirtualFile> roots = getNotExcludedRoots();
            if (roots.isEmpty()) {
                return;
            }

            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleJavaPathDescriptor();
            descriptor.withTitle(ProjectLocalize.libraryExcludeChooserTitle());
            descriptor.withDescription(ProjectLocalize.libraryExcludeChooserDescription());
            descriptor.setRoots(roots.toArray(VirtualFile.EMPTY_ARRAY));
            if (roots.size() < 2) {
                descriptor.setIsTreeRootVisible(true);
            }

            VirtualFile toSelect = null;
            for (Object value : mySelectedValues) {
                if (value instanceof LibraryRootValue rootValue) {
                    toSelect = VirtualFileManager.getInstance().findFileByUrl(rootValue.url());
                    break;
                }
            }

            UIAccess uiAccess = UIAccess.current();
            FileChooser.chooseFiles(descriptor, myProject, toSelect).whenCompleteAsync((files, error) -> {
                if (error != null || files.length == 0) {
                    return;
                }
                Application.get().runWriteAction(() -> {
                    for (VirtualFile file : files) {
                        getLibraryEditor().addExcludedRoot(file.getUrl());
                    }
                });
                myLastChosen = files[0];
                libraryChanged();
             }, uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!getNotExcludedRoots().isEmpty());
        }
    }
}
