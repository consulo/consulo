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
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configurable.ConfigurationException;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.LibraryKind;
import consulo.content.library.LibraryProperties;
import consulo.content.library.LibraryType;
import consulo.content.library.OrderRoot;
import consulo.content.library.ui.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FilteringIterator;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author Eugene Zhuravlev
 * @since 2004-01-11
 */
public class LibraryRootsComponent implements Disposable, LibraryEditorComponent {
    static final UrlComparator ourUrlComparator = new UrlComparator();

    private BorderLayoutPanel myPanel;
    @Nullable
    private MultiLineLabel myPropertiesLabel;
    private LibraryPropertiesEditor myPropertiesEditor;
    private Tree myTree;
    private LibraryTableTreeBuilder myTreeBuilder;
    private VirtualFile myLastChosen;

    private final Collection<Runnable> myListeners = Lists.newLockFreeCopyOnWriteList();
    @Nullable
    private final Project myProject;

    private final Supplier<LibraryEditor> myLibraryEditorComputable;
    private LibraryRootsComponentDescriptor myDescriptor;
    private Module myContextModule;
    private LibraryRootsComponent.AddExcludedRootActionButton myAddExcludedRootActionButton;

    public LibraryRootsComponent(@Nullable Project project, @Nonnull LibraryEditor libraryEditor) {
        this(project, () -> libraryEditor);
    }

    public LibraryRootsComponent(@Nullable Project project, @Nonnull Supplier<LibraryEditor> libraryEditorComputable) {
        myProject = project;
        myLibraryEditorComputable = libraryEditorComputable;

        LibraryEditor editor = getLibraryEditor();
        LibraryType type = editor.getType();
        if (type != null) {
            myDescriptor = type.createLibraryRootsComponentDescriptor();
        }

        if (myDescriptor == null) {
            myDescriptor = new DefaultLibraryRootsComponentDescriptor();
        }

        init(new LibraryTreeStructure(this, myDescriptor));
        updatePropertiesLabel();
        onRootsChanged();
    }

    private void onRootsChanged() {
    }

    @Nonnull
    @Override
    public LibraryProperties getProperties() {
        return getLibraryEditor().getProperties();
    }

    @Override
    public boolean isNewLibrary() {
        return getLibraryEditor() instanceof NewLibraryEditor;
    }

    public void updatePropertiesLabel() {
        if (myPropertiesLabel == null) {
            return;
        }

        StringBuilder text = new StringBuilder();
        LibraryType<?> type = getLibraryEditor().getType();
        Set<LibraryKind> excluded =
            type != null ? Collections.<LibraryKind>singleton(type.getKind()) : Collections.<LibraryKind>emptySet();
        for (String description : LibraryPresentationManager.getInstance()
            .getDescriptions(getLibraryEditor().getFiles(BinariesOrderRootType.getInstance()), excluded)) {
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(description);
        }
        myPropertiesLabel.setText(text.toString());
    }

    private void init(AbstractTreeStructure treeStructure) {
        myPanel = new BorderLayoutPanel();

        LibraryEditor editor = getLibraryEditor();
        LibraryType type = editor.getType();
        if (!(myDescriptor instanceof DefaultLibraryRootsComponentDescriptor)) {
            myPropertiesLabel = new MultiLineLabel();
            JPanel propertiesPanel = new JPanel(new VerticalFlowLayout());
            propertiesPanel.add(myPropertiesLabel);

            myPanel.addToTop(propertiesPanel);

            //noinspection unchecked
            myPropertiesEditor = type.createPropertiesEditor(this);
            if (myPropertiesEditor != null) {
                propertiesPanel.add(myPropertiesEditor.createComponent());
            }
        }

        myTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        new LibraryRootsTreeSpeedSearch(myTree);
        myTree.setCellRenderer(new LibraryTreeRenderer());
        myTreeBuilder = new LibraryTableTreeBuilder(myTree, (DefaultTreeModel) myTree.getModel(), treeStructure);

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myTree)
            .disableUpDownActions()
            .setRemoveActionName(ProjectLocalize.libraryRemoveAction().get())
            .disableRemoveAction();
        if (myPropertiesLabel != null) {
            toolbarDecorator.setPanelBorder(new CustomLineBorder(1, 0, 0, 0));
        }
        else {
            toolbarDecorator.setPanelBorder(JBUI.Borders.empty());
        }

        List<AttachRootButtonDescriptor> popupItems = new ArrayList<>();
        for (AttachRootButtonDescriptor descriptor : myDescriptor.createAttachButtons()) {
            Image icon = descriptor.getToolbarIcon();
            if (icon != null) {
                AttachItemAction action = new AttachItemAction(descriptor, descriptor.getButtonText(), icon);
                toolbarDecorator.addExtraAction(action);
            }
            else {
                popupItems.add(descriptor);
            }
        }
        myAddExcludedRootActionButton = new AddExcludedRootActionButton();
        toolbarDecorator.addExtraAction(myAddExcludedRootActionButton);
        toolbarDecorator.addExtraAction(new AnAction("Remove", null, PlatformIconGroup.generalRemove()) {
            {
                registerCustomShortcutSet(CommonShortcuts.getDelete(), null);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Object[] selectedElements = getSelectedElements();
                if (selectedElements.length == 0) {
                    return;
                }
                Application.get().runWriteAction(() -> {
                    for (Object selectedElement : selectedElements) {
                        if (selectedElement instanceof ItemElement itemElement) {
                            getLibraryEditor().removeRoot(itemElement.getUrl(), itemElement.getRootType());
                        }
                        else if (selectedElement instanceof OrderRootTypeElement rootTypeElement) {
                            OrderRootType rootType = rootTypeElement.getOrderRootType();
                            String[] urls = getLibraryEditor().getUrls(rootType);
                            for (String url : urls) {
                                getLibraryEditor().removeRoot(url, rootType);
                            }
                        }
                        else if (selectedElement instanceof ExcludedRootElement excludedRootElement) {
                            getLibraryEditor().removeExcludedRoot(excludedRootElement.getUrl());
                        }
                    }
                });
                libraryChanged(true);
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                Object[] elements = getSelectedElements();
                Presentation presentation = e.getPresentation();
                if (ContainerUtil.and(elements, new FilteringIterator.InstanceOf<>(ExcludedRootElement.class))) {
                    presentation.setText("Cancel Exclusion");
                }
                else {
                    presentation.setTextValue(getTemplatePresentation().getTextValue());
                }
            }
        });
        toolbarDecorator.setAddAction(button -> {
            if (popupItems.isEmpty()) {
                new AttachFilesAction(myDescriptor.getAttachFilesActionName()).actionPerformed(null);
                return;
            }

            List<AnAction> actions = new ArrayList<>();
            actions.add(new AttachFilesAction(myDescriptor.getAttachFilesActionName()));
            for (AttachRootButtonDescriptor descriptor : popupItems) {
                actions.add(new AttachItemAction(descriptor, descriptor.getButtonText(), null));
            }
            DefaultActionGroup group = new DefaultActionGroup(actions);
            JBPopupFactory.getInstance()
                .createActionGroupPopup(
                    null,
                    group,
                    DataManager.getInstance().getDataContext(button.getContextComponent()),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true
                )
                .show(button.getPreferredPopupPoint());
        });

        myPanel.add(toolbarDecorator.createPanel());
        Disposer.register(this, myTreeBuilder);
    }

    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    @Nullable
    public Project getProject() {
        return myProject;
    }

    public void setContextModule(Module module) {
        myContextModule = module;
    }

    @Override
    @Nullable
    public VirtualFile getExistingRootDirectory() {
        for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
            VirtualFile[] existingRoots = getLibraryEditor().getFiles(orderRootType);
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
    @Nullable
    public VirtualFile getBaseDirectory() {
        if (myProject != null) {
            //todo[nik] perhaps we shouldn't select project base dir if global library is edited
            return myProject.getBaseDir();
        }
        return null;
    }

    @Override
    public LibraryEditor getLibraryEditor() {
        return myLibraryEditorComputable.get();
    }

    @RequiredUIAccess
    public boolean hasChanges() {
        return myPropertiesEditor != null && myPropertiesEditor.isModified() || getLibraryEditor().hasChanges();
    }

    private Object[] getSelectedElements() {
        if (myTreeBuilder == null || myTreeBuilder.isDisposed()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }
        TreePath[] selectionPaths = myTreeBuilder.getTree().getSelectionPaths();
        if (selectionPaths == null) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }
        List<Object> elements = new ArrayList<>();
        for (TreePath selectionPath : selectionPaths) {
            Object pathElement = getPathElement(selectionPath);
            if (pathElement != null) {
                elements.add(pathElement);
            }
        }
        return ArrayUtil.toObjectArray(elements);
    }

    @Nullable
    private static Object getPathElement(TreePath selectionPath) {
        if (selectionPath == null) {
            return null;
        }
        DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (lastPathComponent != null
            && lastPathComponent.getUserObject() instanceof NodeDescriptor nodeDescriptor
            && nodeDescriptor.getElement() instanceof LibraryTableTreeContentElement contentElement) {
            return contentElement;
        }
        return null;
    }

    @Override
    public void renameLibrary(String newName) {
        LibraryEditor libraryEditor = getLibraryEditor();
        libraryEditor.setName(newName);
        libraryChanged(false);
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        if (myPropertiesEditor != null) {
            myPropertiesEditor.disposeUIResources();
        }
        myTreeBuilder = null;
    }

    @RequiredUIAccess
    public void resetProperties() {
        if (myPropertiesEditor != null) {
            myPropertiesEditor.reset();
        }
    }

    @RequiredUIAccess
    public void applyProperties() {
        if (myPropertiesEditor != null && myPropertiesEditor.isModified()) {
            try {
                myPropertiesEditor.apply();
            }
            catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateRootsTree() {
        if (myTreeBuilder != null) {
            myTreeBuilder.queueUpdate();
        }
    }

    @Nullable
    private VirtualFile getFileToSelect() {
        if (myLastChosen != null) {
            return myLastChosen;
        }

        VirtualFile directory = getExistingRootDirectory();
        if (directory != null) {
            return directory;
        }
        return getBaseDirectory();
    }

    private class AttachFilesAction extends AttachItemActionBase {
        public AttachFilesAction(String title) {
            super(title);
        }

        @Override
        @RequiredUIAccess
        protected List<OrderRoot> selectRoots(@Nullable VirtualFile initialSelection) {
            String name = getLibraryEditor().getName();
            FileChooserDescriptor chooserDescriptor = myDescriptor.createAttachFilesChooserDescriptor(name);
            if (myContextModule != null) {
                chooserDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, myContextModule);
            }
            VirtualFile[] files = IdeaFileChooser.chooseFiles(chooserDescriptor, myPanel, myProject, initialSelection);
            if (files.length == 0) {
                return Collections.emptyList();
            }

            return RootDetectionUtil.detectRoots(Arrays.asList(files), myPanel, myProject, myDescriptor);
        }
    }

    public abstract class AttachItemActionBase extends DumbAwareAction {
        protected AttachItemActionBase(String text) {
            super(text);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nullable AnActionEvent e) {
            VirtualFile toSelect = getFileToSelect();
            List<OrderRoot> roots = selectRoots(toSelect);
            if (roots.isEmpty()) {
                return;
            }

            List<OrderRoot> attachedRoots = attachFiles(roots);
            OrderRoot first = ContainerUtil.getFirstItem(attachedRoots);
            if (first != null) {
                myLastChosen = first.getFile();
            }
            fireLibraryChanged();
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
        }

        protected abstract List<OrderRoot> selectRoots(@Nullable VirtualFile initialSelection);
    }

    private class AttachItemAction extends AttachItemActionBase {
        private final AttachRootButtonDescriptor myDescriptor;

        protected AttachItemAction(AttachRootButtonDescriptor descriptor, String title, Image icon) {
            super(title);
            getTemplatePresentation().setIcon(icon);
            myDescriptor = descriptor;
        }

        @Override
        protected List<OrderRoot> selectRoots(@Nullable VirtualFile initialSelection) {
            VirtualFile[] files = myDescriptor.selectFiles(
                myPanel,
                initialSelection,
                DataContext.builder().add(Module.KEY, myContextModule).build(),
                getLibraryEditor()
            );
            if (files.length == 0) {
                return Collections.emptyList();
            }

            List<OrderRoot> roots = new ArrayList<>();
            for (VirtualFile file : myDescriptor.scanForActualRoots(files, myPanel)) {
                roots.add(new OrderRoot(file, myDescriptor.getRootType(), myDescriptor.addAsJarDirectories()));
            }
            return roots;
        }
    }

    @RequiredUIAccess
    private List<OrderRoot> attachFiles(List<OrderRoot> roots) {
        List<OrderRoot> rootsToAttach = filterAlreadyAdded(roots);
        if (!rootsToAttach.isEmpty()) {
            Application.get().runWriteAction(() -> getLibraryEditor().addRoots(rootsToAttach));
            updatePropertiesLabel();
            onRootsChanged();
            myTreeBuilder.queueUpdate();
        }
        return rootsToAttach;
    }

    private List<OrderRoot> filterAlreadyAdded(@Nonnull List<OrderRoot> roots) {
        List<OrderRoot> result = new ArrayList<>();
        for (OrderRoot root : roots) {
            VirtualFile[] libraryFiles = getLibraryEditor().getFiles(root.getType());
            if (!ArrayUtil.contains(root.getFile(), libraryFiles)) {
                result.add(root);
            }
        }
        return result;
    }

    private void libraryChanged(boolean putFocusIntoTree) {
        onRootsChanged();
        updatePropertiesLabel();
        myTreeBuilder.queueUpdate();
        if (putFocusIntoTree) {
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
        }
        fireLibraryChanged();
    }

    private void fireLibraryChanged() {
        for (Runnable listener : myListeners) {
            listener.run();
        }
    }

    public void addListener(Runnable listener) {
        myListeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        myListeners.remove(listener);
    }

    private Set<VirtualFile> getNotExcludedRoots() {
        Set<VirtualFile> roots = new LinkedHashSet<>();
        String[] excludedRootUrls = getLibraryEditor().getExcludedRootUrls();
        Set<VirtualFile> excludedRoots = new HashSet<>();
        for (String url : excludedRootUrls) {
            ContainerUtil.addIfNotNull(excludedRoots, VirtualFileManager.getInstance().findFileByUrl(url));
        }
        for (OrderRootType type : OrderRootType.getAllTypes()) {
            VirtualFile[] files = getLibraryEditor().getFiles(type);
            for (VirtualFile file : files) {
                if (!VfsUtilCore.isUnder(file, excludedRoots)) {
                    roots.add(VirtualFilePathUtil.getLocalFile(file));
                }
            }
        }
        return roots;
    }

    private class AddExcludedRootActionButton extends AnAction {
        public AddExcludedRootActionButton() {
            super("Exclude", null, PlatformIconGroup.modulesAddexcludedroot());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleJavaPathDescriptor();
            descriptor.setTitle("Exclude from Library");
            descriptor.setDescription(
                "Select directories which should be excluded from the library content. Content of excluded directories won't be processed by IDE."
            );
            Set<VirtualFile> roots = getNotExcludedRoots();
            descriptor.setRoots(roots.toArray(new VirtualFile[roots.size()]));
            if (roots.size() < 2) {
                descriptor.setIsTreeRootVisible(true);
            }
            VirtualFile toSelect = null;
            for (Object o : getSelectedElements()) {
                if (o instanceof ExcludedRootElement excludedRootElem
                    && excludedRootElem.getParentDescriptor() instanceof ItemElement itemElem) {
                    toSelect = VirtualFileManager.getInstance().findFileByUrl(itemElem.getUrl());
                    break;
                }
            }

            FileChooser.chooseFiles(descriptor, myPanel, myProject, toSelect).doWhenDone(files -> {
                Application.get().runWriteAction(() -> {
                    for (VirtualFile file : files) {
                        getLibraryEditor().addExcludedRoot(file.getUrl());
                    }
                });
                myLastChosen = files[0];
                libraryChanged(true);
            });
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(!getNotExcludedRoots().isEmpty());
        }
    }
}
