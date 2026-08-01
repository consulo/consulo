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
package consulo.web.internal.ui;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.selection.SelectionModel;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TreeSelectEvent;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
@SuppressWarnings("unchecked")
public class WebTreeImpl<NODE> extends VaadinComponentDelegate<WebTreeImpl.Vaadin> implements Tree<NODE> {
    private static final List CANCELED_RESULT = new ArrayList<>();

    private static final int EXPAND_ALL_DEPTH = 3;

    @Tag("vaadin-grid-tree-toggle")
    public static class VaadinGridTreeToggle extends com.vaadin.flow.component.Component
        implements HasComponents, ClickNotifier<VaadinGridTreeToggle> {
    }

    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips
    // rebuilding on css only changes, and the tree look was left one build behind
    @StyleSheet("/tree/webTree.css")
    public class Vaadin extends TreeGrid<WebTreeNodeImpl<NODE>> implements FromVaadinComponentWrapper {
        private final Map<String, WebTreeNodeImpl<NODE>> myNodeMap = new LinkedHashMap<>();

        private WebTreeNodeImpl<NODE> myRootNode;
        private TreeModel<NODE> myModel;

        public Vaadin() {
            setAllRowsVisible(true);

            addThemeVariants(GridVariant.NO_ROW_BORDERS, GridVariant.COLUMN_BORDERS, GridVariant.NO_BORDER);

            setSelectionMode(SelectionMode.SINGLE);

            ((SelectionModel.Single) getSelectionModel()).setDeselectAllowed(false);

            addComponentColumn(node -> {
                WebItemPresentationImpl item = new WebItemPresentationImpl();
                if (node instanceof WebTreeNodeImpl.NotLoaded) {
                    item.append("Loading...");
                }
                else {
                    node.getRenderer().accept(node.getValue(), item);
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

            getElement()
                .addEventListener("mousedown", event -> {
                    int index = event.getEventData().path(rowIndex).asInt(-1);
                    // the rows of the shadow dom outlive a rebuild of the data, so an index read from one of
                    // them can point past what the communicator now holds - and getItem throws rather than
                    // answering null for that
                    if (index < 0 || index >= getDataCommunicator().getItemCount()) {
                        return;
                    }

                    WebTreeNodeImpl<NODE> item = getDataCommunicator().getItem(index);
                    if (item != null) {
                        select(item);
                    }
                })
                // header rows carry no index, and only the right button has to move the selection - the left one
                // is the grid's own business
                .addEventData(rowIndex)
                .setFilter("event.button === 2");
        }

        public void init(NODE rootValue, TreeModel<NODE> model) {
            myModel = model;

            myRootNode = new WebTreeNodeImpl<>(null, rootValue, myNodeMap);

            if (myModel.isNeedBuildChildrenBeforeOpen(myRootNode)) {
                fetchChildren(myRootNode, false);
            }

            initTreeData(true);

            addExpandListener(event -> {
                Collection<WebTreeNodeImpl<NODE>> items = event.getItems();

                for (WebTreeNodeImpl<NODE> item : items) {
                    if (item.isNotLoaded()) {
                        UI ui = UI.getCurrent();
                        // items not loaded
                        queue(item, ui);
                    }
                }
            });
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);

            UI ui = UI.getCurrent();

            invokeLater(() -> {
                fetchChildren(myRootNode, false);

                ui.access(() -> {
                    initTreeData(false);
                });
            });
        }

        private void queue(WebTreeNodeImpl<NODE> parent, UI ui) {
            invokeLater(() -> {
                List<WebTreeNodeImpl<NODE>> children = parent.getChildren();
                if (parent.isNotLoaded()) {
                    WebTreeNodeImpl<NODE> unloaded = children.get(0);

                    children = fetchChildren(parent, false);
                    if (children == CANCELED_RESULT) {
                        return;
                    }

                    List<WebTreeNodeImpl<NODE>> finalChildren = children;
                    ui.access(() -> {
                        TreeData<WebTreeNodeImpl<NODE>> data = getTreeData();

                        data.removeItem(unloaded);

                        data.addItems(parent, finalChildren);

                        // add raw children
                        for (WebTreeNodeImpl<NODE> finalChild : finalChildren) {
                            data.addItems(finalChild, finalChild.getChildren());
                        }

                        myNodeMap.remove(unloaded.getId());

                        getDataProvider().refreshItem(parent, true);

                        ui.push();
                    });
                }
            });
        }

        private void invokeLater(Runnable runnable) {
            AppExecutorUtil.getAppExecutorService().execute(runnable);
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
                    data.addItems(node, node.getChildren());
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
        }

        private List<WebTreeNodeImpl<NODE>> fetchChildren(WebTreeNodeImpl<NODE> parent, boolean fetchNext) {
            List<WebTreeNodeImpl<NODE>> list = new ArrayList<>();
            Map<String, WebTreeNodeImpl<NODE>> nodeMap = new HashMap<>();

            try {
                myModel.buildChildren(
                    node -> {
                        WebTreeNodeImpl<NODE> child = new WebTreeNodeImpl<>(parent, node, nodeMap);
                        list.add(child);
                        return child;
                    },
                    parent.getValue()
                );
            }
            catch (ProcessCanceledException ignored) {
                return CANCELED_RESULT;
            }

            myNodeMap.putAll(nodeMap);

            parent.setChildren(list);

            Comparator<TreeNode<NODE>> nodeComparator = myModel.getNodeComparator();
            if (nodeComparator != null) {
                list.sort(nodeComparator);
            }

            if (list.isEmpty()) {
                parent.setLeaf(true);
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

    @RequiredUIAccess
    public WebTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model, Disposable disposable) {
        Vaadin vaadin = toVaadinComponent();
        vaadin.init(rootValue, model);
        vaadin.asSingleSelect().addValueChangeListener(event -> {
            WebTreeNodeImpl<NODE> value = event.getValue();
            if (value == null || value instanceof WebTreeNodeImpl.NotLoaded) {
                return;
            }

            getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent(this, value));
        });

        vaadin.addItemDoubleClickListener(event -> {
            TreeNode<NODE> selectedNode = getSelectedNode();
            if (selectedNode == null) {
                return;
            }

            // the return value is the contract - true asks the tree to toggle the node, the way the awt trees
            // do, and a model that answered with an action of its own - opening the file - says false
            if (model.onDoubleClick(this, selectedNode) && selectedNode instanceof WebTreeNodeImpl<NODE> node) {
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
    public void expand(TreeNode<NODE> node) {
        toVaadinComponent().expand(node);
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public void expandAll() {
        Vaadin vaadin = toVaadinComponent();

        // the children of a node are fetched when it is first opened, so the depth is unknown here and a
        // recursion over the whole tree could open a project view down to every file. the awt expand all
        // stops at the same place - it only walks what the tree already holds
        vaadin.expandRecursively(vaadin.getTreeData().getRootItems(), EXPAND_ALL_DEPTH);
    }

    @Override
    public void collapseAll() {
        Vaadin vaadin = toVaadinComponent();

        vaadin.collapseRecursively(vaadin.getTreeData().getRootItems(), Integer.MAX_VALUE);
    }
}
