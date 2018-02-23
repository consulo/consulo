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
package consulo.ui.internal;

import consulo.ui.ItemPresentation;
import consulo.ui.TreeNode;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 09-Sep-17
 */
public class WGwtTreeNodeImpl<N> implements TreeNode<N> {
  private final WGwtTreeNodeImpl<N> myParent;
  private final String myId;

  private N myNode;

  private List<WGwtTreeNodeImpl<N>> myChildren;
  private BiConsumer<N, ItemPresentation> myRender = (n, itemPresentation) -> itemPresentation.append(String.valueOf(n));
  private boolean myLeaf;

  public WGwtTreeNodeImpl(@Nullable WGwtTreeNodeImpl<N> parent, @Nullable N node, Map<String, WGwtTreeNodeImpl<N>> nodeMap) {
    myParent = parent;
    myNode = node;
    myId = parent == null ? "root" : UUID.randomUUID().toString();

    nodeMap.put(getId(), this);
  }

  @Nullable
  public WGwtTreeNodeImpl<N> getParent() {
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

  public List<WGwtTreeNodeImpl<N>> getChildren() {
    return myChildren;
  }

  public void setChildren(List<WGwtTreeNodeImpl<N>> children) {
    myChildren = children;
  }

  @Override
  public void setRender(@Nonnull BiConsumer<N, ItemPresentation> render) {
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

  public BiConsumer<N, ItemPresentation> getRender() {
    return myRender;
  }
}
