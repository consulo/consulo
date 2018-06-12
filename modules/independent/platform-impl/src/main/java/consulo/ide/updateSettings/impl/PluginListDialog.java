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
import com.intellij.ide.plugins.*;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.ide.plugins.InstalledPluginsState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
public class PluginListDialog extends DialogWrapper {
  private static final Logger LOGGER = Logger.getInstance(PluginListDialog.class);

  private class OurPluginColumnInfo extends PluginManagerColumnInfo {
    public OurPluginColumnInfo(PluginTableModel model) {
      super(PluginManagerColumnInfo.COLUMN_NAME, model);
    }

    @Override
    public TableCellRenderer getRenderer(final IdeaPluginDescriptor pluginDescriptor) {
      return new PluginsTableRenderer(pluginDescriptor, true) {
        @Override
        protected void updatePresentation(boolean isSelected, @Nonnull IdeaPluginDescriptor pluginNode, TableModel model) {
          Couple<IdeaPluginDescriptor> couple = ContainerUtil.find(myNodes, it -> it.getSecond() == pluginDescriptor);
          assert couple != null;
          IdeaPluginDescriptor oldPlugin = couple.getFirst();
          if (oldPlugin != null) {
            myCategory.setText(oldPlugin.getVersion() + " " + UIUtil.rightArrow() + " " + (myRedStrategy.test(pluginDescriptor.getPluginId()) ? "??" : pluginNode.getVersion()));
          }
          else {
            myCategory.setText(pluginNode.getVersion());
          }

          FileStatus status = FileStatus.MODIFIED;
          if (myGreenStrategy.test(pluginNode.getPluginId())) {
            status = FileStatus.ADDED;
          }
          if (myRedStrategy.test(pluginNode.getPluginId())) {
            status = FileStatus.UNKNOWN;
          }

          myRating.setVisible(false);
          myDownloads.setVisible(false);

          if (!isSelected) myName.setForeground(status.getColor());
        }
      };
    }

    @Override
    public Comparator<IdeaPluginDescriptor> getComparator() {
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
    public void updatePluginsList(List<IdeaPluginDescriptor> list) {
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
    public boolean isPluginDescriptorAccepted(IdeaPluginDescriptor descriptor) {
      return true;
    }
  }

  @Nonnull
  private JComponent myRoot;
  @Nonnull
  private List<Couple<IdeaPluginDescriptor>> myNodes;
  @Nullable
  private Project myProject;
  @Nullable
  private Consumer<Collection<IdeaPluginDescriptor>> myAfterCallback;
  @Nullable
  private String myPlatformVersion;
  @Nonnull
  private Predicate<PluginId> myGreenStrategy;
  @Nonnull
  private Predicate<PluginId> myRedStrategy;
  @Nonnull
  private PlatformOrPluginUpdateResult.Type myType;

  public PluginListDialog(@Nullable Project project,
                          @Nonnull PlatformOrPluginUpdateResult updateResult,
                          @Nullable Predicate<PluginId> greenStrategy,
                          @Nullable Predicate<PluginId> redStrategy,
                          @Nullable Consumer<Collection<IdeaPluginDescriptor>> afterCallback) {
    super(project);
    myProject = project;
    myAfterCallback = afterCallback;
    myType = updateResult.getType();
    setTitle(updateResult.getType() == PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL ? IdeBundle.message("plugin.install.dialog.title") : IdeBundle.message("update.available.group"));

    myNodes = updateResult.getPlugins();

    if (redStrategy != null) {
      myRedStrategy = redStrategy;
    }
    else {
      myRedStrategy = pluginId -> false;
    }

    if (greenStrategy != null) {
      myGreenStrategy = greenStrategy;
    }
    else {
      myGreenStrategy = pluginId -> {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);
        boolean platform = PlatformOrPluginUpdateChecker.isPlatform(pluginId);
        return plugin == null && !platform;
      };
    }

    List<IdeaPluginDescriptor> list = updateResult.getPlugins().stream().map(x -> x.getSecond()).collect(Collectors.toList());

    ContainerUtil.sort(list, (o1, o2) -> o1.getName().compareTo(o2.getName()));

    ContainerUtil.weightSort(list, pluginDescriptor -> {
      if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
        return 200;
      }

      if (myRedStrategy.test(pluginDescriptor.getPluginId())) {
        return 100;
      }

      return 0;
    });

    Optional<IdeaPluginDescriptor> platform = list.stream().filter(x -> PlatformOrPluginUpdateChecker.isPlatform(x.getPluginId())).findAny();
    platform.ifPresent(plugin -> myPlatformVersion = plugin.getVersion());

    OurPluginModel model = new OurPluginModel();
    model.updatePluginsList(list);

    PluginTable pluginList = new PluginTable(model);

    myRoot = JBUI.Panels.simplePanel().addToCenter(ScrollPaneFactory.createScrollPane(pluginList, true));
    setResizable(false);
    init();
  }

  @Override
  public void doOKAction() {
    super.doOKAction();

    List<Couple<IdeaPluginDescriptor>> brokenPlugins = myNodes.stream().filter(c -> myRedStrategy.test(c.getFirst().getPluginId())).collect(Collectors.toList());
    if (!brokenPlugins.isEmpty()) {

      if (Messages.showOkCancelDialog(myProject, "Few plugins will be not updated. Those plugins will be disabled after update. Are you sure?", "Consulo", Messages.getErrorIcon()) != Messages.OK) {
        return;
      }
    }

    Task.Backgroundable.queue(myProject, IdeBundle.message("progress.download.plugins"), true, PluginManagerUISettings.getInstance(), indicator -> {
      List<IdeaPluginDescriptor> installed = new ArrayList<>(myNodes.size());

      for (Couple<IdeaPluginDescriptor> couple : myNodes) {
        IdeaPluginDescriptor pluginDescriptor = couple.getSecond();
        if (myRedStrategy.test(pluginDescriptor.getPluginId())) {
          // update list contains broken plugins
          continue;
        }

        try {
          PluginDownloader downloader = PluginDownloader.createDownloader(pluginDescriptor, myPlatformVersion, myType != PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL);
          if (downloader.prepareToInstall(indicator)) {
            InstalledPluginsState.getInstance().getUpdatedPlugins().add(pluginDescriptor.getPluginId());

            downloader.install(indicator, true);

            if (pluginDescriptor instanceof PluginNode) {
              ((PluginNode)pluginDescriptor).setStatus(PluginNode.STATUS_DOWNLOADED);
            }

            installed.add(pluginDescriptor);
          }
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          LOGGER.error(e);
        }
      }

      if (myAfterCallback != null) {
        myAfterCallback.accept(installed);
      }

    }, () -> {
      if (myType != PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL) {
        if (PluginManagerConfigurable.showRestartIDEADialog() == Messages.YES) {
          ApplicationManagerEx.getApplicationEx().restart(true);
        }
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
