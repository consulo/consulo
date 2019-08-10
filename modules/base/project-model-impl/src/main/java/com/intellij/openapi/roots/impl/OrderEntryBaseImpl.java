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

package com.intellij.openapi.roots.impl;

import consulo.logging.Logger;
import com.intellij.openapi.roots.OrderEntry;
import consulo.roots.impl.ModuleRootLayerImpl;
import javax.annotation.Nonnull;
import consulo.roots.orderEntry.OrderEntryType;

public abstract class OrderEntryBaseImpl extends BaseModuleRootLayerChild implements OrderEntry {
  public static final Logger LOGGER = Logger.getInstance(OrderEntryBaseImpl.class);

  private static int _hc = 0;

  @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
  private final int hc = _hc++;

  private int myIndex;
  private OrderEntryType<?> myType;

  protected OrderEntryBaseImpl(@Nonnull OrderEntryType<?> provider, @Nonnull ModuleRootLayerImpl rootLayer) {
    super(rootLayer);
    myType = provider;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  @Nonnull
  @Override
  public OrderEntryType<?> getType() {
    return myType;
  }

  @Override
  public int compareTo(@Nonnull OrderEntry orderEntry) {
    LOGGER.assertTrue(orderEntry.getOwnerModule() == getOwnerModule());
    return myIndex - ((OrderEntryBaseImpl)orderEntry).myIndex;
  }

  @Override
  @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
  public final boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public final int hashCode() {
    return hc;
  }

  @Nonnull
  @Override
  public String toString() {
    return getOwnerModule().getName() + " -> " + getPresentableName();
  }
}
