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
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
@Logger
public class PluginListDialog extends DialogWrapper {
  private static class OurPluginColumnInfo extends PluginManagerColumnInfo {
    public OurPluginColumnInfo(PluginTableModel model) {
      super(PluginManagerColumnInfo.COLUMN_NAME, model);
    }

    @Override
    public TableCellRenderer getRenderer(final IdeaPluginDescriptor pluginDescriptor) {
      return new PluginsTableRenderer(pluginDescriptor, true) {
        @Override
        protected void updatePresentation(boolean isSelected, PluginNode pluginNode) {
          FileStatus status = FileStatus.MODIFIED;
          IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginNode.getPluginId());
          if (plugin == null && !PlatformOrPluginUpdateChecker.isPlatform(pluginNode.getPluginId())) {
            status = FileStatus.ADDED;
          }

          if (!isSelected) myName.setForeground(status.getColor());
        }
      };
    }

    @Override
    public Comparator<IdeaPluginDescriptor> getComparator() {
      return null;
    }
  }

  private static class OurPluginModel extends PluginTableModel {
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

  private JComponent myRoot;
  private List<IdeaPluginDescriptor> myNodes;
  @Nullable
  private Project myProject;
  @Nullable
  private String myPlatformVersion;

  public PluginListDialog(@Nullable Project project, PlatformOrPluginUpdateResult updateResult) {
    super(project);
    myProject = project;
    setTitle(IdeBundle.message("update.available.group"));

    myNodes = updateResult.getPlugins().stream().map(x -> x.getSecond()).collect(Collectors.toList());

    ContainerUtil.sort(myNodes, (o1, o2) -> o1.getName().compareTo(o2.getName()));

    Optional<IdeaPluginDescriptor> platform = myNodes.stream().filter(x -> PlatformOrPluginUpdateChecker.isPlatform(x.getPluginId())).findAny();
    platform.ifPresent(plugin -> {
      // move platform node to top
      myNodes.remove(plugin);
      myNodes.add(0, plugin);

      myPlatformVersion = plugin.getVersion();
    });

    OurPluginModel model = new OurPluginModel();
    model.updatePluginsList(myNodes);

    PluginTable pluginList = new PluginTable(model);

    myRoot = JBUI.Panels.simplePanel().addToCenter(ScrollPaneFactory.createScrollPane(pluginList, true));
    setResizable(false);
    init();
  }

  @Override
  protected void doOKAction() {
    Task.Backgroundable.queue(myProject, IdeBundle.message("progress.download.plugins"), true, PluginManagerUISettings.getInstance(), indicator -> {
      for (IdeaPluginDescriptor pluginDescriptor : myNodes) {
        try {
          PluginDownloader downloader = PluginDownloader.createDownloader(pluginDescriptor, myPlatformVersion);
          if (downloader.prepareToInstall(indicator)) {
            downloader.install(indicator, true);
          }
        }
        catch (Exception e) {
          LOGGER.error(e);
        }
      }

      UIUtil.invokeLaterIfNeeded(() -> {
        if (PluginManagerConfigurable.showRestartIDEADialog() == Messages.YES) {
          ApplicationManagerEx.getApplicationEx().restart(true);
        }
      });
    });

    super.doOKAction();
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
