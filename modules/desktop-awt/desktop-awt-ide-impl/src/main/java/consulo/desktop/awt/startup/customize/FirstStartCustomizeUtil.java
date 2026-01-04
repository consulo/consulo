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
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.update.UpdateSettings;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StyleManager;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author VISTALL
 * @since 2014-11-27
 */
public class FirstStartCustomizeUtil {
    private static final Logger LOG = Logger.getInstance(FirstStartCustomizeUtil.class);

    private static final String TEMPLATE_PLUGIN = "template.plugin";

    private static final int IMAGE_SIZE = 100;

    @RequiredUIAccess
    public static void showDialog(Application application) {
        UIAccess uiAccess = UIAccess.current();

        DialogWrapper downloadDialog = new DialogWrapper(false) {
            {
                setResizable(false);
                pack();
                init();
            }

            @RequiredUIAccess
            @Nullable
            @Override
            protected JComponent createSouthPanel() {
                return null;
            }

            @Override
            protected JComponent createCenterPanel() {
                return new JLabel("Connecting to plugin repository");
            }
        };

        application.executeOnPooledThread(() -> {
            MultiMap<String, PluginDescriptor> pluginDescriptors = new MultiMap<>();
            Map<PluginId, PluginTemplate> predefinedTemplateSets = new LinkedHashMap<>();
            try {
                List<PluginDescriptor> ideaPluginDescriptors = RepositoryHelper.loadOnlyPluginsFromRepository(null,
                    UpdateSettings.getInstance().getChannel(),
                    EarlyAccessProgramManager.getInstance()
                );

                for (PluginDescriptor pluginDescriptor : ideaPluginDescriptors) {
                    Set<String> tags = pluginDescriptor.getTags();
                    if (tags.contains(TEMPLATE_PLUGIN)) {
                        PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();

                        Set<PluginId> targetPlugins = Set.of(dependentPluginIds);

                        Image[] images = new Image[]{
                            ImageEffects.resize(PluginIconHolder.initializeImage(pluginDescriptor, false), IMAGE_SIZE),
                            ImageEffects.resize(PluginIconHolder.initializeImage(pluginDescriptor, true), IMAGE_SIZE)
                        };

                        PluginTemplate template = new PluginTemplate(
                            pluginDescriptor.getPluginId(),
                            targetPlugins,
                            pluginDescriptor.getName(),
                            pluginDescriptor.getDescription(),
                            images,
                            pluginDescriptor.getDownloads()
                        );

                        predefinedTemplateSets.put(pluginDescriptor.getPluginId(), template);
                    }

                    if (tags.isEmpty()) {
                        pluginDescriptors.putValue("unknown", pluginDescriptor);
                    }
                    else {
                        for (String tag : tags) {
                            pluginDescriptors.putValue(tag, pluginDescriptor);
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.warn(e);
            }

            uiAccess.give(() -> {
                downloadDialog.close(DialogWrapper.OK_EXIT_CODE);

                boolean dark = StyleManager.get().getCurrentStyle().isDark();

                new CustomizeIDEWizardDialog(dark, pluginDescriptors, predefinedTemplateSets).showAsync();
            });
        });
        downloadDialog.showAsync();
    }
}
