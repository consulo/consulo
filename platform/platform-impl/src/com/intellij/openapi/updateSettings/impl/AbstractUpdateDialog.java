/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 04-Dec-2007
 */
package com.intellij.openapi.updateSettings.impl;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerUISettings;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TableUtil;
import com.intellij.util.ui.UIUtil;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Logger
public abstract class AbstractUpdateDialog extends DialogWrapper {
  private final boolean myEnableLink;
  protected final List<Couple<IdeaPluginDescriptor>> myUploadedPlugins;
  protected boolean myShowConfirmation = true;

  protected AbstractUpdateDialog(boolean canBeParent, boolean enableLink, final List<Couple<IdeaPluginDescriptor>> updatePlugins) {
    super(canBeParent);
    myEnableLink = enableLink;
    myUploadedPlugins = updatePlugins;
  }

  @Override
  protected void init() {
    setButtonsText();
    super.init();
  }

  protected void initPluginsPanel(final JPanel panel, JPanel pluginsPanel, JEditorPane updateLinkPane) {
    pluginsPanel.setVisible(myUploadedPlugins != null);
    if (myUploadedPlugins != null) {
      final DetectedPluginsPanel foundPluginsPanel = new DetectedPluginsPanel();

      foundPluginsPanel.addStateListener(new DetectedPluginsPanel.Listener() {
        @Override
        public void stateChanged() {
          setButtonsText();
        }
      });
      for (Couple<IdeaPluginDescriptor> uploadedPlugin : myUploadedPlugins) {
        foundPluginsPanel.add(uploadedPlugin);
      }
      TableUtil.ensureSelectionExists(foundPluginsPanel.getEntryTable());
      pluginsPanel.add(foundPluginsPanel, BorderLayout.CENTER);
    }
    updateLinkPane.setBackground(UIUtil.getPanelBackground());
    String css = UIUtil.getCssFontDeclaration(UIUtil.getLabelFont());
    if (UIUtil.isUnderDarcula()) {
      css += "<style>body {background: #" + ColorUtil.toHex(UIUtil.getPanelBackground()) + ";}</style>";
    }
    updateLinkPane.setBorder(IdeBorderFactory.createEmptyBorder(0));
    updateLinkPane.setText(IdeBundle.message("updates.configure.label", css));
    updateLinkPane.setEditable(false);
    LabelTextReplacingUtil.replaceText(panel);

    if (myEnableLink) {
      updateLinkPane.addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(final HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
            UpdateSettingsConfigurable updatesSettings = new UpdateSettingsConfigurable();
            updatesSettings.setCheckNowEnabled(false);
            util.editConfigurable(panel, updatesSettings);
          }
        }
      });
    }
  }

  private void setButtonsText() {
    setOKButtonText(getOkButtonText());
    setCancelButtonText(getCancelButtonText());
  }

  protected String getCancelButtonText() {
    return CommonBundle.getCancelButtonText();
  }

  protected String getOkButtonText() {
    return CommonBundle.getOkButtonText();
  }

  @Override
  protected void doOKAction() {
    if (doDownloadAndPrepare() && isShowConfirmation()) {
      final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      // do not stack several modal dialogs (native & swing)
      app.invokeLater(new Runnable() {
        @Override
        public void run() {
          app.restart(true);
        }
      });
    }
    super.doOKAction();
  }

  protected boolean doDownloadAndPrepare() {
    if (myUploadedPlugins != null) {
      UpdateChecker.saveDisabledToUpdatePlugins();

      final List<IdeaPluginDescriptor> pluginsForDownload = new ArrayList<IdeaPluginDescriptor>();
      Set<String> disabledToUpdatePlugins = UpdateChecker.getDisabledToUpdatePlugins();
      for (Couple<IdeaPluginDescriptor> uploadedPlugin : myUploadedPlugins) {
        IdeaPluginDescriptor second = uploadedPlugin.getSecond();
        if (disabledToUpdatePlugins.contains(second.getPluginId().getIdString())) {
          continue;
        }

        pluginsForDownload.add(uploadedPlugin.getSecond());
      }
      if(pluginsForDownload.isEmpty()) {
        return false;
      }

      new Task.Backgroundable(null, IdeBundle.message("progress.download.plugins"), true, PluginManagerUISettings.getInstance()) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          for (IdeaPluginDescriptor pluginDescriptor : pluginsForDownload) {
            try {
              PluginDownloader downloader = PluginDownloader.createDownloader(pluginDescriptor);
              if (downloader.prepareToInstall(indicator)) {
                downloader.install(true);
              }
            }
            catch (Exception e) {
              LOGGER.error(e);
            }
          }

          UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
              if (PluginManagerConfigurable.showRestartIDEADialog() == Messages.YES) {
                ApplicationManagerEx.getApplicationEx().restart(true);
              }
            }
          });
        }
      }.queue();
    }
    return false;
  }

  public void setShowConfirmation(boolean showConfirmation) {
    myShowConfirmation = showConfirmation;
  }

  public boolean isShowConfirmation() {
    return myShowConfirmation;
  }
}
