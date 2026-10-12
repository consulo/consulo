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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Length;
import consulo.ui.RadioGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TileLayout;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

/**
 * Every style as a card with a picture of what it looks like. The choice applies at once.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class ThemeStartupCustomizeStep implements StartupCustomizeStep {
    private @Nullable Component myComponent;

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizeThemeTitle();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        StyleManager styleManager = StyleManager.get();

        RadioGroup<Style> group = RadioGroup.create();
        TileLayout tiles = TileLayout.create(Length.ofFont(17));
        for (Style style : styleManager.getStyles()) {
            tiles.add(StartupCustomizeUI.choiceCard(
                group,
                style,
                LocalizeValue.of(style.getName()),
                LocalizeValue.empty(),
                ImageBox.create(Style.LIGHT_ID.equals(style.getId()) ? PlatformIconGroup.lafsLightpreview() : PlatformIconGroup.lafsDarkpreview())
            ));
        }
        group.setValue(styleManager.getCurrentStyle(), false);
        group.addValueListener(style -> {
            if (style != null && !style.equals(styleManager.getCurrentStyle())) {
                styleManager.setCurrentStyle(style);
                UIAccess.current().give(styleManager::forceRepaintAll);
            }
        });

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizeThemeHeader(), ScrollableLayout.create(tiles));
        myComponent = component;
        return component;
    }
}
