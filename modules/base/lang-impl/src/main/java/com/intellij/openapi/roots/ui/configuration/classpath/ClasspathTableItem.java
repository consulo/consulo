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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExportableOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import consulo.roots.orderEntry.OrderEntryType;
import consulo.roots.orderEntry.OrderEntryTypeEditor;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class ClasspathTableItem<T extends OrderEntry> {
  @Nonnull
  @SuppressWarnings("unchecked")
  public static ClasspathTableItem<?> createItem(@Nonnull OrderEntry orderEntry,
                                                 @Nonnull Project project,
                                                 @Nonnull ModulesConfigurator modulesConfigurator,
                                                 @Nonnull LibrariesConfigurator librariesConfigurator) {
    OrderEntryType<?> type = orderEntry.getType();

    OrderEntryTypeEditor editor = OrderEntryTypeEditor.FACTORY.getByKey(type);
    if (editor != null) {
      return editor.createTableItem(orderEntry, project, modulesConfigurator, librariesConfigurator);
    }
    return new ClasspathTableItem<>(orderEntry);
  }

  @Nonnull
  protected final T myEntry;

  public ClasspathTableItem(@Nonnull T entry) {
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

  @Nonnull
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
