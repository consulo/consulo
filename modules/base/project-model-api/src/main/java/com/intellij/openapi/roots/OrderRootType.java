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
package com.intellij.openapi.roots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.annotation.DeprecationInfo;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Root types that can be queried from OrderEntry.
 *
 * @author dsl
 * @see OrderEntry
 */
public class OrderRootType {
  private static final ExtensionPointName<OrderRootType> EP_NAME = ExtensionPointName.create("com.intellij.orderRootType");

  private static NotNullLazyValue<List<OrderRootType>> ourSortExtensions = NotNullLazyValue.createValue(() -> {
    List<OrderRootType> extensions = new ArrayList<>(EP_NAME.getExtensionList());
    Collections.sort(extensions, (o1, o2) -> o1.getName().compareTo(o2.getName()));
    return Collections.unmodifiableList(extensions);
  });

  /**
   * Binaries without output directories for modules.
   * Includes:
   * <li>  binaries roots for libraries and sdk
   * <li>  recursively for module dependencies: only exported items
   */
  @Deprecated
  @DeprecationInfo(value = "Use BinariesOrderRootType#getInstance()", until = "2.0")
  public static final OrderRootType BINARIES = BinariesOrderRootType.getInstance();

  /**
   * Classpath without output directories for modules.
   * Includes:
   * <li>  classes roots for libraries and sdk
   * <li>  recursively for module dependencies: only exported items
   */
  @Deprecated
  @DeprecationInfo(value = "Use BinariesOrderRootType#getInstance()", until = "2.0")
  public static final OrderRootType CLASSES = BinariesOrderRootType.getInstance();

  /**
   * Sources.
   * Includes:
   * <li>  production and test source roots for modules
   * <li>  source roots for libraries and sdk
   * <li>  recursively for module dependencies: only exported items
   */
  @Deprecated
  @DeprecationInfo(value = "Use SourcesOrderRootType#getInstance()", until = "2.0")
  public static final OrderRootType SOURCES = SourcesOrderRootType.getInstance();

  /**
   * Documentation.
   * Generic documentation order root type
   */
  @Deprecated
  @DeprecationInfo(value = "Use DocumentationOrderRootType#getInstance()", until = "2.0")
  public static final OrderRootType DOCUMENTATION = DocumentationOrderRootType.getInstance();

  private final String myName;

  protected OrderRootType(@NonNls String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  @Deprecated
  @DeprecationInfo(value = "Use getName()", until = "3.0")
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
    return ourSortExtensions.getValue();
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
