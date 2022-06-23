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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @see CustomOrderEntryTypeEditor
 * @since 06-Jun-16
 */
@Extension(ComponentScope.APPLICATION)
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
  ExtensionPointCacheKey<OrderEntryTypeEditor, Map<String, OrderEntryTypeEditor>> CACHE_KEY = ExtensionPointCacheKey.create("OrderEntryTypeEditor", orderEntryTypeEditors -> {
    Map<String, OrderEntryTypeEditor> map = new HashMap<>();
    for (OrderEntryTypeEditor editor : orderEntryTypeEditors) {
      map.put(editor.getOrderTypeId(), editor);
    }
    return map;
  });

  @Nonnull
  static OrderEntryTypeEditor getEditor(String id) {
    ExtensionPoint<OrderEntryTypeEditor> extensionPoint = Application.get().getExtensionPoint(OrderEntryTypeEditor.class);

    Map<String, OrderEntryTypeEditor> map = extensionPoint.getOrBuildCache(CACHE_KEY);
    OrderEntryTypeEditor editor = map.get(id);
    if (editor != null) {
      return editor;
    }

    return Objects.requireNonNull(map.get(""), "can't find unknown order entry type");
  }

  @Nonnull
  String getOrderTypeId();

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
