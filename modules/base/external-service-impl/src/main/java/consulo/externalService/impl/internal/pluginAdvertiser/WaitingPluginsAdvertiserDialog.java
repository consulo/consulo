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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionPanel;
import consulo.externalService.impl.internal.plugin.ui.PluginsList;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.project.Project;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.WholeWestDialogWrapper;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.util.ShowNotifier;
import consulo.util.lang.Couple;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @see PreloadedPluginsAdvertiserDialog
 * @since 2026-05-02
 */
public class WaitingPluginsAdvertiserDialog extends WholeWestDialogWrapper {
    private final @Nullable Project myProject;
    private final ExtensionPreview myExtensionPreview;
    private final PluginAdvertiserHelper myPluginAdvertiserHelper;

    public WaitingPluginsAdvertiserDialog(
        @Nullable Project project,
        @NonNull ExtensionPreview extensionPreview,
        PluginAdvertiserHelper pluginAdvertiserHelper) {
        super(project);
        myProject = project;
        myExtensionPreview = extensionPreview;
        myPluginAdvertiserHelper = pluginAdvertiserHelper;
        setTitle("Choose Plugins to Install");
        init();
    }

    @Override
    @RequiredUIAccess
    public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
        PluginDescriptionPanel descriptionPanel = new PluginDescriptionPanel(null);

        LoadingLayout<DockLayout> layout = LoadingLayout.create(DockLayout.create(), getDisposable());

        ShowNotifier.once(layout, () -> {
            layout.startLoading(() -> {
                try {
                    return myPluginAdvertiserHelper.findPluginsForSuggest(myExtensionPreview).get();
                }
                catch (InterruptedException | ExecutionException e) {
                    return new PluginAdvertiserHelper.PluginsInfo(List.of(), Set.of());
                }
            }, (dockLayout, value) -> {
                PluginsList pluginsList = new PluginsList(null);
                pluginsList.modifyPluginsList(new ArrayList<>(value.featurePlugins()));

                pluginsList.getComponent().addListSelectionListener(e -> {
                    PluginDescriptor selectValue = pluginsList.getComponent().getSelectedValue();
                    if (selectValue != null) {
                        descriptionPanel.update(selectValue, null, value.allPlugins(), null, true);
                    }
                });

                ScrollingUtil.ensureSelectionExists(pluginsList.getComponent());
                
                dockLayout.center(TargetAWT.wrap(ScrollPaneFactory.createScrollPane(pluginsList.getComponent(), true)));
            });
        });

        return Couple.of((JComponent) TargetAWT.to(layout), descriptionPanel.getPanel());
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

    @Override
    protected @Nullable String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Override
    @RequiredUIAccess
    protected @Nullable JComponent createSouthPanel() {
        // no actions buttons
        return null;
    }
}
