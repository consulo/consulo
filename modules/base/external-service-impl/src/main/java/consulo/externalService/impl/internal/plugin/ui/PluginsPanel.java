/*
 * Copyright 2013-2020 consulo.io
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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginFromDiskAction;
import consulo.externalService.impl.internal.plugin.ui.action.PluginsOptionGroup;
import consulo.externalService.impl.internal.plugin.ui.action.ReloadAllAction;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-26
 */
public class PluginsPanel implements Disposable {
    public static final int FROM_REPOSITORY = 0;
    public static final int INSTALLED = 1;

    public static final Key<PluginsPanel> KEY = Key.create("PluginsPanel");

    private final JTabbedPane myTabbedPane;

    private InstalledPluginsTab myInstalledTab;

    private RepositoryPluginsTab myRepositoryTab;

    private final Map<PluginId, Set<PluginId>> myDependentToRequiredListMap = new HashMap<>();

    public PluginsPanel() {
        myTabbedPane = new JTabbedPane();

        myInstalledTab = new InstalledPluginsTab(this);
        Disposer.register(this, myInstalledTab);

        myRepositoryTab = new RepositoryPluginsTab(this);
        Disposer.register(this, myRepositoryTab);

        myInstalledTab.setInstalledTab(myInstalledTab);
        myInstalledTab.setAvailableTab(myRepositoryTab);

        myRepositoryTab.setInstalledTab(myInstalledTab);
        myRepositoryTab.setAvailableTab(myRepositoryTab);

        myTabbedPane.addTab("Repository", myRepositoryTab.getMainPanel());
        myTabbedPane.addTab("Installed", myInstalledTab.getMainPanel());

        int pluginsCount = PluginManager.getPluginsCount();
        // set default Repository tab if no plugins installed
        select(pluginsCount == 0 ? myRepositoryTab : myInstalledTab);

        DataManager.registerDataProvider(myTabbedPane, dataId -> {
            if (dataId == KEY) {
                return this;
            }

            return null;
        });

        PluginsOptionGroup group = new PluginsOptionGroup();
        group.add(new InstallPluginFromDiskAction(this));
        group.add(AnSeparator.create());
        group.add(new ReloadAllAction(myRepositoryTab, myInstalledTab));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(getClass().getSimpleName() + "Toolbar",
            ActionGroup.newImmutableBuilder().add(group).build(),
            true);
        toolbar.setTargetComponent(myTabbedPane);

        myTabbedPane.putClientProperty("JTabbedPane.trailingComponent", toolbar.getComponent());
    }

    public static boolean isDownloaded(@Nonnull PluginDescriptor node) {
        if (node instanceof PluginNode && ((PluginNode) node).getInstallStatus() == PluginNode.STATUS_DOWNLOADED) {
            return true;
        }
        final PluginId pluginId = node.getPluginId();
        if (PluginManager.findPlugin(pluginId) != null) {
            return false;
        }

        return InstalledPluginsState.getInstance().getInstalledPlugins().contains(pluginId);
    }

    public void setBannerComponent(JComponent bannerComponent) {
        String key = "JTabbedPane.leadingComponent";

        // we need reset it - due parent of component can be changed outside, and need override it
        myTabbedPane.putClientProperty(key, null);
        myTabbedPane.putClientProperty(key, bannerComponent);
    }

    public void filter(String option) {
        select(myInstalledTab);

        myInstalledTab.filter(option);
    }

    public void selectInstalled(PluginId pluginId) {
        myInstalledTab.select(pluginId);
    }

    public void selectAvailable(PluginId pluginId) {
        myRepositoryTab.select(pluginId);
    }

    public void select(PluginTab main) {
        myTabbedPane.setSelectedComponent(main.getMainPanel());
    }

    public int getSelectedIndex() {
        return myTabbedPane.getSelectedIndex();
    }

    @Nonnull
    public PluginTab getSelected() {
        int index = getSelectedIndex();

        if (index == FROM_REPOSITORY) {
            return myRepositoryTab;
        }

        return myInstalledTab;
    }

    public JComponent getComponent() {
        return myTabbedPane;
    }

    public InstalledPluginsTab getInstalledTab() {
        return myInstalledTab;
    }

    @Override
    public void dispose() {
    }

    public void reset() {
        myInstalledTab.reset();
        myRepositoryTab.reset();
    }

    public boolean isModified() {
        return myInstalledTab.isModified() || myRepositoryTab.isModified();
    }

    public Map<PluginId, Set<PluginId>> getDependentToRequiredListMap() {
        return myDependentToRequiredListMap;
    }

    public boolean hasProblematicDependencies(PluginId pluginId) {
        final Set<PluginId> ids = myDependentToRequiredListMap.get(pluginId);
        return ids != null && !ids.isEmpty();
    }

    @Nullable
    public Set<PluginId> getRequiredPlugins(PluginId pluginId) {
        return myDependentToRequiredListMap.get(pluginId);
    }

    @RequiredUIAccess
    public void checkInstalledPluginDependencies(PluginDescriptor pluginDescriptor) {
        final Set<PluginId> notInstalled = new HashSet<>();
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final PluginId[] optionalDependentPluginIds = pluginDescriptor.getOptionalDependentPluginIds();
        for (PluginId id : dependentPluginIds) {
            if (ArrayUtil.find(optionalDependentPluginIds, id) > -1) {
                continue;
            }
            notInstalled.add(id);
        }

        if (!notInstalled.isEmpty()) {
            Messages.showWarningDialog(
                "Plugin " + pluginDescriptor.getName() + " depends on unknown plugin" + (notInstalled.size() > 1 ? "s " : " ") + StringUtil.join(
                    notInstalled,
                    PluginId::toString,
                    ", "
                ),
                CommonLocalize.titleWarning().get()
            );
        }
    }

    public boolean appendOrUpdateDescriptor(PluginDescriptor descriptor) {
        final PluginId descrId = descriptor.getPluginId();
        final PluginDescriptor installed = PluginManager.findPlugin(descrId);
        InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

        if (installed != null) {
            pluginsState.updateExistingPlugin(descriptor, installed);
            return true;
        }
        else if (!pluginsState.getAllPlugins().contains(descriptor)) {
            pluginsState.getAllPlugins().add(descriptor);

            myInstalledTab.reload();
            return true;
        }
        return false;
    }

    public boolean isIncompatible(PluginDescriptor descriptor) {
        PluginDescriptorStatus status = descriptor.getStatus();
        if (status != PluginDescriptorStatus.OK && status != PluginDescriptorStatus.DISABLED_BY_USER) {
            return true;
        }
        return PluginValidator.isIncompatible(descriptor) || hasProblematicDependencies(descriptor.getPluginId());
    }

    public void apply() throws ConfigurationException {
        String applyMessage = myInstalledTab.apply();
        myRepositoryTab.apply();

        if (applyMessage != null) {
            throw new ConfigurationException(applyMessage);
        }

        if (myInstalledTab.isRequireShutdown()) {
            final Application app = Application.get();

            int response = app.isRestartCapable() ? PluginInstallUtil.showRestartIDEADialog() : PluginInstallUtil.showShutDownIDEADialog();
            if (response == Messages.YES) {
                app.restart(true);
            }
            else {
                myInstalledTab.ignoreChanges();
            }
        }
    }
}
