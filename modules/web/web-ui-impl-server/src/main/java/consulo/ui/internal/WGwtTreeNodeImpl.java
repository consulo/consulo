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

import java.util.List;
import java.util.UUID;

/**
 * @author VISTALL
 * @since 09-Sep-17
 */
public class WGwtTreeNodeImpl<N> {
  private final WGwtItemPresentationImpl myPresentation = new WGwtItemPresentationImpl();
  private String myParentId;
  private N myNode;
  private String myId = UUID.randomUUID().toString();

  private List<WGwtTreeNodeImpl<N>> myChildren;

  public WGwtTreeNodeImpl(String parentId, N node) {
    myParentId = parentId;
    myNode = node;
  }

  public String getParentId() {
    return myParentId;
  }

  public N getNode() {
    return myNode;
  }

  public String getId() {
    return myId;
  }

  public WGwtItemPresentationImpl getPresentation() {
    return myPresentation;
  }

  public List<WGwtTreeNodeImpl<N>> getChildren() {
    return myChildren;
  }

  public void setChildren(List<WGwtTreeNodeImpl<N>> children) {
    myChildren = children;
  }
}
