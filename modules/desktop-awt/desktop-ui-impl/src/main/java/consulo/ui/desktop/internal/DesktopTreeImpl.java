/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.awt.impl.TargetAWTFacadeImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 14/07/2021
 */
public class DesktopTreeImpl<E> extends SwingComponentDelegate<DesktopTreeImpl.MyTree> implements Tree<E> {
  private static class MyTreeNodeImpl<K> implements TreeNode<K> {
    private boolean myLeaf;

    private final K myValue;

    private BiConsumer<K, TextItemPresentation> myRender = (e, t) -> t.append(e == null ? "null" : e.toString());

    private MyTreeNodeImpl(K value) {
      myValue = value;
    }

    @Override
    public void setRender(@Nonnull BiConsumer<K, TextItemPresentation> render) {
      myRender = render;
    }

    @Override
    public void setLeaf(boolean leaf) {
      myLeaf = leaf;
    }

    @Override
    public boolean isLeaf() {
      return myLeaf;
    }

    @Nullable
    @Override
    public K getValue() {
      return myValue;
    }
  }

  private static class MyNodeDescriptor<K> extends PresentableNodeDescriptor {

    private final Object myRootElement;
    private final Object myElement;

    protected MyNodeDescriptor(Object rootElement, Object element, @Nullable NodeDescriptor parentDescriptor) {
      super(null, parentDescriptor);
      myRootElement = rootElement;
      myElement = element;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void update(PresentationData presentation) {
      if (myElement == myRootElement) {
        return;
      }

      MyTreeNodeImpl<K> node = (MyTreeNodeImpl)myElement;

      BiConsumer<K, TextItemPresentation> render = node.myRender;

      render.accept(node.myValue, new TextItemPresentation() {
        @Override
        public void clearText() {
          presentation.clearText();
        }

        @Nonnull
        @Override
        public TextItemPresentation withIcon(@Nullable Image image) {
          presentation.setIcon(image);
          return this;
        }

        @Override
        public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
          presentation.addText(text.getValue(), TargetAWTFacadeImpl.from(textAttribute));
        }
      });
    }

    @Override
    public boolean isWasDeclaredAlwaysLeaf() {
      if (myElement instanceof MyTreeNodeImpl k) {
        return k.isLeaf();
      }
      return false;
    }

    @Override
    public Object getElement() {
      return myElement;
    }
  }

  public static class MyStructureWrapper<K> extends AbstractTreeStructure {
    private final Object myRootValue;
    private final TreeModel<K> myModel;

    public MyStructureWrapper(K rootValue, TreeModel<K> model) {
      myModel = model;
      myRootValue = rootValue == null ? ObjectUtil.NULL : rootValue;
    }

    @Nonnull
    @Override
    public Object getRootElement() {
      return myRootValue;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Object[] getChildElements(@Nonnull Object element) {
      K targetParent = null;
      if (element == myRootValue) {
        targetParent = null;
      }
      else if (element instanceof MyTreeNodeImpl node) {
        targetParent = (K)node.getValue();
      }

      List<MyTreeNodeImpl<K>> nodes = new ArrayList<>();
      myModel.buildChildren(k -> {
        MyTreeNodeImpl<K> node = new MyTreeNodeImpl<>(k);
        nodes.add(node);
        return node;
      }, targetParent);

      Comparator<TreeNode<K>> comparator = myModel.getNodeComparator();
      if (comparator != null) {
        nodes.sort(comparator);
      }
      return nodes.toArray(MyTreeNodeImpl[]::new);
    }

    @Nullable
    @Override
    public Object getParentElement(@Nonnull Object element) {
      return null;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public NodeDescriptor createDescriptor(@Nonnull Object element, @Nullable NodeDescriptor parentDescriptor) {
      return new MyNodeDescriptor(myRootValue, element, parentDescriptor);
    }

    @Override
    public void commit() {

    }

    @Override
    public boolean hasSomethingToCommit() {
      return false;
    }
  }

  public class MyTree extends DnDAwareTree implements FromSwingComponentWrapper {
    public MyTree(Disposable disposable, E rootValue, TreeModel<E> model) {
      super(new AsyncTreeModel(new StructureTreeModel<>(new MyStructureWrapper<>(rootValue, model), disposable), disposable));
    }


    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopTreeImpl.this;
    }
  }

  public DesktopTreeImpl(E rootValue, TreeModel<E> model) {
    initialize(new MyTree(this, rootValue, model));
    MyTree tree = toAWTComponent();
    tree.setRootVisible(false);
    tree.setCellRenderer(new NodeRenderer());
    tree.addTreeSelectionListener(e -> {
      TreePath path = TreeUtil.getSelectedPathIfOne(tree);
      if (path == null) {
        return;
      }

      Object object = TreeUtil.getLastUserObject(path);

      if (object instanceof MyNodeDescriptor node) {
        MyTreeNodeImpl element = (MyTreeNodeImpl)node.getElement();

        getListenerDispatcher(SelectListener.class).onSelected(element);
      }
    });
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public TreeNode<E> getSelectedNode() {
    TreePath path = TreeUtil.getSelectedPathIfOne(toAWTComponent());
    if (path == null) {
      return null;
    }

    Object object = TreeUtil.getLastUserObject(path);

    if (object instanceof MyNodeDescriptor node) {
      return (MyTreeNodeImpl)node.getElement();
    }

    return null;
  }

  @Override
  public void expand(@Nonnull TreeNode<E> node) {

  }
}
