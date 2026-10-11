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
package consulo.ide.setting.module;

import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.ExportableOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
public class ClasspathTableItem<T extends OrderEntry> {
    @SuppressWarnings("unchecked")
    public static ClasspathTableItem<?> createItem(
        OrderEntry orderEntry,
        Project project,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        OrderEntryType<?> type = orderEntry.getType();

        OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
        if (editor != null) {
            return editor.createTableItem(orderEntry, project, modulesConfigurator, librariesConfigurator);
        }
        return new ClasspathTableItem<>(orderEntry);
    }

    protected final T myEntry;

    public ClasspathTableItem(T entry) {
        myEntry = entry;
    }

    public final boolean isExportable() {
        return myEntry instanceof ExportableOrderEntry;
    }

    public final boolean isExported() {
        return myEntry instanceof ExportableOrderEntry exportableOrderEntry && exportableOrderEntry.isExported();
    }

    public final void setExported(boolean isExported) {
        if (myEntry instanceof ExportableOrderEntry exportableOrderEntry) {
            exportableOrderEntry.setExported(isExported);
        }
    }

    public final @Nullable DependencyScope getScope() {
        return myEntry instanceof ExportableOrderEntry exportableOrderEntry ? exportableOrderEntry.getScope() : null;
    }

    public final void setScope(DependencyScope scope) {
        if (myEntry instanceof ExportableOrderEntry exportableOrderEntry) {
            exportableOrderEntry.setScope(scope);
        }
    }

    public final T getEntry() {
        return myEntry;
    }

    public boolean isRemovable() {
        return !myEntry.isSynthetic();
    }

    public boolean isEditable() {
        return false;
    }

    @RequiredUIAccess
    public CompletableFuture<?> doEdit(ClasspathPanel panel) {
        return CompletableFuture.completedFuture(null);
    }

    public LocalizeValue getTooltipText() {
        return LocalizeValue.empty();
    }
}
