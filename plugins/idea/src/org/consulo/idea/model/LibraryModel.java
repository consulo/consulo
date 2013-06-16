/*
 * Copyright 2013 Consulo.org
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
package org.consulo.idea.model;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.util.containers.MultiMap;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 9:47/16.06.13
 */
public class LibraryModel {
  private final MultiMap<OrderRootType, String> myOrderRoots = new MultiMap<OrderRootType, String>();
  private String myName;

  public LibraryModel() {
  }

  public void load(Element element) {
    final Element libraryElement = element.getChild("library");

    myName = libraryElement.getAttributeValue("name");
    for(Element libraryEntry : libraryElement.getChildren()) {
      final String libraryEntryName = libraryEntry.getName();

      for(OrderRootType orderRootType : OrderRootType.getAllTypes()) {
        if(orderRootType.name().equals(libraryEntryName)) {
          parse(element, orderRootType);
        }
      }
    }
  }

  private void parse(Element element, OrderRootType orderRootType) {
    for(Element child : element.getChildren()) {
      final String name = child.getName();
      if("root".equals(name)) {
        final String url = child.getAttributeValue("url");

        myOrderRoots.putValue(orderRootType, url);
      }
    }
  }

  public MultiMap<OrderRootType, String> getOrderRoots() {
    return myOrderRoots;
  }

  public String getName() {
    return myName;
  }
}
