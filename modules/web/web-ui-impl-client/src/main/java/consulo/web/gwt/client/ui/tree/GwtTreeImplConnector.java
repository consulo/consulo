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

import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.GwtComponentSizeUpdater;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.tree.TreeClientRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeServerRpc;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebTreeImpl.Vaadin")
public class GwtTreeImplConnector extends AbstractComponentConnector {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForComponent(this);
  }

  @Override
  protected void init() {
    super.init();

    getTree().setChildrenOpenHandler(state -> getRpcProxy(TreeServerRpc.class).onOpen(state.myId));
    getTree().setDoubleClickHandler((state) -> getRpcProxy(TreeServerRpc.class).onDoubleClick(state.myId));
    getTree().getTreeViewModel().getSelectionModel().addSelectionChangeHandler(event -> {
      TreeNode value = getTree().getKeyboardSelectedTreeNode();
      TreeState.TreeNodeState childValue = (TreeState.TreeNodeState)value.getValue();
      getRpcProxy(TreeServerRpc.class).onSelected(childValue.myId);
    });
    getTree().setOnShow(() -> getRpcProxy(TreeServerRpc.class).onShow());
    getTree().addHandler(new ContextMenuHandler() {
      @Override
      public void onContextMenu(ContextMenuEvent event) {
        event.preventDefault();
        event.stopPropagation();

        getRpcProxy(TreeServerRpc.class).onContextMenu(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
      }
    }, ContextMenuEvent.getType());

    registerRpc(TreeClientRpc.class, new TreeClientRpc() {
      @Override
      public void expand(String nodeId) {
        getTree().expand(nodeId);
      }
    });
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    getTree().handleChanges(getState().myChanges);
  }

  @Override
  public TreeState getState() {
    return (TreeState)super.getState();
  }

  @Override
  public ScrollPanel getWidget() {
    return (ScrollPanel)super.getWidget();
  }

  @Nonnull
  private GwtTreeImpl getTree() {
    ScrollPanel widget = getWidget();
    return (GwtTreeImpl)widget.getWidget();
  }

  @Override
  protected ScrollPanel createWidget() {
    ScrollPanel panel = new ScrollPanel(new GwtTreeImpl());
    panel.addStyleName("ui-scroll-panel-marker");
    GwtUIUtil.fill(panel);
    return panel;
  }
}
