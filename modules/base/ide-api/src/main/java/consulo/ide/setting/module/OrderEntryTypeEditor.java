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
package consulo.ide.setting.module;

import consulo.application.Application;
import consulo.application.extension.KeyedExtensionFactory;
import consulo.component.extension.ExtensionPointName;
import consulo.component.extension.KeyedFactoryEPBean;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
  ExtensionPointName<KeyedFactoryEPBean> EP_NAME = ExtensionPointName.create("consulo.orderEntryTypeEditor");

  KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType> FACTORY = new KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType>(OrderEntryTypeEditor.class, EP_NAME, Application.get()) {
    @Override
    public OrderEntryTypeEditor getByKey(@Nullable OrderEntryType key) {
      OrderEntryTypeEditor editor = super.getByKey(key);
      if (editor == null) {
        return super.getByKey(null);
      }
      return editor;
    }

    @Override
    @Nonnull
    public String getKey(@Nullable final OrderEntryType key) {
      return key == null ? "" : key.getId();
    }
  };

  @Nonnull
  default Consumer<ColoredTextContainer> getRender(@Nonnull T orderEntry) {
    return it -> it.append(orderEntry.getPresentableName());
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
