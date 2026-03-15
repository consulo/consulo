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
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;


import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @see CustomOrderEntryTypeEditor
 * @since 2016-06-06
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
    ExtensionPointCacheKey<OrderEntryTypeEditor, Map<String, OrderEntryTypeEditor>> CACHE_KEY =
        ExtensionPointCacheKey.groupBy("OrderEntryTypeEditor", OrderEntryTypeEditor::getOrderTypeId);

    
    static OrderEntryTypeEditor getEditor(String id) {
        ExtensionPoint<OrderEntryTypeEditor> extensionPoint = Application.get().getExtensionPoint(OrderEntryTypeEditor.class);

        Map<String, OrderEntryTypeEditor> map = extensionPoint.getOrBuildCache(CACHE_KEY);
        OrderEntryTypeEditor editor = map.get(id);
        if (editor != null) {
            return editor;
        }

        return Objects.requireNonNull(map.get(""), "can't find unknown order entry type. Id: " + id);
    }

    
    String getOrderTypeId();

    
    default Consumer<ColoredTextContainer> getRender(T orderEntry) {
        return it -> it.append(orderEntry.getPresentableName());
    }

    
    default ClasspathTableItem<T> createTableItem(
        T orderEntry,
        Project project,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        return new ClasspathTableItem<>(orderEntry);
    }

    @RequiredUIAccess
    default void navigate(T orderEntry) {
        Project project = orderEntry.getOwnerModule().getProject();
        ShowSettingsUtil.getInstance()
            .showProjectStructureDialog(project, config -> config.selectOrderEntry(orderEntry.getOwnerModule(), orderEntry));
    }
}
