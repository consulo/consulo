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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.ValueProvider;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebTreeImpl<NODE> extends VaadinComponentDelegate<WebTreeImpl.Vaadin> implements Tree<NODE> {


  public class Vaadin extends org.vaadin.tatu.Tree<WebTreeNodeImpl<NODE>> implements FromVaadinComponentWrapper {
    //private final List<TreeState.TreeChange> myChanges = new ArrayList<>();
    private final Map<String, WebTreeNodeImpl<NODE>> myNodeMap = new LinkedHashMap<>();

    private String mySelectedValue;

    private WebTreeNodeImpl<NODE> myRootNode;
    private TreeModel<NODE> myModel;

    public Vaadin() {
      super(ValueProvider.identity());
    }

    public void init(NODE rootValue, TreeModel<NODE> model) {
      myModel = model;

      myRootNode = new WebTreeNodeImpl<>(null, rootValue, myNodeMap);

      if (myModel.isNeedBuildChildrenBeforeOpen(myRootNode)) {
        fetchChildren(myRootNode, false);
      }

      updateData();

      setItemCaptionProvider(node -> {
        if (node instanceof WebTreeNodeImpl.NotLoaded) {
          return "loading...";
        }
        return Objects.toString(node.getValue());
      });

      addExpandListener(event -> {
        Collection<WebTreeNodeImpl<NODE>> items = event.getItems();

        for (WebTreeNodeImpl<NODE> item : items) {
          List<WebTreeNodeImpl<NODE>> children = item.getChildren();
          if (children.size() == 1 && children.get(0) instanceof WebTreeNodeImpl.NotLoaded) {
            // items not loaded
            queue(item);
          }
        }
      });
    }


    // TODO async tree
    private void queue(@Nonnull WebTreeNodeImpl<NODE> parent) {
      UI ui = UI.getCurrent();

      AppExecutorUtil.getAppExecutorService().execute(() -> {
        List<WebTreeNodeImpl<NODE>> children = parent.getChildren();
        if (children.size() == 1 && children.get(0) instanceof WebTreeNodeImpl.NotLoaded) {
          WebTreeNodeImpl<NODE> unloaded = children.get(0);

          children = fetchChildren(parent, true);

          ui.access(() -> {
            getDataProvider().refreshItem(parent, true);

            ui.push();

            myNodeMap.remove(unloaded.getId());
          });
        }
      });
    }

    private void updateData() {
      TreeDataProvider<WebTreeNodeImpl<NODE>> provider = new TreeDataProvider<>(
        new TreeData<WebTreeNodeImpl<NODE>>().addItems(List.of(myRootNode), WebTreeNodeImpl::getChildren)) {
        @Override
        public Object getId(WebTreeNodeImpl<NODE> item) {
          return item.getId();
        }
      };

      try {
        Field field = org.vaadin.tatu.Tree.class.getDeclaredField("treeGrid");
        field.setAccessible(true);
        TreeGrid o = (TreeGrid)field.get(this);
        o.setUniqueKeyDataGenerator("key", item -> {
          if (item instanceof WebTreeNodeImpl webTreeNode) {
            return webTreeNode.getId();
          }
          return item.toString();
        });
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      setDataProvider(provider);
      getDataCommunicator().getKeyMapper().setIdentifierGetter(WebTreeNodeImpl::getId);
    }


//
//    @Override
//    public void beforeClientResponse(boolean initial) {
//      super.beforeClientResponse(initial);
//
//      synchronized (myChanges) {
//        TreeState state = getState();
//        state.myChanges.clear();
//
//        state.myChanges.addAll(myChanges);
//
//        myChanges.clear();
//      }
//    }
//
//    @Nonnull
//    private TreeState.TreeNodeState convert(WebTreeNodeImpl<E> child) {
//      TreeState.TreeNodeState e = new TreeState.TreeNodeState();
//      e.myId = child.getId();
//      e.myLeaf = child.isLeaf();
//      e.myParentId = child.getParent() == null ? null : child.getId();
//
//      WebItemPresentationImpl presentation = new WebItemPresentationImpl();
//
//      child.getRender().accept(child.getValue(), presentation);
//      e.myItemSegments = presentation.getItem().myItemSegments;
//      e.myImageState = presentation.getItem().myImageState;
//      return e;
//    }

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
    public TreeNode<NODE> getSelectedNode() {
      if (mySelectedValue == null) {
        return null;
      }
      return myNodeMap.get(mySelectedValue);
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebTreeImpl.this;
    }
  }

  public WebTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model, Disposable disposable) {
    getVaadinComponent().init(rootValue, model);
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Nullable
  @Override
  public TreeNode<NODE> getSelectedNode() {
    return getVaadinComponent().getSelectedNode();
  }

  @Override
  public void expand(@Nonnull TreeNode<NODE> node) {
    //TODO getVaadinComponent().queue((WebTreeNodeImpl<NODE>)node, TreeState.TreeChangeType.SET);
  }
}
