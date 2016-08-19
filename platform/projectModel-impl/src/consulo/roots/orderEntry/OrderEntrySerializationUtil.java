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
package consulo.roots.orderEntry;

import consulo.roots.ModuleRootLayer;
import com.intellij.openapi.roots.OrderEntry;
import consulo.roots.impl.ModuleRootLayerImpl;
import consulo.roots.impl.UnknownOrderEntryImpl;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.lombok.annotations.Lazy;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class OrderEntrySerializationUtil {
  @NonNls
  public static final String ORDER_ENTRY_ELEMENT_NAME = "orderEntry";

  @NonNls
  public static final String ORDER_ENTRY_TYPE_ATTR = "type";

  @NotNull
  @Lazy
  public static Map<String, OrderEntryType> getProvidersAsMap() {
    return ContainerUtil.map2Map(OrderEntryType.EP_NAME.getExtensions(), new Function<OrderEntryType, Pair<String, OrderEntryType>>() {
      @Override
      public Pair<String, OrderEntryType> fun(OrderEntryType orderEntryType) {
        return new Pair<String, OrderEntryType>(orderEntryType.getId(), orderEntryType);
      }
    });
  }

  @Nullable
  public static OrderEntry loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) {
    String type = element.getAttributeValue(ORDER_ENTRY_TYPE_ATTR);
    if(type == null) {
      return null;
    }
    OrderEntryType orderEntryType = getProvidersAsMap().get(type);
    if(orderEntryType == null) {
      return new UnknownOrderEntryImpl(new UnknownOrderEntryType(type, element), (ModuleRootLayerImpl)moduleRootLayer);
    }

    try {
      return orderEntryType.loadOrderEntry(element, moduleRootLayer);
    }
    catch (InvalidDataException e) {
      return new UnknownOrderEntryImpl(new UnknownOrderEntryType(type, element), (ModuleRootLayerImpl)moduleRootLayer);
    }
  }

  @NotNull
  public static Element storeOrderEntry(@NotNull OrderEntry entry) {
    OrderEntryType provider = entry.getType();

    Element element = new Element(ORDER_ENTRY_ELEMENT_NAME);
    element.setAttribute(ORDER_ENTRY_TYPE_ATTR, provider.getId());

    //noinspection unchecked
    provider.storeOrderEntry(element, entry);
    return element;
  }
}
