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
package consulo.web.internal.ui;

import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 09-Sep-17
 */
public class WebTreeNodeImpl<N> implements TreeNode<N> {
  public static class NotLoaded<K> extends WebTreeNodeImpl<K> {
    public NotLoaded(@Nullable WebTreeNodeImpl<K> parent,
                     @Nullable K node,
                     Map<String, WebTreeNodeImpl<K>> stringWebTreeNodeMap) {
      super(parent, node, stringWebTreeNodeMap);
    }
  }

  private final WebTreeNodeImpl<N> myParent;
  private final String myId;

  private N myNode;

  private List<WebTreeNodeImpl<N>> myChildren = List.of();
  private BiConsumer<N, TextItemPresentation> myRender = (n, itemPresentation) -> itemPresentation.append(String.valueOf(n));
  private boolean myLeaf;

  public WebTreeNodeImpl(@Nullable WebTreeNodeImpl<N> parent, @Nullable N node, Map<String, WebTreeNodeImpl<N>> nodeMap) {
    myParent = parent;
    myNode = node;
    myId = parent == null ? "root" : UUID.randomUUID().toString();

    if (!(this instanceof NotLoaded)) {
      myChildren = List.of(new NotLoaded<>(this, null, nodeMap));
    }

    nodeMap.put(getId(), this);
  }

  public boolean isNotLoaded() {
    return myChildren.size() == 1 && myChildren.get(0) instanceof NotLoaded;
  }

  @Nullable
  public WebTreeNodeImpl<N> getParent() {
    return myParent;
  }

  @Nullable
  @Override
  public N getValue() {
    return myNode;
  }

  public String getId() {
    return myId;
  }

  public List<WebTreeNodeImpl<N>> getChildren() {
    return myChildren;
  }

  public void setChildren(List<WebTreeNodeImpl<N>> children) {
    myChildren = children;
  }

  @Override
  public void setRender(@Nonnull BiConsumer<N, TextItemPresentation> render) {
    myRender = render;
  }

  @Override
  public void setLeaf(boolean leaf) {
    myLeaf = leaf;
    if (leaf) {
      myChildren = List.of();
    }
  }

  @Override
  public boolean isLeaf() {
    return myLeaf;
  }

  public BiConsumer<N, TextItemPresentation> getRender() {
    return myRender;
  }

  public int getLevel() {
    // we start with -1, due we already has root node
    int level = -1;

    WebTreeNodeImpl parent = this;
    while ((parent = parent.getParent()) != null) {
      level++;
    }
    return level;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("WebTreeNodeImpl{");
    sb.append("myNode=").append(myNode);
    sb.append('}');
    return sb.toString();
  }
}
