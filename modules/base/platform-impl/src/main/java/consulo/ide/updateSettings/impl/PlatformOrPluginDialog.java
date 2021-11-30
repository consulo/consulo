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
package consulo.ide.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.SettingsEntryPointAction;
import com.intellij.ide.plugins.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.plugins.PluginActionListener;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
public class PlatformOrPluginDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(PlatformOrPluginDialog.class);

  private class OurPluginColumnInfo extends PluginManagerColumnInfo {
    public OurPluginColumnInfo(PluginTableModel model) {
      super(PluginManagerColumnInfo.COLUMN_NAME, model);
    }

    @Override
    public TableCellRenderer getRenderer(final PluginDescriptor pluginDescriptor) {
      return new PluginsTableRenderer(pluginDescriptor, true) {
        @Override
        protected void updatePresentation(boolean isSelected, @Nonnull PluginDescriptor pluginNode, TableModel model) {
          PlatformOrPluginNode node = ContainerUtil.find(myNodes, it -> it.getPluginId().equals(pluginDescriptor.getPluginId()));
          assert node != null;

          PluginDescriptor currentDescriptor = node.getCurrentDescriptor();
          if (currentDescriptor != null) {
            myCategory.setText(currentDescriptor.getVersion() + " " + UIUtil.rightArrow() + " " + (node.getFutureDescriptor() == null ? "??" : pluginNode.getVersion()));
          }
          else {
            myCategory.setText(pluginNode.getVersion());
          }

          FileStatus status = FileStatus.MODIFIED;
          if (myGreenStrategy.test(pluginNode.getPluginId())) {
            status = FileStatus.ADDED;
          }
          if (node.getFutureDescriptor() == null) {
            status = FileStatus.UNKNOWN;
          }

          myRating.setVisible(false);
          myDownloads.setVisible(false);

          if (!isSelected) myName.setForeground(TargetAWT.to(status.getColor()));
        }
      };
    }

    @Override
    public Comparator<PluginDescriptor> getComparator() {
      return null;
    }
  }

  private class OurPluginModel extends PluginTableModel {
    private OurPluginModel() {
      setSortKey(new RowSorter.SortKey(getNameColumn(), SortOrder.ASCENDING));
      super.columns = new ColumnInfo[]{new OurPluginColumnInfo(this)};
      view = new ArrayList<>();
    }

    @Override
    public void updatePluginsList(List<PluginDescriptor> list) {
      view.clear();
      filtered.clear();
      view.addAll(list);
      fireTableDataChanged();
    }

    @Override
    public int getNameColumn() {
      return 0;
    }

    @Override
    public boolean isPluginDescriptorAccepted(PluginDescriptor descriptor) {
      return true;
    }
  }

  @Nonnull
  private JComponent myRoot;
  @Nonnull
  private List<PlatformOrPluginNode> myNodes;
  @Nullable
  private Project myProject;
  @Nullable
  private Consumer<Collection<PluginDescriptor>> myAfterCallback;
  @Nullable
  private String myPlatformVersion;
  @Nonnull
  private Predicate<PluginId> myGreenStrategy;
  @Nonnull
  private PlatformOrPluginUpdateResult.Type myType;

  public PlatformOrPluginDialog(@Nullable Project project,
                                @Nonnull PlatformOrPluginUpdateResult updateResult,
                                @Nullable Predicate<PluginId> greenStrategy,
                                @Nullable Consumer<Collection<PluginDescriptor>> afterCallback) {
    super(project);
    myProject = project;
    myAfterCallback = afterCallback;
    myType = updateResult.getType();
    setTitle(updateResult.getType() == PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL ? IdeBundle.message("plugin.install.dialog.title") : IdeBundle.message("update.available.group"));

    myNodes = updateResult.getPlugins();

    if (greenStrategy != null) {
      myGreenStrategy = greenStrategy;
    }
    else {
      myGreenStrategy = pluginId -> {
        PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
        boolean platform = PlatformOrPluginUpdateChecker.isPlatform(pluginId);
        return plugin == null && !platform;
      };
    }

    Set<PluginId> brokenPlugins = new HashSet<>();
    List<PluginDescriptor> toShowPluginList = new ArrayList<>();
    for (PlatformOrPluginNode node : myNodes) {
      PluginDescriptor futureDescriptor = node.getFutureDescriptor();
      if (futureDescriptor != null) {
        toShowPluginList.add(futureDescriptor);
      }
      else {
        brokenPlugins.add(node.getPluginId());

        toShowPluginList.add(node.getCurrentDescriptor());
      }

      if (PlatformOrPluginUpdateChecker.isPlatform(node.getPluginId())) {
        assert futureDescriptor != null;

        myPlatformVersion = futureDescriptor.getVersion();
      }
    }

    ContainerUtil.sort(toShowPluginList, (o1, o2) -> o1.getName().compareTo(o2.getName()));

    ContainerUtil.weightSort(toShowPluginList, pluginDescriptor -> {
      if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
        return 100;
      }

      if (brokenPlugins.contains(pluginDescriptor.getPluginId())) {
        return 200;
      }

      return 0;
    });

    OurPluginModel model = new OurPluginModel();
    model.updatePluginsList(toShowPluginList);

    PluginTable pluginList = new PluginTable(model);

    myRoot = JBUI.Panels.simplePanel().addToCenter(ScrollPaneFactory.createScrollPane(pluginList, true));
    setResizable(false);
    init();
  }

  @Override
  @RequiredUIAccess
  public void doOKAction() {
    super.doOKAction();

    PlatformOrPluginNode brokenPlugin = myNodes.stream().filter(c -> c.getFutureDescriptor() == null).findFirst().orElse(null);
    if (brokenPlugin != null) {
      if (Messages.showOkCancelDialog(myProject, "Few plugins will be not updated. Those plugins will be disabled after update. Are you sure?", "Consulo", Messages.getErrorIcon()) != Messages.OK) {
        return;
      }
    }

    UIAccess uiAccess = UIAccess.current();

    Task.Backgroundable.queue(myProject, IdeBundle.message("progress.download.plugins"), true, PluginManagerUISettings.getInstance(), indicator -> {
      List<PluginDescriptor> installed = new ArrayList<>(myNodes.size());

      List<PluginDownloader> forInstall = new ArrayList<>(myNodes.size());
      for (PlatformOrPluginNode platformOrPluginNode : myNodes) {
        PluginDescriptor pluginDescriptor = platformOrPluginNode.getFutureDescriptor();
        // update list contains broken plugins
        if (pluginDescriptor == null) {
          continue;
        }

        try {
          PluginDownloader downloader = PluginDownloader.createDownloader(pluginDescriptor, myPlatformVersion, myType != PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL);

          forInstall.add(downloader);

          downloader.download(indicator);
        }
        catch (PluginDownloadFailedException e) {
          uiAccess.give(() -> Alerts.okError(e.getLocalizeMessage()).showAsync());
          return;
        }
      }

      indicator.setTextValue(IdeLocalize.progressInstallingPlugins());

      Application application = Application.get();
      UpdateHistory updateHistory = application.getInstance(UpdateHistory.class);

      InstalledPluginsState installedPluginsState = InstalledPluginsState.getInstance();
      for (PluginDownloader downloader : forInstall) {
        try {
          // already was installed
          if (installedPluginsState.wasUpdated(downloader.getPluginId())) {
            continue;
          }

          installedPluginsState.getUpdatedPlugins().add(downloader.getPluginId());

          downloader.install(indicator, true);

          PluginDescriptor pluginDescriptor = downloader.getPluginDescriptor();

          if (pluginDescriptor instanceof PluginNode) {
            ((PluginNode)pluginDescriptor).setStatus(PluginNode.STATUS_DOWNLOADED);

            application.getMessageBus().syncPublisher(PluginActionListener.TOPIC).pluginInstalled(pluginDescriptor.getPluginId());

            if (myType == PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL && pluginDescriptor.isExperimental()) {
              updateHistory.setShowExperimentalWarning(true);
            }
          }

          installed.add(pluginDescriptor);
        }
        catch (IOException e) {
          uiAccess.give(() -> Alerts.okError(LocalizeValue.of(e.getLocalizedMessage())).showAsync());
          return;
        }
      }

      Map<String, String> pluginHistory = new HashMap<>();
      for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
        if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
          continue;
        }

        pluginHistory.put(descriptor.getPluginId().getIdString(), StringUtil.notNullize(descriptor.getVersion()));
      }

      pluginHistory.put(PlatformOrPluginUpdateChecker.getPlatformPluginId().getIdString(), ApplicationInfo.getInstance().getBuild().toString());

      updateHistory.replaceHistory(pluginHistory);

      if (myAfterCallback != null) {
        myAfterCallback.accept(installed);
      }

      if (myType != PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL) {
        SwingUtilities.invokeLater(() -> {
          UpdateSettings updateSettings = UpdateSettings.getInstance();
          updateSettings.setLastCheckResult(PlatformOrPluginUpdateResult.Type.RESTART_REQUIRED);

          SettingsEntryPointAction.updateState(updateSettings);

          if (PluginInstallUtil.showRestartIDEADialog() == Messages.YES) {
            Application.get().restart(true);
          }
        });
      }
    });
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    setScalableSize(600, 300);
    return getClass().getSimpleName();
  }

  @Nullable
  @Override
  protected Border createContentPaneBorder() {
    return null;
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    JComponent southPanel = super.createSouthPanel();
    if (southPanel != null) {
      southPanel.add(new JBLabel("Following nodes will be downloaded & installed"), BorderLayout.WEST);
      southPanel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));

      BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
      borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
      return borderLayoutPanel;
    }
    return null;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myRoot;
  }
}
