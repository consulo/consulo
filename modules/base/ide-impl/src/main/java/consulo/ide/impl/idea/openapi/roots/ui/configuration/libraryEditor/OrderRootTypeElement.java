/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;

/**
 * @author nik
 */
public class OrderRootTypeElement extends LibraryTableTreeContentElement<OrderRootTypeElement> {
  private final String myRootTypeId;

  public OrderRootTypeElement(NodeDescriptor rootElementDescriptor, String rootTypeId, String nodeText, Image icon) {
    super(rootElementDescriptor);
    myRootTypeId = rootTypeId;
    setIcon(icon);
    myName = nodeText;
  }

  public String getOrderRootType() {
    return myRootTypeId;
  }

  @Override
  public int hashCode() {
    return myRootTypeId.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OrderRootTypeElement && ((OrderRootTypeElement)obj).getOrderRootType().equals(myRootTypeId);
  }
}
