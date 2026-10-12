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
package consulo.externalService.impl.internal.startup;

import consulo.disposer.Disposable;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.layout.TileLayout;
import org.jspecify.annotations.Nullable;

/**
 * The two macOS keymaps, the active one chosen. The choice applies at once.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class KeymapStartupCustomizeStep implements StartupCustomizeStep {
    private static final String SHOW_PROPERTY = "consulo.startup.customize.keymap";

    private @Nullable Component myComponent;

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizeKeymapTitle();
    }

    @Override
    public boolean isVisible(StartupCustomizeContext context) {
        return Platform.current().os().isMac() || Boolean.getBoolean(SHOW_PROPERTY);
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        KeymapManager keymapManager = KeymapManager.getInstance();

        RadioGroup<Keymap> group = RadioGroup.create();
        TileLayout tiles = TileLayout.create(Length.ofFont(14));
        for (String name : new String[]{KeymapManager.MAC_OS_X_10_5_PLUS_KEYMAP, KeymapManager.MAC_OS_X_KEYMAP}) {
            Keymap keymap = keymapManager.getKeymap(name);
            if (keymap != null) {
                tiles.add(StartupCustomizeUI.choiceCard(group, keymap, LocalizeValue.of(keymap.getPresentableName()), LocalizeValue.empty(), null));
            }
        }
        group.setValue(keymapManager.getActiveKeymap(), false);
        group.addValueListener(keymap -> {
            if (keymap != null) {
                keymapManager.setActiveKeymap(keymap);
            }
        });

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizeKeymapHeader(), tiles);
        myComponent = component;
        return component;
    }
}
