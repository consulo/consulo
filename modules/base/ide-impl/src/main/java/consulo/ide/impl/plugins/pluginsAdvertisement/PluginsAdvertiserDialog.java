/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.plugins.pluginsAdvertisement;

import consulo.application.Application;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.idea.ide.plugins.InstallPluginAction;
import consulo.ide.impl.idea.ide.plugins.PluginManagerMain;
import consulo.ide.impl.idea.ide.plugins.PluginTable;
import consulo.ide.impl.idea.ide.plugins.pluginsAdvertisement.PluginAdvertiserRequester;
import consulo.ide.impl.plugins.PluginDescriptionPanel;
import consulo.project.Project;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22-Jun-17
 */
public class PluginsAdvertiserDialog extends WholeWestDialogWrapper {
    @Nullable
    private final Project myProject;
    private final List<PluginDescriptor> myToInstallPlugins;
    @Nonnull
    private final List<PluginDescriptor> myAllPlugins;
    private boolean myUserAccepted;
    private final Map<PluginId, Boolean> myDownloadState;

    public PluginsAdvertiserDialog(
        @Nullable Project project,
        @Nonnull List<PluginDescriptor> allPlugins,
        @Nonnull List<PluginDescriptor> toInstallPlugins
    ) {
        super(project);
        myAllPlugins = allPlugins;
        myDownloadState = new HashMap<>(toInstallPlugins.size());

        for (PluginDescriptor pluginDescriptor : toInstallPlugins) {
            myDownloadState.put(pluginDescriptor.getPluginId(), Boolean.TRUE);
        }

        myProject = project;
        myToInstallPlugins = toInstallPlugins;
        setTitle("Choose Plugins to Install");
        init();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
        PluginAdvertiserPluginModel model = new PluginAdvertiserPluginModel(myDownloadState, myToInstallPlugins);
        model.addTableModelListener(e -> setOKActionEnabled(myDownloadState.values().stream().filter(it -> it).findAny().isPresent()));
        PluginTable pluginTable = new PluginTable(model);

        PluginDescriptionPanel descriptionPanel = new PluginDescriptionPanel();

        pluginTable.getSelectionModel().addListSelectionListener(e -> {
            final int selectedRow = pluginTable.getSelectedRow();
            if (selectedRow != -1) {
                final PluginDescriptor selection = model.getObjectAt(selectedRow);
                if (selection != null) {
                    descriptionPanel.update(selection, null, myAllPlugins, null);
                }
            }
        });

        TableUtil.ensureSelectionExists(pluginTable);
        return Couple.<JComponent>of(ScrollPaneFactory.createScrollPane(pluginTable, true), descriptionPanel.getPanel());
    }

    @Override
    public float getSplitterDefaultValue() {
        return 0.5f;
    }

    @Override
    public Size getDefaultSize() {
        return new Size(500, 800);
    }

    @Override
    protected void doOKAction() {
        Application application = Application.get();
        List<PluginDescriptor> loadedPluginDescriptors =
            application.getInstance(PluginAdvertiserRequester.class).getLoadedPluginDescriptors();
        List<PluginDescriptor> toDownload = myToInstallPlugins.stream().filter(it -> myDownloadState.get(it.getPluginId())).toList();
        myUserAccepted = InstallPluginAction
            .downloadAndInstallPlugins(myProject, toDownload, loadedPluginDescriptors, ideaPluginDescriptors -> {
                if (!ideaPluginDescriptors.isEmpty()) {
                    PluginManagerMain.notifyPluginsWereInstalled(ideaPluginDescriptors, null);
                }
            });
        super.doOKAction();
    }

    @Override
    protected Border createContentPaneBorder() {
        return JBUI.Borders.empty();
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanel = super.createSouthPanel();
        if (southPanel != null) {
            southPanel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));
            BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
            borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
            return borderLayoutPanel;
        }
        return null;
    }

    public boolean isUserInstalledPlugins() {
        return isOK() && myUserAccepted;
    }
}
