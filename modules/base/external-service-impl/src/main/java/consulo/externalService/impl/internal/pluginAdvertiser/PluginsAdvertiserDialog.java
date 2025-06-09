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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionPanel;
import consulo.externalService.impl.internal.plugin.ui.PluginsList;
import consulo.project.Project;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.WholeWestDialogWrapper;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.List;

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

    public PluginsAdvertiserDialog(@Nullable Project project, @Nonnull List<PluginDescriptor> allPlugins, @Nonnull List<PluginDescriptor> toInstallPlugins) {
        super(project);
        myAllPlugins = allPlugins;

        myProject = project;
        myToInstallPlugins = toInstallPlugins;
        setTitle("Choose Plugins to Install");
        init();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
        PluginsList pluginsList = new PluginsList(null);
        pluginsList.modifyPluginsList(myToInstallPlugins);

        PluginDescriptionPanel descriptionPanel = new PluginDescriptionPanel(null);

        pluginsList.getComponent().addListSelectionListener(e -> {
            PluginDescriptor value = pluginsList.getComponent().getSelectedValue();
            if (value != null) {
                descriptionPanel.update(value, null, myAllPlugins, null, true);
            }
        });

        UiNotifyConnector.doWhenFirstShown(pluginsList.getComponent(), () -> {
            ScrollingUtil.ensureSelectionExists(pluginsList.getComponent());
        });

        return Couple.<JComponent>of(ScrollPaneFactory.createScrollPane(pluginsList.getComponent(), true), descriptionPanel.getPanel());
    }

    @Override
    public float getSplitterDefaultValue() {
        return 0.5f;
    }

    @Override
    public Size2D getDefaultSize() {
        return new Size2D(600, 900);
    }

    @Override
    protected void doOKAction() {
    }

    @Override
    protected Border createContentPaneBorder() {
        return JBUI.Borders.empty();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        // no actions buttons
        return null;
    }
}
