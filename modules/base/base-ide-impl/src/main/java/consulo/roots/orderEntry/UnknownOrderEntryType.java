/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.orderEntry;

import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.UnknownOrderEntryImpl;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Attribute;
import org.jdom.Element;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class UnknownOrderEntryType implements OrderEntryType<UnknownOrderEntryImpl> {
  private String myId;
  private Element myElement;

  public UnknownOrderEntryType(String id, Element element) {
    myId = id;
    myElement = element;
  }

  @Nonnull
  @Override
  public String getId() {
    return myId;
  }

  @Nonnull
  @Override
  public UnknownOrderEntryImpl loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    throw new IllegalArgumentException("this method ill never call");
  }

  @Override
  public void storeOrderEntry(@Nonnull Element element, @Nonnull UnknownOrderEntryImpl orderEntry) {
    List<Attribute> attributes = myElement.getAttributes();
    for (Attribute attribute : attributes) {
      element.setAttribute(attribute.getName(), attribute.getValue());
    }

    List<Element> children = myElement.getChildren();
    for (Element child : children) {
      element.addContent(child.clone());
    }
  }
}
