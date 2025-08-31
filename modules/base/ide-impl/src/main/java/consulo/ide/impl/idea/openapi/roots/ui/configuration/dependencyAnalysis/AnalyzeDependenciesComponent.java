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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.dependencyAnalysis;

import consulo.application.AllIcons;
import consulo.component.messagebus.MessageBusConnection;
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.module.Module;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.ModuleSourceOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * The classpath details component
 */
public class AnalyzeDependenciesComponent extends MasterDetailsComponent {
    /**
     * Data key for order path element
     */
    public static Key<ModuleDependenciesAnalyzer.OrderPathElement> ORDER_PATH_ELEMENT_KEY = Key.create("ORDER_PATH_ELEMENT");
    /**
     * The module being analyzed
     */
    private final Module myModule;
    /**
     * The settings for UI mode
     */
    private final AnalyzeDependenciesSettings mySettings;
    /**
     * The cached analyzed classpaths for this module
     */
    private final HashMap<Pair<ClasspathType, Boolean>, ModuleDependenciesAnalyzer> myClasspaths = new HashMap<>();

    /**
     * The message bus connection to use
     */
    private MessageBusConnection myMessageBusConnection;

    /**
     * The constructor
     *
     * @param module the module to analyze
     */
    public AnalyzeDependenciesComponent(Module module) {
        super(() -> null);
        myModule = module;
        mySettings = AnalyzeDependenciesSettings.getInstance(myModule.getProject());
        initTree();
        init();
        getSplitter().setProportion(0.3f);
        myMessageBusConnection = myModule.getProject().getMessageBus().connect();
        myMessageBusConnection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                myClasspaths.clear();
                updateTree();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myMessageBusConnection != null) {
            myMessageBusConnection.disconnect();
        }
    }

    /**
     * Initialize components
     */
    private void init() {
        myTree.setCellRenderer(new ColoredTreeCellRenderer() {
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
                if (value instanceof MyNode node && !(value instanceof MyRootNode)) {
                    PathNode<?> n = (PathNode<?>) node.getUserObject();
                    Consumer<ColoredTextContainer> a = n.getRender(selected, node.isDisplayInBold());
                    a.accept(this);
                }
            }
        });
        myTree.setShowsRootHandles(false);
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        reloadTree();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ArrayList<AnAction> createActions(boolean fromPopup) {
        if (!fromPopup) {
            ArrayList<AnAction> rc = new ArrayList<>();
            rc.add(new ClasspathTypeAction());
            rc.add(new SdkFilterAction());
            rc.add(new UrlModeAction());
            return rc;
        }
        else {
            return super.createActions(fromPopup);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processRemovedItems() {
        // no remove action so far
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean wasObjectStored(Object editableObject) {
        // no modifications so far
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Nls
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Classpath Details");
    }

    /**
     * Reload tree
     */
    public void reloadTree() {
        myRoot.removeAllChildren();
        ModuleDependenciesAnalyzer a = getAnalyzer();
        if (mySettings.isUrlMode()) {
            for (ModuleDependenciesAnalyzer.UrlExplanation urlExplanation : a.getUrls()) {
                myRoot.add(new MyNode(new UrlNode(urlExplanation)));
            }
        }
        else {
            for (ModuleDependenciesAnalyzer.OrderEntryExplanation explanation : a.getOrderEntries()) {
                myRoot.add(new MyNode(new OrderEntryNode(explanation)));
            }
        }
        ((DefaultTreeModel) myTree.getModel()).reload(myRoot);
    }

    /**
     * @return the analyzer for the current settings
     */
    public ModuleDependenciesAnalyzer getAnalyzer() {
        Pair<ClasspathType, Boolean> key = Pair.create(getClasspathType(), mySettings.isSdkIncluded());
        ModuleDependenciesAnalyzer a = myClasspaths.get(key);
        if (a == null) {
            a = new ModuleDependenciesAnalyzer(myModule, !mySettings.isTest(), !mySettings.isRuntime(), mySettings.isSdkIncluded());
            myClasspaths.put(key, a);
        }
        return a;
    }

    /**
     * @return the current classpath type from settings
     */
    private ClasspathType getClasspathType() {
        return mySettings.isRuntime()
            ? (mySettings.isTest() ? ClasspathType.TEST_RUNTIME : ClasspathType.PRODUCTION_RUNTIME)
            : (mySettings.isTest() ? ClasspathType.TEST_COMPILE : ClasspathType.PRODUCTION_COMPILE);
    }

    /**
     * Schedule updating the tree
     */
    void updateTree() {
        // TODO make loading in the background if there will be significant delays on big projects
        reloadTree();
    }

    /**
     * The action that allows navigating to the path element
     */
    static class NavigateAction extends DumbAwareAction {

        /**
         * The constructor
         */
        NavigateAction() {
            super("Navigate to ...", "Navigate to place where path element is defined", null);
        }

        /**
         * {@inheritDoc}
         */
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Module module = e.getData(Module.KEY);
            if (module == null) {
                return;
            }
            ModuleDependenciesAnalyzer.OrderPathElement element = e.getData(ORDER_PATH_ELEMENT_KEY);
            if (element != null && element instanceof ModuleDependenciesAnalyzer.OrderEntryPathElement o) {
                OrderEntry entry = o.entry();
                Module m = entry.getOwnerModule();

                ShowSettingsUtil.getInstance().showProjectStructureDialog(
                    m.getProject(),
                    projectStructureSelector -> projectStructureSelector.selectOrderEntry(m, entry)
                );
            }
        }
    }

    /**
     * Base class for nodes
     *
     * @param <T> the actual explanation type
     */
    abstract class PathNode<T extends ModuleDependenciesAnalyzer.Explanation> extends NamedConfigurable<T> implements DataProvider {
        /**
         * The cut off length, after which URLs are not shown (only suffix)
         */
        public static final int CUTOFF_LENGTH = 80;

        /**
         * The explanation
         */
        protected final T myExplanation;
        /**
         * The tree with explanation
         */
        private Tree myExplanationTree;

        /**
         * The constructor
         *
         * @param explanation the wrapped explanation
         */
        public PathNode(T explanation) {
            myExplanation = explanation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T getEditableObject() {
            return myExplanation;
        }

        /**
         * @return a created tree component (to be used as
         */
        private JComponent createTreeComponent() {
            myExplanationTree = new Tree(new DefaultTreeModel(buildTree()));
            myExplanationTree.setRootVisible(false);
            myExplanationTree.setCellRenderer(new ExplanationTreeRenderer());
            DataManager.registerDataProvider(myExplanationTree, this);
            TreeUtil.expandAll(myExplanationTree);
            NavigateAction navigateAction = new NavigateAction();
            navigateAction.registerCustomShortcutSet(
                new CustomShortcutSet(CommonShortcuts.DOUBLE_CLICK_1.getShortcuts()[0]),
                myExplanationTree
            );
            DefaultActionGroup group = new DefaultActionGroup();
            group.addAction(navigateAction);
            PopupHandler.installUnknownPopupHandler(myExplanationTree, group, ActionManager.getInstance());
            return new JBScrollPane(myExplanationTree);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getData(@Nonnull @NonNls Key<?> dataId) {
            if (Project.KEY == dataId) {
                return myModule.getProject();
            }
            if (Module.KEY == dataId) {
                return myModule;
            }
            TreePath selectionPath = myExplanationTree.getSelectionPath();
            DefaultMutableTreeNode node = selectionPath == null ? null : (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            Object o = node == null ? null : node.getUserObject();
            return o instanceof ModuleDependenciesAnalyzer.OrderPathElement && ORDER_PATH_ELEMENT_KEY == dataId ? o : null;
        }

        /**
         * Build tree for the dependencies
         *
         * @return a tree model
         */
        private DefaultMutableTreeNode buildTree() {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");
            for (ModuleDependenciesAnalyzer.OrderPath orderPath : myExplanation.paths()) {
                addDependencyPath(root, orderPath, 0);
            }
            return root;
        }

        /**
         * Add the dependency path
         *
         * @param parent    the parent to which path is added
         * @param orderPath the order entry path
         * @param i         the position in the path
         */
        private void addDependencyPath(DefaultMutableTreeNode parent, ModuleDependenciesAnalyzer.OrderPath orderPath, int i) {
            if (i >= orderPath.entries().size()) {
                return;
            }
            ModuleDependenciesAnalyzer.OrderPathElement e = orderPath.entries().get(i);
            int sz = parent.getChildCount();
            DefaultMutableTreeNode n;
            if (sz == 0) {
                n = null;
            }
            else {
                n = (DefaultMutableTreeNode) parent.getChildAt(sz - 1);
                if (!n.getUserObject().equals(e)) {
                    n = null;
                }
            }
            if (n == null) {
                n = new DefaultMutableTreeNode(e);
                parent.add(n);
            }
            addDependencyPath(n, orderPath, i + 1);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void setDisplayName(String name) {
            // do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JComponent createOptionsPanel() {
            JComponent tree = createTreeComponent();
            JPanel panel = new JPanel(new BorderLayout());
            JLabel paths = new JLabel("Available Through Paths:");
            paths.setDisplayedMnemonic('P');
            paths.setLabelFor(tree);
            panel.add(paths, BorderLayout.NORTH);
            panel.add(tree, BorderLayout.CENTER);
            return panel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @RequiredUIAccess
        public boolean isModified() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @RequiredUIAccess
        public void apply() throws ConfigurationException {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @RequiredUIAccess
        public void reset() {
            //Do nothing
        }

        /**
         * Get appearance for rendering in master list
         *
         * @param selected true if selected
         * @param bold     true if bold
         * @return the result appearance
         */
        public abstract Consumer<ColoredTextContainer> getRender(boolean selected, boolean bold);

        /**
         * @return the string cut so it would fit the banner (the prefix is dropped)
         */
        protected String suffixForBanner(String p) {
            if (p.length() > CUTOFF_LENGTH) {
                p = "..." + p.substring(p.length() - CUTOFF_LENGTH);
            }
            return p;
        }

        /**
         * @return the string cut so it would fit the banner (the suffix is dropped)
         */
        protected String prefixForBanner(String p) {
            if (p.length() > CUTOFF_LENGTH) {
                p = p.substring(0, CUTOFF_LENGTH) + "...";
            }
            return p;
        }
    }

    /**
     * Cell renderer for explanation tree
     */
    static class ExplanationTreeRenderer extends ColoredTreeCellRenderer {
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
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) value;
            Object userObject = n.getUserObject();
            if (!(userObject instanceof ModuleDependenciesAnalyzer.OrderPathElement)) {
                return;
            }
            ModuleDependenciesAnalyzer.OrderPathElement e = (ModuleDependenciesAnalyzer.OrderPathElement) userObject;
            Consumer<ColoredTextContainer> appearance = e.getRender(selected);
            appearance.accept(this);
        }
    }

    /**
     * The entry node in URL node
     */
    class UrlNode extends PathNode<ModuleDependenciesAnalyzer.UrlExplanation> {

        /**
         * The constructor
         *
         * @param url the wrapped explanation
         */
        public UrlNode(ModuleDependenciesAnalyzer.UrlExplanation url) {
            super(url);
            setNameFieldShown(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Consumer<ColoredTextContainer> getRender(boolean selected, boolean isBold) {
            return component -> {
                component.setIcon(myExplanation.getIcon());
                Font font = UIUtil.getTreeFont();
                if (isBold) {
                    component.setFont(font.deriveFont(Font.BOLD));
                }
                else {
                    component.setFont(font.deriveFont(Font.PLAIN));
                }
                String p = VirtualFilePathUtil.toPresentableUrl(getEditableObject().url());
                component.append(
                    PathUtil.getFileName(p),
                    isBold ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES
                );
                component.append(" (" + PathUtil.getParentPath(p) + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getBannerSlogan() {
            VirtualFile f = myExplanation.getLocalFile();
            String p = f == null ? myExplanation.url() : f.getPath();
            p = suffixForBanner(p);
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.ofNullable(myExplanation.url());
        }
    }

    /**
     * The wrapper for order entries
     */
    class OrderEntryNode extends PathNode<ModuleDependenciesAnalyzer.OrderEntryExplanation> {

        /**
         * The constructor
         *
         * @param orderEntryExplanation the explanation to wrap
         */
        public OrderEntryNode(ModuleDependenciesAnalyzer.OrderEntryExplanation orderEntryExplanation) {
            super(orderEntryExplanation);
            setNameFieldShown(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Consumer<ColoredTextContainer> getRender(boolean selected, boolean isBold) {
            if (myExplanation.entry() instanceof ModuleSourceOrderEntry e) {
                if (e.getOwnerModule() == myModule) {
                    return component -> {
                        component.setIcon(AllIcons.Nodes.Module);
                        component.append("<This Module>", SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
                    };
                }
                else {
                    return OrderEntryAppearanceService.getInstance().getRenderForModule(e.getOwnerModule());
                }
            }
            return OrderEntryAppearanceService.getInstance().getRenderForOrderEntry(myExplanation.entry());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getBannerSlogan() {
            if (myExplanation.entry() instanceof ModuleSourceOrderEntry e) {
                return prefixForBanner("Module " + e.getOwnerModule().getName());
            }
            else {
                String p =
                    myExplanation.entry().getPresentableName() + " in module " + myExplanation.entry().getOwnerModule().getName();
                return suffixForBanner(p);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.ofNullable(myExplanation.entry().getPresentableName());
        }
    }

    /**
     * The action that allows including and excluding SDK entries from analysis
     */
    private class SdkFilterAction extends ToggleAction {

        /**
         * The constructor
         */
        public SdkFilterAction() {
            super("Include SDK", "If selected, the SDK classes are included", AllIcons.General.Jdk);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySettings.isSdkIncluded();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            mySettings.setIncludeSdk(state);
            updateTree();
        }
    }

    /**
     * The action that allows switching class path between URL and order entry modes
     */
    private class UrlModeAction extends ToggleAction {
        /**
         * The constructor
         */
        public UrlModeAction() {
            super("Use URL mode", "If selected, the URLs are displayed, otherwise order entries", AllIcons.Nodes.PpFile);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySettings.isUrlMode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            mySettings.setUrlMode(state);
            updateTree();
        }
    }

    /**
     * Classpath type action for the analyze classpath
     */
    private class ClasspathTypeAction extends ComboBoxAction {
        /**
         * The filter action group
         */
        DefaultActionGroup myItems;

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        public DefaultActionGroup createPopupActionGroup(JComponent component) {
            if (myItems == null) {
                myItems = new DefaultActionGroup();
                for (final ClasspathType classpathType : ClasspathType.values()) {
                    myItems.addAction(new DumbAwareAction(classpathType.getDescription()) {
                        @Override
                        @RequiredUIAccess
                        public void actionPerformed(@Nonnull AnActionEvent e) {
                            mySettings.setRuntime(classpathType.isRuntime());
                            mySettings.setTest(classpathType.isTest());
                            updateTree();
                        }
                    });
                }
            }
            return myItems;
        }

        /**
         * {@inheritDoc}
         */
        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            updateText(presentation);
        }

        /**
         * Update the text for the combobox
         *
         * @param presentation the presentaiton to update
         */
        private void updateText(Presentation presentation) {
            ClasspathType classpathType = getClasspathType();
            String t = classpathType.getDescription();
            presentation.setText(t);
        }
    }

    /**
     * The enumeration type that represents classpath entry filter
     */
    private static enum ClasspathType {
        /**
         * The production compile mode
         */
        PRODUCTION_COMPILE(false, false, "Production Compile"),
        /**
         * The production runtime mode
         */
        PRODUCTION_RUNTIME(false, true, "Production Runtime"),
        /**
         * The test runtime mode
         */
        TEST_RUNTIME(true, true, "Test Runtime"),
        /**
         * The test compile mode
         */
        TEST_COMPILE(true, false, "Test Compile");

        /**
         * true, if test mode
         */
        final private boolean myIsTest;
        /**
         * true, if runtime mode
         */
        final private boolean myIsRuntime;
        /**
         * The description text
         */
        final private String myDescription;

        /**
         * The constructor
         *
         * @param isTest      true if the test mode
         * @param isRuntime   true if the runtime ode
         * @param description the description text
         */
        ClasspathType(boolean isTest, boolean isRuntime, String description) {
            myIsTest = isTest;
            myIsRuntime = isRuntime;
            myDescription = description;
        }

        /**
         * @return true if the test mode
         */
        public boolean isTest() {
            return myIsTest;
        }

        /**
         * @return true if the runtime mode
         */
        public boolean isRuntime() {
            return myIsRuntime;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return myDescription;
        }

        /**
         * @return the description for the entry
         */
        public String getDescription() {
            return myDescription;
        }
    }
}
