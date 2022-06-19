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
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Root types that can be queried from OrderEntry.
 *
 * @author dsl
 * @see OrderEntry
 */
@Extension(ComponentScope.APPLICATION)
public class OrderRootType {
  private static final ExtensionPointName<OrderRootType> EP_NAME = ExtensionPointName.create(OrderRootType.class);

  private static Supplier<List<OrderRootType>> ourSortExtensions = LazyValue.notNull(() -> {
    List<OrderRootType> extensions = new ArrayList<>(EP_NAME.getExtensionList());
    Collections.sort(extensions, (o1, o2) -> o1.getName().compareTo(o2.getName()));
    return Collections.unmodifiableList(extensions);
  });

  private final String myName;

  protected OrderRootType(@NonNls String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  @Deprecated
  @DeprecationInfo(value = "Use getName()")
  public String name() {
    return getName();
  }

  public boolean isMe(@Nonnull String type) {
    return type.equals(getName());
  }

  @Nonnull
  public static List<OrderRootType> getAllTypes() {
    return EP_NAME.getExtensionList();
  }

  @Nonnull
  public static List<OrderRootType> getSortedRootTypes() {
    return ourSortExtensions.get();
  }

  @Nonnull
  public static <T extends OrderRootType> T getOrderRootType(final Class<? extends T> orderRootTypeClass) {
    return EP_NAME.findExtensionOrFail(orderRootTypeClass);
  }

  @Override
  public String toString() {
    return "Root " + getName();
  }
}
