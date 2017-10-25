/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.client.ui.tree;

import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class GwtTreeModel implements TreeViewModel {
  private final SelectionModel<TreeState.TreeNodeState> mySelectionModel = new SingleSelectionModel<>();

  private Consumer<TreeState.TreeNodeState> myChildrenOpenHandler = state -> {
  };

  private Consumer<TreeState.TreeNodeState> myDoubleClickHandler = state -> {
  };

  private final Map<String, GwtTreeNode> myNodes = new HashMap<>();

  private final GwtTreeNode myRootNode = new GwtTreeNode(mySelectionModel);

  private GwtTreeImpl myCellTree;

  public GwtTreeModel() {
    myNodes.put("root", myRootNode);
  }

  public void init(GwtTreeImpl cellTree) {
    myCellTree = cellTree;
    myRootNode.init(cellTree);
  }

  @Override
  public <T> NodeInfo<?> getNodeInfo(T value) {
    if (value == null) {
      return myRootNode;
    }

    TreeState.TreeNodeState state = (TreeState.TreeNodeState)value;

    myChildrenOpenHandler.accept(state);

    GwtTreeNode node = new GwtTreeNode(mySelectionModel);
    node.init(myCellTree);

    myNodes.put(state.myId, node);
    return node;
  }

  @Override
  public boolean isLeaf(Object value) {
    if (value == null) {
      return false;
    }
    TreeState.TreeNodeState state = (TreeState.TreeNodeState)value;
    return state.myLeaf;
  }

  public Map<String, GwtTreeNode> getNodes() {
    return myNodes;
  }

  public void setChildrenOpenHandler(Consumer<TreeState.TreeNodeState> childrenOpenHandler) {
    myChildrenOpenHandler = childrenOpenHandler;
  }

  public void setDoubleClickHandler(Consumer<TreeState.TreeNodeState> doubleClickHandler) {
    myDoubleClickHandler = doubleClickHandler;
  }

  public SelectionModel<TreeState.TreeNodeState> getSelectionModel() {
    return mySelectionModel;
  }

  public Consumer<TreeState.TreeNodeState> getDoubleClickHandler() {
    return myDoubleClickHandler;
  }
}
