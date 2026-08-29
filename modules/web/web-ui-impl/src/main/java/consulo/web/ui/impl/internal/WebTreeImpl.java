/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.ui.impl.internal;

import consulo.ui.Length;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.TransferHandler;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.selection.SelectionModel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.Component;
import com.vaadin.flow.dom.Style;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeStyle;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.color.ColorValue;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.collection.ContainerUtil;
import consulo.ui.Point2D;
import consulo.ui.PopupOwner;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
@SuppressWarnings("unchecked")
public class WebTreeImpl<NODE> extends VaadinComponentDelegate<WebTreeImpl.Vaadin> implements Tree<NODE>, PopupOwner {
    private static final Logger LOG = Logger.getInstance(WebTreeImpl.class);

    private @Nullable TransferHandler<TreeNode<NODE>> myTransferHandler;
    private @Nullable Function<TreeNode<NODE>, Length> myItemHeightGetter;
    private @Nullable Function<TreeNode<NODE>, String> mySpeedSearchConverter;

    /** where the row of the last right click ended up, which is what a popup raised over the tree hangs off */
    private volatile @Nullable Point2D myPopupPosition;

    /**
     * Levels of rows an expand all opens, counting the top level ones.
     */
    private static final int EXPAND_ALL_DEPTH = 4;

    @Tag("vaadin-grid-tree-toggle")
    public static class VaadinGridTreeToggle extends com.vaadin.flow.component.Component
        implements HasComponents, ClickNotifier<VaadinGridTreeToggle> {
    }

    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips
    // rebuilding on css only changes, and the tree look was left one build behind
    @StyleSheet("/tree/webTree.css")
    public class Vaadin extends TreeGrid<WebTreeNodeImpl<NODE>> implements FromVaadinComponentWrapper {
        // written by the background fetches and read by the ui thread, and now also walked when the state of
        // the tree is written out
        private final Map<String, WebTreeNodeImpl<NODE>> myNodeMap = new ConcurrentHashMap<>();
        private final CompletableFuture<Void> myRootLoaded = new CompletableFuture<>();

        private TreeExecutor myExecutor;

        private WebTreeNodeImpl<NODE> myRootNode;
        private TreeModel<NODE> myModel;

        private List<TreeNode<NODE>> myDraggedItems = List.of();
        private DataTransfer myDragTransfer = DataTransfer.EMPTY;
        private boolean myDragAndDropBound;

        /**
         * The toolkit tells the browser what may be dropped where, and only reports a drop that got
         * past it, so the check pass the handler is owed is run here right before the drop itself.
         */
        private void bindDragAndDrop(DragAndDropTransferHandler<TreeNode<NODE>> handler) {
            if (myDragAndDropBound) {
                return;
            }
            myDragAndDropBound = true;

            addDragStartListener(event -> {
                myDraggedItems = new ArrayList<>(event.getDraggedItems());
                DataTransfer transfer = handler.createDragTransfer(WebTreeImpl.this, myDraggedItems, true);
                myDragTransfer = transfer == null ? DataTransfer.EMPTY : transfer;
            });

            addDragEndListener(event -> {
                myDraggedItems = List.of();
                myDragTransfer = DataTransfer.EMPTY;
            });

            addDropListener(event -> {
                WebTreeNodeImpl<NODE> target = event.getDropTargetItem().orElse(null);
                DragAndDropTransferHandler.DropPosition position = positionOf(event.getDropLocation());
                if (target == null || position == null) {
                    return;
                }

                DropContextImpl context = new DropContextImpl(target, position, myDragTransfer, myDraggedItems, true);
                if (!handler.drop(WebTreeImpl.this, context)) {
                    return;
                }

                handler.drop(WebTreeImpl.this, context.toPerforming());
            });
        }

        public Vaadin() {
            setAllRowsVisible(true);

            addThemeVariants(GridVariant.NO_ROW_BORDERS, GridVariant.COLUMN_BORDERS, GridVariant.NO_BORDER);

            setSelectionMode(SelectionMode.SINGLE);

            ((SelectionModel.Single) getSelectionModel()).setDeselectAllowed(false);

            addComponentColumn(node -> {
                // nothing is asked of the model here - the presentation was computed on the executor while the
                // level was built, and this only turns it into the components of the row
                WebItemPresentationImpl item = node.getPresentation();
                if (item == null) {
                    item = new WebItemPresentationImpl();
                    if (node instanceof WebTreeNodeImpl.NotLoaded) {
                        item.append(UILocalize.treenodeLoading());
                    }
                }

                VaadinGridTreeToggle toggle = new VaadinGridTreeToggle();
                toggle.getElement().setAttribute("leaf", node.isLeaf());
                toggle.getElement().setAttribute("level", String.valueOf(node.getLevel()));
                if (isExpanded(node)) {
                    toggle.getElement().setAttribute("expanded", true);
                }

                // a click anywhere on the toggle selects - the label is inside it - while opening a node is
                // the chevron's alone. the filter keeps the label clicks off the wire entirely, and the
                // client half of this is treeToggle.js, which stops the element flipping itself for them
                toggle.addClickListener(event -> select(node));

                toggle.getElement().addEventListener("click", event -> {
                    if (getDataCommunicator().hasChildren(node)) {
                        if (isExpanded(node)) {
                            collapse(List.of(node), true);
                        }
                        else {
                            expand(List.of(node), true);
                        }
                    }
                }).setFilter("event.composedPath().some(node => node.getAttribute && node.getAttribute('part') === 'toggle')");

                // the background of an item means the whole row, the way the awt tree paints a file colour.
                // the value is only handed to the stylesheet - webTree.css decides what the row draws with it
                ColorValue background = item.getBackgroundColor();
                if (background != null) {
                    toggle.getElement().getStyle().set("--consulo-tree-row-background", WebColors.toCssColor(background));
                }

                applyItemHeight(node);

                toggle.add(item.toComponent());
                return toggle;
            }).setAutoWidth(true).setFlexGrow(1);

            installSelectOnRightClick();
        }

        /**
         * The awt trees move the selection to the row under the pointer before showing their popup - see
         * {@code PopupHandler#installFollowingSelectionTreePopup} - and the popup is filled from the selection.
         * The vaadin grid only selects on the left button, so a right click would answer for whatever row was
         * selected before.
         */
        private void installSelectOnRightClick() {
            // the rows live in the shadow dom of the grid while the cell contents are slotted light dom children,
            // so closest() cannot reach the row - the composed path is the only way across the boundary
            String rowIndex = "(event.composedPath().find(node => node.localName === 'tr') || {}).index";

            // where the row ended up is only measurable in the browser, and the same click which moves the
            // selection is the one a popup is raised from - so it is reported here rather than asked for later
            String rowLeft = rowMetric("Math.round(row.left - grid.left)");
            String rowBottom = rowMetric("Math.round(row.bottom - grid.top)");

            getElement()
                .addEventListener("mousedown", event -> {
                    int index = event.getEventData().path(rowIndex).asInt(-1);

                    WebTreeNodeImpl<NODE> item =
                        HierarchicalDataCommunicatorAccess.getItemByFlatIndex(getDataCommunicator(), index);
                    if (item != null) {
                        select(item);
                    }

                    int left = event.getEventData().path(rowLeft).asInt(-1);
                    int bottom = event.getEventData().path(rowBottom).asInt(-1);
                    // mirrors the awt trees, which anchor at the bottom left of the selected row
                    myPopupPosition = left < 0 || bottom < 0 ? null : new Point2D(left + 2, bottom - 1);
                })
                // header rows carry no index, and only the right button has to move the selection - the left one
                // is the grid's own business
                .addEventData(rowIndex)
                .addEventData(rowLeft)
                .addEventData(rowBottom)
                .setFilter("event.button === 2");
        }

        private static String rowMetric(String expression) {
            return "(() => {"
                + "const tr = event.composedPath().find(node => node.localName === 'tr');"
                + "if (!tr) { return -1; }"
                + "const row = tr.getBoundingClientRect();"
                + "const grid = element.getBoundingClientRect();"
                + "return " + expression + ";"
                + "})()";
        }

        public void init(NODE rootValue, TreeModel<NODE> model) {
            myModel = model;

            myRootNode = new WebTreeNodeImpl<>(null, rootValue, myNodeMap);
            // every node built below gets one from fetchChildren, and without it here the root answers its own
            // placeholder child to a search instead of building the level - which is where a path walk starts
            myRootNode.setLoader(this::loadChildrenAsync);

            initTreeData(true);

            addExpandListener(event -> {
                Collection<WebTreeNodeImpl<NODE>> items = event.getItems();

                for (WebTreeNodeImpl<NODE> item : items) {
                    item.setExpanded(true);

                    if (item.isNotLoaded()) {
                        UI ui = UI.getCurrent();
                        // items not loaded
                        queue(item, ui);
                    }

                    fireExpand(item);
                }
            });

            addCollapseListener(event -> {
                for (WebTreeNodeImpl<NODE> item : event.getItems()) {
                    item.setExpanded(false);

                    fireCollapse(item);
                }
            });
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);

            // a refresh moves this grid into a freshly created ui and attaches it again. the nodes are the same
            // objects, and building them anew would throw away what they hold - the children below them and the
            // record of which were open - so only the data of the new grid is filled in
            if (!myRootNode.isNotLoaded()) {
                initTreeData(false);

                myRootLoaded.complete(null);
                return;
            }

            UI ui = UI.getCurrent();

            myExecutor.execute(WebTreeImpl.this, () -> fetchChildren(myRootNode, false))
                .whenComplete((children, error) -> {
                    if (error != null) {
                        logBuildError(error);
                        // everything a walk down a stored path chains on hangs off this one
                        myRootLoaded.complete(null);
                        return;
                    }

                    ui.access(() -> {
                        initTreeData(false);

                        myRootLoaded.complete(null);
                    });
                });
        }

        private void queue(WebTreeNodeImpl<NODE> parent, UI ui) {
            loadChildren(parent, ui);
        }

        private CompletableFuture<List<WebTreeNodeImpl<NODE>>> loadChildren(WebTreeNodeImpl<NODE> parent, @Nullable UI ui) {
            if (ui == null) {
                return CompletableFuture.completedFuture(List.of());
            }

            if (!parent.isNotLoaded()) {
                // the whole chain that a walk down a path hangs on this future runs where it is completed,
                // and touching the grid outside the ui lock is what corrupts a session
                CompletableFuture<List<WebTreeNodeImpl<NODE>>> loaded = new CompletableFuture<>();
                List<WebTreeNodeImpl<NODE>> children = parent.getChildren();
                ui.access(() -> loaded.complete(children));
                return loaded;
            }

            WebTreeNodeImpl<NODE> unloaded = parent.getChildren().get(0);

            return myExecutor.execute(WebTreeImpl.this, () -> fetchChildren(parent, false))
                .handle((children, error) -> {
                    CompletableFuture<List<WebTreeNodeImpl<NODE>>> result = new CompletableFuture<>();

                    if (error != null) {
                        logBuildError(error);
                        result.complete(List.of());
                        return result;
                    }

                    ui.access(() -> {
                        TreeData<WebTreeNodeImpl<NODE>> data = getTreeData();

                        data.removeItem(unloaded);

                        data.addItems(parent, children);

                        // add raw children
                        for (WebTreeNodeImpl<NODE> child : children) {
                            data.addItems(child, child.getChildren());
                        }

                        myNodeMap.remove(unloaded.getId());

                        getDataProvider().refreshItem(parent, true);

                        ui.push();

                        result.complete(children);
                    });

                    return result;
                })
                .thenCompose(Function.identity());
        }

        /**
         * The children of the root are fetched once the grid is attached, so a walk down a path - restoring the
         * state of a previous session - has to wait for that rather than start a fetch of its own.
         */
        /**
         * {@link UI#getCurrent()} answers only on a thread the framework is driving, and a walk which opens one
         * level after another runs its later steps on whichever thread completed the step before. The grid knows
         * the ui it hangs in either way.
         */
        private @Nullable UI currentUI() {
            UI ui = UI.getCurrent();
            return ui != null ? ui : getUI().orElse(null);
        }

        public CompletableFuture<List<WebTreeNodeImpl<NODE>>> loadChildrenAsync(WebTreeNodeImpl<NODE> node) {
            if (node == myRootNode) {
                return myRootLoaded.thenApply(ignored -> myRootNode.getChildren());
            }

            return loadChildren(node, currentUI());
        }

        public void selectDeep(WebTreeNodeImpl<NODE> node) {
            List<WebTreeNodeImpl<NODE>> path = pathTo(node);

            CompletableFuture<Void> expanded = CompletableFuture.completedFuture(null);
            for (int i = 0; i < path.size() - 1; i++) {
                WebTreeNodeImpl<NODE> ancestor = path.get(i);
                expanded = expanded.thenCompose(ignored -> expandNode(ancestor));
            }

            expanded.thenRun(() -> {
                UI ui = currentUI();
                if (ui != null) {
                    ui.access(() -> select(node));
                }
            });
        }

        public CompletableFuture<Void> expandNode(WebTreeNodeImpl<NODE> node) {
            return loadChildrenAsync(node).thenCompose(children -> {
                if (node == myRootNode) {
                    return CompletableFuture.completedFuture(null);
                }

                UI ui = currentUI();
                if (ui == null) {
                    return CompletableFuture.completedFuture(null);
                }

                CompletableFuture<Void> expanded = new CompletableFuture<>();

                ui.access(() -> {
                    // stored paths overlap - a parent is named by every path that runs through it - and asking
                    // the grid to open a node it already holds open resets the subtree, closing what a previous
                    // path opened below it
                    if (!isExpanded(node)) {
                        expand(List.of(node));

                        // the toggle of a row is built once by the component column, and an expand driven from
                        // here does not rebuild it - the row would keep the chevron of a closed node
                        getDataProvider().refreshItem(node);
                    }

                    expanded.complete(null);
                });

                return expanded;
            });
        }

        /**
         * Opens the nodes and the levels below them, one level at a time. {@link #expandRecursively} walks only
         * the levels the grid already holds, and the children of a node are fetched when it is first opened, so
         * a level has to be waited for before the one below it can be asked for.
         */
        public CompletableFuture<Void> expandDeep(Collection<WebTreeNodeImpl<NODE>> nodes, int depth) {
            if (depth <= 0 || nodes.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<?>[] futures = nodes.stream()
                // a leaf has nothing to open, and the placeholder row of a level still being fetched is not a
                // node of the model
                .filter(node -> !node.isLeaf() && !(node instanceof WebTreeNodeImpl.NotLoaded))
                // the children are read after the node is open - a fetch replaces the list it held before
                .map(node -> expandNode(node).thenCompose(ignored -> expandDeep(node.getChildren(), depth - 1)))
                .toArray(CompletableFuture[]::new);

            return CompletableFuture.allOf(futures);
        }

        /**
         * The root is not a row of the grid, so a depth of one opens the top level rows.
         */
        public CompletableFuture<Void> expandAllDeep(int depth) {
            return loadChildrenAsync(myRootNode).thenCompose(children -> expandDeep(children, depth));
        }

        public WebTreeNodeImpl<NODE> getRootNode() {
            return myRootNode;
        }

        public List<List<WebTreeNodeImpl<NODE>>> collectExpandedPaths() {
            List<List<WebTreeNodeImpl<NODE>>> paths = new ArrayList<>();
            for (WebTreeNodeImpl<NODE> node : myNodeMap.values()) {
                if (!(node instanceof WebTreeNodeImpl.NotLoaded) && node.isExpanded()) {
                    paths.add(pathTo(node));
                }
            }
            return paths;
        }

        /**
         * The root is a part of the path, the way the awt trees write theirs - the two frontends read the same
         * state out of the workspace.
         */
        public List<WebTreeNodeImpl<NODE>> pathTo(WebTreeNodeImpl<NODE> node) {
            LinkedList<WebTreeNodeImpl<NODE>> path = new LinkedList<>();
            for (WebTreeNodeImpl<NODE> current = node; current != null; current = current.getParent()) {
                path.addFirst(current);
            }
            return path;
        }

        /**
         * The children live in the {@link TreeData} rather than being asked from the model on every paint, so a
         * change behind the tree only reaches it by rebuilding them here.
         */
        public void refreshNode(WebTreeNodeImpl<NODE> node, boolean refreshChildren) {
            if (node == myRootNode) {
                refreshRoot();
                return;
            }

            TreeData<WebTreeNodeImpl<NODE>> data = getTreeData();
            if (!data.contains(node)) {
                return;
            }

            // a node whose children were never fetched has nothing to rebuild - it holds the placeholder, and
            // opening it fetches them
            if (!refreshChildren || node.isNotLoaded()) {
                getDataProvider().refreshItem(node);
                return;
            }

            UI ui = UI.getCurrent();
            myExecutor.execute(WebTreeImpl.this, () -> fetchChildren(node, false))
                .whenComplete((children, error) -> {
                    if (error != null) {
                        logBuildError(error);
                        return;
                    }

                    ui.access(() -> {
                        TreeData<WebTreeNodeImpl<NODE>> treeData = getTreeData();

                        for (WebTreeNodeImpl<NODE> old : List.copyOf(treeData.getChildren(node))) {
                            myNodeMap.remove(old.getId());
                            treeData.removeItem(old);
                        }

                        treeData.addItems(node, children);
                        for (WebTreeNodeImpl<NODE> child : children) {
                            treeData.addItems(child, child.getChildren());
                        }

                        getDataProvider().refreshItem(node, true);

                        ui.push();
                    });
                });
        }

        public CompletableFuture<?> refreshRoot() {
            UI ui = currentUI();
            if (ui == null) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<Void> result = new CompletableFuture<>();

            myExecutor.execute(WebTreeImpl.this, () -> fetchChildren(myRootNode, false))
                .whenComplete((children, error) -> {
                    if (error != null) {
                        logBuildError(error);
                        result.complete(null);
                        return;
                    }

                    ui.access(() -> {
                        initTreeData(false);

                        ui.push();

                        result.complete(null);
                    });
                });

            return result;
        }

        private void initTreeData(boolean init) {
            TreeData<WebTreeNodeImpl<NODE>> data = new TreeData<>();
            // will set not loaded node
            if (init) {
                data.addRootItems(List.of(new WebTreeNodeImpl.NotLoaded<>(null, null, myNodeMap)));
            }
            else {
                data.addRootItems(myRootNode.getChildren());
                for (WebTreeNodeImpl<NODE> node : myRootNode.getChildren()) {
                    addLoadedChildren(data, node);
                }
            }

            TreeDataProvider<WebTreeNodeImpl<NODE>> provider = new TreeDataProvider<>(data) {
                @Override
                public Object getId(WebTreeNodeImpl<NODE> item) {
                    return item.getId();
                }
            };

            setUniqueKeyDataGenerator("key", WebTreeNodeImpl::getId);

            setDataProvider(provider);
            getDataCommunicator().getKeyMapper().setIdentifierGetter(WebTreeNodeImpl::getId);

            if (!init) {
                restoreExpanded(data);
            }
        }

        /**
         * A node holds the children it was given, and the placeholder while it has none - putting the whole of
         * what is already known into the data is what lets the nodes that were open be opened again.
         */
        private void addLoadedChildren(TreeData<WebTreeNodeImpl<NODE>> data, WebTreeNodeImpl<NODE> node) {
            List<WebTreeNodeImpl<NODE>> children = node.getChildren();
            if (children.isEmpty()) {
                return;
            }

            data.addItems(node, children);

            if (node.isNotLoaded()) {
                return;
            }

            for (WebTreeNodeImpl<NODE> child : children) {
                addLoadedChildren(data, child);
            }
        }

        private void restoreExpanded(TreeData<WebTreeNodeImpl<NODE>> data) {
            List<WebTreeNodeImpl<NODE>> expanded = new ArrayList<>();
            for (WebTreeNodeImpl<NODE> node : myNodeMap.values()) {
                if (node.isExpanded() && data.contains(node)) {
                    expanded.add(node);
                }
            }

            if (!expanded.isEmpty()) {
                // a node can only be opened once the one above it is, and the nodes are held in no order
                expanded.sort(Comparator.comparingInt(WebTreeNodeImpl::getLevel));

                expand(expanded);
            }
        }

        /**
         * Runs on the executor of the tree. A {@code ProcessCanceledException} is deliberately let through: the
         * executor of a model over application data cancels its task when a write action arrives and restarts it
         * by itself, and swallowing the exception here would turn the restart into an empty level.
         */
        private List<WebTreeNodeImpl<NODE>> fetchChildren(WebTreeNodeImpl<NODE> parent, boolean fetchNext) {
            List<WebTreeNodeImpl<NODE>> list = new ArrayList<>();
            Map<String, WebTreeNodeImpl<NODE>> nodeMap = new HashMap<>();

            myModel.buildChildren(
                node -> {
                    WebTreeNodeImpl<NODE> child = new WebTreeNodeImpl<>(parent, node, nodeMap);
                    child.setLoader(this::loadChildrenAsync);
                    list.add(child);
                    return child;
                },
                parent.getValue()
            );

            myNodeMap.putAll(nodeMap);

            parent.setChildren(list);

            Comparator<TreeNode<NODE>> nodeComparator = myModel.getNodeComparator();
            if (nodeComparator != null) {
                list.sort(nodeComparator);
            }

            if (list.isEmpty()) {
                parent.setLeaf(true);
            }

            // the row is built on the ui thread and the model must not be touched there, so what it would have
            // asked for is computed here, while the level is being built
            for (WebTreeNodeImpl<NODE> child : list) {
                child.computePresentation();
            }

            if (fetchNext) {
                for (WebTreeNodeImpl<NODE> child : list) {
                    if (myModel.isNeedBuildChildrenBeforeOpen(child)) {
                        fetchChildren(child, false);
                    }
                }
            }

            return list;
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebTreeImpl.this;
        }
    }

    /**
     * A cancelled build - the tree left its ui, or the executor went down with its disposable - is the quiet end
     * of a chain nobody is waiting on. Anything else on this path would otherwise vanish without a trace, since
     * the future of a build is rarely looked at.
     */
    private static void logBuildError(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (!(cause instanceof CancellationException)) {
            LOG.error(cause);
        }
    }

    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }
    @RequiredUIAccess
    public WebTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model, TreeExecutor executor) {
        Vaadin vaadin = toVaadinComponent();
        vaadin.myExecutor = executor;

        vaadin.init(rootValue, model);
        vaadin.asSingleSelect().addValueChangeListener(event -> {
            WebTreeNodeImpl<NODE> value = event.getValue();
            if (value == null || value instanceof WebTreeNodeImpl.NotLoaded) {
                return;
            }

            getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent(this, value));
        });

        WebInputDetails.addClickListener(vaadin.getElement(), "dblclick", inputDetails -> {
            TreeNode<NODE> selectedNode = getSelectedNode();
            if (selectedNode == null) {
                return;
            }

            getListenerDispatcher(TreeDoubleClickEvent.class)
                .onEvent(new TreeDoubleClickEvent<>(this, selectedNode, inputDetails));

            if (model.onDoubleClick(this, selectedNode, inputDetails) && selectedNode instanceof WebTreeNodeImpl<NODE> node) {
                if (vaadin.isExpanded(node)) {
                    vaadin.collapse(List.of(node));
                }
                else {
                    vaadin.expand(List.of(node));
                }
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable TreeNode<NODE> getSelectedNode() {
        Set selectedItems = toVaadinComponent().getSelectedItems();
        return (TreeNode<NODE>) ContainerUtil.getFirstItem(selectedItems);
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<NODE> node, int depth) {
        if (!(node instanceof WebTreeNodeImpl<NODE> webNode)) {
            return CompletableFuture.completedFuture(null);
        }

        return toVaadinComponent().expandDeep(List.of(webNode), depth);
    }

    @Override
    public TreeNode<NODE> getRootNode() {
        return toVaadinComponent().getRootNode();
    }

    @Override
    public List<List<TreeNode<NODE>>> getExpandedPaths() {
        List<List<WebTreeNodeImpl<NODE>>> collected = toVaadinComponent().collectExpandedPaths();

        List<List<TreeNode<NODE>>> paths = new ArrayList<>();
        for (List<WebTreeNodeImpl<NODE>> path : collected) {
            paths.add(List.copyOf(path));
        }
        return paths;
    }

    @Override
    public List<TreeNode<NODE>> getSelectedPath() {
        TreeNode<NODE> selected = getSelectedNode();
        if (!(selected instanceof WebTreeNodeImpl<NODE> node)) {
            return List.of();
        }
        return List.copyOf(toVaadinComponent().pathTo(node));
    }

    @Override
    public void select(TreeNode<NODE> node) {
        if (node instanceof WebTreeNodeImpl<NODE> webNode) {
            toVaadinComponent().selectDeep(webNode);
        }
    }

    @Override
    public void refreshItem(TreeNode<NODE> node, boolean refreshChildren) {
        if (node instanceof WebTreeNodeImpl<NODE> webNode) {
            toVaadinComponent().refreshNode(webNode, refreshChildren);
        }
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return toVaadinComponent().refreshRoot();
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll() {
        // the children of a node are fetched when it is first opened, and every level costs a round trip, so
        // an unbounded expand all would open a project view down to every file - the depth is capped instead
        return expandAll(EXPAND_ALL_DEPTH);
    }

    @Override
    public CompletableFuture<?> expandAll(int depth) {
        return toVaadinComponent().expandAllDeep(depth);
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        Vaadin vaadin = toVaadinComponent();

        // only the levels the grid holds can be open in the first place
        vaadin.collapseRecursively(vaadin.getTreeData().getRootItems(), Integer.MAX_VALUE);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The placeholder row of a node whose children are still being fetched is a row of the grid but not a node
     * of the model, and is left out the way the selection listener leaves it out.
     */
    private void fireExpand(WebTreeNodeImpl<NODE> node) {
        if (node instanceof WebTreeNodeImpl.NotLoaded) {
            return;
        }

        getListenerDispatcher(TreeExpandEvent.class).onEvent(new TreeExpandEvent(this, node));
    }

    private void fireCollapse(WebTreeNodeImpl<NODE> node) {
        if (node instanceof WebTreeNodeImpl.NotLoaded) {
            return;
        }

        getListenerDispatcher(TreeCollapseEvent.class).onEvent(new TreeCollapseEvent(this, node));
    }

    /**
     * A drop on no row at all is refused rather than aimed at the root, so nothing lands somewhere
     * the user did not point at.
     */
    private static DragAndDropTransferHandler.@Nullable DropPosition positionOf(GridDropLocation location) {
        return switch (location) {
            case ON_TOP -> DragAndDropTransferHandler.DropPosition.INTO;
            case ABOVE -> DragAndDropTransferHandler.DropPosition.ABOVE;
            case BELOW -> DragAndDropTransferHandler.DropPosition.BELOW;
            case EMPTY -> null;
        };
    }

    private class DropContextImpl implements DragAndDropTransferHandler.DropContext<TreeNode<NODE>> {
        private final TreeNode<NODE> myTarget;
        private final DragAndDropTransferHandler.DropPosition myPosition;
        private final DataTransfer myTransfer;
        private final List<TreeNode<NODE>> myItems;
        private final boolean myCheckOnly;

        private DropContextImpl(TreeNode<NODE> target,
                                DragAndDropTransferHandler.DropPosition position,
                                DataTransfer transfer,
                                List<TreeNode<NODE>> items,
                                boolean checkOnly) {
            myTarget = target;
            myPosition = position;
            myTransfer = transfer;
            myItems = items;
            myCheckOnly = checkOnly;
        }

        private DropContextImpl toPerforming() {
            return new DropContextImpl(myTarget, myPosition, myTransfer, myItems, false);
        }

        @Override
        public TreeNode<NODE> getTarget() {
            return myTarget;
        }

        @Override
        public DragAndDropTransferHandler.DropPosition getPosition() {
            return myPosition;
        }

        @Override
        public boolean isCheckOnly() {
            return myCheckOnly;
        }

        @Override
        public DataTransfer getTransfer() {
            return myTransfer;
        }

        @Override
        public List<TreeNode<NODE>> getItems() {
            return myItems;
        }
    }

    /**
     * The grid lays every row out on one measurement of its own, so a height belongs to the whole body rather
     * than to a row - webTree.css reads it off this property, and the toggle is already stretched over it. A
     * height written on the toggle instead leaves it short of the row, which is where the selection is drawn.
     */
    void applyItemHeight(@Nullable TreeNode<NODE> node) {
        Function<TreeNode<NODE>, Length> getter = myItemHeightGetter;
        if (getter == null || node == null) {
            return;
        }

        getVaadinComponent().getStyle().set("--consulo-tree-row-height", WebLength.toCss(getter.apply(node)));
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<TreeNode<NODE>, Length> getter) {
        myItemHeightGetter = getter;

        if (getter == null) {
            getVaadinComponent().getStyle().remove("--consulo-tree-row-height");
        }

        getVaadinComponent().getDataProvider().refreshAll();
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<NODE>, String> converter) {
        mySpeedSearchConverter = converter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void addStyle(TreeStyle style) {
        Style vaadinStyle = getVaadinComponent().getStyle();

        if (style == TreeStyle.TRANSPARENT_BACKGROUND) {
            vaadinStyle.set("background", "transparent");
            vaadinStyle.set("--vaadin-grid-background", "transparent");
            return;
        }

        vaadinStyle.set("font-size", fontSize(style));
    }

    /**
     * The ladder jetbrains gives a label - h4/h3/h2 are one, three and five points above it, and medium and
     * small one and two below. The css keywords are absolute sizes and step far wider than that, so the size
     * is written against the one inherited, where {@code em} is the size of the parent.
     */
    private static String fontSize(TreeStyle style) {
        return switch (style) {
            case FONT_XX_SMALL -> "calc(1em - 3px)";
            case FONT_X_SMALL -> "calc(1em - 2px)";
            case FONT_SMALL -> "calc(1em - 1px)";
            case FONT_LARGE -> "calc(1em + 1px)";
            case FONT_X_LARGE -> "calc(1em + 3px)";
            case FONT_XX_LARGE -> "calc(1em + 5px)";
            default -> "1em";
        };
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<TreeNode<NODE>> handler) {
        myTransferHandler = handler;

        Vaadin vaadin = toVaadinComponent();
        if (!(handler instanceof DragAndDropTransferHandler<TreeNode<NODE>> dragAndDrop)) {
            vaadin.setRowsDraggable(false);
            vaadin.setDropMode(null);
            return;
        }

        vaadin.setRowsDraggable(true);
        vaadin.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        vaadin.bindDragAndDrop(dragAndDrop);
    }

    @Override
    public @Nullable TransferHandler<TreeNode<NODE>> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public @Nullable Point2D getBestPopupPosition() {
        return myPopupPosition;
    }
}
