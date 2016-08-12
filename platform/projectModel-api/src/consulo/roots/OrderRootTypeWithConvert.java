/*
 * Copyright 2013-2014 must-be.org
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
package consulo.roots;

import com.intellij.openapi.roots.OrderRootType;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17.08.14
 */
public class OrderRootTypeWithConvert extends OrderRootType {
  private String[] myOldNames;

  public OrderRootTypeWithConvert(@NonNls String name, String... oldNames) {
    super(name);
    myOldNames = oldNames;
    assert oldNames.length != 0;
  }

  public static Element findFirstElement(Element parent, OrderRootType orderRootType) {
    Element child = parent.getChild(orderRootType.getName());
    if(child != null) {
      return child;
    }
    if(orderRootType instanceof OrderRootTypeWithConvert) {
      for (String oldName : ((OrderRootTypeWithConvert)orderRootType).getOldNames()) {
        child = parent.getChild(oldName);
        if(child != null) {
          return child;
        }
      }
    }
    return null;
  }

  @Override
  public boolean isMe(@NotNull String type) {
    if(super.isMe(type)) {
      return true;
    }

    for (String oldName : myOldNames) {
      if(oldName.equals(type)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public String[] getOldNames() {
    return myOldNames;
  }
}
