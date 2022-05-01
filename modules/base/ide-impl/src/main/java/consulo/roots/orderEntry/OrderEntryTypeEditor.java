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

import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.component.extension.KeyedFactoryEPBean;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.impl.internal.layer.orderEntry.UnknownOrderEntryType;
import consulo.project.Project;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.ide.impl.idea.openapi.roots.ui.CellAppearanceEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import consulo.ide.impl.idea.openapi.roots.ui.util.SimpleTextCellAppearance;
import consulo.application.extension.KeyedExtensionFactory;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
  ExtensionPointName<KeyedFactoryEPBean> EP_NAME = ExtensionPointName.create("consulo.orderEntryTypeEditor");

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
