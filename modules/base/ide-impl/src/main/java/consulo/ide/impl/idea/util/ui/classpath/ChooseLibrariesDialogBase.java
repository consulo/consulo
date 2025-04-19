/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.ui.classpath;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.function.CommonProcessors;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.disposer.Disposer;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Gregory.Shrago
 */
@Deprecated
@DeprecationInfo("Use ChooseLibrariesFromTablesDialog")
public abstract class ChooseLibrariesDialogBase extends DialogWrapper {
    private final SimpleTree myTree = new SimpleTree();
    private AbstractTreeBuilder myBuilder;
    private List<Library> myResult;
    private final Map<Object, Object> myParentsMap = new HashMap<>();

    protected ChooseLibrariesDialogBase(JComponent parentComponent, @Nonnull LocalizeValue title) {
        super(parentComponent, false);
        setTitle(title);
    }

    protected ChooseLibrariesDialogBase(Project project, @Nonnull LocalizeValue title) {
        super(project, false);
        setTitle(title);
    }

    @Override
    protected void init() {
        super.init();
        updateOKAction();
    }

    private static String notEmpty(String nodeText) {
        return StringUtil.isNotEmpty(nodeText) ? nodeText : "<unnamed>";
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.util.ui.classpath.ChooseLibrariesDialog";
    }

    protected int getLibraryTableWeight(@Nonnull LibraryTable libraryTable) {
        return 0;
    }

    protected boolean isAutoExpandLibraryTable(@Nonnull LibraryTable libraryTable) {
        return false;
    }

    @Override
    protected void doOKAction() {
        processSelection(new CommonProcessors.CollectProcessor<>(myResult = new ArrayList<>()));
        super.doOKAction();
    }

    private void updateOKAction() {
        setOKActionEnabled(!processSelection(new CommonProcessors.FindFirstProcessor<>()));
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTree;
    }

    @Nonnull
    public List<Library> getSelectedLibraries() {
        return myResult == null ? Collections.<Library>emptyList() : myResult;
    }

    protected void queueUpdateAndSelect(@Nonnull Library library) {
        myBuilder.queueUpdate().doWhenDone(() -> myBuilder.select(library));
    }

    private boolean processSelection(Predicate<Library> processor) {
        for (Object element : myBuilder.getSelectedElements()) {
            if (element instanceof Library library && !processor.test(library)) {
                return false;
            }
        }
        return true;
    }

    protected boolean acceptsElement(Object element) {
        return true;
    }

    @Override
    protected JComponent createNorthPanel() {
        return null;
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        myBuilder = new SimpleTreeBuilder(myTree, new DefaultTreeModel(new DefaultMutableTreeNode()),
            new MyStructure(getProject()),
            WeightBasedComparator.FULL_INSTANCE
        );
        myBuilder.initRootNode();

        myTree.setDragEnabled(false);

        myTree.setShowsRootHandles(true);
        UIUtil.setLineStyleAngled(myTree);

        myTree.setRootVisible(false);
        myTree.addTreeSelectionListener(e -> updateOKAction());
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                if (isOKActionEnabled()) {
                    doOKAction();
                    return true;
                }
                return false;
            }
        }.installOn(myTree);

        myTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ENTER");
        myTree.getActionMap().put("ENTER", getOKAction());
        JScrollPane pane = ScrollPaneFactory.createScrollPane(myTree);
        pane.setPreferredSize(new Dimension(300, 80));
        return pane;
    }

    @Nonnull
    protected Project getProject() {
        return ProjectManager.getInstance().getDefaultProject();
    }

    protected LibrariesTreeNodeBase<Library> createLibraryDescriptor(NodeDescriptor parentDescriptor, Library library) {
        return new LibraryDescriptor(getProject(), parentDescriptor, library);
    }

    @RequiredReadAction
    protected void collectChildren(Object element, List<Object> result) {
        if (element instanceof Project project) {
            Collections.addAll(result, ModuleManager.getInstance(project).getModules());
            result.add(LibraryTablesRegistrar.getInstance().getLibraryTable((Project)element));
        }
        else if (element instanceof LibraryTable libraryTable) {
            Collections.addAll(result, libraryTable.getLibraries());
        }
        else if (element instanceof Module module) {
            for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
                if (entry instanceof LibraryOrderEntry libraryOrderEntry
                    && LibraryEx.MODULE_LEVEL.equals(libraryOrderEntry.getLibraryLevel())) {
                    Library library = libraryOrderEntry.getLibrary();
                    result.add(library);
                }
            }
        }
    }

    @Override
    protected void dispose() {
        Disposer.dispose(myBuilder);
        super.dispose();
    }

    protected static class LibrariesTreeNodeBase<T> extends SimpleNode {
        private final T myElement;

        protected LibrariesTreeNodeBase(Project project, NodeDescriptor parentDescriptor, T element) {
            super(project, parentDescriptor);
            myElement = element;
        }

        @Override
        public T getElement() {
            return myElement;
        }

        @Override
        public SimpleNode[] getChildren() {
            return NO_CHILDREN;
        }

        @Override
        public int getWeight() {
            return 0;
        }

        @Nonnull
        @Override
        public Object[] getEqualityObjects() {
            return new Object[]{myElement};
        }

        @Override
        protected void update(PresentationData presentation) {
            //todo[nik] this is workaround for bug in getTemplatePresentation().setIcons()
            presentation.setIcon(getTemplatePresentation().getIcon());
        }
    }

    private static class RootDescriptor extends LibrariesTreeNodeBase<Object> {
        protected RootDescriptor(Project project) {
            super(project, null, ApplicationManager.getApplication());
        }
    }

    private static class ProjectDescriptor extends LibrariesTreeNodeBase<Project> {
        protected ProjectDescriptor(Project project, Project element) {
            super(project, null, element);
            getTemplatePresentation().setIcon(Application.get().getIcon());
            getTemplatePresentation().addText(notEmpty(getElement().getName()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private static class ModuleDescriptor extends LibrariesTreeNodeBase<Module> {
        protected ModuleDescriptor(Project project, NodeDescriptor parentDescriptor, Module element) {
            super(project, parentDescriptor, element);
            PresentationData templatePresentation = getTemplatePresentation();
            templatePresentation.setIcon(PlatformIconGroup.nodesModule());
            templatePresentation.addText(notEmpty(element.getName()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Override
        public int getWeight() {
            return 1;
        }
    }

    private static class LibraryDescriptor extends LibrariesTreeNodeBase<Library> {
        protected LibraryDescriptor(Project project, NodeDescriptor parentDescriptor, Library element) {
            super(project, parentDescriptor, element);
            Consumer<ColoredTextContainer> renderForLibrary =
                OrderEntryAppearanceService.getInstance().getRenderForLibrary(project, element, false);

            ColoredStringBuilder builder = new ColoredStringBuilder();
            renderForLibrary.accept(builder);

            PresentationData templatePresentation = getTemplatePresentation();
            templatePresentation.setIcon(builder.getIcon());
            templatePresentation.addText(notEmpty(builder.toString()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private static class LibraryTableDescriptor extends LibrariesTreeNodeBase<LibraryTable> {
        private final int myWeight;
        private boolean myAutoExpand;

        protected LibraryTableDescriptor(
            Project project,
            NodeDescriptor parentDescriptor,
            LibraryTable table,
            int weight,
            boolean autoExpand
        ) {
            super(project, parentDescriptor, table);
            myWeight = weight;
            myAutoExpand = autoExpand;
            getTemplatePresentation().setIcon(PlatformIconGroup.nodesPplib());
            String nodeText = table.getPresentation().getDisplayName(true);
            getTemplatePresentation().addText(notEmpty(nodeText), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Override
        public boolean isAutoExpandNode() {
            return myAutoExpand;
        }

        @Override
        public int getWeight() {
            return myWeight;
        }
    }

    @RequiredReadAction
    public boolean isEmpty() {
        List<Object> children = new ArrayList<>();
        collectChildren(myBuilder.getTreeStructure().getRootElement(), children);
        return children.isEmpty();
    }

    private class MyStructure extends AbstractTreeStructure {
        private final Project myProject;

        public MyStructure(Project project) {
            myProject = project;
        }

        @Nonnull
        @Override
        public Object getRootElement() {
            return ApplicationManager.getApplication();
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public Object[] getChildElements(@Nonnull Object element) {
            List<Object> result = new ArrayList<>();
            collectChildren(element, result);
            Iterator<Object> it = result.iterator();
            while (it.hasNext()) {
                if (!acceptsElement(it.next())) {
                    it.remove();
                }
            }
            for (Object o : result) {
                myParentsMap.put(o, element);
            }
            return result.toArray();
        }

        @Override
        public Object getParentElement(@Nonnull Object element) {
            return switch (element) {
                case Application a -> null;
                case Project p -> ApplicationManager.getApplication();
                case Module m -> m.getProject();
                case LibraryTable lt -> myParentsMap.get(lt);
                case Library l -> myParentsMap.get(l);
                default -> throw new AssertionError();
            };
        }

        @Nonnull
        @Override
        public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
            return switch (element) {
                case Application a -> new RootDescriptor(myProject);
                case Project p -> new ProjectDescriptor(myProject, p);
                case Module m -> new ModuleDescriptor(myProject, parentDescriptor, m);
                case LibraryTable lt -> new LibraryTableDescriptor(
                    myProject,
                    parentDescriptor,
                    lt,
                    getLibraryTableWeight(lt),
                    isAutoExpandLibraryTable(lt)
                );
                case Library l -> createLibraryDescriptor(parentDescriptor, l);
                default -> throw new AssertionError();
            };
        }

        @Override
        public void commit() {
        }

        @Override
        public boolean hasSomethingToCommit() {
            return false;
        }
    }
}
