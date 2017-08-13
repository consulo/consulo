/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.customize;

import com.intellij.ide.customize.AbstractCustomizeWizardStep;
import com.intellij.ide.customize.CustomizeIDEWizardDialog;
import com.intellij.ide.customize.CustomizePluginsStepPanel;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.mock.MockProgressIndicator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Restarter;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 29.11.14
 */
public class CustomizeDownloadAndStartStepPanel extends AbstractCustomizeWizardStep {
  public static final Logger LOGGER = Logger.getInstance(CustomizeDownloadAndStartStepPanel.class);

  private static class MyProgressIndicator extends MockProgressIndicator {
    private final JBLabel myLabel;
    private final JProgressBar myProgressBar;

    public MyProgressIndicator(JBLabel label, JProgressBar progressBar) {
      myLabel = label;
      myProgressBar = progressBar;
    }

    @Override
    public void setText2(final String text) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          myLabel.setText(text);
        }
      });
    }

    @Override
    public void setFraction(final double fraction) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          myProgressBar.setValue((int)(fraction * 100d));
        }
      });
    }

    @Override
    public void setIndeterminate(final boolean indeterminate) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          myProgressBar.setIndeterminate(indeterminate);
        }
      });
    }
  }

  private final CustomizeIDEWizardDialog myCustomizeIDEWizardDialog;
  private final CustomizePluginsStepPanel myPluginsStepPanel;

  private boolean myDone;

  public CustomizeDownloadAndStartStepPanel(CustomizeIDEWizardDialog customizeIDEWizardDialog, @Nullable CustomizePluginsStepPanel pluginsStepPanel) {
    myCustomizeIDEWizardDialog = customizeIDEWizardDialog;
    myPluginsStepPanel = pluginsStepPanel;
    setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE));
  }

  private JButton createStartButton() {
    JButton button = new JButton(getStartName());
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCustomizeIDEWizardDialog.close(DialogWrapper.CLOSE_EXIT_CODE);
        ApplicationManagerEx.getApplicationEx().restart(true);
      }
    });
    return button;
  }

  @Override
  public boolean beforeShown(boolean forward) {
    final Set<IdeaPluginDescriptor> pluginsForDownload = myPluginsStepPanel == null ? Collections.<IdeaPluginDescriptor>emptySet() : myPluginsStepPanel.getPluginsForDownload();
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

      final ProgressIndicator indicator = new MyProgressIndicator(infoLabel, progressBar);
      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          for (IdeaPluginDescriptor ideaPluginDescriptor : pluginsForDownload) {
            try {
              PluginDownloader downloader = PluginDownloader.createDownloader(ideaPluginDescriptor, false);
              downloader.prepareToInstall(indicator);
              downloader.install(true);
            }
            catch (Exception e) {
              CustomizeDownloadAndStartStepPanel.LOGGER.warn(e);
            }
          }

          myDone = true;
          UIUtil.invokeLaterIfNeeded(() -> placeStartButton());
        }
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
  protected String getTitle() {
    return myPluginsStepPanel == null ? getStartName() : "Download plugins";
  }

  @Override
  protected String getHTMLHeader() {
    Set<IdeaPluginDescriptor> pluginsForDownload =
            myPluginsStepPanel == null ? Collections.<IdeaPluginDescriptor>emptySet() : myPluginsStepPanel.getPluginsForDownload();
    return pluginsForDownload.isEmpty() || myDone ? "" : "<html><body><h2>Downloading plugins</h2></body></html>";
  }

  @Override
  protected String getHTMLFooter() {
    return null;
  }

  @NotNull
  private static String getStartName() {
    boolean supported = Restarter.isSupported();
    if(supported) {
      return "Start using " + ApplicationNamesInfo.getInstance().getFullProductName();
    }
    else {
      return "Manual start " + ApplicationNamesInfo.getInstance().getFullProductName();
    }
  }
}
