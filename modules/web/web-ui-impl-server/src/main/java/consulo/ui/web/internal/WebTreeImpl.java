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
package consulo.ui.web.internal;

import com.vaadin.ui.UI;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.web.gwt.shared.ui.state.tree.TreeClientRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeServerRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebTreeImpl<NODE> extends UIComponentWithVaadinComponent<WebTreeImpl.Vaadin<NODE>> implements Tree<NODE> {
  public static class Vaadin<E> extends VaadinComponent {
    @SuppressWarnings("unchecked")
    private final TreeServerRpc myTreeServerRpc = new TreeServerRpc() {
      @Override
      public void onOpen(String id) {
        WebTreeNodeImpl<E> node = myNodeMap.get(id);
        if (node == null) {
          return;
        }

        queue(node, TreeState.TreeChangeType.SET);
      }

      @Override
      public void onDoubleClick(String id) {
        WebTreeNodeImpl<E> node = myNodeMap.get(id);
        if (node == null) {
          return;
        }

        if (myModel.onDoubleClick((Tree<E>)toUIComponent(), node)) {
          getRpcProxy(TreeClientRpc.class).expand(id);
        }
      }

      @Override
      @RequiredUIAccess
      public void onSelected(String id) {
        mySelectedValue = id;

        WebTreeNodeImpl<E> gwtTreeNode = myNodeMap.get(id);
        if (gwtTreeNode != null) {
          toUIComponent().getListenerDispatcher(SelectListener.class).onSelected(gwtTreeNode);
        }
      }

      @Override
      public void onContextMenu(int x, int y) {
        if (myContextHandler != null) {
          myContextHandler.show(x, y);
        }
      }
    };

    private final Executor myUpdater = Executors.newSingleThreadExecutor();  //TODO [VISTALL] very bad idea without dispose
    private final List<TreeState.TreeChange> myChanges = new ArrayList<>();
    private final Map<String, WebTreeNodeImpl<E>> myNodeMap = new LinkedHashMap<>();

    private ContextHandler myContextHandler;

    private String mySelectedValue;

    private WebTreeNodeImpl<E> myRootNode;
    private TreeModel<E> myModel;

    public Vaadin() {
      registerRpc(myTreeServerRpc);
    }

    public void init(E rootValue, TreeModel<E> model) {
      myModel = model;

      myRootNode = new WebTreeNodeImpl<>(null, rootValue, myNodeMap);

      if (myModel.isNeedBuildChildrenBeforeOpen(myRootNode)) {
        fetchChildren(myRootNode, false);
      }
    }

    private void queue(@Nonnull WebTreeNodeImpl<E> parent, TreeState.TreeChangeType type) {
      UI ui = UI.getCurrent();
      myUpdater.execute(() -> {
        WebUIThreadLocal.setUI(ui);

        try {
          List<WebTreeNodeImpl<E>> children = parent.getChildren();
          if (children == null) {
            children = fetchChildren(parent, true);
          }

          mapChanges(parent, children, type);

          ui.access(this::markAsDirty);
        }
        finally {
          WebUIThreadLocal.setUI(null);
        }
      });
    }

    private void mapChanges(@Nonnull WebTreeNodeImpl<E> parent, List<WebTreeNodeImpl<E>> children, TreeState.TreeChangeType type) {
      synchronized (myChanges) {
        TreeState.TreeChange change = new TreeState.TreeChange();
        change.myId = parent.getId();
        change.myType = type;

        for (WebTreeNodeImpl<E> node : children) {
          change.myNodes.add(convert(node));
        }

        myChanges.add(change);
      }
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      synchronized (myChanges) {
        TreeState state = getState();
        state.myChanges.clear();

        state.myChanges.addAll(myChanges);

        myChanges.clear();
      }
    }

    @Override
    @RequiredUIAccess
    public void attach() {
      super.attach();

      queue(myRootNode, TreeState.TreeChangeType.SET);
    }

    @Nonnull
    private TreeState.TreeNodeState convert(WebTreeNodeImpl<E> child) {
      TreeState.TreeNodeState e = new TreeState.TreeNodeState();
      e.myId = child.getId();
      e.myLeaf = child.isLeaf();
      e.myParentId = child.getParent() == null ? null : child.getId();

      WebItemPresentationImpl presentation = new WebItemPresentationImpl();

      child.getRender().accept(child.getValue(), presentation);
      e.myItemSegments = presentation.getItem().myItemSegments;
      e.myImageState = presentation.getItem().myImageState;
      return e;
    }

    @Nonnull
    private List<WebTreeNodeImpl<E>> fetchChildren(@Nonnull WebTreeNodeImpl<E> parent, boolean fetchNext) {
      List<WebTreeNodeImpl<E>> list = new ArrayList<>();

      myModel.fetchChildren(node -> {
        WebTreeNodeImpl<E> child = new WebTreeNodeImpl<>(parent, node, myNodeMap);
        list.add(child);
        return child;
      }, parent.getValue());

      parent.setChildren(list);

      Comparator<TreeNode<E>> nodeComparator = myModel.getNodeComparator();
      if (nodeComparator != null) {
        list.sort(nodeComparator);
      }

      if (list.isEmpty()) {
        parent.setLeaf(true);
      }

      if (fetchNext) {
        for (WebTreeNodeImpl<E> child : list) {
          if (myModel.isNeedBuildChildrenBeforeOpen(child)) {
            fetchChildren(child, false);
          }
        }
      }

      return list;
    }

    @Nullable
    public TreeNode<E> getSelectedNode() {
      if (mySelectedValue == null) {
        return null;
      }
      return myNodeMap.get(mySelectedValue);
    }

    @Override
    public TreeState getState() {
      return (TreeState)super.getState();
    }
  }

  public WebTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model) {
    getVaadinComponent().init(rootValue, model);
  }

  @Override
  @Nonnull
  public Vaadin<NODE> createVaadinComponent() {
    return new Vaadin<>();
  }

  @Nullable
  @Override
  public TreeNode<NODE> getSelectedNode() {
    return getVaadinComponent().getSelectedNode();
  }

  @Override
  public void expand(@Nonnull TreeNode<NODE> node) {
    getVaadinComponent().queue((WebTreeNodeImpl<NODE>)node, TreeState.TreeChangeType.SET);
  }

  @Override
  public void setContextHandler(@Nonnull ContextHandler contextHandler) {
    getVaadinComponent().myContextHandler = contextHandler;
  }
}
