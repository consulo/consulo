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
package consulo.web.gwt.client.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 19-Jun-16
 */
public class GwtTreeImpl extends CellTree {
  private static class ChildNode extends TreeViewModel.DefaultNodeInfo<TreeState.TreeNodeState> {

    public ChildNode() {
      super(new ListDataProvider<>(Collections.emptyList()), new AbstractCell<TreeState.TreeNodeState>() {
        @Override
        public void render(Context context, TreeState.TreeNodeState value, SafeHtmlBuilder sb) {
          GwtHorizontalLayoutImpl layout = GwtComboBoxImplConnector.buildItem(value);

          SafeHtml safeValue = SafeHtmlUtils.fromSafeConstant(layout.toString());

          sb.append(safeValue);
        }
      });
    }

    public void setItems(List<TreeState.TreeNodeState> list) {
      ListDataProvider<TreeState.TreeNodeState> provider = (ListDataProvider<TreeState.TreeNodeState>)getProvidesKey();

      provider.setList(list);
      provider.refresh();
    }
  }

  private static class OurModel implements TreeViewModel {
    private final ChildNode myRootNode = new ChildNode();

    private Map<String, ChildNode> myNodes = new HashMap<>();

    private Consumer<String> myChildrenOpen = id -> {
    };

    private OurModel() {
      myNodes.put(null, myRootNode);
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == null) {
        return myRootNode;
      }

      TreeState.TreeNodeState state = (TreeState.TreeNodeState)value;

      myChildrenOpen.accept(state.myId);

      ChildNode node = new ChildNode();
      myNodes.put(state.myId, node);
      return node;
    }

    @Override
    public boolean isLeaf(Object value) {
      if(value == null) {
        return false;
      }
      TreeState.TreeNodeState state = (TreeState.TreeNodeState)value;
      return state.myLeaf;
    }
  }

  public GwtTreeImpl() {
    super(new OurModel(), null, GWT.<CellTree.Resources>create(DefaultCellTreeResources.class), new CellTreeMessages() {
      @Override
      public String showMore() {
        return null;
      }

      @Override
      public String emptyTree() {
        return "loading...";
      }
    });
  }

  public void setChildrenOpen(Consumer<String> childrenOpen) {
    getTreeViewModel().myChildrenOpen = childrenOpen;
  }

  @Override
  public OurModel getTreeViewModel() {
    return (OurModel)super.getTreeViewModel();
  }

  public void handleChanges(List<TreeState.TreeChange> changes) {
    for (TreeState.TreeChange change : changes) {
      switch (change.myType) {
        case ADD:
          //todo [vistall] add
          break;
        case REMOVE:
          //todo [vistall] remove
          break;
        case SET:
          ChildNode info = getTreeViewModel().myNodes.get(change.myId);
          if (info == null) {
            Window.alert("NULL");
            continue;
          }

          info.setItems(change.myNodes);
          break;
      }
    }
  }
}
