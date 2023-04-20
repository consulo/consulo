/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.content;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root types that can be queried from OrderEntry.
 *
 * @author dsl
 * @see OrderEntry
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public class OrderRootType {
  private static final ExtensionPointCacheKey<OrderRootType, List<OrderRootType>> SORTED_KEY = ExtensionPointCacheKey.create("SortedOrderRootType", walker -> {
    List<OrderRootType> extensions = new ArrayList<>();
    walker.walk(extensions::add);
    Collections.sort(extensions, (o1, o2) -> o1.getId().compareToIgnoreCase(o2.getId()));
    return List.copyOf(extensions);
  });

  private final String myId;

  protected OrderRootType(String id) {
    myId = id;
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  @Deprecated
  @DeprecationInfo(value = "Use getId()")
  public String name() {
    return getId();
  }

  public boolean isMe(@Nonnull String type) {
    return type.equals(getId());
  }

  @Nonnull
  public static List<OrderRootType> getAllTypes() {
    return Application.get().getExtensionPoint(OrderRootType.class).getExtensionList();
  }

  @Nonnull
  public static List<OrderRootType> getSortedRootTypes() {
    return Application.get().getExtensionPoint(OrderRootType.class).getOrBuildCache(SORTED_KEY);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use ExtensionInstance#current()")
  public static <T extends OrderRootType> T getOrderRootType(final Class<? extends T> orderRootTypeClass) {
    return Application.get().getExtensionPoint(OrderRootType.class).findExtensionOrFail(orderRootTypeClass);
  }

  @Override
  public String toString() {
    return "Root " + getId();
  }
}
