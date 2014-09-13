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
package com.intellij.openapi.roots.ui.configuration.classpath;

import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.OrderEntryTypeProvider;
import org.mustbe.consulo.roots.impl.OrderEntryTypeProviderEx;

/**
 * @author nik
 */
public class ClasspathTableItem<T extends OrderEntry> {
  @NotNull
  @SuppressWarnings("unchecked")
  public static ClasspathTableItem<?> createItem(OrderEntry orderEntry, StructureConfigurableContext context) {
    OrderEntryTypeProvider<?> provider = orderEntry.getProvider();
    if(provider instanceof OrderEntryTypeProviderEx) {
      return ((OrderEntryTypeProviderEx)provider).createTableItem(orderEntry, context);
    }
    return new ClasspathTableItem<OrderEntry>(orderEntry);
  }

  @NotNull
  protected final T myEntry;

  public ClasspathTableItem(@NotNull T entry) {
    myEntry = entry;
  }

  public final boolean isExportable() {
    return myEntry instanceof ExportableOrderEntry;
  }

  public final boolean isExported() {
    return myEntry instanceof ExportableOrderEntry && ((ExportableOrderEntry)myEntry).isExported();
  }

  public final void setExported(boolean isExported) {
    if (myEntry instanceof ExportableOrderEntry) {
      ((ExportableOrderEntry)myEntry).setExported(isExported);
    }
  }

  @Nullable
  public final DependencyScope getScope() {
    return myEntry instanceof ExportableOrderEntry ? ((ExportableOrderEntry)myEntry).getScope() : null;
  }

  public final void setScope(DependencyScope scope) {
    if (myEntry instanceof ExportableOrderEntry) {
      ((ExportableOrderEntry)myEntry).setScope(scope);
    }
  }

  @NotNull
  public final T getEntry() {
    return myEntry;
  }

  public boolean isRemovable() {
    return !myEntry.isSynthetic();
  }

  public boolean isEditable() {
    return false;
  }

  public void doEdit(ClasspathPanelImpl panel) {

  }

  @Nullable
  public String getTooltipText() {
    return null;
  }
}
