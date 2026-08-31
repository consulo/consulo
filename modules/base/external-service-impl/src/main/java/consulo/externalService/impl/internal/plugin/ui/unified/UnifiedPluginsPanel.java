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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.impl.internal.plugin.ui.action.PluginsOptionGroup;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.layout.TabbedLayout;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginsPanel implements Disposable {
    private final TabbedLayout myTabbedLayout;

    private final UnifiedRepositoryPluginsTab myRepositoryTab;
    private final UnifiedInstalledPluginsTab myInstalledTab;

    private final Tab myInstalledTabItem;

    @RequiredUIAccess
    public UnifiedPluginsPanel() {
        myTabbedLayout = TabbedLayout.create();

        Label title = Label.create(ExternalServiceLocalize.titlePlugins());
        title.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 8);
        title.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 8);
        myTabbedLayout.setPrefixComponent(title);

        myRepositoryTab = new UnifiedRepositoryPluginsTab(this);
        Disposer.register(this, myRepositoryTab);

        myInstalledTab = new UnifiedInstalledPluginsTab();
        Disposer.register(this, myInstalledTab);

        myTabbedLayout.addTab(LocalizeValue.localizeTODO("Repository").get(), myRepositoryTab.getComponent());
        myInstalledTabItem = myTabbedLayout.addTab(LocalizeValue.localizeTODO("Installed").get(), myInstalledTab.getComponent());

        // a plugin installed from the repository tab is only in the state of the session, so the installed list
        // is rebuilt as it is shown rather than left as it was when the dialog opened
        myTabbedLayout.addSelectListener(event -> {
            if (event.getTab() == myInstalledTabItem) {
                myInstalledTab.reload();
            }
        });

        PluginsOptionGroup optionGroup = new PluginsOptionGroup();
        optionGroup.add(new UnifiedInstallPluginFromDiskAction(this));
        optionGroup.add(AnSeparator.create());
        optionGroup.add(new UnifiedReloadAllAction(myRepositoryTab, myInstalledTab));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
            "UnifiedPluginsPanelToolbar",
            ActionGroup.newImmutableBuilder().add(optionGroup).build(),
            true
        );
        toolbar.setTargetUIComponent(myTabbedLayout);
        myTabbedLayout.setSuffixComponent(toolbar.getUIComponent());

        myRepositoryTab.reload();
    }

    public Component getComponent() {
        return myTabbedLayout;
    }

    @RequiredUIAccess
    public void selectInstalled(PluginId pluginId) {
        myInstalledTabItem.select();
        myInstalledTab.select(pluginId);
    }

    @Override
    public void dispose() {
    }
}
