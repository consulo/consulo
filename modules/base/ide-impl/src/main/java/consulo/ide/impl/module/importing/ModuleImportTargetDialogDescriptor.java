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
package consulo.ide.impl.module.importing;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.layout.LabeledLayout;

import java.util.List;

/**
 * Asks which of the providers able to read the chosen file should read it.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public class ModuleImportTargetDialogDescriptor extends DialogDescriptor {
    private final List<ModuleImportProvider> myProviders;

    private ComboBox<ModuleImportProvider> myBox;

    public ModuleImportTargetDialogDescriptor(List<ModuleImportProvider> providers) {
        super(LocalizeValue.localizeTODO("Import Target"));

        myProviders = providers;
    }

    public ModuleImportProvider getProvider() {
        return myBox.getValue();
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        myBox = ComboBox.create(myProviders);
        myBox.setRender((renderer, renderItem) -> {
            ModuleImportProvider item = renderItem.getValue();
            if (item == null) {
                return;
            }

            renderer.withIcon(item.getIcon());
            renderer.append(item.getName());
        });
        myBox.setValueByIndex(0);

        return LabeledLayout.create(LocalizeValue.localizeTODO("Select import target"), myBox);
    }
}
