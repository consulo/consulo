/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.startup.customizeNew;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.startup.customize.CustomizeWizardContext;
import consulo.disposer.Disposable;
import consulo.ide.impl.plugins.PluginIconHolder;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.ProgressBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 09/10/2021
 */
public class CustomizeDownloadStep implements WizardStep<CustomizeWizardContext> {
  private final Runnable myNextAction;

  private JPanel myPluginsList;

  public CustomizeDownloadStep(Runnable nextAction) {
    myNextAction = nextAction;
  }

  @Override
  public boolean isVisible(@Nonnull CustomizeWizardContext context) {
    return !context.getPluginsForDownload().isEmpty();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent(CustomizeWizardContext context, @Nonnull Disposable uiDisposable) {
    throw new UnsupportedOperationException("unsupported ui");
  }

  @Override
  @RequiredUIAccess
  public void onStepEnter(@Nonnull CustomizeWizardContext context) {
    myPluginsList.removeAll();

    for (PluginId pluginId : context.getPluginsForDownload()) {
      PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
      if (plugin != null) {
        continue;
      }

      PluginDescriptor pluginDescriptor = context.getPluginDescriptors().get(pluginId);
      if (pluginDescriptor == null) {
        continue;
      }

      DockLayout pluginPanel = DockLayout.create();
      pluginPanel.left(ImageBox.create(PluginIconHolder.get(pluginDescriptor)));

      VerticalLayout infoPanel = VerticalLayout.create(10);
      infoPanel.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 5);

      infoPanel.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of(pluginDescriptor.getName()))));
      infoPanel.add(ProgressBar.create());

      pluginPanel.center(infoPanel);

      myPluginsList.add(TargetAWT.to(pluginPanel));
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public java.awt.Component getSwingComponent(@Nonnull CustomizeWizardContext context, @Nonnull Disposable uiDisposable) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel("<html><body><h2>Downloading plugins...</h2></body></html>"), BorderLayout.NORTH);

    myPluginsList = new JPanel(new VerticalFlowLayout(true, false));

    panel.add(ScrollPaneFactory.createScrollPane(myPluginsList));
    return panel;
  }
}