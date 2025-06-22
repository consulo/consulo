/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.application.Application;
import consulo.application.internal.plugin.PluginsLoader;
import consulo.application.util.TempFileService;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.StatusText;
import consulo.util.io.FileUtil;
import consulo.util.io.zip.ZipUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author anna
 */
public class InstalledPluginsTab extends PluginTab {
    @RequiredUIAccess
    public InstalledPluginsTab(PluginsPanel pluginsPanel) {
        super(pluginsPanel);
        init();

        final StatusText emptyText = myPluginList.getEmptyText();
        emptyText.setText("Nothing to show.");
        emptyText.appendText(" Click ");
        emptyText.appendText("View available plugins...", SimpleTextAttributes.LINK_ATTRIBUTES, new BrowseRepoListener());
        emptyText.appendText(" to view available plugins.");

        reload();
    }

    @RequiredUIAccess
    @Override
    public void reload() {
        List<PluginDescriptor> pluginDescriptors = new ArrayList<>(PluginManager.getPlugins());
        pluginDescriptors.addAll(InstalledPluginsState.getInstance().getAllPlugins());

        for (Iterator<PluginDescriptor> iterator = pluginDescriptors.iterator(); iterator.hasNext(); ) {
            final PluginId pluginId = iterator.next().getPluginId();
            if (PluginIds.isPlatformPlugin(pluginId)) {
                iterator.remove();
            }
        }
        myPluginList.modifyPluginsList(pluginDescriptors);
    }

    @Override
    public List<PluginSorter> getSorters() {
        return List.of(PluginSorter.NAME, PluginSorter.STATUS);
    }

    @Nullable
    public static PluginDescriptor loadDescriptorFromArchive(final File file) throws IOException {
        PluginDescriptor descriptor = null;

        TempFileService tempFileService = Application.get().getInstance(TempFileService.class);
        final Path outputDirPath = tempFileService.createTempDirectory("plugin", "");
        File outputDir = outputDirPath.toFile();
        try {
            ZipUtil.extract(file, outputDir, null);
            final File[] files = outputDir.listFiles();
            if (files != null && files.length == 1) {
                descriptor = PluginsLoader.loadPluginDescriptor(files[0]);
            }
        }
        finally {
            FileUtil.delete(outputDir);
        }
        return descriptor;
    }

    @Override
    public boolean isModified() {
        return super.isModified();
    }

    @Override
    public String apply() {
        final String apply = super.apply();
        if (apply != null) {
            return apply;
        }

        return null;
    }

    @Override
    protected String canApply() {
        final Map<PluginId, Set<PluginId>> dependentToRequiredListMap = new HashMap<>(myPluginsPanel.getDependentToRequiredListMap());
        for (Iterator<PluginId> iterator = dependentToRequiredListMap.keySet().iterator(); iterator.hasNext(); ) {
            PluginId item = iterator.next();
            // ignore
            iterator.remove();
        }
        if (!dependentToRequiredListMap.isEmpty()) {
            return "<html><body style=\"padding: 5px;\">Unable to apply changes: plugin" +
                (dependentToRequiredListMap.size() == 1 ? " " : "s ") +
                StringUtil.join(dependentToRequiredListMap.keySet(), pluginId -> {
                    final PluginDescriptor ideaPluginDescriptor = PluginManager.findPlugin(pluginId);
                    return "\"" + (ideaPluginDescriptor != null ? ideaPluginDescriptor.getName() : pluginId.getIdString()) + "\"";
                }, ", ") +
                " won't be able to load.</body></html>";
        }
        return super.canApply();
    }

    private class BrowseRepoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginsPanel pluginsPanel = DataManager.getInstance().getDataContext(getMainPanel()).getData(PluginsPanel.KEY);

            if (pluginsPanel != null) {
                pluginsPanel.select(getAvailable());
            }
        }
    }
}
