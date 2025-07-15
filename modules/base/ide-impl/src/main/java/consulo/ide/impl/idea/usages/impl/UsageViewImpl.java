// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.usages.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.concurent.BoundedTaskExecutor;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.codeEditor.Editor;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.find.FindManager;
import consulo.ide.impl.idea.concurrency.JobSchedulerImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.TextCopyProvider;
import consulo.ide.impl.idea.ide.actions.exclusion.ExclusionHandler;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.usages.UsageDataUtil;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.SmartExpander;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUIHelper;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.content.Content;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.*;
import consulo.usage.internal.UsageViewEx;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.LinkedMultiMap;
import consulo.util.collection.Lists;
import consulo.util.collection.MultiMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author max
 */
public class UsageViewImpl implements UsageViewEx {
    private static final Logger LOG = Logger.getInstance(UsageViewImpl.class);
    public static final String SHOW_RECENT_FIND_USAGES_ACTION_ID = "UsageView.ShowRecentFindUsages";

    private final UsageNodeTreeBuilder myBuilder;
    private MyPanel myRootPanel; // accessed in EDT only
    private JTree myTree; // accessed in EDT only
    private final ScheduledFuture<?> myFireEventsFuture;
    private Content myContent;

    private final UsageViewPresentation myPresentation;
    private final UsageTarget[] myTargets;
    private final Supplier<UsageSearcher> myUsageSearcherFactory;
    private final Project myProject;

    private volatile boolean mySearchInProgress = true;
    private final ExporterToTextFile myTextFileExporter = new ExporterToTextFile(this, getUsageViewSettings());
    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    private final ExclusionHandlerEx<DefaultMutableTreeNode> myExclusionHandler;
    private final Map<Usage, UsageNode> myUsageNodes = new ConcurrentHashMap<>();
    public static final UsageNode NULL_NODE = new UsageNode(null, NullUsage.INSTANCE);
    private final ButtonPanel myButtonPanel;
    private final JComponent myAdditionalComponent = new JPanel(new BorderLayout());
    private volatile boolean isDisposed;
    private volatile boolean myChangesDetected;
    public static final Comparator<Usage> USAGE_COMPARATOR = (o1, o2) -> {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == NullUsage.INSTANCE) {
            return -1;
        }
        if (o2 == NullUsage.INSTANCE) {
            return 1;
        }
        if (o1 instanceof Comparable && o2 instanceof Comparable && o1.getClass() == o2.getClass()) {
            //noinspection unchecked
            int selfcompared = ((Comparable<Usage>)o1).compareTo(o2);
            if (selfcompared != 0) {
                return selfcompared;
            }

            if (o1 instanceof UsageInFile u1 && o2 instanceof UsageInFile u2) {
                VirtualFile f1 = u1.getFile();
                VirtualFile f2 = u2.getFile();

                if (f1 != null && f1.isValid() && f2 != null && f2.isValid()) {
                    return f1.getPresentableUrl().compareTo(f2.getPresentableUrl());
                }
            }

            return 0;
        }
        return o1.toString().compareTo(o2.toString());
    };
    public static final String HELP_ID = "ideaInterface.find";
    private UsageContextPanel myCurrentUsageContextPanel;
    private List<UsageContextPanelProvider> myUsageContextPanelProviders;
    private UsageContextPanelProvider myCurrentUsageContextProvider;

    private JPanel myCentralPanel; // accessed in EDT only

    private final GroupNode myRoot;
    private final UsageViewTreeModelBuilder myModel;
    private final Object lock = new Object();
    private Splitter myPreviewSplitter; // accessed in EDT only
    private volatile ProgressIndicator associatedProgress; // the progress that current find usages is running under

    // true if usages tree is currently expanding or collapsing
    // (either at the end of find usages thanks to the 'expand usages after find' setting or
    // because the user pressed 'expand all' or 'collapse all' button. During this, some ugly hacks applied
    // to speed up the expanding (see getExpandedDescendants() here and UsageViewTreeCellRenderer.customizeCellRenderer())
    private boolean myExpandingCollapsing;
    private final UsageViewTreeCellRenderer myUsageViewTreeCellRenderer;
    private Usage myOriginUsage;
    @Nullable
    private Action myRerunAction;
    private boolean myDisposeSmartPointersOnClose = true;
    private final ExecutorService updateRequests = AppExecutorUtil.createBoundedApplicationPoolExecutor(
        "Usage View Update Requests",
        PooledThreadExecutor.getInstance(),
        JobSchedulerImpl.getJobPoolParallelism(),
        this
    );
    private final List<ExcludeListener> myExcludeListeners = Lists.newLockFreeCopyOnWriteList();

    public UsageViewImpl(
        @Nonnull Project project,
        @Nonnull UsageViewPresentation presentation,
        @Nonnull UsageTarget[] targets,
        Supplier<UsageSearcher> usageSearcherFactory
    ) {
        // fire events every 50 ms, not more often to batch requests
        myFireEventsFuture = project.getUIAccess().getScheduler()
            .scheduleWithFixedDelay(this::fireEvents, 50, 50, TimeUnit.MILLISECONDS);
        Disposer.register(this, () -> myFireEventsFuture.cancel(false));

        myPresentation = presentation;
        myTargets = targets;
        myUsageSearcherFactory = usageSearcherFactory;
        myProject = project;

        myButtonPanel = new ButtonPanel(project);

        myModel = new UsageViewTreeModelBuilder(myPresentation, targets);
        myRoot = (GroupNode)myModel.getRoot();

        UsageModelTracker myModelTracker = new UsageModelTracker(project);
        Disposer.register(this, myModelTracker);

        myBuilder = new UsageNodeTreeBuilder(
            myTargets,
            getActiveGroupingRules(project, getUsageViewSettings()),
            getActiveFilteringRules(project),
            myRoot,
            myProject
        );

        MessageBusConnection messageBusConnection = myProject.getMessageBus().connect(this);
        messageBusConnection.subscribe(UsageFilteringRuleListener.class, this::rulesChanged);

        myUsageViewTreeCellRenderer = new UsageViewTreeCellRenderer(this);
        if (!myPresentation.isDetachedMode()) {
            UIUtil.invokeLaterIfNeeded(() -> {
                // lock here to avoid concurrent execution of this init and dispose in other thread
                synchronized (lock) {
                    if (isDisposed()) {
                        return;
                    }
                    myTree = new Tree(myModel) {
                        {
                            ToolTipManager.sharedInstance().registerComponent(this);
                        }

                        @Override
                        public boolean isRootVisible() {
                            return false;  // to avoid re-building model when it calls setRootVisible(true)
                        }

                        @Override
                        public String getToolTipText(MouseEvent e) {
                            TreePath path = getPathForLocation(e.getX(), e.getY());
                            if (path != null) {
                                if (getCellRenderer() instanceof UsageViewTreeCellRenderer) {
                                    return UsageViewTreeCellRenderer.getTooltipFromPresentation(path.getLastPathComponent());
                                }
                            }
                            return null;
                        }

                        @Override
                        public boolean isPathEditable(TreePath path) {
                            return path.getLastPathComponent() instanceof UsageViewTreeModelBuilder.TargetsRootNode;
                        }

                        // hack to avoid quadratic expandAll()
                        @Override
                        public Enumeration<TreePath> getExpandedDescendants(TreePath parent) {
                            return myExpandingCollapsing ? Collections.emptyEnumeration() : super.getExpandedDescendants(parent);
                        }
                    };
                    myTree.setName("UsageViewTree");

                    myRootPanel = new MyPanel(myTree);
                    Disposer.register(this, myRootPanel);
                    myTree.setModel(myModel);

                    myRootPanel.setLayout(new BorderLayout());

                    SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(false, true);
                    myRootPanel.add(toolWindowPanel, BorderLayout.CENTER);

                    toolWindowPanel.setToolbar(createActionsToolbar());

                    myCentralPanel = new JPanel(new BorderLayout());
                    setupCentralPanel();

                    initTree();
                    toolWindowPanel.setContent(myCentralPanel);

                    myTree.setCellRenderer(myUsageViewTreeCellRenderer);
                    //noinspection SSBasedInspection
                    SwingUtilities.invokeLater(() -> {
                        if (isDisposed() || myProject.isDisposed()) {
                            return;
                        }
                        collapseAll();
                    });

                    myModelTracker.addListener(isPropertyChange -> {
                        if (!isPropertyChange) {
                            myChangesDetected = true;
                        }
                        updateLater();
                    }, this);

                    if (myPresentation.isShowCancelButton()) {
                        addButtonToLowerPane(this::close, UsageLocalize.usageViewCancelButton().get());
                    }

                    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                        @Override
                        public void valueChanged(TreeSelectionEvent e) {
                            //noinspection SSBasedInspection
                            SwingUtilities.invokeLater(() -> {
                                if (isDisposed() || myProject.isDisposed()) {
                                    return;
                                }
                                updateOnSelectionChanged();
                            });
                        }
                    });

                    myTree.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            if (rulesChanged) {
                                rulesChanged = false;
                                rulesChanged();
                            }
                        }
                    });
                }
            });
        }
        myExclusionHandler = new ExclusionHandlerEx<>() {
            @Override
            public boolean isNodeExclusionAvailable(@Nonnull DefaultMutableTreeNode node) {
                return node instanceof UsageNode;
            }

            @Override
            public boolean isNodeExcluded(@Nonnull DefaultMutableTreeNode node) {
                return ((UsageNode)node).isDataExcluded();
            }

            @Override
            @RequiredUIAccess
            public void excludeNode(@Nonnull DefaultMutableTreeNode node) {
                Set<Node> nodes = new HashSet<>();
                collectAllChildNodes(node, nodes);
                collectParentNodes(node, true, nodes);
                setExcludeNodes(nodes, true, true);
            }

            @Override
            @RequiredUIAccess
            public void excludeNodeSilently(@Nonnull DefaultMutableTreeNode node) {
                Set<Node> nodes = new HashSet<>();
                collectAllChildNodes(node, nodes);
                collectParentNodes(node, true, nodes);
                setExcludeNodes(nodes, true, false);
            }

            // include the parent if its all children (except the "node" itself) excluded flags are "almostAllChildrenExcluded"
            private void collectParentNodes(
                @Nonnull DefaultMutableTreeNode node,
                boolean almostAllChildrenExcluded,
                @Nonnull Set<? super Node> nodes
            ) {
                TreeNode parent = node.getParent();
                if (parent == myRoot || !(parent instanceof GroupNode)) {
                    return;
                }
                GroupNode parentNode = (GroupNode)parent;
                List<Node> otherNodes;
                synchronized (parentNode) {
                    otherNodes = ContainerUtil.filter(parentNode.getChildren(), n -> n.isExcluded() != almostAllChildrenExcluded);
                }
                if (otherNodes.size() == 1 && otherNodes.get(0) == node) {
                    nodes.add(parentNode);
                    collectParentNodes(parentNode, almostAllChildrenExcluded, nodes);
                }
            }

            @RequiredUIAccess
            private void setExcludeNodes(@Nonnull Set<? extends Node> nodes, boolean excluded, boolean updateImmediately) {
                Set<Usage> affectedUsages = new LinkedHashSet<>();
                for (Node node : nodes) {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof Usage usage) {
                        affectedUsages.add(usage);
                    }
                    node.setExcluded(excluded, edtNodeChangedQueue);
                }

                if (updateImmediately) {
                    updateImmediatelyNodesUpToRoot(nodes);

                    for (ExcludeListener listener : myExcludeListeners) {
                        listener.fireExcluded(affectedUsages, excluded);
                    }
                }
            }

            @Override
            @RequiredUIAccess
            public void includeNode(@Nonnull DefaultMutableTreeNode node) {
                Set<Node> nodes = new HashSet<>();
                collectAllChildNodes(node, nodes);
                collectParentNodes(node, false, nodes);
                setExcludeNodes(nodes, false, true);
            }

            @Override
            public boolean isActionEnabled(boolean isExcludeAction) {
                return getPresentation().isExcludeAvailable();
            }

            @Override
            @RequiredUIAccess
            public void onDone(boolean isExcludeAction) {
                UIAccess.assertIsUIThread();
                if (myRootPanel.hasNextOccurence()) {
                    myRootPanel.goNextOccurence();
                }
            }
        };
    }

    @Nonnull
    UsageViewSettings getUsageViewSettings() {
        return UsageViewSettings.getInstance();
    }

    // nodes just changed: parent node -> changed child
    // this collection is needed for firing javax.swing.tree.DefaultTreeModel.nodesChanged() events in batch
    // has to be linked because events for child nodes should be fired after events for parent nodes
    private final MultiMap<Node, Node> changedNodesToFire = new LinkedMultiMap<>(); // guarded by changedNodesToFire

    private final Consumer<Node> edtNodeChangedQueue = node -> {
        if (!getPresentation().isDetachedMode()) {
            synchronized (changedNodesToFire) {
                Node parent = (Node)node.getParent();
                if (parent != null) {
                    changedNodesToFire.putValue(parent, node);
                }
            }
        }
    };

    // parent nodes under which the child node was just inserted.
    // it is needed for firing javax.swing.tree.DefaultTreeModel.fireTreeNodesInserted() events in batch.
    // has to be linked because events for child nodes should be fired after events for parent nodes.
    private final Set<Node> nodesInsertedUnder = new LinkedHashSet<>(); // guarded by nodesInsertedUnder

    private final Consumer<Node> edtNodeInsertedUnderQueue = (@Nonnull Node parent) -> {
        if (!getPresentation().isDetachedMode()) {
            synchronized (nodesInsertedUnder) {
                nodesInsertedUnder.add(parent);
            }
        }
    };

    // this method is called regularly every 50ms to fire events in batch
    @RequiredUIAccess
    private void fireEvents() {
        UIAccess.assertIsUIThread();
        List<Node> insertedUnder;
        synchronized (nodesInsertedUnder) {
            insertedUnder = new ArrayList<>(nodesInsertedUnder);
            nodesInsertedUnder.clear();
        }
        // for each node synchronize its Swing children (javax.swing.tree.DefaultMutableTreeNode.children)
        // and its model children (consulo.ide.impl.idea.usages.impl.GroupNode.getChildren())
        // by issuing corresponding javax.swing.tree.DefaultMutableTreeNode.insert() and then calling javax.swing.tree.DefaultTreeModel.nodesWereInserted()
        IntList indicesToFire = IntLists.newArrayList();
        List<Node> nodesToFire = new ArrayList<>();
        for (Node parentNode : insertedUnder) {
            List<Node> swingChildren = ((GroupNode)parentNode).getSwingChildren();
            synchronized (parentNode) {
                List<Node> modelChildren = ((GroupNode)parentNode).getChildren();
                assert modelChildren.size() >= swingChildren.size();

                int k = 0; // index in swingChildren
                for (int i = 0; i < modelChildren.size(); i++) {
                    Node modelNode = modelChildren.get(i);
                    Node swingNode = k >= swingChildren.size() ? null : swingChildren.get(k);
                    if (swingNode == modelNode) {
                        k++;
                        continue;
                    }
                    parentNode.insertNewNode(modelNode, i);
                    indicesToFire.add(i);
                    nodesToFire.add(modelNode);
                    if (k == i) {
                        k++; // ignore just inserted node
                    }
                    if (modelNode instanceof UsageNode && modelNode.getParent() instanceof GroupNode groupNode) {
                        groupNode.incrementUsageCount();
                    }
                }
            }

            myModel.fireTreeNodesInserted(
                parentNode,
                myModel.getPathToRoot(parentNode),
                indicesToFire.toArray(),
                nodesToFire.toArray(new Node[0])
            );
            nodesToFire.clear();
            indicesToFire.clear();
        }

        // group nodes from changedNodesToFire by their parents and issue corresponding javax.swing.tree.DefaultTreeModel.fireTreeNodesChanged()
        List<Map.Entry<Node, Collection<Node>>> changed;
        synchronized (changedNodesToFire) {
            changed = new ArrayList<>(changedNodesToFire.entrySet());
            changedNodesToFire.clear();
        }
        for (Map.Entry<Node, Collection<Node>> entry : changed) {
            Node parentNode = entry.getKey();
            Set<Node> childrenToUpdate = new HashSet<>(entry.getValue());

            for (int i = 0; i < parentNode.getChildCount(); i++) {
                Node childNode = (Node)parentNode.getChildAt(i);
                if (childrenToUpdate.contains(childNode)) {
                    nodesToFire.add(childNode);
                    indicesToFire.add(i);
                }
            }

            myModel.fireTreeNodesChanged(
                parentNode,
                myModel.getPathToRoot(parentNode),
                indicesToFire.toArray(),
                nodesToFire.toArray(new Node[0])
            );
            nodesToFire.clear();
            indicesToFire.clear();
        }
    }

    @Override
    public void searchFinished() {
        drainQueuedUsageNodes();
        setSearchInProgress(false);
    }

    @Override
    public boolean searchHasBeenCancelled() {
        ProgressIndicator progress = associatedProgress;
        return progress != null && progress.isCanceled();
    }

    @Override
    public void cancelCurrentSearch() {
        ProgressIndicator progress = associatedProgress;
        if (progress != null) {
            ProgressWrapper.unwrap(progress).cancel();
        }
    }

    @RequiredUIAccess
    private int getVisibleRowCount() {
        UIAccess.assertIsUIThread();
        return TreeUtil.getVisibleRowCount(myTree);
    }

    @RequiredUIAccess
    private void setupCentralPanel() {
        UIAccess.assertIsUIThread();

        JScrollPane treePane = ScrollPaneFactory.createScrollPane(myTree);
        myPreviewSplitter = new OnePixelSplitter(false, 0.5f, 0.1f, 0.9f);
        myPreviewSplitter.setFirstComponent(treePane);

        myCentralPanel.add(myPreviewSplitter, BorderLayout.CENTER);

        updateUsagesContextPanels();

        myCentralPanel.add(myAdditionalComponent, BorderLayout.SOUTH);
        myAdditionalComponent.add(myButtonPanel, BorderLayout.SOUTH);
    }

    @RequiredUIAccess
    private void updateUsagesContextPanels() {
        UIAccess.assertIsUIThread();
        disposeUsageContextPanels();
        if (isPreviewUsages()) {
            myPreviewSplitter.setProportion(getUsageViewSettings().getPreviewUsagesSplitterProportion());
            JBTabbedPane tabbedPane = new JBTabbedPane(SwingConstants.BOTTOM);
            tabbedPane.setTabComponentInsets(null);

            List<UsageContextPanelProvider> extensions = UsageContextPanelProvider.EP_NAME.getExtensionList(myProject);
            myUsageContextPanelProviders = ContainerUtil.filter(extensions, provider -> provider.isAvailableFor(this));
            Map<String, JComponent> components = new LinkedHashMap<>();
            for (UsageContextPanelProvider provider : myUsageContextPanelProviders) {
                JComponent component;
                if (myCurrentUsageContextProvider == null || myCurrentUsageContextProvider == provider) {
                    myCurrentUsageContextProvider = provider;
                    myCurrentUsageContextPanel = provider.create(this);
                    component = myCurrentUsageContextPanel.createComponent();
                }
                else {
                    component = new JLabel();
                }
                components.put(provider.getTabTitle(), component);
            }
            JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
            if (components.size() == 1) {
                panel.add(components.values().iterator().next(), BorderLayout.CENTER);
            }
            else {
                for (Map.Entry<String, JComponent> entry : components.entrySet()) {
                    tabbedPane.addTab(entry.getKey(), entry.getValue());
                }
                int index = myUsageContextPanelProviders.indexOf(myCurrentUsageContextProvider);
                tabbedPane.setSelectedIndex(index);
                tabbedPane.addChangeListener(e -> {
                    int currentIndex = tabbedPane.getSelectedIndex();
                    UsageContextPanelProvider selectedProvider = myUsageContextPanelProviders.get(currentIndex);
                    if (selectedProvider != myCurrentUsageContextProvider) {
                        tabSelected(selectedProvider);
                    }
                });
                panel.add(tabbedPane, BorderLayout.CENTER);
            }
            myPreviewSplitter.setSecondComponent(panel);
        }
        else {
            myPreviewSplitter.setSecondComponent(null);
            myPreviewSplitter.setProportion(1);
        }

        myRootPanel.revalidate();
        myRootPanel.repaint();
    }

    @RequiredUIAccess
    private void tabSelected(@Nonnull UsageContextPanelProvider provider) {
        UIAccess.assertIsUIThread();
        myCurrentUsageContextProvider = provider;
        updateUsagesContextPanels();
        updateOnSelectionChanged();
    }

    @RequiredUIAccess
    private void disposeUsageContextPanels() {
        UIAccess.assertIsUIThread();
        if (myCurrentUsageContextPanel != null) {
            saveSplitterProportions();
            Disposer.dispose(myCurrentUsageContextPanel);
            myCurrentUsageContextPanel = null;
        }
    }

    public boolean isPreviewUsages() {
        return myPresentation.isReplaceMode() ? getUsageViewSettings().isReplacePreviewUsages() : getUsageViewSettings().isPreviewUsages();
    }

    public void setPreviewUsages(boolean state) {
        if (myPresentation.isReplaceMode()) {
            getUsageViewSettings().setReplacePreviewUsages(state);
        }
        else {
            getUsageViewSettings().setPreviewUsages(state);
        }
    }

    @Nonnull
    private static UsageFilteringRule[] getActiveFilteringRules(Project project) {
        List<UsageFilteringRuleProvider> providers = UsageFilteringRuleProvider.EP_NAME.getExtensionList();
        List<UsageFilteringRule> list = new ArrayList<>(providers.size());
        for (UsageFilteringRuleProvider provider : providers) {
            ContainerUtil.addAll(list, provider.getActiveRules(project));
        }
        return list.toArray(UsageFilteringRule.EMPTY_ARRAY);
    }

    @Nonnull
    private static UsageGroupingRule[] getActiveGroupingRules(
        @Nonnull Project project,
        @Nonnull UsageViewSettings usageViewSettings
    ) {
        List<UsageGroupingRuleProvider> providers = UsageGroupingRuleProvider.EP_NAME.getExtensionList();
        List<UsageGroupingRule> list = new ArrayList<>(providers.size());
        for (UsageGroupingRuleProvider provider : providers) {
            ContainerUtil.addAll(list, provider.getActiveRules(project, usageViewSettings));
        }

        Collections.sort(list, Comparator.comparingInt(UsageGroupingRule::getRank));
        return list.toArray(UsageGroupingRule.EMPTY_ARRAY);
    }

    @RequiredUIAccess
    private void initTree() {
        UIAccess.assertIsUIThread();
        myTree.setShowsRootHandles(true);
        SmartExpander.installOn(myTree);
        TreeUtil.installActions(myTree);
        EditSourceOnDoubleClickHandler.install(myTree);
        myTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    TreePath leadSelectionPath = myTree.getLeadSelectionPath();
                    if (leadSelectionPath == null) {
                        return;
                    }

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)leadSelectionPath.getLastPathComponent();
                    if (node instanceof UsageNode usageNode) {
                        Usage usage = usageNode.getUsage();
                        usage.navigate(false);
                        usage.highlightInEditor();
                    }
                    else if (node.isLeaf()) {
                        Navigatable navigatable = getNavigatableForNode(node, !myPresentation.isReplaceMode());
                        if (navigatable != null && navigatable.canNavigate()) {
                            navigatable.navigate(false);
                        }
                    }
                }
            }
        });

        TreeUtil.promiseSelectFirst(myTree);
        PopupHandler.installPopupHandler(myTree, IdeActions.GROUP_USAGE_VIEW_POPUP, ActionPlaces.USAGE_VIEW_POPUP);

        myTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            @RequiredUIAccess
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                Object component = path.getLastPathComponent();
                if (component instanceof Node node && !myExpandingCollapsing && node.needsUpdate()) {
                    List<Node> toUpdate = new ArrayList<>();
                    checkNodeValidity(node, path, toUpdate);
                    queueUpdateBulk(toUpdate, EmptyRunnable.getInstance());
                }
            }

            @Override
            @RequiredUIAccess
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });

        TreeUIHelper.getInstance().installTreeSpeedSearch(
            myTree,
            o -> {
                Object value = o.getLastPathComponent();
                TreeCellRenderer renderer = myTree.getCellRenderer();
                if (renderer instanceof UsageViewTreeCellRenderer coloredRenderer) {
                    return coloredRenderer.getPlainTextForNode(value);
                }
                return value == null ? null : value.toString();
            },
            true
        );
    }

    @Nonnull
    @RequiredUIAccess
    private JComponent createActionsToolbar() {
        UIAccess.assertIsUIThread();

        DefaultActionGroup group = new DefaultActionGroup() {
            @Override
            public boolean isDumbAware() {
                return true;
            }
        };

        AnAction[] actions = createActions();
        for (AnAction action : actions) {
            if (action != null) {
                group.add(action);
            }
        }
        return toUsageViewToolbar(group);
    }

    @Nonnull
    @RequiredUIAccess
    private JComponent toUsageViewToolbar(@Nonnull DefaultActionGroup group) {
        UIAccess.assertIsUIThread();
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.USAGE_VIEW_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(myRootPanel);
        return actionToolbar.getComponent();
    }

    @SuppressWarnings("WeakerAccess") // used in rider
    protected boolean isPreviewUsageActionEnabled() {
        return true;
    }

    @RequiredUIAccess
    public void addFilteringActions(@Nonnull Consumer<AnAction> group) {
        UIAccess.assertIsUIThread();
        addFilteringActions(group, true);
    }

    private void addFilteringActions(@Nonnull Consumer<AnAction> group, boolean includeExtensionPoints) {
        if (getPresentation().isMergeDupLinesAvailable()) {
            MergeDupLines mergeDupLines = new MergeDupLines();
            JComponent component = myRootPanel;
            if (component != null) {
                mergeDupLines.registerCustomShortcutSet(mergeDupLines.getShortcutSet(), component, this);
            }
            group.accept(mergeDupLines);
        }
        if (includeExtensionPoints) {
            addFilteringFromExtensionPoints(group);
        }
    }

    private void addFilteringFromExtensionPoints(@Nonnull Consumer<AnAction> group) {
        for (UsageFilteringRuleProvider provider : UsageFilteringRuleProvider.EP_NAME.getExtensionList()) {
            AnAction[] actions = provider.createFilteringActions(this);
            for (AnAction action : actions) {
                group.accept(action);
            }
        }
    }

    @Nonnull
    @RequiredUIAccess
    protected AnAction[] createActions() {
        UIAccess.assertIsUIThread();
        TreeExpander treeExpander = new TreeExpander() {
            @Override
            @RequiredUIAccess
            public void expandAll() {
                UsageViewImpl.this.expandAll();
                getUsageViewSettings().setExpanded(true);
            }

            @Override
            public boolean canExpand() {
                return true;
            }

            @Override
            @RequiredUIAccess
            public void collapseAll() {
                UsageViewImpl.this.collapseAll();
                getUsageViewSettings().setExpanded(false);
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };

        CommonActionsManager actionsManager = CommonActionsManager.getInstance();

        JComponent component = getComponent();

        AnAction expandAllAction = actionsManager.createExpandAllAction(treeExpander, component);
        AnAction collapseAllAction = actionsManager.createCollapseAllAction(treeExpander, component);

        Disposer.register(
            this,
            () -> {
                expandAllAction.unregisterCustomShortcutSet(component);
                collapseAllAction.unregisterCustomShortcutSet(component);
            }
        );

        DefaultActionGroup group = new DefaultActionGroup();
        group.setPopup(true);
        group.getTemplatePresentation().setIcon(PlatformIconGroup.actionsGroupby());
        group.getTemplatePresentation().setTextValue(UsageLocalize.actionGroupByTitle());
        group.getTemplatePresentation().setDescriptionValue(UsageLocalize.actionGroupByTitle());
        AnAction[] groupingActions = createGroupingActions();
        if (groupingActions.length > 0) {
            group.add(new AnSeparator(UsageLocalize.actionGroupByTitle()));
            group.addAll(groupingActions);
            group.add(new AnSeparator());
        }

        addFilteringActions(group::add, false);
        DefaultActionGroup filteringSubgroup = new DefaultActionGroup();
        addFilteringFromExtensionPoints(filteringSubgroup::add);

        return new AnAction[]{
            ActionManager.getInstance().getAction("UsageView.Rerun"),
            actionsManager.createPrevOccurenceAction(myRootPanel),
            actionsManager.createNextOccurenceAction(myRootPanel),
            new AnSeparator(),
            group,
            filteringSubgroup,
            expandAllAction,
            collapseAllAction,
            new AnSeparator(),
            isPreviewUsageActionEnabled() ? new PreviewUsageAction(this) : null,
            new AnSeparator(),
            canShowSettings() ? new ShowSettings() : null,
        };
    }

    private boolean canShowSettings() {
        if (myTargets.length == 0) {
            return false;
        }
        NavigationItem target = myTargets[0];
        return target instanceof ConfigurableUsageTarget;
    }

    @Nonnull
    private AnAction[] createGroupingActions() {
        List<UsageGroupingRuleProvider> providers = UsageGroupingRuleProvider.EP_NAME.getExtensionList();
        List<AnAction> list = new ArrayList<>(providers.size());
        for (UsageGroupingRuleProvider provider : providers) {
            ContainerUtil.addAll(list, provider.createGroupingActions(this));
        }
        ActionImplUtil.sortAlphabetically(list);
        ActionImplUtil.moveActionTo(list, "Module", "Flatten Modules", true);
        return list.toArray(AnAction.EMPTY_ARRAY);
    }

    @RequiredUIAccess
    private boolean shouldTreeReactNowToRuleChanges() {
        UIAccess.assertIsUIThread();
        return myPresentation.isDetachedMode() || myTree.isShowing();
    }

    private boolean rulesChanged;

    @RequiredUIAccess
    private void rulesChanged() {
        UIAccess.assertIsUIThread();
        if (!shouldTreeReactNowToRuleChanges()) {
            rulesChanged = true;
            return;
        }

        List<UsageState> states = new ArrayList<>();
        if (myTree != null) {
            captureUsagesExpandState(new TreePath(myTree.getModel().getRoot()), states);
        }
        List<Usage> allUsages = new ArrayList<>(myUsageNodes.keySet());
        Collections.sort(allUsages, USAGE_COMPARATOR);
        Set<Usage> excludedUsages = getExcludedUsages();
        reset();
        myBuilder.setGroupingRules(getActiveGroupingRules(myProject, getUsageViewSettings()));
        myBuilder.setFilteringRules(getActiveFilteringRules(myProject));
        for (int i = allUsages.size() - 1; i >= 0; i--) {
            Usage usage = allUsages.get(i);
            if (!usage.isValid()) {
                allUsages.remove(i);
                continue;
            }
            if (usage instanceof MergeableUsage mergeableUsage) {
                mergeableUsage.reset();
            }
        }
        //noinspection SSBasedInspection
        appendUsagesInBulk(allUsages).thenRun(() -> SwingUtilities.invokeLater(() -> {
            if (isDisposed()) {
                return;
            }
            if (myTree != null) {
                excludeUsages(excludedUsages.toArray(Usage.EMPTY_ARRAY));
                restoreUsageExpandState(states);
                updateImmediately();
            }
        }));
        if (myCentralPanel != null) {
            updateUsagesContextPanels();
        }
    }

    @RequiredUIAccess
    private void captureUsagesExpandState(@Nonnull TreePath pathFrom, @Nonnull Collection<? super UsageState> states) {
        UIAccess.assertIsUIThread();
        if (!myTree.isExpanded(pathFrom)) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)pathFrom.getLastPathComponent();
        int childCount = node.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            TreeNode child = node.getChildAt(idx);
            if (child instanceof UsageNode usageNode) {
                Usage usage = usageNode.getUsage();
                states.add(new UsageState(usage, myTree.getSelectionModel().isPathSelected(pathFrom.pathByAddingChild(child))));
            }
            else {
                captureUsagesExpandState(pathFrom.pathByAddingChild(child), states);
            }
        }
    }

    @RequiredUIAccess
    private void restoreUsageExpandState(@Nonnull Collection<? extends UsageState> states) {
        UIAccess.assertIsUIThread();
        //always expand the last level group
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)myTree.getModel().getRoot();
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
            if (child instanceof GroupNode) {
                TreePath treePath = new TreePath(child.getPath());
                myTree.expandPath(treePath);
            }
        }
        myTree.getSelectionModel().clearSelection();
        for (UsageState usageState : states) {
            usageState.restore();
        }
    }

    @Override
    @RequiredUIAccess
    public void expandAll() {
        doExpandingCollapsing(() -> TreeUtil.expandAll(myTree));
    }

    @RequiredUIAccess
    private void expandTree(int levels) {
        doExpandingCollapsing(() -> TreeUtil.expand(myTree, levels));
    }

    /**
     * Allows to skip a lot of {@link #clearRendererCache}, received via {@link TreeExpansionListener}.
     *
     * @param task that expands or collapses a tree
     */
    @RequiredUIAccess
    private void doExpandingCollapsing(@Nonnull Runnable task) {
        if (isDisposed()) {
            return;
        }
        UIAccess.assertIsUIThread();
        fireEvents();  // drain all remaining insertion events in the queue

        myExpandingCollapsing = true;
        try {
            task.run();
        }
        finally {
            myExpandingCollapsing = false;
        }
    }

    @RequiredUIAccess
    private void collapseAll() {
        doExpandingCollapsing(() -> {
            TreeUtil.collapseAll(myTree, 3);
            TreeUtil.expand(myTree, 2);
        });
    }

    @RequiredUIAccess
    public void expandRoot() {
        expandTree(1);
    }

    @Nonnull
    @RequiredUIAccess
    DefaultMutableTreeNode getModelRoot() {
        UIAccess.assertIsUIThread();
        return (DefaultMutableTreeNode)myTree.getModel().getRoot();
    }

    @Override
    @RequiredUIAccess
    public void select() {
        UIAccess.assertIsUIThread();
        // can be null during ctr execution
        if (myTree != null) {
            myTree.requestFocusInWindow();
        }
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Nullable
    @Deprecated
    public static KeyboardShortcut getShowUsagesWithSettingsShortcut() {
        return UsageViewUtil.getShowUsagesWithSettingsShortcut();
    }

    @Override
    public void associateProgress(@Nonnull ProgressIndicator indicator) {
        associatedProgress = indicator;
    }

    private class MergeDupLines extends RuleAction {
        private MergeDupLines() {
            super(UsageViewImpl.this, UsageLocalize.actionMergeSameLine(), PlatformIconGroup.toolbarFilterdups());
            setShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK)));
        }

        @Override
        protected boolean getOptionValue() {
            return getUsageViewSettings().isFilterDuplicatedLine();
        }

        @Override
        protected void setOptionValue(boolean value) {
            getUsageViewSettings().setFilterDuplicatedLine(value);
        }
    }

    private class ShowSettings extends AnAction {
        @RequiredUIAccess
        private ShowSettings() {
            super(UsageLocalize.actionTextUsageViewSettings(), LocalizeValue.empty(), PlatformIconGroup.generalGearplain());
            ConfigurableUsageTarget configurableUsageTarget = UsageViewUtil.getConfigurableTarget(myTargets);
            LocalizeValue description = null;
            try {
                description = configurableUsageTarget == null
                    ? LocalizeValue.empty()
                    : UsageLocalize.actionShowsettingsShowSettingsForDescription(configurableUsageTarget.getLongDescriptiveName());
            }
            catch (IndexNotReadyException ignored) {
            }
            if (description == null) {
                description = UsageLocalize.actionShowsettingsShowFindUsagesSettingsDialogDescription();
            }
            getTemplatePresentation().setDescriptionValue(description);
            KeyboardShortcut shortcut = configurableUsageTarget == null
                ? UsageViewUtil.getShowUsagesWithSettingsShortcut()
                : configurableUsageTarget.getShortcut();
            if (shortcut != null) {
                registerCustomShortcutSet(new CustomShortcutSet(shortcut), getComponent());
            }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(!e.hasData(Editor.KEY));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            FindManager.getInstance(getProject()).showSettingsAndFindUsages(myTargets);
        }
    }

    @RequiredUIAccess
    public void refreshUsages() {
        reset();
        doReRun();
    }

    /**
     * @return usage view which will be shown after re-run (either {@code this} if it knows how to re-run itself, or the new created one otherwise)
     */
    @SuppressWarnings("WeakerAccess") // used in rider
    protected UsageView doReRun() {
        myChangesDetected = false;
        if (myRerunAction == null) {
            UsageViewPresentation rerunPresentation = myPresentation.copy();
            rerunPresentation.setOpenInNewTab(false);
            return UsageViewManager.getInstance(getProject()).searchAndShowUsages(
                myTargets,
                myUsageSearcherFactory,
                true,
                false,
                rerunPresentation,
                null
            );
        }
        myRerunAction.actionPerformed(null);
        return this;
    }

    @RequiredUIAccess
    private void reset() {
        UIAccess.assertIsUIThread();
        myUsageNodes.clear();
        myModel.reset();
        if (!myPresentation.isDetachedMode()) {
            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(() -> expandTree(2));
        }
    }

    void drainQueuedUsageNodes() {
        assert !Application.get().isDispatchThread() : Thread.currentThread();
        UIUtil.invokeAndWaitIfNeeded((Runnable)this::fireEvents);
    }

    @Override
    @RequiredReadAction
    public void appendUsage(@Nonnull Usage usage) {
        if (Application.get().isDispatchThread()) {
            addUpdateRequest(() -> AccessRule.read((ThrowableRunnable<RuntimeException>)() -> doAppendUsage(usage)));
        }
        else {
            doAppendUsage(usage);
        }
    }

    private void addUpdateRequest(@Nonnull Runnable request) {
        updateRequests.execute(request);
    }

    @Override
    public void waitForUpdateRequestsCompletion() {
        assert !Application.get().isDispatchThread();
        try {
            ((BoundedTaskExecutor)updateRequests).waitAllTasksExecuted(10, TimeUnit.MINUTES);
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<?> appendUsagesInBulk(@Nonnull Collection<? extends Usage> usages) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        addUpdateRequest(() -> AccessRule.read(() -> {
            try {
                for (Usage usage : usages) {
                    doAppendUsage(usage);
                }
                result.complete(null);
            }
            catch (Exception e) {
                result.completeExceptionally(e);
                throw e;
            }
        }));
        return result;
    }

    @RequiredReadAction
    public UsageNode doAppendUsage(@Nonnull Usage usage) {
        assert !Application.get().isDispatchThread();
        // invoke in ReadAction to be be sure that usages are not invalidated while the tree is being built
        Application.get().assertReadAccessAllowed();
        if (!usage.isValid()) {
            // because the view is built incrementally, the usage may be already invalid, so need to filter such cases
            return null;
        }

        for (UsageViewElementsListener listener : UsageViewElementsListener.EP_NAME.getExtensionList()) {
            listener.beforeUsageAdded(this, usage);
        }

        UsageNode child = myBuilder.appendOrGet(usage, isFilterDuplicateLines(), edtNodeInsertedUnderQueue);
        myUsageNodes.put(usage, child == null ? NULL_NODE : child);

        if (child != null && getPresentation().isExcludeAvailable()) {
            for (UsageViewElementsListener listener : UsageViewElementsListener.EP_NAME.getExtensionList()) {
                if (listener.isExcludedByDefault(this, usage)) {
                    myExclusionHandler.excludeNodeSilently(child);
                }
            }
        }

        for (Node node = child; node != myRoot && node != null; node = (Node)node.getParent()) {
            node.update(this, edtNodeChangedQueue);
        }

        return child;
    }

    @Override
    @RequiredUIAccess
    public void removeUsage(@Nonnull Usage usage) {
        removeUsagesBulk(Collections.singleton(usage));
    }

    @Override
    @RequiredUIAccess
    public void removeUsagesBulk(@Nonnull Collection<Usage> usages) {
        Usage toSelect = getNextToSelect(usages);
        UsageNode nodeToSelect = toSelect != null ? myUsageNodes.get(toSelect) : null;

        Set<UsageNode> nodes = usagesToNodes(usages.stream()).collect(Collectors.toSet());
        usages.forEach(myUsageNodes::remove);
        if (!myUsageNodes.isEmpty()) {
            Set<UsageInfo> mergedInfos = usages.stream()
                .filter(usage -> usage instanceof UsageInfo2UsageAdapter usageAdapter && usageAdapter.getMergedInfos().length > 1)
                .flatMap(usage -> Arrays.stream(((UsageInfo2UsageAdapter)usage).getMergedInfos()))
                .collect(Collectors.toSet());
            if (!mergedInfos.isEmpty()) {
                myUsageNodes.keySet().removeIf(
                    usage -> usage instanceof UsageInfo2UsageAdapter usageAdapter
                        && mergedInfos.contains(usageAdapter.getUsageInfo())
                );
            }
        }

        if (!nodes.isEmpty() && !myPresentation.isDetachedMode()) {
            UIUtil.invokeLaterIfNeeded(() -> {
                if (isDisposed()) {
                    return;
                }
                DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
                ((GroupNode)treeModel.getRoot()).removeUsagesBulk(nodes, treeModel);
                if (nodeToSelect != null) {
                    TreePath path = new TreePath(nodeToSelect.getPath());
                    myTree.addSelectionPath(path);
                }
            });
        }
    }

    @Override
    public void includeUsages(@Nonnull Usage[] usages) {
        usagesToNodes(Arrays.stream(usages)).forEach(myExclusionHandler::includeNode);
    }

    @Override
    public void excludeUsages(@Nonnull Usage[] usages) {
        usagesToNodes(Arrays.stream(usages)).forEach(myExclusionHandler::excludeNode);
    }

    private Stream<UsageNode> usagesToNodes(Stream<Usage> usages) {
        return usages.map(myUsageNodes::get).filter(node -> node != NULL_NODE && node != null);
    }

    @Override
    @RequiredUIAccess
    public void selectUsages(@Nonnull Usage[] usages) {
        UIAccess.assertIsUIThread();
        TreePath[] paths = usagesToNodes(Arrays.stream(usages)).map(node -> new TreePath(node.getPath())).toArray(TreePath[]::new);

        myTree.setSelectionPaths(paths);
        if (paths.length != 0) {
            myTree.scrollPathToVisible(paths[0]);
        }
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusableComponent() {
        UIAccess.assertIsUIThread();
        return myTree != null ? myTree : getComponent();
    }

    @Override
    @Nonnull
    @RequiredUIAccess
    public JComponent getComponent() {
        UIAccess.assertIsUIThread();
        return myRootPanel == null ? new JLabel() : myRootPanel;
    }

    @Override
    public int getUsagesCount() {
        return myUsageNodes.size();
    }

    @Override
    public void addExcludeListener(@Nonnull Disposable disposable, @Nonnull ExcludeListener listener) {
        myExcludeListeners.add(listener);
        Disposer.register(disposable, () -> myExcludeListeners.remove(listener));
    }

    void setContent(@Nonnull Content content) {
        myContent = content;
        content.setDisposer(this);
    }

    @RequiredUIAccess
    private void updateImmediately() {
        UIAccess.assertIsUIThread();
        if (isDisposed()) {
            return;
        }
        TreeNode root = (TreeNode)myTree.getModel().getRoot();
        List<Node> toUpdate = new ArrayList<>();
        checkNodeValidity(root, new TreePath(root), toUpdate);
        queueUpdateBulk(toUpdate, EmptyRunnable.getInstance());
        updateOnSelectionChanged();
    }

    private void queueUpdateBulk(@Nonnull List<? extends Node> toUpdate, @Nonnull Runnable onCompletedInEdt) {
        if (toUpdate.isEmpty()) {
            return;
        }
        addUpdateRequest(() -> {
            for (Node node : toUpdate) {
                try {
                    if (isDisposed()) {
                        break;
                    }
                    if (!runReadActionWithRetries(() -> node.update(this, edtNodeChangedQueue))) {
                        Application.get().invokeLater(() -> queueUpdateBulk(toUpdate, onCompletedInEdt));
                        return;
                    }
                }
                catch (IndexNotReadyException ignore) {
                }
            }
            Application.get().invokeLater(onCompletedInEdt);
        });
    }

    private boolean runReadActionWithRetries(@Nonnull Runnable r) {
        if (Application.get().isDispatchThread()) {
            r.run();
            return true;
        }

        int MAX_RETRIES = 5;
        for (int i = 0; i < MAX_RETRIES; i++) {
            if (isDisposed()) {
                return true;
            }

            if (ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(r)) {
                return true;
            }
            ProgressIndicatorUtils.yieldToPendingWriteActions();
        }
        return false;
    }

    @RequiredUIAccess
    private void updateImmediatelyNodesUpToRoot(@Nonnull Collection<? extends Node> nodes) {
        UIAccess.assertIsUIThread();
        if (isDisposed()) {
            return;
        }
        TreeNode root = (TreeNode)myTree.getModel().getRoot();
        Set<Node> queued = new HashSet<>();
        List<Node> toUpdate = new ArrayList<>();
        while (true) {
            Set<Node> parents = new HashSet<>();
            for (Node node : nodes) {
                toUpdate.add(node);
                TreeNode parent = node.getParent();
                if (parent != root && parent instanceof Node parentNode && queued.add(parentNode)) {
                    parents.add(parentNode);
                }
            }
            if (parents.isEmpty()) {
                break;
            }
            nodes = parents;
        }
        queueUpdateBulk(toUpdate, EmptyRunnable.getInstance());
        updateImmediately();
    }


    @RequiredUIAccess
    private void updateOnSelectionChanged() {
        UIAccess.assertIsUIThread();
        if (myCurrentUsageContextPanel != null) {
            try {
                myCurrentUsageContextPanel.updateLayout(getSelectedUsageInfos());
            }
            catch (IndexNotReadyException ignore) {
            }
        }
    }

    @RequiredUIAccess
    private void checkNodeValidity(@Nonnull TreeNode node, @Nonnull TreePath path, @Nonnull List<? super Node> result) {
        UIAccess.assertIsUIThread();
        boolean shouldCheckChildren = true;
        if (myTree.isCollapsed(path)) {
            if (node instanceof Node mutableNode) {
                mutableNode.markNeedUpdate();
            }
            shouldCheckChildren = false;
            // optimization: do not call expensive update() on invisible node
        }
        UsageViewTreeCellRenderer.RowLocation isVisible =
            myUsageViewTreeCellRenderer.isRowVisible(
                myTree.getRowForPath(new TreePath(((DefaultMutableTreeNode)node).getPath())),
                myTree.getVisibleRect()
            );

        // if row is below visible rectangle, no sense to update it or any children
        if (shouldCheckChildren && isVisible != UsageViewTreeCellRenderer.RowLocation.AFTER_VISIBLE_RECT) {
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                checkNodeValidity(child, path.pathByAddingChild(child), result);
            }
        }

        // call update last, to let children a chance to update their cache first
        if (node instanceof Node mutableNode && node != getModelRoot()
            && isVisible == UsageViewTreeCellRenderer.RowLocation.INSIDE_VISIBLE_RECT) {
            result.add(mutableNode);
        }
    }

    void updateLater() {
        myUpdateAlarm.cancelAllRequests();
        myUpdateAlarm.addRequest(
            () -> {
                if (isDisposed()) {
                    return;
                }
                PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject);
                documentManager.cancelAndRunWhenAllCommitted("UpdateUsageView", this::updateImmediately);
            },
            300
        );
    }

    @Override
    public void close() {
        cancelCurrentSearch();
        if (myContent != null) {
            UsageViewContentManager.getInstance(myProject).closeContent(myContent);
        }
    }

    @RequiredUIAccess
    private void saveSplitterProportions() {
        UIAccess.assertIsUIThread();
        getUsageViewSettings().setPreviewUsagesSplitterProportion(myPreviewSplitter.getProportion());
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        UIAccess.assertIsUIThread();
        disposeUsageContextPanels();
        synchronized (lock) {
            isDisposed = true;
            cancelCurrentSearch();
            myRerunAction = null;
            if (myTree != null) {
                ToolTipManager.sharedInstance().unregisterComponent(myTree);
            }
            myUpdateAlarm.cancelAllRequests();
        }
        if (myDisposeSmartPointersOnClose) {
            disposeSmartPointers();
        }
    }

    private void disposeSmartPointers() {
        List<SmartPsiElementPointer<?>> smartPointers = new ArrayList<>();
        for (Usage usage : myUsageNodes.keySet()) {
            if (usage instanceof UsageInfo2UsageAdapter usageAdapter) {
                SmartPsiElementPointer<?> pointer = usageAdapter.getUsageInfo().getSmartPointer();
                smartPointers.add(pointer);
            }
        }

        if (!smartPointers.isEmpty()) {
            for (SmartPsiElementPointer<?> pointer : smartPointers) {
                SmartPointerManager.getInstance(pointer.getProject()).removePointer(pointer);
            }
        }
        myUsageNodes.clear();
    }

    @Override
    public boolean isSearchInProgress() {
        return mySearchInProgress;
    }

    @Override
    public void setSearchInProgress(boolean searchInProgress) {
        mySearchInProgress = searchInProgress;
        if (!myPresentation.isDetachedMode()) {
            UIUtil.invokeLaterIfNeeded(() -> {
                if (isDisposed()) {
                    return;
                }
                UsageNode firstUsageNode = myModel.getFirstUsageNode();
                if (firstUsageNode == null) {
                    return;
                }

                Node node = getSelectedNode();
                if (node != null && !Comparing.equal(new TreePath(node.getPath()), TreeUtil.getFirstNodePath(myTree))) {
                    // user has selected node already
                    return;
                }
                showNode(firstUsageNode);
                if (getUsageViewSettings().isExpanded() && myUsageNodes.size() < 10000) {
                    expandAll();
                }
            });
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    @RequiredUIAccess
    private void showNode(@Nonnull UsageNode node) {
        UIAccess.assertIsUIThread();
        if (!isDisposed() && !myPresentation.isDetachedMode()) {
            fireEvents();
            TreePath usagePath = new TreePath(node.getPath());
            myTree.expandPath(usagePath.getParentPath());
            TreeUtil.selectPath(myTree, usagePath);
        }
    }

    @Override
    public void setRerunAction(@Nonnull Action rerunAction) {
        myRerunAction = rerunAction;
    }

    @Override
    @RequiredUIAccess
    public void addButtonToLowerPane(@Nonnull AnAction action) {
        UIAccess.assertIsUIThread();

        myButtonPanel.addButtonAction(action);
    }

    @Override
    public void setAdditionalComponent(@Nullable JComponent comp) {
        BorderLayout layout = (BorderLayout)myAdditionalComponent.getLayout();
        Component prev = layout.getLayoutComponent(myAdditionalComponent, BorderLayout.CENTER);
        if (prev == comp) {
            return;
        }
        if (prev != null) {
            myAdditionalComponent.remove(prev);
        }
        if (comp != null) {
            myAdditionalComponent.add(comp, BorderLayout.CENTER);
        }
        myAdditionalComponent.revalidate();
    }

    @Override
    @RequiredUIAccess
    public void addPerformOperationAction(
        @Nonnull Runnable processRunnable,
        @Nonnull String commandName,
        String cannotMakeString,
        @Nonnull String shortDescription
    ) {
        addPerformOperationAction(processRunnable, commandName, cannotMakeString, shortDescription, true);
    }

    @Override
    @RequiredUIAccess
    public void addPerformOperationAction(
        @Nonnull Runnable processRunnable,
        @Nonnull String commandName,
        String cannotMakeString,
        @Nonnull String shortDescription,
        boolean checkReadOnlyStatus
    ) {
        Runnable runnable =
            new MyPerformOperationRunnable(processRunnable, LocalizeValue.of(commandName), cannotMakeString, checkReadOnlyStatus);
        addButtonToLowerPane(runnable, shortDescription);
    }

    @RequiredReadAction
    private boolean allTargetsAreValid() {
        for (UsageTarget target : myTargets) {
            if (!target.isValid()) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    @Override
    public UsageViewPresentation getPresentation() {
        return myPresentation;
    }

    @RequiredReadAction
    public boolean canPerformReRun() {
        if (myRerunAction != null && myRerunAction.isEnabled()) {
            return allTargetsAreValid();
        }
        try {
            return myUsageSearcherFactory != null && allTargetsAreValid() && myUsageSearcherFactory.get() != null;
        }
        catch (PsiInvalidElementAccessException e) {
            return false;
        }
    }

    private boolean checkReadonlyUsages() {
        Set<VirtualFile> readOnlyUsages = getReadOnlyUsagesFiles();

        return readOnlyUsages.isEmpty()
            || !ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(readOnlyUsages).hasReadonlyFiles();
    }

    @Nonnull
    private Set<Usage> getReadOnlyUsages() {
        Set<Usage> result = new HashSet<>();
        Set<Map.Entry<Usage, UsageNode>> usages = myUsageNodes.entrySet();
        for (Map.Entry<Usage, UsageNode> entry : usages) {
            Usage usage = entry.getKey();
            UsageNode node = entry.getValue();
            if (node != null && node != NULL_NODE && !node.isExcluded() && usage.isReadOnly()) {
                result.add(usage);
            }
        }
        return result;
    }

    @Nonnull
    private Set<VirtualFile> getReadOnlyUsagesFiles() {
        Set<Usage> usages = getReadOnlyUsages();
        Set<VirtualFile> result = new HashSet<>();
        for (Usage usage : usages) {
            if (usage instanceof UsageInFile usageInFile) {
                VirtualFile file = usageInFile.getFile();
                if (file != null && file.isValid()) {
                    result.add(file);
                }
            }

            if (usage instanceof UsageInFiles usageInFiles) {
                ContainerUtil.addAll(result, usageInFiles.getFiles());
            }
        }
        for (UsageTarget target : myTargets) {
            VirtualFile[] files = target.getFiles();
            if (files == null) {
                continue;
            }
            ContainerUtil.addAll(result, files);
        }
        return result;
    }

    @Override
    @Nonnull
    public Set<Usage> getExcludedUsages() {
        Set<Usage> result = new HashSet<>();
        for (Map.Entry<Usage, UsageNode> entry : myUsageNodes.entrySet()) {
            UsageNode node = entry.getValue();
            Usage usage = entry.getKey();
            if (node == NULL_NODE || node == null) {
                continue;
            }
            if (node.isExcluded()) {
                result.add(usage);
            }
        }

        return result;
    }


    @Nullable
    @RequiredUIAccess
    private Node getSelectedNode() {
        UIAccess.assertIsUIThread();
        TreePath leadSelectionPath = myTree.getLeadSelectionPath();
        if (leadSelectionPath == null) {
            return null;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)leadSelectionPath.getLastPathComponent();
        return node instanceof Node mutableNode ? mutableNode : null;
    }

    @Nullable
    @RequiredUIAccess
    private Node[] getSelectedNodes() {
        UIAccess.assertIsUIThread();
        TreePath[] leadSelectionPath = myTree.getSelectionPaths();
        if (leadSelectionPath == null || leadSelectionPath.length == 0) {
            return null;
        }

        List<Node> result = new ArrayList<>();
        for (TreePath comp : leadSelectionPath) {
            Object lastPathComponent = comp.getLastPathComponent();
            if (lastPathComponent instanceof Node node) {
                result.add(node);
            }
        }
        return result.isEmpty() ? null : result.toArray(new Node[0]);
    }

    @Override
    @Nonnull
    @RequiredUIAccess
    public Set<Usage> getSelectedUsages() {
        UIAccess.assertIsUIThread();
        TreePath[] selectionPaths = myTree.getSelectionPaths();
        if (selectionPaths == null) {
            return Collections.emptySet();
        }

        Set<Usage> usages = new HashSet<>();
        for (TreePath selectionPath : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
            collectUsages(node, usages);
        }

        return usages;
    }

    @Override
    @Nonnull
    public Set<Usage> getUsages() {
        return myUsageNodes.keySet();
    }

    @Override
    @Nonnull
    public List<Usage> getSortedUsages() {
        List<Usage> usages = new ArrayList<>(getUsages());
        Collections.sort(usages, USAGE_COMPARATOR);
        return usages;
    }

    private static void collectUsages(@Nonnull DefaultMutableTreeNode node, @Nonnull Set<? super Usage> usages) {
        if (node instanceof UsageNode usageNode) {
            Usage usage = usageNode.getUsage();
            usages.add(usage);
        }

        Enumeration enumeration = node.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)enumeration.nextElement();
            collectUsages(child, usages);
        }
    }

    private static void collectAllChildNodes(@Nonnull DefaultMutableTreeNode node, @Nonnull Set<? super Node> nodes) {
        if (node instanceof Node mutableNode) {
            nodes.add(mutableNode);
        }

        Enumeration enumeration = node.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)enumeration.nextElement();
            collectAllChildNodes(child, nodes);
        }
    }

    @Nullable
    @RequiredUIAccess
    private UsageTarget[] getSelectedUsageTargets() {
        UIAccess.assertIsUIThread();
        TreePath[] selectionPaths = myTree.getSelectionPaths();
        if (selectionPaths == null) {
            return null;
        }

        Set<UsageTarget> targets = new HashSet<>();
        for (TreePath selectionPath : selectionPaths) {
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if (lastPathComponent instanceof UsageTargetNode usageTargetNode) {
                UsageTarget target = usageTargetNode.getTarget();
                if (target.isValid()) {
                    targets.add(target);
                }
            }
        }

        return targets.isEmpty() ? null : targets.toArray(UsageTarget.EMPTY_ARRAY);
    }

    @Nullable
    private static Navigatable getNavigatableForNode(@Nonnull DefaultMutableTreeNode node, boolean allowRequestFocus) {
        Object userObject = node.getUserObject();
        if (userObject instanceof Navigatable navigatable) {
            return navigatable.canNavigate() ? new Navigatable() {
                @Override
                public void navigate(boolean requestFocus) {
                    navigatable.navigate(allowRequestFocus && requestFocus);
                }

                @Override
                public boolean canNavigate() {
                    return navigatable.canNavigate();
                }

                @Override
                public boolean canNavigateToSource() {
                    return navigatable.canNavigateToSource();
                }
            } : null;
        }
        return null;
    }

    /* nodes with non-valid data are not included */
    private static Navigatable[] getNavigatablesForNodes(Node[] nodes) {
        if (nodes == null) {
            return null;
        }
        List<Navigatable> result = new ArrayList<>();
        for (Node node : nodes) {
            /*
            if (!node.isDataValid()) {
                continue;
            }
            */
            Object userObject = node.getUserObject();
            if (userObject instanceof Navigatable navigatable) {
                result.add(navigatable);
            }
        }
        return result.toArray(new Navigatable[0]);
    }

    boolean areTargetsValid() {
        return myModel.areTargetsValid();
    }

    private class MyPanel extends JPanel implements TypeSafeDataProvider, OccurenceNavigator, Disposable {
        @Nullable
        private OccurenceNavigatorSupport mySupport;
        private final CopyProvider myCopyProvider;

        private MyPanel(@Nonnull JTree tree) {
            mySupport = new OccurenceNavigatorSupport(tree) {
                @Override
                protected Navigatable createDescriptorForNode(@Nonnull DefaultMutableTreeNode node) {
                    if (node.getChildCount() > 0) {
                        return null;
                    }
                    if (node instanceof Node mutableNode && mutableNode.isExcluded()) {
                        return null;
                    }
                    return getNavigatableForNode(node, !myPresentation.isReplaceMode());
                }

                @Nonnull
                @Override
                public String getNextOccurenceActionName() {
                    return UsageLocalize.actionNextOccurrence().get();
                }

                @Nonnull
                @Override
                public String getPreviousOccurenceActionName() {
                    return UsageLocalize.actionPreviousOccurrence().get();
                }
            };
            myCopyProvider = new TextCopyProvider() {
                @Nullable
                @Override
                @RequiredUIAccess
                public Collection<String> getTextLinesToCopy() {
                    Node[] selectedNodes = getSelectedNodes();
                    if (selectedNodes != null && selectedNodes.length > 0) {
                        ArrayList<String> lines = new ArrayList<>();
                        for (Node node : selectedNodes) {
                            lines.add(node.getText(UsageViewImpl.this));
                        }
                        return lines;
                    }
                    return null;
                }
            };
        }

        // [tav] todo: a temp workaround for IDEA-192713
        @Override
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);
            if (e.getID() == FocusEvent.FOCUS_GAINED) {
                transferFocus();
            }
        }

        @Override
        public void dispose() {
            mySupport = null;
        }

        @Override
        public boolean hasNextOccurence() {
            return mySupport != null && mySupport.hasNextOccurence();
        }

        @Override
        public boolean hasPreviousOccurence() {
            return mySupport != null && mySupport.hasPreviousOccurence();
        }

        @Override
        public OccurenceInfo goNextOccurence() {
            return mySupport != null ? mySupport.goNextOccurence() : null;
        }

        @Override
        public OccurenceInfo goPreviousOccurence() {
            return mySupport != null ? mySupport.goPreviousOccurence() : null;
        }

        @Nonnull
        @Override
        public String getNextOccurenceActionName() {
            return mySupport != null ? mySupport.getNextOccurenceActionName() : "";
        }

        @Nonnull
        @Override
        public String getPreviousOccurenceActionName() {
            return mySupport != null ? mySupport.getPreviousOccurenceActionName() : "";
        }

        @Override
        @RequiredUIAccess
        public void calcData(@Nonnull Key key, @Nonnull DataSink sink) {
            if (key == Project.KEY) {
                sink.put(Project.KEY, myProject);
            }
            else if (key == USAGE_VIEW_KEY) {
                sink.put(USAGE_VIEW_KEY, UsageViewImpl.this);
            }
            else if (key == ExclusionHandler.EXCLUSION_HANDLER) {
                sink.put(ExclusionHandler.EXCLUSION_HANDLER, myExclusionHandler);
            }
            else if (key == Navigatable.KEY_OF_ARRAY) {
                Node[] nodes = Application.get().isDispatchThread() ? getSelectedNodes() : null;
                sink.put(Navigatable.KEY_OF_ARRAY, getNavigatablesForNodes(nodes));
            }
            else if (key == PlatformDataKeys.EXPORTER_TO_TEXT_FILE) {
                sink.put(PlatformDataKeys.EXPORTER_TO_TEXT_FILE, myTextFileExporter);
            }
            else if (key == USAGES_KEY) {
                Set<Usage> selectedUsages = Application.get().isDispatchThread() ? getSelectedUsages() : null;
                sink.put(USAGES_KEY, selectedUsages == null ? null : selectedUsages.toArray(Usage.EMPTY_ARRAY));
            }
            else if (key == USAGE_TARGETS_KEY) {
                UsageTarget[] targets = Application.get().isDispatchThread() ? getSelectedUsageTargets() : null;
                sink.put(USAGE_TARGETS_KEY, targets);
            }
            else if (key == VirtualFile.KEY_OF_ARRAY) {
                Set<Usage> usages = Application.get().isDispatchThread() ? getSelectedUsages() : null;
                Usage[] ua = usages == null ? null : usages.toArray(Usage.EMPTY_ARRAY);
                UsageTarget[] usageTargets = Application.get().isDispatchThread() ? getSelectedUsageTargets() : null;
                VirtualFile[] data = UsageDataUtil.provideVirtualFileArray(ua, usageTargets);
                sink.put(VirtualFile.KEY_OF_ARRAY, data);
            }
            else if (key == HelpManager.HELP_ID) {
                sink.put(HelpManager.HELP_ID, HELP_ID);
            }
            else if (key == CopyProvider.KEY) {
                sink.put(CopyProvider.KEY, myCopyProvider);
            }
            else if (key == PsiElement.KEY_OF_ARRAY) {
                if (Application.get().isDispatchThread()) {
                    sink.put(
                        PsiElement.KEY_OF_ARRAY,
                        getSelectedUsages().stream()
                            .filter(u -> u instanceof PsiElementUsage)
                            .map(u -> ((PsiElementUsage)u).getElement())
                            .filter(Objects::nonNull)
                            .toArray(PsiElement.ARRAY_FACTORY::create)
                    );
                }
            }
            else {
                // can arrive here outside EDT from usage view preview.
                // ignore all these fancy actions in this case.
                Node node = Application.get().isDispatchThread() ? getSelectedNode() : null;
                if (node != null) {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof TypeSafeDataProvider typeSafeDataProvider) {
                        typeSafeDataProvider.calcData(key, sink);
                    }
                    else if (userObject instanceof DataProvider dataProvider) {
                        Object data = dataProvider.getData(key);
                        if (data != null) {
                            sink.put(key, data);
                        }
                    }
                }
            }
        }
    }

    private final class ButtonPanel extends JPanel {
        private ActionToolbar myActionToolbar;
        private DefaultActionGroup myActionGroup;

        private ButtonPanel(Project project) {
            super(new FlowLayout(FlowLayout.LEFT, 6, 0));

            project.getMessageBus().connect(UsageViewImpl.this).subscribe(DumbModeListener.class, new DumbModeListener() {
                @Override
                public void enteredDumbMode() {
                    project.getUIAccess().give(() -> update());
                }

                @Override
                public void exitDumbMode() {
                    project.getUIAccess().give(() -> update());
                }
            });
        }

        @RequiredUIAccess
        private void addButtonAction(AnAction action) {
            if (myActionToolbar == null) {
                myActionGroup = new DefaultActionGroup();
                myActionToolbar =
                    ActionToolbarFactory.getInstance().createActionToolbar("UsageViewToolbar", myActionGroup, ActionToolbar.Style.BUTTON);
                myActionToolbar.setTargetComponent(this);

                add(myActionToolbar.getComponent());
            }

            myActionGroup.add(action);

            myActionToolbar.updateActionsAsync();
        }

        @RequiredUIAccess
        void update() {
            if (myActionToolbar != null) {
                myActionToolbar.updateActionsAsync();
            }
        }
    }

    private class UsageState {
        private final Usage myUsage;
        private final boolean mySelected;

        private UsageState(@Nonnull Usage usage, boolean isSelected) {
            myUsage = usage;
            mySelected = isSelected;
        }

        @RequiredUIAccess
        private void restore() {
            UIAccess.assertIsUIThread();
            UsageNode node = myUsageNodes.get(myUsage);
            if (node == NULL_NODE || node == null) {
                return;
            }
            DefaultMutableTreeNode parentGroupingNode = (DefaultMutableTreeNode)node.getParent();
            if (parentGroupingNode != null) {
                TreePath treePath = new TreePath(parentGroupingNode.getPath());
                myTree.expandPath(treePath);
                if (mySelected) {
                    myTree.addSelectionPath(treePath.pathByAddingChild(node));
                }
            }
        }
    }

    private class MyPerformOperationRunnable implements Runnable {
        private final String myCannotMakeString;
        private final Runnable myProcessRunnable;
        @Nonnull
        private final LocalizeValue myCommandName;
        private final boolean myCheckReadOnlyStatus;

        private MyPerformOperationRunnable(
            @Nonnull Runnable processRunnable,
            @Nonnull LocalizeValue commandName,
            String cannotMakeString,
            boolean checkReadOnlyStatus
        ) {
            myCannotMakeString = cannotMakeString;
            myProcessRunnable = processRunnable;
            myCommandName = commandName;
            myCheckReadOnlyStatus = checkReadOnlyStatus;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            if (myCheckReadOnlyStatus && !checkReadonlyUsages()) {
                return;
            }
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            if (myCannotMakeString != null && myChangesDetected) {
                LocalizeValue title = UsageLocalize.changesDetectedErrorTitle();
                if (canPerformReRun()) {
                    String message = myCannotMakeString + "\n\n" + UsageLocalize.dialogRerunSearch();
                    int answer = Messages.showOkCancelDialog(
                        myProject,
                        message,
                        title.get(),
                        UsageLocalize.actionDescriptionRerun().get(),
                        UsageLocalize.usageViewCancelButton().get(),
                        UIUtil.getErrorIcon()
                    );
                    if (answer == Messages.OK) {
                        refreshUsages();
                    }
                }
                else {
                    Messages.showMessageDialog(myProject, myCannotMakeString, title.get(), UIUtil.getErrorIcon());
                    //todo[myakovlev] request focus to tree
                    //myUsageView.getTree().requestFocus();
                }
                return;
            }

            // can't dispose pointers because refactoring might want to re-use the usage infos from the preview
            myDisposeSmartPointersOnClose = false;
            close();

            try {
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .name(myCommandName)
                    .run(myProcessRunnable);
            }
            finally {
                disposeSmartPointers();
            }
        }
    }

    @RequiredUIAccess
    private List<UsageInfo> getSelectedUsageInfos() {
        UIAccess.assertIsUIThread();
        return DataManager.getInstance().getDataContext(myRootPanel).getData(USAGE_INFO_LIST_KEY);
    }

    public GroupNode getRoot() {
        return myRoot;
    }

    @TestOnly
    public String getNodeText(@Nonnull TreeNode node) {
        return myUsageViewTreeCellRenderer.getPlainTextForNode(node);
    }

    public boolean isVisible(@Nonnull Usage usage) {
        return myBuilder != null && myBuilder.isVisible(usage);
    }

    @Nonnull
    public UsageTarget[] getTargets() {
        return myTargets;
    }

    /**
     * The element the "find usages" action was invoked on.
     * E.g. if the "find usages" was invoked on the reference "getName(2)" pointing to the method "getName()" then the origin usage is this reference.
     */
    public void setOriginUsage(@Nonnull Usage usage) {
        myOriginUsage = usage;
    }

    /**
     * true if the {@param usage} points to the element the "find usages" action was invoked on
     */
    public boolean isOriginUsage(@Nonnull Usage usage) {
        return myOriginUsage instanceof UsageInfo2UsageAdapter usageAdapter1 && usage instanceof UsageInfo2UsageAdapter usageAdapter2
            && usageAdapter1.getUsageInfo().equals(usageAdapter2.getUsageInfo());
    }

    private boolean isFilterDuplicateLines() {
        return myPresentation.isMergeDupLinesAvailable() && getUsageViewSettings().isFilterDuplicatedLine();
    }

    @RequiredUIAccess
    public Usage getNextToSelect(@Nonnull Usage toDelete) {
        UIAccess.assertIsUIThread();
        UsageNode usageNode = myUsageNodes.get(toDelete);
        if (usageNode == null) {
            return null;
        }

        DefaultMutableTreeNode node = myRootPanel.mySupport.findNextNodeAfter(myTree, usageNode, true);
        if (node == null) {
            node = myRootPanel.mySupport.findNextNodeAfter(myTree, usageNode, false); // last node
        }

        return node != null && node.getUserObject() instanceof Usage usage ? usage : null;
    }

    @RequiredUIAccess
    public Usage getNextToSelect(@Nonnull Collection<? extends Usage> toDelete) {
        UIAccess.assertIsUIThread();
        Usage toSelect = null;
        for (Usage usage : toDelete) {
            Usage next = getNextToSelect(usage);
            if (!toDelete.contains(next)) {
                toSelect = next;
                break;
            }
        }
        return toSelect;
    }

    private interface ExclusionHandlerEx<Node> extends ExclusionHandler<Node> {
        void excludeNodeSilently(@Nonnull Node node);
    }
}
