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

import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class GwtTreeNode extends TreeViewModel.DefaultNodeInfo<TreeState.TreeNodeState> {

  public GwtTreeNode(SelectionModel<TreeState.TreeNodeState> model) {
    super(new ListDataProvider<>(Collections.emptyList()), new GwtTreeCell(), model, null);
  }

  public void setItems(List<TreeState.TreeNodeState> list) {
    ListDataProvider<TreeState.TreeNodeState> provider = (ListDataProvider<TreeState.TreeNodeState>)getProvidesKey();

    provider.setList(list);
    provider.refresh();
  }

  @Override
  public GwtTreeCell getCell() {
    return (GwtTreeCell)super.getCell();
  }

  public void init(GwtTreeImpl cellTree) {
    getCell().init(cellTree);
  }
}
