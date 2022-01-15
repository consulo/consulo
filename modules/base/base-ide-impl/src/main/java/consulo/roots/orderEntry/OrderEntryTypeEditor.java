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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.KeyedFactoryEPBean;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.openapi.util.KeyedExtensionFactory;
import com.intellij.ui.SimpleTextAttributes;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
  ExtensionPointName<KeyedFactoryEPBean> EP_NAME = ExtensionPointName.create("com.intellij.orderEntryTypeEditor");

  KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType> FACTORY = new KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType>(OrderEntryTypeEditor.class, EP_NAME, Application.get()) {
    @Override
    public OrderEntryTypeEditor getByKey(@Nonnull OrderEntryType key) {
      // special hack for unknown order entry type
      if (key instanceof UnknownOrderEntryType) {
        return new UnknownOrderEntryTypeEditor();
      }
      return super.getByKey(key);
    }

    @Override
    public String getKey(@Nonnull final OrderEntryType key) {
      return key.getId();
    }
  };

  @Nonnull
  default CellAppearanceEx getCellAppearance(@Nonnull T orderEntry) {
    return new SimpleTextCellAppearance(orderEntry.getPresentableName(), null, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @Nonnull
  default ClasspathTableItem<T> createTableItem(@Nonnull T orderEntry,
                                                @Nonnull Project project,
                                                @Nonnull ModulesConfigurator modulesConfigurator,
                                                @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new ClasspathTableItem<>(orderEntry);
  }

  @RequiredUIAccess
  default void navigate(@Nonnull final T orderEntry) {
    Project project = orderEntry.getOwnerModule().getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.selectOrderEntry(orderEntry.getOwnerModule(), orderEntry));
  }
}
