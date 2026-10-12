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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.externalService.internal.PluginIconHolder;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Length;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TileLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Template plugins - plugins tagged as a set, which only depend on the plugins making up a kind of work - shown as cards. A
 * set chosen here puts itself and everything it needs into the plugins to install.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class PluginSetStartupCustomizeStep implements StartupCustomizeStep {
    private static final String TEMPLATE_TAG = "template.plugin";
    private static final int ICON_SIZE = 64;

    private final StartupCustomizePlugins myPlugins;
    private final List<PluginDescriptor> myTemplates;

    private final Map<PluginId, CheckBox> myCheckBoxes = new LinkedHashMap<>();
    private final Set<PluginId> myAdded = new LinkedHashSet<>();
    private @Nullable Component myComponent;

    public PluginSetStartupCustomizeStep(StartupCustomizeContext context) {
        myPlugins = new StartupCustomizePlugins(context.getRepositoryPlugins());
        myTemplates = context.getRepositoryPlugins()
            .stream()
            .filter(descriptor -> descriptor.getTags().contains(TEMPLATE_TAG))
            .sorted(Comparator.comparingInt(PluginDescriptor::getDownloads).reversed())
            .toList();
    }

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizePluginSetsTitle();
    }

    @Override
    public boolean isVisible(StartupCustomizeContext context) {
        return !myTemplates.isEmpty();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        TileLayout tiles = TileLayout.create(Length.ofFont(16));
        for (PluginDescriptor template : myTemplates) {
            CheckBox checkBox = CheckBox.create(LocalizeValue.empty());
            myCheckBoxes.put(template.getPluginId(), checkBox);

            Card card = Card.create();
            card.setMedia(ImageBox.create(ImageEffects.resize(PluginIconHolder.initializeImage(template), ICON_SIZE)));
            card.setHeaderPrefix(checkBox);
            card.setTitle(LocalizeValue.of(template.getName()));
            card.setSubtitle(LocalizeValue.of(StringUtil.notNullize(template.getDescription()).trim().replaceAll("\\s+", " ")));
            card.addClickListener(event -> checkBox.setValue(!checkBox.getValueOrError()));
            tiles.add(card);
        }

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizePluginSetsHeader(), ScrollableLayout.create(tiles));
        myComponent = component;
        return component;
    }

    @Override
    public void onStepLeave(StartupCustomizeContext context) {
        Set<PluginId> toInstall = context.getPluginsToInstall();
        toInstall.removeAll(myAdded);
        myAdded.clear();

        for (Map.Entry<PluginId, CheckBox> entry : myCheckBoxes.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue().getValue())) {
                continue;
            }

            myAdded.addAll(myPlugins.withDependencies(entry.getKey()));
        }

        toInstall.addAll(myAdded);
    }
}
