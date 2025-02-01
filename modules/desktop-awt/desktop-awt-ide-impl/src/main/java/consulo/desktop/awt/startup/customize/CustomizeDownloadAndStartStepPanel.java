/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.startup.customize;

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.container.plugin.PluginDescriptor;
import consulo.desktop.application.util.Restarter;
import consulo.externalService.impl.internal.update.PluginDownloader;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalFlowLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2014-11-29
 */
public class CustomizeDownloadAndStartStepPanel extends AbstractCustomizeWizardStep {
    private static final Logger LOG = Logger.getInstance(CustomizeDownloadAndStartStepPanel.class);

    private static class MyProgressIndicator extends EmptyProgressIndicator {
        private final JBLabel myLabel;
        private final JProgressBar myProgressBar;

        public MyProgressIndicator(JBLabel label, JProgressBar progressBar) {
            myLabel = label;
            myProgressBar = progressBar;
        }

        @Override
        public void setText2Value(final LocalizeValue text) {
            UIUtil.invokeLaterIfNeeded(() -> myLabel.setText(text.get()));
        }

        @Override
        public void setFraction(final double fraction) {
            UIUtil.invokeLaterIfNeeded(() -> myProgressBar.setValue((int)(fraction * 100d)));
        }

        @Override
        public void setIndeterminate(final boolean indeterminate) {
            UIUtil.invokeLaterIfNeeded(() -> myProgressBar.setIndeterminate(indeterminate));
        }
    }

    private final CustomizeIDEWizardDialog myCustomizeIDEWizardDialog;
    private final CustomizePluginsStepPanel myPluginsStepPanel;

    private boolean myDone;

    public CustomizeDownloadAndStartStepPanel(
        CustomizeIDEWizardDialog customizeIDEWizardDialog,
        @Nullable CustomizePluginsStepPanel pluginsStepPanel
    ) {
        myCustomizeIDEWizardDialog = customizeIDEWizardDialog;
        myPluginsStepPanel = pluginsStepPanel;
        setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE));
    }

    private JButton createStartButton() {
        JButton button = new JButton(getStartName());
        button.addActionListener(e -> {
            myCustomizeIDEWizardDialog.close(DialogWrapper.CLOSE_EXIT_CODE);
            Application.get().restart(true);
        });
        return button;
    }

    @Override
    @RequiredUIAccess
    public boolean beforeShown(boolean forward) {
        final Set<PluginDescriptor> pluginsForDownload =
            myPluginsStepPanel == null ? Collections.<PluginDescriptor>emptySet() : myPluginsStepPanel.getPluginsForDownload();
        if (pluginsForDownload.isEmpty()) {
            add(createStartButton());
        }
        else {
            JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, true, true));
            JBLabel infoLabel = new JBLabel("");
            panel.add(infoLabel);
            JProgressBar progressBar = new JProgressBar();
            panel.add(progressBar);
            add(panel);

            UIAccess uiAccess = UIAccess.current();

            final ProgressIndicator indicator = new MyProgressIndicator(infoLabel, progressBar);
            Application.get().executeOnPooledThread(() -> {
                for (PluginDescriptor pluginDescriptor : pluginsForDownload) {
                    try {
                        PluginDownloader downloader = PluginDownloader.createDownloader(pluginDescriptor, false);
                        downloader.download(indicator);
                        downloader.install(indicator, true);
                    }
                    catch (Exception e) {
                        LOG.warn(e);
                    }
                }

                myDone = true;
                uiAccess.give(this::placeStartButton);
            });
        }

        return true;
    }

    private void placeStartButton() {
        myCustomizeIDEWizardDialog.updateHeader();
        removeAll();
        add(createStartButton());
    }

    @Override
    @NonNls
    protected String getTitle() {
        return myPluginsStepPanel == null ? getStartName() : "Download plugins";
    }

    @Override
    @NonNls
    protected String getHTMLHeader() {
        Set<PluginDescriptor> pluginsForDownload =
            myPluginsStepPanel == null ? Collections.<PluginDescriptor>emptySet() : myPluginsStepPanel.getPluginsForDownload();
        return pluginsForDownload.isEmpty() || myDone ? "" : "<html><body><h2>Downloading plugins</h2></body></html>";
    }

    @Override
    protected String getHTMLFooter() {
        return null;
    }

    @Nonnull
    @NonNls
    private static String getStartName() {
        boolean supported = Restarter.isSupported();
        if (supported) {
            return "Start using " + Application.get().getName();
        }
        else {
            return "Manual restart " + Application.get().getName();
        }
    }
}
