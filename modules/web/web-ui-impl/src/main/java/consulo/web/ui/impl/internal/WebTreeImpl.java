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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.selection.SelectionModel;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.server.VaadinSession;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Length;
import consulo.ui.Point2D;
import consulo.ui.PopupOwner;
import consulo.ui.TextItemPresentation;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.color.ColorValue;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.impl.tree.TreeController;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeWidget;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebTreeImpl<NODE> extends VaadinComponentDelegate<WebTreeImpl.Vaadin> implements Tree<NODE>, PopupOwner {
    private @Nullable TransferHandler<TreeNode<NODE>> myTransferHandler;
    private @Nullable Function<TreeNode<NODE>, Length> myItemHeightGetter;
    private @Nullable Function<TreeNode<NODE>, String> mySpeedSearchConverter;

    /** where the row of the last right click ended up, which is what a popup raised over the tree hangs off */
    private volatile @Nullable Point2D myPopupPosition;

    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    private final TreeController<NODE> myController;

    @Tag("vaadin-grid-tree-toggle")
    public static class VaadinGridTreeToggle extends com.vaadin.flow.component.Component
        implements HasComponents, ClickNotifier<VaadinGridTreeToggle> {
    }

    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips
    // rebuilding on css only changes, and the tree look was left one build behind
    @StyleSheet("/tree/webTree.css")
    public class Vaadin extends TreeGrid<WebTreeRow<NODE>> implements FromVaadinComponentWrapper {
        private final Map<TreeNodeImpl<NODE>, WebTreeRow<NODE>> myRows = new HashMap<>();
        private TreeData<WebTreeRow<NODE>> myData = new TreeData<>();

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
                myDraggedItems = nodesOf(event.getDraggedItems());
                DataTransfer transfer = handler.createDragTransfer(WebTreeImpl.this, myDraggedItems, true);
                myDragTransfer = transfer == null ? DataTransfer.EMPTY : transfer;
            });

            addDragEndListener(event -> {
                myDraggedItems = List.of();
                myDragTransfer = DataTransfer.EMPTY;
            });

            addDropListener(event -> {
                WebTreeRow<NODE> target = event.getDropTargetItem().orElse(null);
                TreeNodeImpl<NODE> targetNode = target == null ? null : target.getNode();
                DragAndDropTransferHandler.DropPosition position = positionOf(event.getDropLocation());
                if (targetNode == null || position == null) {
                    return;
                }

                DropContextImpl context = new DropContextImpl(targetNode, position, myDragTransfer, myDraggedItems, true);
                if (!handler.drop(WebTreeImpl.this, context)) {
                    return;
                }

                handler.drop(WebTreeImpl.this, context.toPerforming());
            });
        }

        public Vaadin() {
            addThemeVariants(GridVariant.NO_ROW_BORDERS, GridVariant.COLUMN_BORDERS, GridVariant.NO_BORDER);

            setSelectionMode(SelectionMode.SINGLE);

            ((SelectionModel.Single) getSelectionModel()).setDeselectAllowed(false);

            addComponentColumn(row -> {
                // nothing is asked of the model here - the presentation was computed on the executor while the
                // level was built, and this only turns it into the components of the row
                TreeNodeImpl<NODE> node = row.getNode();
                WebItemPresentationImpl item = node != null && node.getPresentation() instanceof WebItemPresentationImpl presentation
                    ? presentation
                    : null;
                if (item == null) {
                    item = new WebItemPresentationImpl();
                    if (node == null) {
                        item.append(UILocalize.treenodeLoading());
                    }
                }

                VaadinGridTreeToggle toggle = new VaadinGridTreeToggle();
                toggle.getElement().setAttribute("leaf", node == null || node.isLeaf());
                toggle.getElement().setAttribute("level", String.valueOf(levelOf(row)));
                if (isExpanded(row)) {
                    toggle.getElement().setAttribute("expanded", true);
                }

                // a click anywhere on the toggle selects - the label is inside it - while opening a node is
                // the chevron's alone. the filter keeps the label clicks off the wire entirely, and the
                // client half of this is treeToggle.js, which stops the element flipping itself for them
                toggle.addClickListener(event -> selectRow(row));

                toggle.getElement().addEventListener("click", event -> {
                    if (getDataCommunicator().hasChildren(row)) {
                        if (isExpanded(row)) {
                            collapse(List.of(row), true);
                        }
                        else {
                            expand(List.of(row), true);
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

            addExpandListener(event -> {
                if (!event.isFromClient()) {
                    return;
                }

                for (WebTreeRow<NODE> row : event.getItems()) {
                    TreeNodeImpl<NODE> node = row.getNode();
                    if (node != null) {
                        myController.onExpanded(node, ProgrammaticInputDetails.INSTANCE);
                    }
                }
            });

            addCollapseListener(event -> {
                if (!event.isFromClient()) {
                    return;
                }

                for (WebTreeRow<NODE> row : event.getItems()) {
                    TreeNodeImpl<NODE> node = row.getNode();
                    if (node != null) {
                        myController.onCollapsed(node, ProgrammaticInputDetails.INSTANCE);
                    }
                }
            });

            installSelectOnRightClick();

            installData(myData);
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

                    WebTreeRow<NODE> item = HierarchicalDataCommunicatorAccess.getItemByFlatIndex(getDataCommunicator(), index);
                    if (item != null) {
                        selectRow(item);
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

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);

            resetData();

            myController.bind();
        }

        private void installData(TreeData<WebTreeRow<NODE>> data) {
            myData = data;

            TreeDataProvider<WebTreeRow<NODE>> provider = new TreeDataProvider<>(data) {
                @Override
                public Object getId(WebTreeRow<NODE> item) {
                    return item.getId();
                }
            };

            setUniqueKeyDataGenerator("key", WebTreeRow::getId);

            setDataProvider(provider);
            getDataCommunicator().getKeyMapper().setIdentifierGetter(WebTreeRow::getId);
        }

        private void resetData() {
            myRows.clear();

            TreeData<WebTreeRow<NODE>> data = new TreeData<>();
            if (!myController.getRoot().isLoaded()) {
                data.addRootItems(List.of(WebTreeRow.placeholder()));
            }
            installData(data);
        }

        private void selectRow(WebTreeRow<NODE> row) {
            TreeNodeImpl<NODE> node = row.getNode();
            if (node == null) {
                return;
            }

            select(row);
            myController.onSelected(node, ProgrammaticInputDetails.INSTANCE);
        }

        private @Nullable WebTreeRow<NODE> liveRow(TreeNodeImpl<NODE> node) {
            WebTreeRow<NODE> row = myRows.get(node);
            return row != null && myData.contains(row) ? row : null;
        }

        private int levelOf(WebTreeRow<NODE> row) {
            int level = 0;
            for (WebTreeRow<NODE> parent = myData.getParent(row); parent != null; parent = myData.getParent(parent)) {
                level++;
            }
            return level;
        }

        private List<TreeNode<NODE>> nodesOf(Collection<WebTreeRow<NODE>> rows) {
            List<TreeNode<NODE>> nodes = new ArrayList<>(rows.size());
            for (WebTreeRow<NODE> row : rows) {
                TreeNodeImpl<NODE> node = row.getNode();
                if (node != null) {
                    nodes.add(node);
                }
            }
            return nodes;
        }

        private void showLoading(TreeNodeImpl<NODE> node) {
            if (node == myController.getRoot()) {
                if (myData.getRootItems().isEmpty()) {
                    myData.addItem(null, WebTreeRow.placeholder());
                    getDataProvider().refreshAll();
                }
                return;
            }

            WebTreeRow<NODE> row = liveRow(node);
            if (row != null && syncPlaceholder(row)) {
                getDataProvider().refreshItem(row, true);
            }
        }

        private void setChildren(TreeNodeImpl<NODE> node, List<TreeNodeImpl<NODE>> children) {
            boolean root = node == myController.getRoot();
            WebTreeRow<NODE> parent = root ? null : liveRow(node);
            if (!root && parent == null) {
                return;
            }

            List<WebTreeRow<NODE>> current = List.copyOf(myData.getChildren(parent));
            Set<WebTreeRow<NODE>> present = new HashSet<>(current);

            List<WebTreeRow<NODE>> wanted = new ArrayList<>(children.size());
            for (TreeNodeImpl<NODE> child : children) {
                WebTreeRow<NODE> row = myRows.get(child);
                if (row == null || !present.contains(row)) {
                    row = WebTreeRow.of(child);
                    myRows.put(child, row);
                    myData.addItem(parent, row);
                }
                wanted.add(row);
            }

            Set<WebTreeRow<NODE>> kept = new HashSet<>(wanted);
            for (WebTreeRow<NODE> row : current) {
                if (!kept.contains(row)) {
                    removeRow(row);
                }
            }

            if (!myData.getChildren(parent).equals(wanted)) {
                WebTreeRow<NODE> previous = null;
                for (WebTreeRow<NODE> row : wanted) {
                    myData.moveAfterSibling(row, previous);
                    previous = row;
                }
            }

            for (WebTreeRow<NODE> row : wanted) {
                syncPlaceholder(row);
            }

            if (root) {
                getDataProvider().refreshAll();
            }
            else {
                getDataProvider().refreshItem(parent, true);
            }
        }

        private boolean syncPlaceholder(WebTreeRow<NODE> row) {
            TreeNodeImpl<NODE> node = row.getNode();
            if (node == null || node.isLoaded()) {
                return false;
            }

            List<WebTreeRow<NODE>> children = List.copyOf(myData.getChildren(row));
            if (node.isLeaf()) {
                for (WebTreeRow<NODE> child : children) {
                    removeRow(child);
                }
                return !children.isEmpty();
            }

            if (children.isEmpty()) {
                myData.addItem(row, WebTreeRow.placeholder());
                return true;
            }
            return false;
        }

        private void removeRow(WebTreeRow<NODE> row) {
            forget(row);
            myData.removeItem(row);
        }

        private void forget(WebTreeRow<NODE> row) {
            for (WebTreeRow<NODE> child : myData.getChildren(row)) {
                forget(child);
            }

            TreeNodeImpl<NODE> node = row.getNode();
            if (node != null && myRows.get(node) == row) {
                myRows.remove(node);
            }
        }

        private void setExpanded(TreeNodeImpl<NODE> node, boolean expanded) {
            WebTreeRow<NODE> row = liveRow(node);
            if (row == null || isExpanded(row) == expanded) {
                return;
            }

            if (expanded) {
                expand(List.of(row));
            }
            else {
                collapse(List.of(row));
            }

            getDataProvider().refreshItem(row);
        }

        private void setSelected(@Nullable TreeNodeImpl<NODE> node) {
            if (node == null) {
                deselectAll();
                return;
            }

            WebTreeRow<NODE> row = liveRow(node);
            if (row != null) {
                select(row);
            }
        }

        private void update(TreeNodeImpl<NODE> node) {
            WebTreeRow<NODE> row = liveRow(node);
            if (row == null) {
                return;
            }

            syncPlaceholder(row);
            getDataProvider().refreshItem(row, true);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebTreeImpl.this;
        }
    }

    private class Binding implements TreeWidget<NODE> {
        @Override
        public @Nullable UIAccess getUIAccess() {
            return WebTreeImpl.this.getUIAccess();
        }

        @Override
        public boolean isUIThread() {
            UI ui = toVaadinComponent().getUI().orElse(null);
            VaadinSession session = ui == null ? null : ui.getSession();
            return session != null && session.hasLock();
        }

        @Override
        public TextItemPresentation createPresentation() {
            return new WebItemPresentationImpl();
        }

        @Override
        public void showLoading(TreeNodeImpl<NODE> node) {
            toVaadinComponent().showLoading(node);
        }

        @Override
        public void hideLoading(TreeNodeImpl<NODE> node) {
        }

        @Override
        public void setChildren(TreeNodeImpl<NODE> node, List<TreeNodeImpl<NODE>> children) {
            toVaadinComponent().setChildren(node, children);
        }

        @Override
        public void setExpanded(TreeNodeImpl<NODE> node, boolean expanded) {
            toVaadinComponent().setExpanded(node, expanded);
        }

        @Override
        public void setSelected(@Nullable TreeNodeImpl<NODE> node) {
            toVaadinComponent().setSelected(node);
        }

        @Override
        public void update(TreeNodeImpl<NODE> node) {
            toVaadinComponent().update(node);
        }
    }

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }

    @RequiredUIAccess
    public WebTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model, TreeExecutor executor) {
        myController = new TreeController<>(this, rootValue, model, executor, new Binding());
        Disposer.register(myDestroyHook, myController);

        Vaadin vaadin = toVaadinComponent();
        vaadin.resetData();

        vaadin.asSingleSelect().addValueChangeListener(event -> {
            WebTreeRow<NODE> row = event.getValue();
            TreeNodeImpl<NODE> node = row == null ? null : row.getNode();
            if (event.isFromClient() && node != null) {
                myController.onSelected(node, ProgrammaticInputDetails.INSTANCE);
            }
        });

        WebInputDetails.addClickListener(vaadin.getElement(), "dblclick", inputDetails -> {
            TreeNodeImpl<NODE> selectedNode = myController.getSelected();
            if (selectedNode != null) {
                myController.onDoubleClick(selectedNode, inputDetails);
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable TreeNode<NODE> getSelectedNode() {
        return myController.getSelected();
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<NODE> node, int depth) {
        return myController.expand(node, depth);
    }

    @Override
    public TreeNode<NODE> getRootNode() {
        return myController.getRoot();
    }

    @Override
    public List<List<TreeNode<NODE>>> getExpandedPaths() {
        return myController.getExpandedPaths();
    }

    @Override
    public List<TreeNode<NODE>> getSelectedPath() {
        return myController.getSelectedPath();
    }

    @Override
    public void select(TreeNode<NODE> node) {
        myController.select(node);
    }

    @Override
    public void refreshItem(TreeNode<NODE> node, boolean refreshChildren) {
        myController.refreshItem(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return myController.refreshAll();
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll() {
        return myController.expandAll();
    }

    @Override
    public CompletableFuture<?> expandAll(int depth) {
        return myController.expandAll(depth);
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        return myController.collapseAll();
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
