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
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.web.gwt.shared.ui.state.tree.TreeClientRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeServerRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      WGwtTreeNodeImpl<NODE> node = myChildren.get(id);
      if (node == null) {
        return;
      }

      queue(node.getValue(), node, TreeState.TreeChangeType.SET);
    }

    @Override
    public void onDoubleClick(String id) {
      WGwtTreeNodeImpl<NODE> node = myChildren.get(id);
      if (node == null) {
        return;
      }

      if(myModel.onDoubleClick(WGwtTreeImpl.this, node)) {
        getRpcProxy(TreeClientRpc.class).expand(id);
      }
    }
  };

  private final NODE myRootValue;
  private final TreeModel<NODE> myModel;
  private final Executor myUpdater = Executors.newSingleThreadExecutor();
  private final Map<String, WGwtTreeNodeImpl<NODE>> myChildren = new LinkedHashMap<>();
  private final List<TreeState.TreeChange> myChanges = new ArrayList<>();

  public WGwtTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model) {
    myRootValue = rootValue;
    myModel = model;
    registerRpc(myTreeServerRpc);
  }

  @Override
  @RequiredUIAccess
  public void attach() {
    super.attach();

    myChildren.clear();

    queue(myRootValue, null, TreeState.TreeChangeType.SET);
  }

  @Override
  public void expand(@NotNull TreeNode<NODE> node) {
    WGwtTreeNodeImpl<NODE> gwtTreeNode = (WGwtTreeNodeImpl<NODE>)node;

    queue(gwtTreeNode.getValue(), gwtTreeNode, TreeState.TreeChangeType.SET);
  }

  private void queue(NODE value, @Nullable WGwtTreeNodeImpl<NODE> parent, TreeState.TreeChangeType type) {
    UI ui = UI.getCurrent();
    myUpdater.execute(() -> {
      WGwtUIThreadLocal.setUI(ui);

      try {
        List<WGwtTreeNodeImpl<NODE>> list = new ArrayList<>();

        myModel.fetchChildren(node -> {
          WGwtTreeNodeImpl<NODE> child = new WGwtTreeNodeImpl<>(value, node);
          list.add(child);
          return child;
        }, value);

        if (parent != null) {
          parent.setChildren(list);
        }

        synchronized (myChanges) {
          TreeState.TreeChange change = new TreeState.TreeChange();
          change.myId = parent == null ? null : parent.getId();
          change.myType = type;

          for (WGwtTreeNodeImpl<NODE> node : list) {
            myChildren.put(node.getId(), node);

            change.myNodes.add(convert(node));
          }

          myChanges.add(change);
        }

        ui.access(this::markAsDirty);
      }
      finally {
        WGwtUIThreadLocal.setUI(null);
      }
    });
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

  @NotNull
  private TreeState.TreeNodeState convert(WGwtTreeNodeImpl<NODE> child) {
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

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}
