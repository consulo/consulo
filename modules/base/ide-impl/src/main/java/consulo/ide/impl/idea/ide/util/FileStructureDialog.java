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
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.fileEditor.structureView.tree.*;
import consulo.ide.impl.idea.ide.commander.CommanderPanel;
import consulo.ide.impl.idea.ide.commander.ProjectListBuilder;
import consulo.ide.impl.idea.ide.structureView.newStructureView.TreeModelWrapper;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.SmartTreeStructure;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FileStructureDialog extends DialogWrapper {
    private final Editor myEditor;
    private final Navigatable myNavigatable;
    private final Project myProject;
    private MyCommanderPanel myCommanderPanel;
    private final StructureViewModel myTreeModel;
    private final StructureViewModel myBaseTreeModel;
    private SmartTreeStructure myTreeStructure;
    private final TreeStructureActionsOwner myTreeActionsOwner;

    @NonNls
    private static final String ourPropertyKey = "FileStructure.narrowDown";
    private boolean myShouldNarrowDown = false;

    public FileStructureDialog(
        StructureViewModel structureViewModel,
        @Nullable Editor editor,
        Project project,
        Navigatable navigatable,
        @Nonnull final Disposable auxDisposable,
        final boolean applySortAndFilter
    ) {
        super(project, true);
        myProject = project;
        myEditor = editor;
        myNavigatable = navigatable;
        myBaseTreeModel = structureViewModel;
        if (applySortAndFilter) {
            myTreeActionsOwner = new TreeStructureActionsOwner(myBaseTreeModel);
            myTreeModel = new TreeModelWrapper(structureViewModel, myTreeActionsOwner);
        }
        else {
            myTreeActionsOwner = null;
            myTreeModel = structureViewModel;
        }

        PsiFile psiFile = getPsiFile(project);

        final PsiElement psiElement = getCurrentElement(psiFile);

        //myDialog.setUndecorated(true);
        init();

        if (psiElement != null) {
            if (structureViewModel.shouldEnterElement(psiElement)) {
                myCommanderPanel.getBuilder().enterElement(psiElement, PsiUtilBase.getVirtualFile(psiElement));
            }
            else {
                myCommanderPanel.getBuilder().selectElement(psiElement, PsiUtilBase.getVirtualFile(psiElement));
            }
        }

        Disposer.register(myDisposable, auxDisposable);
    }

    protected PsiFile getPsiFile(final Project project) {
        return PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
    }

    @Override
    @Nullable
    protected Border createContentPaneBorder() {
        return null;
    }

    @Override
    public void dispose() {
        myCommanderPanel.dispose();
        super.dispose();
    }

    @Override
    protected String getDimensionServiceKey() {
        return DockManager.getInstance(myProject).getDimensionKeyForFocus("#consulo.ide.impl.idea.ide.util.FileStructureDialog");
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCommanderPanel);
    }

    @Nullable
    protected PsiElement getCurrentElement(@Nullable final PsiFile psiFile) {
        if (psiFile == null) {
            return null;
        }

        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        return myTreeModel.getCurrentEditorElement() instanceof PsiElement elementAtCursor ? elementAtCursor : null;
    }

    @Override
    protected JComponent createCenterPanel() {
        myCommanderPanel = new MyCommanderPanel(myProject);
        myTreeStructure = new MyStructureTreeStructure();

        List<FileStructureFilter> fileStructureFilters = new ArrayList<FileStructureFilter>();
        List<FileStructureNodeProvider> fileStructureNodeProviders = new ArrayList<FileStructureNodeProvider>();
        if (myTreeActionsOwner != null) {
            for (Filter filter : myBaseTreeModel.getFilters()) {
                if (filter instanceof FileStructureFilter) {
                    final FileStructureFilter fsFilter = (FileStructureFilter)filter;
                    myTreeActionsOwner.setActionIncluded(fsFilter, true);
                    fileStructureFilters.add(fsFilter);
                }
            }

            if (myBaseTreeModel instanceof ProvidingTreeModel providingTreeModel) {
                for (NodeProvider provider : providingTreeModel.getNodeProviders()) {
                    if (provider instanceof FileStructureNodeProvider fileStructureNodeProvider) {
                        fileStructureNodeProviders.add(fileStructureNodeProvider);
                    }
                }
            }
        }

        PsiFile psiFile = getPsiFile(myProject);
        boolean showRoot = isShowRoot(psiFile);
        ProjectListBuilder projectListBuilder = new ProjectListBuilder(myProject, myCommanderPanel, myTreeStructure, null, showRoot) {
            @Override
            protected boolean shouldEnterSingleTopLevelElement(Object rootChild) {
                return myBaseTreeModel.shouldEnterElement(((StructureViewTreeElement)((AbstractTreeNode)rootChild).getValue()).getValue());
            }

            @Override
            protected boolean nodeIsAcceptableForElement(AbstractTreeNode node, Object element) {
                return Comparing.equal(((StructureViewTreeElement)node.getValue()).getValue(), element);
            }

            @Override
            protected void refreshSelection() {
                myCommanderPanel.scrollSelectionInView();
                if (myShouldNarrowDown) {
                    myCommanderPanel.updateSpeedSearch();
                }
            }

            @Override
            protected List<AbstractTreeNode> getAllAcceptableNodes(final Object[] childElements, VirtualFile file) {
                ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
                for (Object childElement : childElements) {
                    result.add((AbstractTreeNode)childElement);
                }
                return result;
            }
        };
        myCommanderPanel.setBuilder(projectListBuilder);
        myCommanderPanel.setTitlePanelVisible(false);

        new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent e) {
                final boolean succeeded = myCommanderPanel.navigateSelectedElement();
                if (succeeded) {
                    unregisterCustomShortcutSet(myCommanderPanel);
                }
            }
        }.registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet(),
            myCommanderPanel
        );

        myCommanderPanel.setPreferredSize(new Dimension(400, 500));

        JPanel panel = new JPanel(new BorderLayout());
        JPanel comboPanel = new JPanel(new GridLayout(0, 2, 0, 0));

        addNarrowDownCheckbox(comboPanel);

        for (FileStructureFilter filter : fileStructureFilters) {
            addCheckbox(comboPanel, filter);
        }

        for (FileStructureNodeProvider provider : fileStructureNodeProviders) {
            addCheckbox(comboPanel, provider);
        }

        myCommanderPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
        panel.add(comboPanel, BorderLayout.NORTH);
        panel.add(myCommanderPanel, BorderLayout.CENTER);
        //new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        return panel;
    }

    @RequiredReadAction
    protected boolean isShowRoot(final PsiFile psiFile) {
        StructureViewBuilder viewBuilder = PsiStructureViewFactory.createBuilderForFile(psiFile);
        return viewBuilder instanceof TreeBasedStructureViewBuilder treeBasedStructureViewBuilder && treeBasedStructureViewBuilder.isRootNodeShown();
    }

    private void addNarrowDownCheckbox(final JPanel panel) {
        final JCheckBox checkBox = new JCheckBox(IdeLocalize.checkboxNarrowDownTheListOnTyping().get());
        checkBox.setSelected(PropertiesComponent.getInstance().isTrueValue(ourPropertyKey));
        checkBox.addChangeListener(e -> {
            myShouldNarrowDown = checkBox.isSelected();
            PropertiesComponent.getInstance().setValue(ourPropertyKey, Boolean.toString(myShouldNarrowDown));

            ProjectListBuilder builder = (ProjectListBuilder)myCommanderPanel.getBuilder();
            if (builder == null) {
                return;
            }
            builder.addUpdateRequest();
        });

        checkBox.setFocusable(false);
        panel.add(checkBox);
        //,new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    }

    private void addCheckbox(final JPanel panel, final TreeAction action) {
        String text = action instanceof FileStructureFilter fileStructureFilter
            ? fileStructureFilter.getCheckBoxText()
            : action instanceof FileStructureNodeProvider fileStructureNodeProvider
            ? fileStructureNodeProvider.getCheckBoxText()
            : null;

        if (text == null) {
            return;
        }

        Shortcut[] shortcuts = action instanceof FileStructureFilter fileStructureFilter
            ? fileStructureFilter.getShortcut()
            : ((FileStructureNodeProvider)action).getShortcut();


        final JCheckBox chkFilter = new JCheckBox();
        chkFilter.addActionListener(e -> {
            ProjectListBuilder builder = (ProjectListBuilder)myCommanderPanel.getBuilder();
            PsiElement currentParent = null;
            if (builder != null) {
                final AbstractTreeNode parentNode = builder.getParentNode();
                if (parentNode.getValue() instanceof StructureViewTreeElement structureViewTreeElement
                    && structureViewTreeElement.getValue() instanceof PsiElement elementValue) {
                    currentParent = elementValue;
                }
            }
            final boolean state = chkFilter.isSelected();
            myTreeActionsOwner.setActionIncluded(action, action instanceof FileStructureFilter ? !state : state);
            myTreeStructure.rebuildTree();
            if (builder != null) {
                if (currentParent != null) {
                    boolean oldNarrowDown = myShouldNarrowDown;
                    myShouldNarrowDown = false;
                    try {
                        builder.enterElement(currentParent, PsiUtilBase.getVirtualFile(currentParent));
                    }
                    finally {
                        myShouldNarrowDown = oldNarrowDown;
                    }
                }
                builder.updateList(true);
            }

            if (SpeedSearchBase.hasActiveSpeedSearch(myCommanderPanel.getList())) {
                final SpeedSearchSupply supply = SpeedSearchBase.getSupply(myCommanderPanel.getList());
                if (supply != null && supply.isPopupActive()) {
                    supply.refreshSelection();
                }
            }
        });
        chkFilter.setFocusable(false);

        if (shortcuts.length > 0) {
            text += " (" + KeymapUtil.getShortcutText(shortcuts[0]) + ")";
            new AnAction() {
                @Override
                public void actionPerformed(final AnActionEvent e) {
                    chkFilter.doClick();
                }
            }.registerCustomShortcutSet(new CustomShortcutSet(shortcuts), myCommanderPanel);
        }
        chkFilter.setText(text);
        panel.add(chkFilter);
        //,new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    }

    @Override
    @Nullable
    protected JComponent createSouthPanel() {
        return null;
    }

    public CommanderPanel getPanel() {
        return myCommanderPanel;
    }

    private class MyCommanderPanel extends CommanderPanel implements DataProvider {
        @Override
        protected boolean shouldDrillDownOnEmptyElement(final AbstractTreeNode node) {
            return false;
        }

        public MyCommanderPanel(Project _project) {
            super(_project, false, true);
            myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            myListSpeedSearch.addChangeListener(evt -> {
                ProjectListBuilder builder = (ProjectListBuilder)getBuilder();
                if (builder == null) {
                    return;
                }
                builder.addUpdateRequest(hasPrefixShortened(evt));
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int index = myList.getSelectedIndex();
                        if (index != -1 && index < myList.getModel().getSize()) {
                            myList.clearSelection();
                            ScrollingUtil.selectItem(myList, index);
                        }
                        else {
                            ScrollingUtil.ensureSelectionExists(myList);
                        }
                    }
                });

                myList.repaint(); // to update match highlighting
            });
            myListSpeedSearch.setComparator(createSpeedSearchComparator());
        }

        private boolean hasPrefixShortened(final PropertyChangeEvent evt) {
            return evt.getNewValue() != null && evt.getOldValue() != null
                && ((String)evt.getNewValue()).length() < ((String)evt.getOldValue()).length();
        }

        @RequiredUIAccess
        @Override
        public boolean navigateSelectedElement() {
            final SimpleReference<Boolean> succeeded = new SimpleReference<>();
            final CommandProcessor commandProcessor = CommandProcessor.getInstance();
            commandProcessor.newCommand(() -> {
                    succeeded.set(MyCommanderPanel.super.navigateSelectedElement());
                    IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();
                })
                .withProject(myProject)
                .withName(LanguageLocalize.commandNameNavigate())
                .execute();
            if (succeeded.get()) {
                close(CANCEL_EXIT_CODE);
            }
            return succeeded.get();
        }

        @Override
        public Object getData(@Nonnull Key dataId) {
            Object selectedElement = myCommanderPanel.getSelectedValue();

            if (selectedElement instanceof StructureViewTreeElement treeElement) {
                selectedElement = treeElement.getValue();
            }

            if (Navigatable.KEY == dataId) {
                return selectedElement instanceof Navigatable ? selectedElement : myNavigatable;
            }

            if (OpenFileDescriptorImpl.NAVIGATE_IN_EDITOR == dataId) {
                return myEditor;
            }

            return getDataImpl(dataId);
        }

        public String getEnteredPrefix() {
            return myListSpeedSearch.getEnteredPrefix();
        }

        public void updateSpeedSearch() {
            myListSpeedSearch.refreshSelection();
        }

        public void scrollSelectionInView() {
            int selectedIndex = myList.getSelectedIndex();
            if (selectedIndex >= 0) {
                ScrollingUtil.ensureIndexIsVisible(myList, selectedIndex, 0);
            }
        }
    }

    private class MyStructureTreeStructure extends SmartTreeStructure {
        public MyStructureTreeStructure() {
            super(FileStructureDialog.this.myProject, myTreeModel);
        }

        @Override
        public Object[] getChildElements(Object element) {
            Object[] childElements = super.getChildElements(element);

            if (!myShouldNarrowDown) {
                return childElements;
            }

            String enteredPrefix = myCommanderPanel.getEnteredPrefix();
            if (enteredPrefix == null) {
                return childElements;
            }

            ArrayList<Object> filteredElements = new ArrayList<>(childElements.length);
            SpeedSearchComparator speedSearchComparator = createSpeedSearchComparator();

            for (Object child : childElements) {
                if (child instanceof AbstractTreeNode treeNode
                    && treeNode.getValue() instanceof TreeElement treeElement) {
                    String name = treeElement.getPresentation().getPresentableText();
                    if (name == null || speedSearchComparator.matchingFragments(enteredPrefix, name) == null) {
                        continue;
                    }
                }
                filteredElements.add(child);
            }
            return ArrayUtil.toObjectArray(filteredElements);
        }

        @Override
        public void rebuildTree() {
            getChildElements(getRootElement());   // for some reason necessary to rebuild tree correctly
            super.rebuildTree();
        }
    }

    private static SpeedSearchComparator createSpeedSearchComparator() {
        return new SpeedSearchComparator(false);
    }
}
