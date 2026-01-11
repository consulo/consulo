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
package consulo.ide.impl.idea.ide.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectTreeStructure;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectTreeBuilder;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameModel;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePanel;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopupComponent;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileCellRenderer;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.editor.ui.TreeFileChooser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.search.FilenameIndex;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.PsiFileNode;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.tree.AlphaComparator;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class TreeFileChooserDialog extends DialogWrapper implements TreeFileChooser {
    private Tree myTree;
    private PsiFile mySelectedFile = null;
    private final Project myProject;
    private BaseProjectTreeBuilder myBuilder;
    private TabbedPaneWrapper myTabbedPane;
    private ChooseByNamePanel myGotoByNamePanel;
    @Nullable
    private final PsiFile myInitialFile;
    @Nullable
    private final Predicate<PsiFile> myFilter;
    @Nullable
    private final FileType myFileType;

    private final boolean myDisableStructureProviders;
    private final boolean myShowLibraryContents;
    private boolean mySelectSearchByNameTab = false;

    public TreeFileChooserDialog(
        Project project,
        String title,
        @Nullable PsiFile initialFile,
        @Nullable FileType fileType,
        @Nullable Predicate<PsiFile> filter,
        boolean disableStructureProviders,
        boolean showLibraryContents
    ) {
        super(project, true);
        myInitialFile = initialFile;
        myFilter = filter;
        myFileType = fileType;
        myDisableStructureProviders = disableStructureProviders;
        myShowLibraryContents = showLibraryContents;
        setTitle(title);
        myProject = project;
        init();
        if (initialFile != null) {
            // dialog does not exist yet
            SwingUtilities.invokeLater(() -> selectFile(initialFile));
        }

        SwingUtilities.invokeLater(this::handleSelectionChanged);
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
        myTree = new Tree(model);

        ProjectAbstractTreeStructureBase treeStructure = new AbstractProjectTreeStructure(myProject) {
            @Override
            public boolean isFlattenPackages() {
                return false;
            }

            @Override
            public boolean isShowMembers() {
                return false;
            }

            @Override
            public boolean isHideEmptyMiddlePackages() {
                return true;
            }

            @Override
            public Object[] getChildElements(@Nonnull Object element) {
                return filterFiles(super.getChildElements(element));
            }

            @Override
            public boolean isAbbreviatePackageNames() {
                return false;
            }

            @Override
            public boolean isShowLibraryContents() {
                return myShowLibraryContents;
            }

            @Override
            public boolean isShowModules() {
                return false;
            }

            @Override
            public List<TreeStructureProvider> getProviders() {
                return myDisableStructureProviders ? null : super.getProviders();
            }
        };
        myBuilder = new ProjectTreeBuilder(myProject, myTree, model, AlphaComparator.INSTANCE, treeStructure);

        myTree.setRootVisible(false);
        myTree.expandRow(0);
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setCellRenderer(new NodeRenderer());
        UIUtil.setLineStyleAngled(myTree);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        myTree.addKeyListener(new KeyAdapter() {
            @Override
            @RequiredReadAction
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    doOKAction();
                }
            }
        });

        new DoubleClickListener() {
            @Override
            @RequiredReadAction
            protected boolean onDoubleClick(MouseEvent e) {
                TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
                if (path != null && myTree.isPathSelected(path)) {
                    doOKAction();
                    return true;
                }
                return false;
            }
        }.installOn(myTree);

        myTree.addTreeSelectionListener(e -> handleSelectionChanged());

        new TreeSpeedSearch(myTree);

        myTabbedPane = new TabbedPaneWrapper(getDisposable());

        final JPanel dummyPanel = new JPanel(new BorderLayout());
        String name = null;
        if (myInitialFile != null) {
            name = myInitialFile.getName();
        }
        PsiElement context = myInitialFile == null ? null : myInitialFile;
        myGotoByNamePanel = new ChooseByNamePanel(myProject, new MyGotoFileModel(), name, true, context) {
            @Override
            @RequiredReadAction
            protected void close(boolean isOk) {
                super.close(isOk);

                if (isOk) {
                    doOKAction();
                }
                else {
                    doCancelAction();
                }
            }

            @Override
            protected void initUI(
                ChooseByNamePopupComponent.Callback callback,
                ModalityState modalityState,
                boolean allowMultipleSelection
            ) {
                super.initUI(callback, modalityState, allowMultipleSelection);
                dummyPanel.add(myGotoByNamePanel.getPanel(), BorderLayout.CENTER);
                //IdeFocusTraversalPolicy.getPreferredFocusedComponent(myGotoByNamePanel.getPanel()).requestFocus();
                if (mySelectSearchByNameTab) {
                    myTabbedPane.setSelectedIndex(1);
                }
            }

            @Override
            protected void showTextFieldPanel() {
            }

            @Override
            @RequiredReadAction
            protected void chosenElementMightChange() {
                handleSelectionChanged();
            }
        };

        myTabbedPane.addTab(IdeLocalize.tabChooserProject().get(), scrollPane);
        myTabbedPane.addTab(IdeLocalize.tabChooserSearchByName().get(), dummyPanel);

        SwingUtilities.invokeLater(() -> myGotoByNamePanel.invoke(
            new MyCallback(),
            IdeaModalityState.stateForComponent(getRootPane()),
            false
        ));

        myTabbedPane.addChangeListener(e -> handleSelectionChanged());

        return myTabbedPane.getComponent();
    }

    public void selectSearchByNameTab() {
        mySelectSearchByNameTab = true;
    }

    @RequiredReadAction
    private void handleSelectionChanged() {
        PsiFile selection = calcSelectedClass();
        setOKActionEnabled(selection != null);
    }

    @Override
    @RequiredReadAction
    protected void doOKAction() {
        mySelectedFile = calcSelectedClass();
        if (mySelectedFile == null) {
            return;
        }
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        mySelectedFile = null;
        super.doCancelAction();
    }

    @Override
    public PsiFile getSelectedFile() {
        return mySelectedFile;
    }

    @Override
    public void selectFile(@Nonnull PsiFile file) {
        // Select element in the tree
        Application.get().invokeLater(
            () -> {
                if (myBuilder != null) {
                    myBuilder.select(file, file.getVirtualFile(), true);
                }
            },
            IdeaModalityState.stateForComponent(getWindow())
        );
    }

    @Override
    @RequiredUIAccess
    public void showDialog() {
        show();
    }

    @RequiredReadAction
    private PsiFile calcSelectedClass() {
        if (myTabbedPane.getSelectedIndex() == 1) {
            return (PsiFile) myGotoByNamePanel.getChosenElement();
        }
        else {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (!(userObject instanceof ProjectViewNode pvNode)) {
                return null;
            }
            VirtualFile vFile = pvNode.getVirtualFile();
            if (vFile != null && !vFile.isDirectory()) {
                return PsiManager.getInstance(myProject).findFile(vFile);
            }

            return null;
        }
    }

    @Override
    public void dispose() {
        if (myBuilder != null) {
            Disposer.dispose(myBuilder);
            myBuilder = null;
        }
        super.dispose();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.ide.util.TreeFileChooserDialog";
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTree;
    }

    private final class MyGotoFileModel implements ChooseByNameModel {
        private final int myMaxSize = WindowManagerEx.getInstance().getFrame(myProject).getSize().width;

        @Nonnull
        @Override
        @RequiredReadAction
        public Object[] getElementsByName(String name, boolean checkBoxState, String pattern) {
            GlobalSearchScope scope =
                myShowLibraryContents ? GlobalSearchScope.allScope(myProject) : GlobalSearchScope.projectScope(myProject);
            PsiFile[] psiFiles = FilenameIndex.getFilesByName(myProject, name, scope);
            return filterFiles(psiFiles);
        }

        @Override
        public String getPromptText() {
            return IdeLocalize.promptFilechooserEnterFileName().get();
        }

        @Override
        public LocalizeValue getCheckBoxName() {
            return LocalizeValue.empty();
        }

        @Override
        public String getNotInMessage() {
            return "";
        }

        @Override
        public String getNotFoundMessage() {
            return "";
        }

        @Override
        public boolean loadInitialCheckBoxState() {
            return true;
        }

        @Override
        public void saveInitialCheckBoxState(boolean state) {
        }

        @Override
        public PsiElementListCellRenderer getListCellRenderer() {
            return new GotoFileCellRenderer(myMaxSize);
        }

        @Override
        @Nonnull
        public String[] getNames(boolean checkBoxState) {
            String[] fileNames;
            if (myFileType != null && myProject != null) {
                GlobalSearchScope scope =
                    myShowLibraryContents ? GlobalSearchScope.allScope(myProject) : GlobalSearchScope.projectScope(myProject);
                Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(myFileType, scope);
                fileNames = ContainerUtil.map2Array(virtualFiles, String.class, VirtualFile::getName);
            }
            else {
                fileNames = FilenameIndex.getAllFilenames(myProject);
            }
            Set<String> array = new HashSet<>();
            for (String fileName : fileNames) {
                if (!array.contains(fileName)) {
                    array.add(fileName);
                }
            }

            String[] result = ArrayUtil.toStringArray(array);
            Arrays.sort(result);
            return result;
        }

        @Override
        public boolean willOpenEditor() {
            return true;
        }

        @Override
        @RequiredReadAction
        public String getElementName(Object element) {
            return element instanceof PsiFile psiFile ? psiFile.getName() : null;
        }

        @Nullable
        @Override
        @RequiredReadAction
        public String getFullName(Object element) {
            if (element instanceof PsiFile file) {
                VirtualFile virtualFile = file.getVirtualFile();
                return virtualFile != null ? virtualFile.getPath() : null;
            }

            return getElementName(element);
        }

        @Override
        public String getHelpId() {
            return null;
        }

        @Override
        @Nonnull
        public String[] getSeparators() {
            return new String[]{"/", "\\"};
        }

        @Override
        public boolean useMiddleMatching() {
            return false;
        }
    }

    private final class MyCallback extends ChooseByNamePopupComponent.Callback {
        @Override
        public void elementChosen(Object element) {
            mySelectedFile = (PsiFile) element;
            close(OK_EXIT_CODE);
        }
    }

    private Object[] filterFiles(Object[] list) {
        Predicate<PsiFile> condition = psiFile -> {
            if (myFilter != null && !myFilter.test(psiFile)) {
                return false;
            }
            boolean accepted = myFileType == null || psiFile.getFileType() == myFileType;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null && !accepted) {
                accepted = virtualFile.getFileType() == myFileType;
            }
            return accepted;
        };
        List<Object> result = new ArrayList<>(list.length);
        for (Object o : list) {
            PsiFile psiFile = switch (o) {
                case PsiFile file -> file;
                case PsiFileNode fileNode -> fileNode.getValue();
                default -> null;
            };
            if (psiFile != null && !condition.test(psiFile)) {
                continue;
            }
            else if (o instanceof ProjectViewNode projectViewNode && !projectViewNode.canHaveChildrenMatching(condition)) {
                continue;
            }
            result.add(o);
        }
        return ArrayUtil.toObjectArray(result);
    }
}
