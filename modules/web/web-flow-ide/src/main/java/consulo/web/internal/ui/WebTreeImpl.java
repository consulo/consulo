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
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
@SuppressWarnings("unchecked")
public class WebTreeImpl<NODE> extends VaadinComponentDelegate<WebTreeImpl.Vaadin> implements Tree<NODE> {
  @Tag("vaadin-grid-tree-toggle")
  public static class VaadinGridTreeToggle extends com.vaadin.flow.component.Component implements HasComponents, ClickNotifier<VaadinGridTreeToggle> {
  }

  public class Vaadin extends TreeGrid<WebTreeNodeImpl<NODE>> implements FromVaadinComponentWrapper {
    private final Map<String, WebTreeNodeImpl<NODE>> myNodeMap = new LinkedHashMap<>();

    private WebTreeNodeImpl<NODE> myRootNode;
    private TreeModel<NODE> myModel;

    public Vaadin() {
      addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COLUMN_BORDERS);

      addComponentColumn(node -> {
        WebItemPresentationImpl item = new WebItemPresentationImpl();
        if (node instanceof WebTreeNodeImpl.NotLoaded) {
          item.append("Loading...");
        }
        else {
          node.getRender().accept(node.getValue(), item);
        }
        VaadinGridTreeToggle toggle = new VaadinGridTreeToggle();
        toggle.getElement().setAttribute("leaf", node.isLeaf());
        toggle.getElement().setAttribute("level", String.valueOf(node.getLevel()));

        toggle.addClickListener(event -> {
          if (getDataCommunicator().hasChildren(node)) {
            if (isExpanded(node)) {
              collapse(List.of(node), true);
            }
            else {
              expand(List.of(node), true);
            }
          }
        });
        toggle.add(item.toComponent());
        return toggle;
      }).setAutoWidth(true).setFlexGrow(1);
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

    private void queue(@Nonnull WebTreeNodeImpl<NODE> parent, UI ui) {
      invokeLater(() -> {
        List<WebTreeNodeImpl<NODE>> children = parent.getChildren();
        if (parent.isNotLoaded()) {
          WebTreeNodeImpl<NODE> unloaded = children.get(0);

          children = fetchChildren(parent, false);

          final List<WebTreeNodeImpl<NODE>> finalChildren = children;
          ui.access(() -> {
            TreeData<WebTreeNodeImpl<NODE>> data = getTreeData();

            data.removeItem(unloaded);

            data.addItems(parent, finalChildren);

            // add raw children
            for (WebTreeNodeImpl<NODE> finalChild : finalChildren) {
              data.addItems(finalChild, finalChild.getChildren());
            }

            myNodeMap.remove(unloaded.getId());

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

    @Nonnull
    private List<WebTreeNodeImpl<NODE>> fetchChildren(@Nonnull WebTreeNodeImpl<NODE> parent, boolean fetchNext) {
      List<WebTreeNodeImpl<NODE>> list = new ArrayList<>();

      myModel.buildChildren(node -> {
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

    @Nullable
    @Override
    public Component toUIComponent() {
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

      getListenerDispatcher(SelectListener.class).onSelected(value);
    });
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Nullable
  @Override
  public TreeNode<NODE> getSelectedNode() {
    Set selectedItems = toVaadinComponent().getSelectedItems();
    return (TreeNode<NODE>)ContainerUtil.getFirstItem(selectedItems);
  }

  @Override
  public void expand(@Nonnull TreeNode<NODE> node) {
    toVaadinComponent().expand(node);
  }
}
