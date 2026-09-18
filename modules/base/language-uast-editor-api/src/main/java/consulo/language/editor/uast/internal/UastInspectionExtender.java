/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.uast.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.uast.UastLocalInspectionTool;
import consulo.language.uast.UastLanguagePlugin;

import java.util.function.Consumer;

/**
 * Fans every {@link UastLocalInspectionTool} out into one {@link UastInspectionAdapter} per registered
 * {@link UastLanguagePlugin}, so that a single UAST inspection is registered as a regular
 * {@link InspectionTool} for every language that has a UAST binding.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
@ExtensionImpl
public class UastInspectionExtender implements ExtensionExtender<InspectionTool> {
    @Override
    public void extend(ComponentManager componentManager, Consumer<InspectionTool> consumer) {
        for (UastLanguagePlugin plugin : componentManager.getExtensionList(UastLanguagePlugin.class)) {
            for (UastLocalInspectionTool tool : componentManager.getExtensionList(UastLocalInspectionTool.class)) {
                consumer.accept(new UastInspectionAdapter(tool, plugin));
            }
        }
    }

    @Override
    public Class<InspectionTool> getExtensionClass() {
        return InspectionTool.class;
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return componentManager.getExtensionPoint(UastLocalInspectionTool.class).hasAnyExtensions();
    }
}
