/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;
import consulo.ui.*;
import consulo.ui.shared.Size;
import consulo.ui.web.internal.WebTreeNodeImpl;
import consulo.ui.web.internal.WebUIThreadLocal;
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
 * @since 16-Jun-16
 */
public class WGwtTreeImpl<NODE> extends AbstractComponent implements Tree<NODE>, VaadinWrapper {
  private final TreeServerRpc myTreeServerRpc = new TreeServerRpc() {
    @Override
    public void onOpen(String id) {
      WebTreeNodeImpl<NODE> node = myNodeMap.get(id);
      if (node == null) {
        return;
      }

      queue(node, TreeState.TreeChangeType.SET);
    }

    @Override
    public void onDoubleClick(String id) {
      WebTreeNodeImpl<NODE> node = myNodeMap.get(id);
      if (node == null) {
        return;
      }

      if (myModel.onDoubleClick(WGwtTreeImpl.this, node)) {
        getRpcProxy(TreeClientRpc.class).expand(id);
      }
    }

    @Override
    public void onSelected(String id) {
      mySelectedValue = id;

      WebTreeNodeImpl<NODE> gwtTreeNode = myNodeMap.get(id);
      if (gwtTreeNode != null) {
        getListenerDispatcher(SelectListener.class).onSelected(gwtTreeNode);
      }
    }

    @Override
    public void onContextMenu(int x, int y) {
      if(myContextHandler != null) {
        myContextHandler.show(x, y);
      }
    }
  };

  private final Executor myUpdater = Executors.newSingleThreadExecutor();  //TODO [VISTALL] very bad idea without dispose
  private final List<TreeState.TreeChange> myChanges = new ArrayList<>();

  private String mySelectedValue;

  private final Map<String, WebTreeNodeImpl<NODE>> myNodeMap = new LinkedHashMap<>();
  private final WebTreeNodeImpl<NODE> myRootNode;
  private final TreeModel<NODE> myModel;
  private ContextHandler myContextHandler;

  public WGwtTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model) {
    myModel = model;

    myRootNode = new WebTreeNodeImpl<>(null, rootValue, myNodeMap);

    registerRpc(myTreeServerRpc);

    if (myModel.isNeedBuildChildrenBeforeOpen(myRootNode)) {
      fetchChildren(myRootNode, false);
    }
  }

  @Override
  @RequiredUIAccess
  public void attach() {
    super.attach();

    queue(myRootNode, TreeState.TreeChangeType.SET);
  }

  @Nullable
  @Override
  public TreeNode<NODE> getSelectedNode() {
    if (mySelectedValue == null) {
      return null;
    }
    return myNodeMap.get(mySelectedValue);
  }

  @Override
  public void expand(@Nonnull TreeNode<NODE> node) {
    queue((WebTreeNodeImpl<NODE>)node, TreeState.TreeChangeType.SET);
  }

  @Override
  public void setContextHandler(@Nonnull ContextHandler contextHandler) {
    myContextHandler = contextHandler;
  }

  private void queue(@Nonnull WebTreeNodeImpl<NODE> parent, TreeState.TreeChangeType type) {
    UI ui = UI.getCurrent();
    myUpdater.execute(() -> {
      WebUIThreadLocal.setUI(ui);

      try {
        List<WebTreeNodeImpl<NODE>> children = parent.getChildren();
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

  private void mapChanges(@Nonnull WebTreeNodeImpl<NODE> parent, List<WebTreeNodeImpl<NODE>> children, TreeState.TreeChangeType type) {
    synchronized (myChanges) {
      TreeState.TreeChange change = new TreeState.TreeChange();
      change.myId = parent.getId();
      change.myType = type;

      for (WebTreeNodeImpl<NODE> node : children) {
        change.myNodes.add(convert(node));
      }

      myChanges.add(change);
    }
  }

  @Nonnull
  private List<WebTreeNodeImpl<NODE>> fetchChildren(@Nonnull WebTreeNodeImpl<NODE> parent, boolean fetchNext) {
    List<WebTreeNodeImpl<NODE>> list = new ArrayList<>();

    myModel.fetchChildren(node -> {
      WebTreeNodeImpl<NODE> child = new WebTreeNodeImpl<>(parent, node, myNodeMap);
      list.add(child);
      return child;
    }, parent.getValue());

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
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    synchronized (myChanges) {
      TreeState state = getState();
      state.myChanges.clear();

      state.myChanges.addAll(myChanges);

      myChanges.clear();
    }
  }

  @Nonnull
  private TreeState.TreeNodeState convert(WebTreeNodeImpl<NODE> child) {
    TreeState.TreeNodeState e = new TreeState.TreeNodeState();
    e.myId = child.getId();
    e.myLeaf = child.isLeaf();
    e.myParentId = child.getParent() == null ? null : child.getId();

    WGwtItemPresentationImpl presentation = new WGwtItemPresentationImpl();

    child.getRender().accept(child.getValue(), presentation);
    e.myItemSegments = presentation.getItem().myItemSegments;
    e.myImageState = presentation.getItem().myImageState;
    return e;
  }

  @Override
  protected TreeState getState() {
    return (TreeState)super.getState();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }
}
