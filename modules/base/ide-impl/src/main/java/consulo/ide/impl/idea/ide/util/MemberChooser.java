/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.application.AllIcons;
import consulo.application.ui.NonFocusableSetting;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.generation.*;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.Image;
import consulo.util.collection.FactoryMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

@Deprecated(forRemoval = true)
@DeprecationInfo("Use MemberChooserBuilder")
public class MemberChooser<T extends ClassMember> extends DialogWrapper implements TypeSafeDataProvider {
    protected Tree myTree;
    private DefaultTreeModel myTreeModel;
    protected JComponent[] myOptionControls;
    private CheckBox myCopyJavadocCheckbox;
    private CheckBox myInsertOverrideAnnotationCheckbox;
    private final ArrayList<MemberNode> mySelectedNodes = new ArrayList<>();

    private final SortEmAction mySortAction;

    private boolean myAlphabeticallySorted = false;
    private boolean myShowClasses = true;
    protected boolean myAllowEmptySelection = false;
    private final boolean myAllowMultiSelection;
    private final Project myProject;
    private final boolean myIsInsertOverrideVisible;
    private final JComponent myHeaderPanel;

    protected T[] myElements;
    protected Comparator<ElementNode> myComparator = new OrderComparator();

    protected final HashMap<MemberNode, ParentNode> myNodeToParentMap = new HashMap<>();
    protected final HashMap<ClassMember, MemberNode> myElementToNodeMap = new HashMap<>();
    protected final ArrayList<ContainerNode> myContainerNodes = new ArrayList<>();

    protected LinkedHashSet<T> mySelectedElements;

    @NonNls
    private static final String PROP_SORTED = "MemberChooser.sorted";
    @NonNls
    private static final String PROP_SHOWCLASSES = "MemberChooser.showClasses";
    @NonNls
    private static final String PROP_COPYJAVADOC = "MemberChooser.copyJavadoc";

    public MemberChooser(
        T[] elements,
        boolean allowEmptySelection,
        boolean allowMultiSelection,
        @Nonnull Project project,
        @Nullable JComponent headerPanel,
        JComponent[] optionControls
    ) {
        this(allowEmptySelection, allowMultiSelection, project, false, headerPanel, optionControls);
        resetElements(elements);
        init();
    }

    public MemberChooser(T[] elements, boolean allowEmptySelection, boolean allowMultiSelection, @Nonnull Project project) {
        this(elements, allowEmptySelection, allowMultiSelection, project, false);
    }

    public MemberChooser(
        T[] elements,
        boolean allowEmptySelection,
        boolean allowMultiSelection,
        @Nonnull Project project,
        boolean isInsertOverrideVisible
    ) {
        this(elements, allowEmptySelection, allowMultiSelection, project, isInsertOverrideVisible, null);
    }

    public MemberChooser(
        T[] elements,
        boolean allowEmptySelection,
        boolean allowMultiSelection,
        @Nonnull Project project,
        boolean isInsertOverrideVisible,
        @Nullable JComponent headerPanel
    ) {
        this(allowEmptySelection, allowMultiSelection, project, isInsertOverrideVisible, headerPanel, null);
        resetElements(elements);
        init();
    }

    protected MemberChooser(
        boolean allowEmptySelection,
        boolean allowMultiSelection,
        @Nonnull Project project,
        boolean isInsertOverrideVisible,
        @Nullable JComponent headerPanel,
        @Nullable JComponent[] optionControls
    ) {
        super(project, true);
        myAllowEmptySelection = allowEmptySelection;
        myAllowMultiSelection = allowMultiSelection;
        myProject = project;
        myIsInsertOverrideVisible = isInsertOverrideVisible;
        myHeaderPanel = headerPanel;
        myTree = createTree();
        myOptionControls = optionControls;
        mySortAction = new SortEmAction();
        mySortAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK)), myTree);
    }

    protected void resetElementsWithDefaultComparator(T[] elements, boolean restoreSelectedElements) {
        myComparator = myAlphabeticallySorted ? new AlphaComparator() : new OrderComparator();
        resetElements(elements, null, restoreSelectedElements);
    }

    public void resetElements(T[] elements) {
        resetElements(elements, null, false);
    }

    @SuppressWarnings("unchecked")
    public void resetElements(T[] elements, @Nullable Comparator<T> sortComparator, boolean restoreSelectedElements) {
        List<T> selectedElements = restoreSelectedElements && mySelectedElements != null ? new ArrayList<>(mySelectedElements) : null;
        myElements = elements;
        if (sortComparator != null) {
            myComparator = new ElementNodeComparatorWrapper(sortComparator);
        }
        mySelectedNodes.clear();
        myNodeToParentMap.clear();
        myElementToNodeMap.clear();
        myContainerNodes.clear();

        myProject.getApplication().runReadAction(() -> {
            myTreeModel = buildModel();
        });

        myTree.setModel(myTreeModel);
        myTree.setRootVisible(false);

        doSort();

        defaultExpandTree();

        if (myOptionControls == null) {
            myCopyJavadocCheckbox = CheckBox.create(IdeLocalize.checkboxCopyJavadoc());
            NonFocusableSetting.initFocusability(myCopyJavadocCheckbox);
            if (myIsInsertOverrideVisible) {
                myInsertOverrideAnnotationCheckbox = CheckBox.create(IdeLocalize.checkboxInsertAtOverride());
                NonFocusableSetting.initFocusability(myInsertOverrideAnnotationCheckbox);
                myOptionControls = new JComponent[]{
                    (JComponent) TargetAWT.to(myCopyJavadocCheckbox),
                    (JComponent) TargetAWT.to(myInsertOverrideAnnotationCheckbox)}
                ;
            }
            else {
                myOptionControls = new JComponent[]{(JComponent) TargetAWT.to(myCopyJavadocCheckbox)};
            }
        }

        myTree.doLayout();
        setOKActionEnabled(myAllowEmptySelection || myElements != null && myElements.length > 0);

        if (selectedElements != null) {
            selectElements(selectedElements.toArray(new ClassMember[selectedElements.size()]));
        }
        if (mySelectedElements == null || mySelectedElements.isEmpty()) {
            expandFirst();
        }
    }

    /**
     * should be invoked in read action
     */
    private DefaultTreeModel buildModel() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        Ref<Integer> count = new Ref<>(0);
        Ref<Map<MemberChooserObject, ParentNode>> mapRef = new Ref<>();
        mapRef.set(FactoryMap.create(key -> {
            ParentNode node = null;
            DefaultMutableTreeNode parentNode1 = rootNode;

            if (supportsNestedContainers() && key instanceof ClassMember) {
                MemberChooserObject parentNodeDelegate = ((ClassMember) key).getParentNodeDelegate();

                if (parentNodeDelegate != null) {
                    parentNode1 = mapRef.get().get(parentNodeDelegate);
                }
            }
            if (isContainerNode(key)) {
                ContainerNode containerNode = new ContainerNode(parentNode1, key, count);
                node = containerNode;
                myContainerNodes.add(containerNode);
            }
            if (node == null) {
                node = new ParentNode(parentNode1, key, count);
            }
            return node;
        }));

        Map<MemberChooserObject, ParentNode> map = mapRef.get();

        for (T object : myElements) {
            ParentNode parentNode = map.get(object.getParentNodeDelegate());
            MemberNode elementNode = createMemberNode(count, object, parentNode);
            myNodeToParentMap.put(elementNode, parentNode);
            myElementToNodeMap.put(object, elementNode);
        }
        return new DefaultTreeModel(rootNode);
    }

    protected MemberNode createMemberNode(Ref<Integer> count, T object, ParentNode parentNode) {
        return new MemberNodeImpl(parentNode, object, count);
    }

    protected boolean supportsNestedContainers() {
        return false;
    }

    protected void defaultExpandTree() {
        TreeUtil.expandAll(myTree);
    }

    protected boolean isContainerNode(MemberChooserObject key) {
        return key instanceof PsiElementMemberChooserObject;
    }

    public void selectElements(ClassMember[] elements) {
        ArrayList<TreePath> selectionPaths = new ArrayList<>();
        for (ClassMember element : elements) {
            MemberNode treeNode = myElementToNodeMap.get(element);
            if (treeNode != null) {
                selectionPaths.add(new TreePath(((DefaultMutableTreeNode) treeNode).getPath()));
            }
        }
        myTree.setSelectionPaths(selectionPaths.toArray(new TreePath[selectionPaths.size()]));
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<>();
        actions.add(getOKAction());
        if (myAllowEmptySelection) {
            actions.add(new SelectNoneAction());
        }
        actions.add(getCancelAction());
        if (getHelpId() != null) {
            actions.add(getHelpAction());
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        if (getHelpId() == null) {
            return;
        }
        super.doHelpAction();
    }

    @RequiredUIAccess
    protected void customizeOptionsPanel() {
        if (myInsertOverrideAnnotationCheckbox != null && myIsInsertOverrideVisible) {
            CodeStyleSettings styleSettings = CodeStyleSettingsManager.getInstance(myProject).getCurrentSettings();
            myInsertOverrideAnnotationCheckbox.setValue(styleSettings.INSERT_OVERRIDE_ANNOTATION);
        }
        if (myCopyJavadocCheckbox != null) {
            myCopyJavadocCheckbox.setValue(PropertiesComponent.getInstance().isTrueValue(PROP_COPYJAVADOC));
        }
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        customizeOptionsPanel();
        JPanel optionsPanel = new JPanel(new VerticalFlowLayout());
        for (JComponent component : myOptionControls) {
            optionsPanel.add(component);
        }

        panel.add(
            optionsPanel,
            new GridBagConstraints(
                0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                JBUI.insetsRight(5), 0, 0
            )
        );

        if (!myAllowEmptySelection && (myElements == null || myElements.length == 0)) {
            setOKActionEnabled(false);
        }
        panel.add(
            super.createSouthPanel(),
            new GridBagConstraints(
                1, 0, 1, 1, 0, 0, GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                JBUI.emptyInsets(), 0, 0
            )
        );
        return panel;
    }

    @Override
    protected JComponent createNorthPanel() {
        return myHeaderPanel;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Toolbar

        DefaultActionGroup group = new DefaultActionGroup();

        fillToolbarActions(group);

        group.addSeparator();

        ExpandAllAction expandAllAction = new ExpandAllAction();
        expandAllAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_EXPAND_ALL)),
            myTree
        );
        group.add(expandAllAction);

        CollapseAllAction collapseAllAction = new CollapseAllAction();
        collapseAllAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_COLLAPSE_ALL)),
            myTree
        );
        group.add(collapseAllAction);

        panel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true).getComponent(), BorderLayout.NORTH);

        // Tree
        expandFirst();
        defaultExpandTree();
        installSpeedSearch();

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        scrollPane.setPreferredSize(new Dimension(350, 450));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void expandFirst() {
        if (getRootNode().getChildCount() > 0) {
            myTree.expandRow(0);
            myTree.setSelectionRow(1);
        }
    }

    protected Tree createTree() {
        final Tree tree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));

        tree.setCellRenderer(getTreeCellRenderer());
        UIUtil.setLineStyleAngled(tree);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addKeyListener(new TreeKeyListener());
        tree.addTreeSelectionListener(new MyTreeSelectionListener());

        if (!myAllowMultiSelection) {
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                if (tree.getPathForLocation(e.getX(), e.getY()) != null) {
                    doOKAction();
                    return true;
                }
                return false;
            }
        }.installOn(tree);

        TreeUtil.installActions(tree);
        return tree;
    }

    protected TreeCellRenderer getTreeCellRenderer() {
        return new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(
                @Nonnull JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
            ) {
                if (value instanceof ElementNode) {
                    ((ElementNode) value).getDelegate().renderTreeNode(this, tree);
                }
            }
        };
    }

    @Nonnull
    protected String convertElementText(@Nonnull String originalElementText) {
        String res = originalElementText;

        int i = res.indexOf(':');
        if (i >= 0) {
            res = res.substring(0, i);
        }
        i = res.indexOf('(');
        if (i >= 0) {
            res = res.substring(0, i);
        }

        return res;
    }

    protected void installSpeedSearch() {
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(myTree, new Convertor<>() {
            @Nullable
            @Override
            public String convert(TreePath path) {
                ElementNode lastPathComponent = (ElementNode) path.getLastPathComponent();
                if (lastPathComponent == null) {
                    return null;
                }
                String text = lastPathComponent.getDelegate().getText();
                if (text != null) {
                    text = convertElementText(text);
                }
                return text;
            }
        });
        treeSpeedSearch.setComparator(getSpeedSearchComparator());
    }

    protected SpeedSearchComparator getSpeedSearchComparator() {
        return new SpeedSearchComparator(false);
    }

    protected void disableAlphabeticalSorting(AnActionEvent event) {
        mySortAction.setSelected(event, false);
    }

    protected void onAlphabeticalSortingEnabled(AnActionEvent event) {
        //do nothing by default
    }

    protected void fillToolbarActions(DefaultActionGroup group) {
        boolean alphabeticallySorted = PropertiesComponent.getInstance().isTrueValue(PROP_SORTED);
        if (alphabeticallySorted) {
            setSortComparator(new AlphaComparator());
        }
        myAlphabeticallySorted = alphabeticallySorted;
        group.add(mySortAction);

        if (!supportsNestedContainers()) {
            ShowContainersAction showContainersAction = getShowContainersAction();
            showContainersAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(
                KeyEvent.VK_C,
                InputEvent.ALT_MASK
            )), myTree);
            setShowClasses(PropertiesComponent.getInstance().getBoolean(PROP_SHOWCLASSES, true));
            group.add(showContainersAction);
        }
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.ide.util.MemberChooser";
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTree;
    }

    public JComponent[] getOptionControls() {
        return myOptionControls;
    }

    @Nullable
    private LinkedHashSet<T> getSelectedElementsList() {
        return getExitCode() == OK_EXIT_CODE ? mySelectedElements : null;
    }

    @Nullable
    public List<T> getSelectedElements() {
        LinkedHashSet<T> list = getSelectedElementsList();
        return list == null ? null : new ArrayList<>(list);
    }

    @Nullable
    public T[] getSelectedElements(T[] a) {
        LinkedHashSet<T> list = getSelectedElementsList();
        if (list == null) {
            return null;
        }
        return list.toArray(a);
    }

    protected final boolean areElementsSelected() {
        return mySelectedElements != null && !mySelectedElements.isEmpty();
    }

    public void setCopyJavadocVisible(boolean state) {
        myCopyJavadocCheckbox.setVisible(state);
    }

    public boolean isCopyJavadoc() {
        return myCopyJavadocCheckbox.getValueOrError();
    }

    public boolean isInsertOverrideAnnotation() {
        return myIsInsertOverrideVisible && myInsertOverrideAnnotationCheckbox.getValueOrError();
    }

    private boolean isAlphabeticallySorted() {
        return myAlphabeticallySorted;
    }

    @SuppressWarnings("unchecked")
    protected void changeSortComparator(Comparator<T> comparator) {
        setSortComparator(new ElementNodeComparatorWrapper(comparator));
    }

    private void setSortComparator(Comparator<ElementNode> sortComparator) {
        if (myComparator.equals(sortComparator)) {
            return;
        }
        myComparator = sortComparator;
        doSort();
    }

    protected void doSort() {
        Pair<ElementNode, List<ElementNode>> pair = storeSelection();

        Enumeration<TreeNode> children = getRootNodeChildren();
        while (children.hasMoreElements()) {
            ParentNode classNode = (ParentNode) children.nextElement();
            sortNode(classNode, myComparator);
            myTreeModel.nodeStructureChanged(classNode);
        }

        restoreSelection(pair);
    }

    private static void sortNode(ParentNode node, Comparator<ElementNode> sortComparator) {
        ArrayList<ElementNode> arrayList = new ArrayList<>();
        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            arrayList.add((ElementNode) children.nextElement());
        }

        Collections.sort(arrayList, sortComparator);

        replaceChildren(node, arrayList);
    }

    private static void replaceChildren(DefaultMutableTreeNode node, Collection<? extends ElementNode> arrayList) {
        node.removeAllChildren();
        for (ElementNode child : arrayList) {
            node.add(child);
        }
    }

    protected void restoreTree() {
        Pair<ElementNode, List<ElementNode>> selection = storeSelection();

        DefaultMutableTreeNode root = getRootNode();
        if (!myShowClasses || myContainerNodes.isEmpty()) {
            List<ParentNode> otherObjects = new ArrayList<>();
            Enumeration<TreeNode> children = getRootNodeChildren();
            ParentNode newRoot = new ParentNode(null, new MemberChooserObjectBase(getAllContainersNodeName()), new Ref<>(0));
            while (children.hasMoreElements()) {
                ParentNode nextElement = (ParentNode) children.nextElement();
                if (nextElement instanceof ContainerNode) {
                    ContainerNode containerNode = (ContainerNode) nextElement;
                    Enumeration<TreeNode> memberNodes = containerNode.children();
                    List<MemberNode> memberNodesList = new ArrayList<>();
                    while (memberNodes.hasMoreElements()) {
                        memberNodesList.add((MemberNode) memberNodes.nextElement());
                    }
                    for (MemberNode memberNode : memberNodesList) {
                        newRoot.add(memberNode);
                    }
                }
                else {
                    otherObjects.add(nextElement);
                }
            }
            replaceChildren(root, otherObjects);
            sortNode(newRoot, myComparator);
            if (newRoot.children().hasMoreElements()) {
                root.add(newRoot);
            }
        }
        else {
            Enumeration<TreeNode> children = getRootNodeChildren();
            while (children.hasMoreElements()) {
                ParentNode allClassesNode = (ParentNode) children.nextElement();
                Enumeration<TreeNode> memberNodes = allClassesNode.children();
                ArrayList<MemberNode> arrayList = new ArrayList<>();
                while (memberNodes.hasMoreElements()) {
                    arrayList.add((MemberNode) memberNodes.nextElement());
                }
                Collections.sort(arrayList, myComparator);
                for (MemberNode memberNode : arrayList) {
                    myNodeToParentMap.get(memberNode).add(memberNode);
                }
            }
            replaceChildren(root, myContainerNodes);
        }
        myTreeModel.nodeStructureChanged(root);

        defaultExpandTree();

        restoreSelection(selection);
    }

    private void setShowClasses(boolean showClasses) {
        myShowClasses = showClasses;
        restoreTree();
    }

    protected String getAllContainersNodeName() {
        return IdeLocalize.nodeMemberchooserAllClasses().get();
    }

    private Enumeration<TreeNode> getRootNodeChildren() {
        return getRootNode().children();
    }

    protected DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) myTreeModel.getRoot();
    }

    private Pair<ElementNode, List<ElementNode>> storeSelection() {
        List<ElementNode> selectedNodes = new ArrayList<>();
        TreePath[] paths = myTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                selectedNodes.add((ElementNode) path.getLastPathComponent());
            }
        }
        TreePath leadSelectionPath = myTree.getLeadSelectionPath();
        return Pair.create(leadSelectionPath != null ? (ElementNode) leadSelectionPath.getLastPathComponent() : null, selectedNodes);
    }


    private void restoreSelection(Pair<ElementNode, List<ElementNode>> pair) {
        List<ElementNode> selectedNodes = pair.second;

        DefaultMutableTreeNode root = getRootNode();

        ArrayList<TreePath> toSelect = new ArrayList<>();
        for (ElementNode node : selectedNodes) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            if (root.isNodeDescendant(treeNode)) {
                toSelect.add(new TreePath(treeNode.getPath()));
            }
        }

        if (!toSelect.isEmpty()) {
            myTree.setSelectionPaths(toSelect.toArray(new TreePath[toSelect.size()]));
        }

        ElementNode leadNode = pair.first;
        if (leadNode != null) {
            myTree.setLeadSelectionPath(new TreePath(((DefaultMutableTreeNode) leadNode).getPath()));
        }
    }

    @Override
    public void dispose() {
        PropertiesComponent instance = PropertiesComponent.getInstance();
        instance.setValue(PROP_SORTED, Boolean.toString(isAlphabeticallySorted()));
        instance.setValue(PROP_SHOWCLASSES, Boolean.toString(myShowClasses));

        if (myCopyJavadocCheckbox != null) {
            instance.setValue(PROP_COPYJAVADOC, Boolean.toString(myCopyJavadocCheckbox.getValueOrError()));
        }

        Container contentPane = getContentPane();
        if (contentPane != null) {
            contentPane.removeAll();
        }
        mySelectedNodes.clear();
        myElements = null;
        super.dispose();
    }

    @Override
    public void calcData(Key key, DataSink sink) {
        if (PsiElement.KEY == key) {
            if (mySelectedElements != null && !mySelectedElements.isEmpty()) {
                T selectedElement = mySelectedElements.iterator().next();
                if (selectedElement instanceof ClassMemberWithElement) {
                    sink.put(PsiElement.KEY, ((ClassMemberWithElement) selectedElement).getElement());
                }
            }
        }
    }

    private class MyTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath[] paths = e.getPaths();
            if (paths == null) {
                return;
            }
            for (int i = 0; i < paths.length; i++) {
                Object node = paths[i].getLastPathComponent();
                if (node instanceof MemberNode) {
                    MemberNode memberNode = (MemberNode) node;
                    if (e.isAddedPath(i)) {
                        if (!mySelectedNodes.contains(memberNode)) {
                            mySelectedNodes.add(memberNode);
                        }
                    }
                    else {
                        mySelectedNodes.remove(memberNode);
                    }
                }
            }
            mySelectedElements = new LinkedHashSet<>();
            for (MemberNode selectedNode : mySelectedNodes) {
                //noinspection unchecked
                mySelectedElements.add((T) selectedNode.getDelegate());
            }
        }
    }

    protected interface ElementNode extends MutableTreeNode {
        MemberChooserObject getDelegate();

        int getOrder();
    }

    protected interface MemberNode extends ElementNode {
    }

    protected abstract static class ElementNodeImpl extends DefaultMutableTreeNode implements ElementNode {
        private final int myOrder;
        private final MemberChooserObject myDelegate;

        public ElementNodeImpl(@Nullable DefaultMutableTreeNode parent, MemberChooserObject delegate, Ref<Integer> order) {
            myOrder = order.get();
            order.set(myOrder + 1);
            myDelegate = delegate;
            if (parent != null) {
                parent.add(this);
            }
        }

        @Override
        public MemberChooserObject getDelegate() {
            return myDelegate;
        }

        @Override
        public int getOrder() {
            return myOrder;
        }
    }

    protected static class MemberNodeImpl extends ElementNodeImpl implements MemberNode {
        public MemberNodeImpl(ParentNode parent, ClassMember delegate, Ref<Integer> order) {
            super(parent, delegate, order);
        }
    }

    protected static class ParentNode extends ElementNodeImpl {
        public ParentNode(@Nullable DefaultMutableTreeNode parent, MemberChooserObject delegate, Ref<Integer> order) {
            super(parent, delegate, order);
        }
    }

    protected static class ContainerNode extends ParentNode {
        public ContainerNode(DefaultMutableTreeNode parent, MemberChooserObject delegate, Ref<Integer> order) {
            super(parent, delegate, order);
        }
    }

    private class SelectNoneAction extends AbstractAction {
        public SelectNoneAction() {
            super(IdeLocalize.actionSelectNone().get());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            myTree.clearSelection();
            doOKAction();
        }
    }

    private class TreeKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            TreePath path = myTree.getLeadSelectionPath();
            if (path == null) {
                return;
            }
            Object lastComponent = path.getLastPathComponent();
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (lastComponent instanceof ParentNode) {
                    return;
                }
                doOKAction();
                e.consume();
            }
            else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                if (lastComponent instanceof ElementNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastComponent;
                    if (!mySelectedNodes.contains(node)) {
                        if (node.getNextNode() != null) {
                            myTree.setSelectionPath(new TreePath(node.getNextNode().getPath()));
                        }
                    }
                    else {
                        if (node.getNextNode() != null) {
                            myTree.removeSelectionPath(new TreePath(node.getPath()));
                            myTree.setSelectionPath(new TreePath(node.getNextNode().getPath()));
                            myTree.repaint();
                        }
                    }
                    e.consume();
                }
            }
        }
    }

    private class SortEmAction extends ToggleAction {
        public SortEmAction() {
            super(
                IdeLocalize.actionSortAlphabetically(),
                IdeLocalize.actionSortAlphabetically(),
                AllIcons.ObjectBrowser.Sorted
            );
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent event) {
            return isAlphabeticallySorted();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent event, boolean flag) {
            myAlphabeticallySorted = flag;
            setSortComparator(flag ? new AlphaComparator() : new OrderComparator());
            if (flag) {
                MemberChooser.this.onAlphabeticalSortingEnabled(event);
            }
        }
    }

    protected ShowContainersAction getShowContainersAction() {
        return new ShowContainersAction(IdeLocalize.actionShowClasses(), AllIcons.Nodes.Class);
    }

    protected class ShowContainersAction extends ToggleAction {
        public ShowContainersAction(LocalizeValue text, Image icon) {
            super(text, text, icon);
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent event) {
            return myShowClasses;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent event, boolean flag) {
            setShowClasses(flag);
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(myContainerNodes.size() > 1);
        }
    }

    private class ExpandAllAction extends AnAction {
        public ExpandAllAction() {
            super(
                IdeLocalize.actionExpandAll(),
                IdeLocalize.actionExpandAll(),
                AllIcons.Actions.Expandall
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            TreeUtil.expandAll(myTree);
        }
    }

    private class CollapseAllAction extends AnAction {
        public CollapseAllAction() {
            super(
                IdeLocalize.actionCollapseAll(),
                IdeLocalize.actionCollapseAll(),
                AllIcons.Actions.Collapseall
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            TreeUtil.collapseAll(myTree, 1);
        }
    }

    private static class AlphaComparator implements Comparator<ElementNode> {
        @Override
        public int compare(ElementNode n1, ElementNode n2) {
            return n1.getDelegate().getText().compareToIgnoreCase(n2.getDelegate().getText());
        }
    }

    protected static class OrderComparator implements Comparator<ElementNode> {
        public OrderComparator() {
        } // To make this class instanceable from the subclasses

        @Override
        public int compare(ElementNode n1, ElementNode n2) {
            if (n1.getDelegate() instanceof ClassMemberWithElement && n2.getDelegate() instanceof ClassMemberWithElement) {
                PsiElement element1 = ((ClassMemberWithElement) n1.getDelegate()).getElement();
                PsiElement element2 = ((ClassMemberWithElement) n2.getDelegate()).getElement();
                if (!(element1 instanceof PsiCompiledElement) && !(element2 instanceof PsiCompiledElement)) {
                    return element1.getTextOffset() - element2.getTextOffset();
                }
            }
            return n1.getOrder() - n2.getOrder();
        }
    }

    private static class ElementNodeComparatorWrapper<T> implements Comparator<ElementNode> {
        private final Comparator<T> myDelegate;

        public ElementNodeComparatorWrapper(Comparator<T> delegate) {
            myDelegate = delegate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compare(ElementNode o1, ElementNode o2) {
            return myDelegate.compare((T) o1.getDelegate(), (T) o2.getDelegate());
        }
    }
}
